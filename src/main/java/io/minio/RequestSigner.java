/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio;

import com.google.common.io.BaseEncoding;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.joda.time.DateTime;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class RequestSigner implements Interceptor {
  //
  // Excerpts from @lsegal - https://github.com/aws/aws-sdk-js/issues/659#issuecomment-120477258
  //
  //  User-Agent:
  //
  //      This is ignored from signing because signing this causes problems with generating pre-signed URLs
  //      (that are executed by other agents) or when customers pass requests through proxies, which may
  //      modify the user-agent.
  //
  //  Content-Length:
  //
  //      This is ignored from signing because generating a pre-signed URL should not provide a content-length
  //      constraint, specifically when vending a S3 pre-signed PUT URL. The corollary to this is that when
  //      sending regular requests (non-pre-signed), the signature contains a checksum of the body, which
  //      implicitly validates the payload length (since changing the number of bytes would change the checksum)
  //      and therefore this header is not valuable in the signature.
  //
  //  Content-Type:
  //
  //      Signing this header causes quite a number of problems in browser environments, where browsers
  //      like to modify and normalize the content-type header in different ways. There is more information
  //      on this in https://github.com/aws/aws-sdk-js/issues/244. Avoiding this field simplifies logic
  //      and reduces the possibility of future bugs
  //
  //  Authorization:
  //
  //      Is skipped for obvious reasons
  //

  // TODO: make set as immutable
  private static final Set<String> ignoredHeaders = new HashSet<String>();

  static {
    ignoredHeaders.add("authorization");
    ignoredHeaders.add("content-type");
    ignoredHeaders.add("content-length");
    ignoredHeaders.add("user-agent");
  }

  private byte[] data;
  private DateTime date;
  private String accessKey;
  private String secretKey;
  private String region;


  public RequestSigner(byte[] data, String accessKey, String secretKey, String region) {
    this(data, accessKey, secretKey, region, new DateTime());
  }


  public RequestSigner(byte[] data, String accessKey, String secretKey, String region, DateTime date) {
    if (data == null) {
      this.data = new byte[0];
    } else {
      this.data = data;
    }
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
    this.date = date;
  }


  private byte[] getSigningKey(String region)
    throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
    String formattedDate = this.date.toString(DateFormat.SIGNER_DATE_FORMAT);
    String dateKeyLine = "AWS4" + secretKey;
    byte[] dateKey = sumHmac(dateKeyLine.getBytes("UTF-8"), formattedDate.getBytes("UTF-8"));
    byte[] dateRegionKey = sumHmac(dateKey, region.getBytes("UTF-8"));
    byte[] dateRegionServiceKey = sumHmac(dateRegionKey, "s3".getBytes("UTF-8"));
    return sumHmac(dateRegionServiceKey, "aws4_request".getBytes("UTF-8"));
  }

  private byte[] sumHmac(byte[] curKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
    SecretKeySpec key = new SecretKeySpec(curKey, "HmacSHA256");
    Mac hmacSha256 = Mac.getInstance("HmacSHA256");
    hmacSha256.init(key);
    hmacSha256.update(data);
    return hmacSha256.doFinal();
  }

  private Request signV4(Request originalRequest)
    throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException, InvalidKeyException {
    if (this.accessKey == null || this.secretKey == null) {
      return originalRequest.newBuilder()
          .header("x-amz-date", this.date.toString(DateFormat.AMZ_DATE_FORMAT))
          .build();
    }

    byte[] dataHashBytes = computeSha256(data);
    String dataHash = BaseEncoding.base16().encode(dataHashBytes).toLowerCase();

    Request signedRequest = originalRequest.newBuilder()
        .header("x-amz-content-sha256", dataHash)
        .header("Host", originalRequest.httpUrl().host())
        .header("x-amz-date", this.date.toString(DateFormat.AMZ_DATE_FORMAT))
        .build();


    // get signed headers
    String signedHeaders = getSignedHeaders(signedRequest);

    // get canonical request and headers to sign
    String canonicalRequest = getCanonicalRequest(signedRequest, dataHash, signedHeaders);

    // get sha256 of canonical request
    byte[] canonicalHashBytes = computeSha256(canonicalRequest.getBytes("UTF-8"));
    String canonicalHash = BaseEncoding.base16().encode(canonicalHashBytes).toLowerCase();

    // get key to sign
    String stringToSign = getStringToSign(region, canonicalHash);
    byte[] signingKey = getSigningKey(region);

    // get signing key
    String signature = BaseEncoding.base16().encode(getSignature(signingKey, stringToSign)).toLowerCase();

    // get authorization header
    String authorization = getAuthorizationHeader(signedHeaders, signature, region);

    signedRequest = signedRequest.newBuilder()
        .header("Authorization", authorization)
        .build();

    return signedRequest;
  }

  private byte[] computeSha256(byte[] data) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return digest.digest(data);
  }

  private String getAuthorizationHeader(String signedHeaders, String signature, String region) {
    return "AWS4-HMAC-SHA256 Credential=" + this.accessKey + "/" + getScope(region) + ", SignedHeaders="
        + signedHeaders + ", Signature=" + signature;
  }

  private byte[] getSignature(byte[] signingKey, String stringToSign) throws UnsupportedEncodingException,
                                                                             NoSuchAlgorithmException,
                                                                             InvalidKeyException {
    return sumHmac(signingKey, stringToSign.getBytes("UTF-8"));
  }

  public String getScope(String region) {
    String formattedDate = date.toString(DateFormat.SIGNER_DATE_FORMAT);
    return formattedDate
        + "/"
        + region
        + "/"
        + "s3"
        + "/"
        + "aws4_request";
  }

  private String getStringToSign(String region, String canonicalHash) {
    return "AWS4-HMAC-SHA256"
        + "\n"
        + date.toString(DateFormat.AMZ_DATE_FORMAT)
        + "\n"
        + getScope(region)
        + "\n"
        + canonicalHash;
  }

  private String getCanonicalRequest(Request request, String bodySha256Hash,
                                     String signedHeaders) throws IOException, InvalidKeyException {
    StringWriter canonicalWriter = new StringWriter();
    PrintWriter canonicalPrinter = new PrintWriter(canonicalWriter, true);
    String method = request.method();
    String path = request.uri().getRawPath();
    String rawQuery = request.uri().getQuery();
    if (rawQuery == null || rawQuery.isEmpty()) {
      rawQuery = "";
    }
    String query = getCanonicalQuery(rawQuery);

    canonicalPrinter.print(method + "\n");
    canonicalPrinter.print(path + "\n");
    canonicalPrinter.print(query + "\n");
    Map<String, String> headers = getCanonicalHeaders(request); // new line already added
    for (Map.Entry<String, String> e : headers.entrySet()) {
      canonicalPrinter.write(e.getKey()
                             + ":"
                             + e.getValue()
                             + '\n');
    }
    canonicalPrinter.print("\n");
    canonicalPrinter.print(signedHeaders + "\n");
    canonicalPrinter.print(bodySha256Hash);
    canonicalPrinter.flush();
    return canonicalWriter.toString();
  }

  private String getCanonicalQuery(String rawQuery) {
    StringBuilder queryBuilder = new StringBuilder();
    if (!rawQuery.equals("")) {
      String[] querySplit = rawQuery.split("&");
      for (int i = 0; i < querySplit.length; i++) {
        if (querySplit[i].contains("=")) {
          String[] split = querySplit[i].split("=", 2);
          try {
            split[1] = URLEncoder.encode(split[1], "UTF-8");
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
          querySplit[i] = split[0]
              + "="
              + split[1];
        }
        querySplit[i] = querySplit[i].trim();
      }
      Arrays.sort(querySplit);
      for (String s : querySplit) {
        if (queryBuilder.length() != 0) {
          queryBuilder.append("&");
        }
        queryBuilder.append(s);
        if (!s.contains("=")) {
          queryBuilder.append('=');
        }
      }
    }
    return queryBuilder.toString();
  }

  private String getSignedHeaders(Request request) {
    StringBuilder builder = new StringBuilder();
    boolean printSeparator = false;
    for (String header : request.headers().names()) {
      if (!ignoredHeaders.contains(header.toLowerCase().trim())) {
        if (printSeparator) {
          builder.append(';');
        } else {
          printSeparator = true;
        }
        builder.append(header.toLowerCase().trim());
      }
    }
    return builder.toString();
  }

  private Map<String, String> getCanonicalHeaders(Request request) throws IOException {
    Map<String, String> map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    for (String s : request.headers().names()) {
      String val = request.headers().get(s);
      if (val != null) {
        String headerKey = s.toLowerCase().trim();
        String headerValue = val.trim();
        if (!ignoredHeaders.contains(headerKey)) {
          map.put(headerKey, headerValue);
        }
      }
    }
    return map;
  }

  private String getPresignCanonicalRequest(Request request, String requestQuery, String expires)
    throws IOException, InvalidKeyException {
    StringWriter canonicalWriter = new StringWriter();
    PrintWriter canonicalPrinter = new PrintWriter(canonicalWriter, true);

    String method = request.method();
    String path = request.uri().getRawPath();

    canonicalPrinter.print(method + "\n");
    canonicalPrinter.print(path + "\n");
    canonicalPrinter.print(requestQuery + "\n");
    Map<String, String> headers = getCanonicalHeaders(request);
    for (Map.Entry<String, String> e : headers.entrySet()) {
      canonicalPrinter.write(e.getKey()
                             + ":"
                             + e.getValue()
                             + '\n');
    }
    canonicalPrinter.print("\n");
    canonicalPrinter.print(getSignedHeaders(request) + "\n");
    canonicalPrinter.print("UNSIGNED-PAYLOAD");
    canonicalPrinter.flush();
    return canonicalWriter.toString();
  }

  public String preSignV4(Request originalRequest, Integer expiresInt) throws IOException, NoSuchAlgorithmException,
                                                                              InvalidKeyException {
    String host = originalRequest.uri().getHost();
    String path = originalRequest.uri().getRawPath();
    String expires = Integer.toString(expiresInt);
    String requestQuery = "";

    Request signedRequest = originalRequest.newBuilder()
        .header("Host", host)
        .build();

    // remove x-amz-date proactively
    ignoredHeaders.add("x-amz-date");

    requestQuery = "X-Amz-Algorithm=AWS4-HMAC-SHA256&";
    requestQuery += "X-Amz-Credential="
        + this.accessKey
        + URLEncoder.encode("/"
                            + getScope(region),
                            "UTF-8")
        + "&";
    requestQuery += "X-Amz-Date="
        + date.toString(DateFormat.AMZ_DATE_FORMAT)
        + "&";
    requestQuery += "X-Amz-Expires="
        + expires
        + "&";
    requestQuery += "X-Amz-SignedHeaders="
        + getSignedHeaders(signedRequest);

    String canonicalRequest = getPresignCanonicalRequest(signedRequest,
                                                         requestQuery,
                                                         expires);
    byte[] canonicalRequestHashBytes = computeSha256(canonicalRequest.getBytes("UTF-8"));
    String canonicalRequestHash = BaseEncoding.base16().encode(canonicalRequestHashBytes).toLowerCase();
    String stringToSign = getStringToSign(region, canonicalRequestHash);
    byte[] signingKey = getSigningKey(region);
    String signature = BaseEncoding.base16().encode(getSignature(signingKey,
                                                                 stringToSign)).toLowerCase();
    String scheme = signedRequest.uri().getScheme();
    return scheme
        + "://"
        + host
        + path
        + "?"
        + requestQuery
        + "&X-Amz-Signature="
        + signature;
  }

  public String postPreSignV4(String stringToSign, String region)
    throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
    byte[] signingKey = getSigningKey(region);
    String signature = BaseEncoding.base16().encode(getSignature(signingKey, stringToSign)).toLowerCase();
    return signature;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    try {
      Request signedRequest = this.signV4(chain.request());
      Response response = chain.proceed(signedRequest);
      return response;
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return new Response.Builder().build();
    } catch (InvalidKeyException e) {
      e.printStackTrace();
      return new Response.Builder().build();
    } catch (IOException e) {
      e.printStackTrace();
      return new Response.Builder().build();
    }
  }
}
