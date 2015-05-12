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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class HttpClient implements Client {
    private static final int PART_SIZE = 5 * 1024 * 1024;
    private final URL url;
    private HttpTransport transport = new NetHttpTransport();
    private String accessKey;
    private String secretKey;
    private String userAgent;
    private String contentType;
    private Logger logger;

    HttpClient(URL url) {
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("HEAD", url);
	HttpHeaders headers = request.getHeaders();
	headers.setUserAgent(this.userAgent);

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
        return requestFactory.buildRequest(method, url, null);
    }

    private GenericUrl getGenericUrlOfKey(String bucket, String key) {
        GenericUrl url = new GenericUrl(this.url);

        List<String> pathParts = new LinkedList<>();
        pathParts.add("");
        pathParts.add(bucket);
        pathParts.add(key);

        url.setPathParts(pathParts);
        return url;
    }

    private GenericUrl getGenericUrlOfBucket(String bucket) {
        GenericUrl url = new GenericUrl(this.url);

        List<String> pathParts = new LinkedList<>();
        pathParts.add("");
        pathParts.add(bucket);

        url.setPathParts(pathParts);
        return url;
    }

    @Override
    public InputStream getObject(String bucket, String key) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

	HttpRequest request = getHttpRequest("GET", url);
        request = request.setRequestMethod("GET");
	HttpHeaders httpHeaders = request.getHeaders();
	httpHeaders.setUserAgent(this.userAgent);
	request.setHeaders(httpHeaders);

        HttpResponse response = request.execute();
        return response.getContent();
    }

    @Override
    public InputStream getObject(String bucket, String key, long offset, long length) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
	HttpHeaders headers = request.getHeaders();
	headers.setUserAgent(this.userAgent);
	headers.setRange(offset + "-" + offset + length);
	request.setHeaders(headers);

        HttpResponse response = request.execute();
        return response.getContent();
    }

    @Override
    public ListBucketResult listObjectsInBucket(String bucket) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

	HttpRequest request = getHttpRequest("GET", url);
	HttpHeaders headers = request.getHeaders();
	headers.setAccept(this.contentType);
	headers.setUserAgent(this.userAgent);
	request.setHeaders(headers);

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

    @Override
    public ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException {
        GenericUrl url = new GenericUrl(this.url);

        HttpRequest request = getHttpRequest("GET", url);
	HttpHeaders headers = request.getHeaders();
	headers.setAccept(this.contentType);
	headers.setUserAgent(this.userAgent);
	request.setHeaders(headers);
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


    @Override
    public boolean testBucketAccess(String bucket) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

	HttpRequest request = getHttpRequest("HEAD", url);
	HttpHeaders headers = request.getHeaders();
	headers.setUserAgent(this.userAgent);
	request.setHeaders(headers);

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

    @Override
    public boolean createBucket(String bucket, String acl) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("PUT", url);
	HttpHeaders headers = request.getHeaders();
	headers.setUserAgent(this.userAgent);
	headers.set("x-amz-acl", acl);
	request.setHeaders(headers);

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

    @Override
    public void createObject(String bucket, String key, String contentType, long size, InputStream data) throws IOException, XmlPullParserException {
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
            List<String> parts = new LinkedList<>();
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

    @Override
    public void setKeys(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public void setUserAgent(String userAgent) {
	this.userAgent = userAgent;
    }

    @Override
    public void setContentType(String contentType) {
	this.contentType = contentType;
    }

    private String newMultipartUpload(String bucket, String key) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploads", "");

	HttpRequest request = getHttpRequest("POST", url);
	HttpHeaders headers = request.getHeaders();
	headers.setUserAgent(this.userAgent);
	request.setHeaders(headers);

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

        List<Part> parts = new LinkedList<>();
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
	HttpHeaders headers = request.getHeaders();
	headers.setUserAgent(this.userAgent);
	request.setHeaders(headers);
        request.setContent(new ByteArrayContent("application/xml", data));

        HttpResponse response = request.execute();
        response.disconnect();
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
	HttpHeaders headers = request.getHeaders();
	headers.setUserAgent(this.userAgent);

        if (md5sum != null) {
            String base64md5sum = Base64.getEncoder().encodeToString(md5sum);
            headers.setContentMD5(base64md5sum);
        }
	request.setHeaders(headers);

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
