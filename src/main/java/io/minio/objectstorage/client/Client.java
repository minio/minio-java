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

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.xml.Xml;
import com.google.api.client.xml.XmlNamespaceDictionary;
import io.minio.objectstorage.client.messages.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Client {
    public static final String ACL_PUBLIC_READ_WRITE = "public-read-write";
    public static final String ACL_PRIVATE = "private";
    public static final String ACL_PUBLIC_READ = "public-read";
    public static final String ACL_AUTHENTICATED_READ = "authenticated-read";
    public static final String ACL_BUCKET_OWNER_READ = "bucket-owner-read";
    public static final String ACL_BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";

    private static final int PART_SIZE = 5 * 1024 * 1024;
    private final URL url;
    private HttpTransport transport = new NetHttpTransport();
    private String accessKey;
    private String secretKey;
    private Logger logger;
    private String userAgent;

    private Client(URL url) {
        this.url = url;
    }

    public static Client getClient(String url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        return getClient(new URL(url));
    }

    public static Client getClient(URL url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        // Set trailing / in path
        if (url.getPath().length() == 0) {
            String path = url.toString() + "/";
            url = new URL(path);
        }

        // Only a trailing path should be present in the path
        if (url.getPath().length() > 0 && !url.getPath().equals("/")) {
            throw new MalformedURLException("Path should be empty: '" + url.getPath() + "'");
        }

        // return a new http client
        return new Client(url);
    }

    public URL getUrl() {
        return url;
    }

    public ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        HttpRequest request = getHttpRequest("HEAD", url);

        HttpResponse response = request.execute();
        try {
            HttpHeaders responseHeaders = response.getHeaders();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            Date lastModified = formatter.parse(responseHeaders.getLastModified());
            return new ObjectMetadata(bucket, key, lastModified, responseHeaders.getContentLength(), responseHeaders.getETag());
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            response.disconnect();
        }
        throw new IOException();
    }

    private HttpRequest getHttpRequest(String method, GenericUrl url) throws IOException {
        return getHttpRequest(method, url, null);
    }

    private HttpRequest getHttpRequest(String method, GenericUrl url, final byte[] data) throws IOException {
        HttpRequestFactory requestFactory = this.transport.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                RequestSigner signer = new RequestSigner(data);
                signer.setAccessKeys(accessKey, secretKey);
                request.setInterceptor(signer);
            }
        });
        HttpRequest request = requestFactory.buildRequest(method, url, null);
        if (userAgent != null) {
            request.getHeaders().setUserAgent(userAgent);
        }
        return request;
    }

    private GenericUrl getGenericUrlOfKey(String bucket, String key) {
        GenericUrl url = new GenericUrl(this.url);

        List<String> pathParts = new LinkedList<String>();
        pathParts.add("");
        pathParts.add(bucket);
        pathParts.add(key);

        url.setPathParts(pathParts);
        return url;
    }

    private GenericUrl getGenericUrlOfBucket(String bucket) {
        GenericUrl url = new GenericUrl(this.url);

        List<String> pathParts = new LinkedList<String>();
        pathParts.add("");
        pathParts.add(bucket);

        url.setPathParts(pathParts);
        return url;
    }

    public InputStream getObject(String bucket, String key) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        request = request.setRequestMethod("GET");

        HttpResponse response = request.execute();
        return response.getContent();
    }

    public InputStream getObject(String bucket, String key, long offset, long length) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        request.getHeaders().setRange(offset + "-" + offset + length);

        HttpResponse response = request.execute();
        return response.getContent();
    }

    public ListBucketResult listObjectsInBucket(String bucket) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("GET", url);

        HttpResponse response = request.execute();

        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            ListBucketResult result = new ListBucketResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result;
        } finally {
            response.disconnect();
        }
    }

    void setTransport(HttpTransport transport) {
        this.transport = transport;
    }

    public ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException {
        GenericUrl url = new GenericUrl(this.url);

        HttpRequest request = getHttpRequest("GET", url);
        request.setFollowRedirects(false);

        HttpResponse response = request.execute();
        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            ListAllMyBucketsResult result = new ListAllMyBucketsResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result;
        } finally {
            response.disconnect();
        }
    }


    public boolean testBucketAccess(String bucket) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("HEAD", url);

        try {
            HttpResponse response = request.execute();
            try {
                return response.getStatusCode() == 200;
            } finally {
                response.disconnect();
            }
        } catch (HttpResponseException e) {
            return false;
        }
    }

    public boolean makeBucket(String bucket, String acl) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("PUT", url);
        request.getHeaders().set("x-amz-acl", acl);

        try {
            HttpResponse execute = request.execute();
            try {
                return execute.getStatusCode() == 200;
            } finally {
                execute.disconnect();
            }
        } catch (HttpResponseException e) {
            return false;
        }
    }

    public void putObject(String bucket, String key, String contentType, long size, InputStream data) throws IOException, XmlPullParserException {
        boolean isMultipart = false;
        int partSize = 0;
        String uploadID = null;

        if (size > PART_SIZE) {
            isMultipart = true;
            partSize = computePartSize(size);
            uploadID = newMultipartUpload(bucket, key);
        }

        if (!isMultipart) {
            byte[] dataArray = readData((int) size, data);
            putObject(bucket, key, contentType, dataArray);
        } else {
            List<String> parts = new LinkedList<String>();
            for (int part = 1; ; part++) {
                byte[] dataArray = readData(partSize, data);
                if (dataArray.length == 0) {
                    break;
                }
                parts.add(putObject(bucket, key, contentType, dataArray, uploadID, part));
            }
            completeMultipart(bucket, key, uploadID, parts);
        }
    }

    public void setKeys(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    private String newMultipartUpload(String bucket, String key) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploads", "");

        HttpRequest request = getHttpRequest("POST", url);

        HttpResponse response = request.execute();
        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result.getUploadId();
        } finally {
            response.disconnect();
        }
    }

    private void completeMultipart(String bucket, String key, String uploadID, List<String> etags) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        List<Part> parts = new LinkedList<Part>();
        for (int i = 0; i < etags.size(); i++) {
            Part part = new Part();
            part.setPartNumber(i + 1);
            part.seteTag(etags.get(i));
            parts.add(part);
        }

        CompleteMultipartUpload completeManifest = new CompleteMultipartUpload();
        completeManifest.setParts(parts);

        byte[] data = completeManifest.toString().getBytes("UTF-8");

        HttpRequest request = getHttpRequest("POST", url, data);
        request.setContent(new ByteArrayContent("application/xml", data));

        HttpResponse response = request.execute();
        response.disconnect();
    }

    private ListPartsResult listObjectParts(String bucket, String key, String uploadID) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        HttpRequest request = getHttpRequest("GET", url);

        HttpResponse response = request.execute();
        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            ListPartsResult result = new ListPartsResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result;
        } finally {
            response.disconnect();
        }
    }

    private boolean abortMultipart(String bucket, String key, String uploadID) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        HttpRequest request = getHttpRequest("DELETE", url);
        try {
            HttpResponse response = request.execute();
            try {
                return response.getStatusCode() == 200;
            } finally {
                response.disconnect();
            }
        } catch (HttpResponseException e) {
            return false;
        }
    }

    private int computePartSize(long size) {
        int minimumPartSize = PART_SIZE; // 5MB
        int partSize = (int) (size / 9999);
        return Math.max(minimumPartSize, partSize);
    }

    private void putObject(String bucket, String key, String contentType, byte[] data) throws IOException {
        putObject(bucket, key, contentType, data, "", 0);
    }

    private String putObject(String bucket, String key, String contentType, byte[] data, String uploadId, int partID) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        if (partID > 0) {
            url.set("partNumber", partID);
            url.set("uploadId", uploadId);
        }

        byte[] md5sum = null;
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            md5sum = md5Digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        HttpRequest request = getHttpRequest("PUT", url, data);

        if (md5sum != null) {
            String base64md5sum = DatatypeConverter.printBase64Binary(md5sum);
            request.getHeaders().setContentMD5(base64md5sum);
        }

        ByteArrayContent content = new ByteArrayContent(contentType, data);
        request.setContent(content);
        HttpResponse response = request.execute();
        response.disconnect();
        if (response.getStatusCode() != 200) {
            throw new IOException("Unexpected result, try resending this part again");
        }
        return response.getHeaders().getETag();
    }

    private byte[] readData(int size, InputStream data) throws IOException {
        int amountRead = 0;
        byte[] fullData = new byte[size];
        while (amountRead != size) {
            byte[] buf = new byte[size - amountRead];
            int curRead = data.read(buf);
            if (curRead == -1) {
                break;
            }
            buf = Arrays.copyOf(buf, curRead);
            System.arraycopy(buf, 0, fullData, amountRead, curRead);
            amountRead += curRead;
        }

        fullData = Arrays.copyOfRange(fullData, 0, amountRead);

        return fullData;
    }

    public void enableLogging() {
        if (logger == null) {
            logger = Logger.getLogger(HttpTransport.class.getName());
            logger.setLevel(Level.CONFIG);
            logger.addHandler(new Handler() {

                @Override
                public void close() throws SecurityException {
                }

                @Override
                public void flush() {
                }

                @Override
                public void publish(LogRecord record) {
                    // default ConsoleHandler will print >= INFO to System.err
                    if (record.getLevel().intValue() < Level.INFO.intValue()) {
                        System.out.println(record.getMessage());
                    }
                }
            });
        }
    }
}
