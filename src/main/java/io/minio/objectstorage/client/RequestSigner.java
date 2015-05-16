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

package io.minio.objectstorage.client;

import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpEncoding;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

class RequestSigner implements HttpExecuteInterceptor {
    private final SimpleDateFormat dateFormatyyyyMMddThhmmssZ;
    private final SimpleDateFormat dateFormatyyyyMMdd;
    private final SimpleDateFormat dateFormat;
    private byte[] data = new byte[0];
    private String accessKey = null;
    private String secretKey = null;
    private byte[] userProvidedSigningKey;

    public RequestSigner(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        this.data = data;

        dateFormatyyyyMMddThhmmssZ = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormatyyyyMMddThhmmssZ.setTimeZone(TimeZone.getTimeZone("GMT"));

        dateFormatyyyyMMdd = new SimpleDateFormat("yyyyMMdd");
        dateFormatyyyyMMdd.setTimeZone(TimeZone.getTimeZone("GMT"));

        dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private void signV4(HttpRequest request, byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        if ((this.accessKey == null || this.secretKey == null) && this.userProvidedSigningKey == null) {
            return;
        }

        String region = getRegion(request);

        Date signingDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        addDate(request, signingDate);

        byte[] dataHashBytes = computeSha256(data);
        String dataHash = DatatypeConverter.printHexBinary(dataHashBytes).toLowerCase();

        request.getHeaders().set("x-amz-content-sha256", dataHash);
        request.getHeaders().setDate(dateFormat.format(signingDate));

        request.getHeaders().set("Host", request.getUrl().getHost());

        // get canonical request and headers to sign
        Tuple2<String, String> canonicalRequestAndHeaders = getCanonicalRequest(request, dataHash);
        String canonicalRequest = canonicalRequestAndHeaders.getFirst();
        String signedHeaders = canonicalRequestAndHeaders.getSecond();

        // get sha256 of canonical request
        byte[] canonicalHashBytes = computeSha256(canonicalRequest.getBytes("UTF-8"));
        String canonicalHash = DatatypeConverter.printHexBinary(canonicalHashBytes).toLowerCase();

        // generate key to sign
        String stringToSign = getStringToSign(region, canonicalHash, signingDate);
        byte[] signingKey = getSigningKey(signingDate, region);

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

    private void addDate(HttpRequest request, Date date) {
        String dateString = dateFormatyyyyMMddThhmmssZ.format(date);
        request.getHeaders().set("x-amz-date", dateString);
    }

    private String getAuthorizationHeader(String signedHeaders, String signature, Date date, String region) {
        return "AWS4-HMAC-SHA256 Credential=" + this.accessKey + "/" + getScope(region, date) + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private byte[] getSignature(byte[] signingKey, String stringToSign) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        return signHmac(signingKey, stringToSign.getBytes("UTF-8"));
    }

    private byte[] getSigningKey(Date date, String region) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        if(this.accessKey != null && this.secretKey != null) {
            String formattedDate = dateFormatyyyyMMdd.format(date);
            String dateKeyLine = "AWS4" + this.secretKey;
            byte[] dateKey = signHmac(dateKeyLine.getBytes("UTF-8"), formattedDate.getBytes("UTF-8"));
            byte[] dateRegionKey = signHmac(dateKey, region.getBytes("UTF-8"));
            byte[] dateRegionServiceKey = signHmac(dateRegionKey, "s3".getBytes("UTF-8"));
            return signHmac(dateRegionServiceKey, "aws4_request".getBytes("UTF-8"));
        }
        return this.userProvidedSigningKey;
    }

    private byte[] signHmac(byte[] curKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(curKey, "HmacSHA256");
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(key);
        hmacSha256.update(data);
        return hmacSha256.doFinal();
    }

    private String getScope(String region, Date date) {
        String formattedDate = dateFormatyyyyMMdd.format(date);
        return formattedDate + "/" + region + "/" + "s3" + "/" + "aws4_request";
    }

    private String getStringToSign(String region, String canonicalHash, Date date) {
        return "AWS4-HMAC-SHA256" + "\n" +
                dateFormatyyyyMMddThhmmssZ.format(date) + "\n" +
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
        for (String s : headers) {
            if (printSeparator) {
                builder.append(';');
            } else {
                printSeparator = true;
            }
            builder.append(s);
        }
        return builder.toString();
    }

    private String[] generateCanonicalHeaders(PrintWriter writer, HttpRequest request) {
        Map<String, String> map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        HttpContent content = request.getContent();

        if (content != null) {
            Long contentLength = null;
            try {
                contentLength = content.getLength();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (contentLength != null) {
                map.put("content-length", contentLength.toString());
            }

            String contentType = content.getType();
            if (contentType != null) {
                map.put("content-type", contentType);
            }

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

        String dateHeader = request.getHeaders().getDate();
        if (dateHeader != null) {
            map.put("date", dateHeader);
        }

        String userAgent = request.getHeaders().getUserAgent();
        if (userAgent != null) {
            map.put("user-agent", userAgent);
        }

        String contentMD5 = request.getHeaders().getContentMD5();
        if (contentMD5 != null) {
            map.put("content-md5", contentMD5);
        }

        for (String s : request.getHeaders().getUnknownKeys().keySet()) {
            map.put(s.toLowerCase().trim(), request.getHeaders().getFirstHeaderStringValue(s).trim());
        }

        for (Map.Entry<String, String> e : map.entrySet()) {
//            System.out.println(e.getKey() + ":" + e.getValue());
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

    public void setAccessKeys(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public void setSigningKey(byte[] signingKey) {
        this.userProvidedSigningKey = signingKey;
    }
}
