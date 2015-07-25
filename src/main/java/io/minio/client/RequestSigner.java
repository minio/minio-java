/*
 * Minio Java Library for Amazon S3 compatible cloud storage, (C) 2015 Minio, Inc.
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

package io.minio.client;


import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.RequestBody;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class RequestSigner implements Interceptor {
    private static final DateTimeFormatter dateFormatyyyyMMddThhmmssZ = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'").withZoneUTC();
    private static final DateTimeFormatter dateFormatyyyyMMdd = DateTimeFormat.forPattern("yyyyMMdd").withZoneUTC();
    private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withZoneUTC();

    private byte[] data = new byte[0];
    private DateTime date = null;
    private String accessKey = null;
    private String secretKey = null;

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
    private Set<String> ignoredHeaders = new HashSet<String>();

    RequestSigner(byte[] data, String accessKey, String secretKey, DateTime date) {
        if (data == null) {
            data = new byte[0];
        }
        this.data = data;
        this.date = date;
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        ignoredHeaders.add("authorization");
        ignoredHeaders.add("content-type");
        ignoredHeaders.add("content-length");
        ignoredHeaders.add("user-agent");
    }


    private static byte[] generateSigningKey(DateTime date, String region, String secretKey) throws NoSuchAlgorithmException,
                                                                                                    InvalidKeyException, UnsupportedEncodingException {
        String formattedDate = date.toString(dateFormatyyyyMMdd);
        String dateKeyLine = "AWS4" + secretKey;
        byte[] dateKey = sumHmac(dateKeyLine.getBytes("UTF-8"), formattedDate.getBytes("UTF-8"));
        byte[] dateRegionKey = sumHmac(dateKey, region.getBytes("UTF-8"));
        byte[] dateRegionServiceKey = sumHmac(dateRegionKey, "s3".getBytes("UTF-8"));
        return sumHmac(dateRegionServiceKey, "aws4_request".getBytes("UTF-8"));
    }

    private static byte[] sumHmac(byte[] curKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(curKey, "HmacSHA256");
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(key);
        hmacSha256.update(data);
        return hmacSha256.doFinal();
    }

    private Request signV4(Request originalRequest, byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, IOException {
        if (this.accessKey == null || this.secretKey == null) {
            return originalRequest;
        }

        String region = getRegion(originalRequest);
        byte[] dataHashBytes = computeSha256(data);
        String dataHash = DatatypeConverter.printHexBinary(dataHashBytes).toLowerCase();

        String host = originalRequest.uri().getHost();
        int port = originalRequest.uri().getPort();
        if(port != -1) {
            String scheme = originalRequest.uri().getScheme();
            if ("http".equals(scheme) && port != 80) {
                host += ":" + originalRequest.uri().getPort();
            } else if ("https".equals(scheme) && port != 443) {
                host += ":" + originalRequest.uri().getPort();
            }
        }

        Request signedRequest = originalRequest.newBuilder()
            .header("x-amz-content-sha256", dataHash)
            .header("Host", host)
            .build();

        // get canonical request and headers to sign
        Tuple2<String, String> canonicalRequestAndHeaders = getCanonicalRequest(signedRequest, dataHash);
        String canonicalRequest = canonicalRequestAndHeaders.getFirst();
        String signedHeaders = canonicalRequestAndHeaders.getSecond();

        // get sha256 of canonical request
        byte[] canonicalHashBytes = computeSha256(canonicalRequest.getBytes("UTF-8"));
        String canonicalHash = DatatypeConverter.printHexBinary(canonicalHashBytes).toLowerCase();

        // generate key to sign
        String stringToSign = getStringToSign(region, canonicalHash, this.date);
        byte[] signingKey = generateSigningKey(this.date, region, this.secretKey);

        // generate signing key
        String signature = DatatypeConverter.printHexBinary(getSignature(signingKey, stringToSign)).toLowerCase();

        // generate authorization header
        String authorization = getAuthorizationHeader(signedHeaders, signature, this.date, region);

        signedRequest = signedRequest.newBuilder()
            .header("Authorization", authorization)
            .build();

        // print debug info
        // System.out.println("--- Canonical Request ---");
        // System.out.println(canonicalRequest);
        // System.out.println(DatatypeConverter.printHexBinary(canonicalRequest.getBytes("UTF-8")));
        // System.out.println("--- Canonical Hash ---");
        // System.out.println(canonicalHash);
        // System.out.println("--- String to Sign ---");
        // System.out.println(stringToSign);
        // System.out.println("--- Signing Key ---");
        // System.out.println(DatatypeConverter.printHexBinary(signingKey));
        // System.out.println("--- Signature ---");
        // System.out.println(signature);

        // System.out.println("--- Authorization ---");
        // System.out.println(authorization);
        // System.out.println("--- End ---");
        return signedRequest;
    }

    private String getRegion(Request request) {
        String host = request.url().getHost();
        return Regions.INSTANCE.getRegion(host);
    }

    private byte[] computeSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    private String getAuthorizationHeader(String signedHeaders, String signature, DateTime date, String region) {
        return "AWS4-HMAC-SHA256 Credential=" + this.accessKey + "/" + getScope(region, date) + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private byte[] getSignature(byte[] signingKey, String stringToSign) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        return sumHmac(signingKey, stringToSign.getBytes("UTF-8"));
    }

    private String getScope(String region, DateTime date) {
        String formattedDate = date.toString(dateFormatyyyyMMdd);
        return formattedDate + "/" + region + "/" + "s3" + "/" + "aws4_request";
    }

    private String getStringToSign(String region, String canonicalHash, DateTime date) {
        return "AWS4-HMAC-SHA256" + "\n" +
                date.toString(dateFormatyyyyMMddThhmmssZ) + "\n" +
                getScope(region, date) + "\n" + canonicalHash;
    }

    private Tuple2<String, String> getCanonicalRequest(Request request, String bodySha256Hash) throws IOException {
        StringWriter canonicalWriter = new StringWriter();
        PrintWriter canonicalPrinter = new PrintWriter(canonicalWriter, true);

        String method = request.method();
        String path = request.uri().getPath();
        String rawQuery = request.uri().getQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            rawQuery = "";
        }
        String query = getCanonicalQuery(rawQuery);

        canonicalPrinter.print(method + "\n");
        canonicalPrinter.print(path + "\n");
        canonicalPrinter.print(query + "\n");
        String[] headers = generateCanonicalHeaders(canonicalPrinter, request); // new line already added
        String signedHeaders = generateSignedHeaders(headers);
        canonicalPrinter.print("\n");
        canonicalPrinter.print(signedHeaders + "\n");
        canonicalPrinter.print(bodySha256Hash);

        canonicalPrinter.flush();
        return new Tuple2<String, String>(canonicalWriter.toString(), signedHeaders);
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
                    querySplit[i] = split[0] + "=" + split[1];
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

    private String generateSignedHeaders(String[] headers) {
        StringBuilder builder = new StringBuilder();
        boolean printSeparator = false;
        for (String header : headers) {
            if(!ignoredHeaders.contains(header)) {
                if (printSeparator) {
                    builder.append(';');
                } else {
                    printSeparator = true;
                }
                builder.append(header);
            }
        }
        return builder.toString();
    }

    private String[] generateCanonicalHeaders(PrintWriter writer, Request request) throws IOException {
        Map<String, String> map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        String contentMD5 = request.headers().get("Content-MD5");
        if (contentMD5 != null) {
            map.put("content-md5", contentMD5);
        }

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

        for (Map.Entry<String, String> e : map.entrySet()) {
            writer.write(e.getKey() + ":" + e.getValue() + '\n');
        }

        Set<String> var = map.keySet();
        String[] headerArray = var.toArray(new String[var.size()]);
        Arrays.sort(headerArray, String.CASE_INSENSITIVE_ORDER);
        return headerArray;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            Request signedRequest = this.signV4(chain.request(), data);
            Response response = chain.proceed(signedRequest);
            return response;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new Response.Builder().build();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return new Response.Builder().build();
        }
    }
}
