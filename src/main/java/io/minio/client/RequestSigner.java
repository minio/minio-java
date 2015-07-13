/*
 * Minimal Object Storage Library, (C) 2015 Minio, Inc.
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

import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpEncoding;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
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
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class RequestSigner implements HttpExecuteInterceptor {
    private static final DateTimeFormatter dateFormatyyyyMMddThhmmssZ = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'").withZoneUTC();
    private static final DateTimeFormatter dateFormatyyyyMMdd = DateTimeFormat.forPattern("yyyyMMdd").withZoneUTC();
    private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withZoneUTC();

    private byte[] data = new byte[0];
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

    RequestSigner(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        this.data = data;

        ignoredHeaders.add("authorization");
        ignoredHeaders.add("date"); // we always set x-amz-date
        ignoredHeaders.add("content-type");
        ignoredHeaders.add("content-length");
        ignoredHeaders.add("user-agent");
    }


    private static byte[] generateSigningKey(DateTime date, String region, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String formattedDate = date.toString(dateFormatyyyyMMdd);
        String dateKeyLine = "AWS4" + secretKey;
        byte[] dateKey = signHmac(dateKeyLine.getBytes("UTF-8"), formattedDate.getBytes("UTF-8"));
        byte[] dateRegionKey = signHmac(dateKey, region.getBytes("UTF-8"));
        byte[] dateRegionServiceKey = signHmac(dateRegionKey, "s3".getBytes("UTF-8"));
        return signHmac(dateRegionServiceKey, "aws4_request".getBytes("UTF-8"));
    }

    private static byte[] signHmac(byte[] curKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(curKey, "HmacSHA256");
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(key);
        hmacSha256.update(data);
        return hmacSha256.doFinal();
    }

    private void signV4(HttpRequest request, byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        if (this.accessKey == null || this.secretKey == null) {
            return;
        }

        String region = getRegion(request);

        DateTime signingDate = new DateTime();
        addDateToHeaders(request, signingDate);

        byte[] dataHashBytes = computeSha256(data);
        String dataHash = DatatypeConverter.printHexBinary(dataHashBytes).toLowerCase();

        request.getHeaders().set("x-amz-content-sha256", dataHash);
        request.getHeaders().setDate(new DateTime(signingDate).toString(dateFormat));

        String host = request.getUrl().getHost();
        int port = request.getUrl().getPort();
        if(port != -1) {
            String scheme = request.getUrl().getScheme();
            if ("http".equals(scheme) && port != 80) {
                host += ":" + request.getUrl().getPort();
            } else if ("https".equals(scheme) && port != 443) {
                host += ":" + request.getUrl().getPort();
            }
        }
        request.getHeaders().set("Host", host);

        // get canonical request and headers to sign
        Tuple2<String, String> canonicalRequestAndHeaders = getCanonicalRequest(request, dataHash);
        String canonicalRequest = canonicalRequestAndHeaders.getFirst();
        String signedHeaders = canonicalRequestAndHeaders.getSecond();

        // get sha256 of canonical request
        byte[] canonicalHashBytes = computeSha256(canonicalRequest.getBytes("UTF-8"));
        String canonicalHash = DatatypeConverter.printHexBinary(canonicalHashBytes).toLowerCase();

        // generate key to sign
        String stringToSign = getStringToSign(region, canonicalHash, signingDate);
        byte[] signingKey = generateSigningKey(signingDate, region, this.secretKey);

        // generate signing key
        String signature = DatatypeConverter.printHexBinary(getSignature(signingKey, stringToSign)).toLowerCase();

        // generate authorization header
        String authorization = getAuthorizationHeader(signedHeaders, signature, signingDate, region);

        // set authorization header
        List<String> authorizationList = new LinkedList<String>();
        authorizationList.add(authorization);
        request.getHeaders().setAuthorization(authorizationList);

        // print debug info
//        System.out.println("--- Canonical Request ---");
//        System.out.println(canonicalRequest);
//        System.out.println(DatatypeConverter.printHexBinary(canonicalRequest.getBytes("UTF-8")));
//        System.out.println("--- Canonical Hash ---");
//        System.out.println(canonicalHash);
//        System.out.println("--- String to Sign ---");
//        System.out.println(stringToSign);
//        System.out.println("--- Signing Key ---");
//        System.out.println(DatatypeConverter.printHexBinary(signingKey));
//        System.out.println("--- Signature ---");
//        System.out.println(signature);
//        System.out.println("--- Authorization ---");
//        System.out.println(authorization);
//        System.out.println("--- End ---");
    }

    private String getRegion(HttpRequest request) {
        String host = request.getUrl().getHost();
        return Regions.INSTANCE.getRegion(host);
    }

    private byte[] computeSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    private void addDateToHeaders(HttpRequest request, DateTime date) {
        String dateString = date.toString(dateFormatyyyyMMddThhmmssZ);
        request.getHeaders().set("x-amz-date", dateString);
    }

    private String getAuthorizationHeader(String signedHeaders, String signature, DateTime date, String region) {
        return "AWS4-HMAC-SHA256 Credential=" + this.accessKey + "/" + getScope(region, date) + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private byte[] getSignature(byte[] signingKey, String stringToSign) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        return signHmac(signingKey, stringToSign.getBytes("UTF-8"));
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

    private Tuple2<String, String> getCanonicalRequest(HttpRequest request, String bodySha256Hash) {
        StringWriter canonicalWriter = new StringWriter();
        PrintWriter canonicalPrinter = new PrintWriter(canonicalWriter, true);

        String method = request.getRequestMethod();
        String path = request.getUrl().getRawPath();
        String rawQuery = request.getUrl().toURI().getQuery();
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

    private String[] generateCanonicalHeaders(PrintWriter writer, HttpRequest request) {
        Map<String, String> map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        HttpContent content = request.getContent();

        if (content != null) {
            HttpEncoding encoding = request.getEncoding();
            String contentEncoding = null;
            if (encoding != null) {
                contentEncoding = encoding.getName();
            }
            if (contentEncoding != null) {
                map.put("content-encoding", contentEncoding);
            }
        }

        String acceptEncoding = request.getHeaders().getAcceptEncoding();
        if (acceptEncoding != null) {
            map.put("accept-encoding", acceptEncoding);
        }

        String contentMD5 = request.getHeaders().getContentMD5();
        if (contentMD5 != null) {
            map.put("content-md5", contentMD5);
        }

        for (String s : request.getHeaders().getUnknownKeys().keySet()) {
            String val = request.getHeaders().getFirstHeaderStringValue(s);
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
    public void intercept(HttpRequest request) throws IOException {
        try {
            this.signV4(request, data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    void setAccessKeys(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}
