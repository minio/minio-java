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

public class HttpClient implements Client {
    private static final int PART_SIZE = 5 * 1024 * 1024;
    private final URL url;
    private HttpTransport transport = new NetHttpTransport();

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

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url);
        httpRequest = httpRequest.setRequestMethod("HEAD");
        HttpResponse response = httpRequest.execute();
        try {
            HttpHeaders headers = response.getHeaders();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            Date lastModified = formatter.parse(headers.getLastModified());
            return new ObjectMetadata(bucket, key, lastModified, headers.getContentLength(), headers.getETag());
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            response.disconnect();
        }
        throw new IOException();
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

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url);
        httpRequest = httpRequest.setRequestMethod("GET");
        HttpResponse response = httpRequest.execute();

        return response.getContent();
    }

    @Override
    public InputStream getObject(String bucket, String key, long offset, long length) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url).setRequestMethod("GET");
        HttpHeaders headers = httpRequest.getHeaders().setRange(offset + "-" + offset + length);
        httpRequest.setHeaders(headers);
        HttpResponse response = httpRequest.execute();

        return response.getContent();
    }

    @Override
    public ListBucketResult listObjectsInBucket(String bucket) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url);
        httpRequest = httpRequest.setRequestMethod("GET");
        HttpResponse response = httpRequest.execute();

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

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url);
        httpRequest = httpRequest.setRequestMethod("GET");
        HttpResponse response = httpRequest.execute();

        XmlPullParser parser = Xml.createParser();
        InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
        parser.setInput(reader);

        ListAllMyBucketsResult result = new ListAllMyBucketsResult();

        Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);

        return result;
    }

    @Override
    public boolean testBucketAccess(String bucket) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest;
        httpRequest = requestFactory.buildGetRequest(url);
        httpRequest = httpRequest.setRequestMethod("HEAD");
        try {
            HttpResponse response = httpRequest.execute();
            try {
                if (response.getStatusCode() == 200) {
                    return true;
                }
                return false;
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

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url).setRequestMethod("PUT");
        HttpHeaders headers = httpRequest.getHeaders();
        headers.set("x-amz-acl", acl);
        try {
            HttpResponse execute = httpRequest.execute();
            try {
                if (execute.getStatusCode() == 200) {
                    return true;
                }
                return false;
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

        if (size > this.PART_SIZE) {
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


    private String newMultipartUpload(String bucket, String key) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploads", "");
//        url.appendRawPath("?uploads");
        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url).setRequestMethod("POST");
        HttpResponse response = httpRequest.execute();
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
        url.set("uploadId" , uploadID);

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url).setRequestMethod("POST");

        List<Part> parts = new LinkedList<>();
        for (int i = 0; i < etags.size(); i++) {
            Part part = new Part();
            part.setPartNumber(i+1);
            part.seteTag(etags.get(i));
            parts.add(part);
        }

        CompleteMultipartUpload completeManifest = new CompleteMultipartUpload();
        completeManifest.setParts(parts);

        byte[] data = completeManifest.toString().getBytes("UTF-8");

        httpRequest.setContent(new ByteArrayContent("application/xml", data));

        HttpResponse response = httpRequest.execute();
        response.disconnect();
    }

    private int computePartSize(long size) {
        int minimumPartSize = this.PART_SIZE; // 5MB
        int partSize = (int) (size / 9999);
        return Math.max(minimumPartSize, partSize);
    }

    private void putObject(String bucket, String key, String contentType, byte[] data) throws IOException {
        putObject(bucket, key, contentType, data, "", 0);
    }

    private String putObject(String bucket, String key, String contentType, byte[] data, String uploadId, int partID) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        if (partID > 0) {
            url.set("partNumber" ,partID);
            url.set("uploadId" , uploadId);
        }

        byte[] md5sum = null;
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            md5sum = md5Digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url).setRequestMethod("PUT");

        if (md5sum != null) {
            String base64md5sum = Base64.getEncoder().encodeToString(md5sum);
            HttpHeaders headers = httpRequest.getHeaders();
            headers.setContentMD5(base64md5sum);
        }

        ByteArrayContent content = new ByteArrayContent(contentType, data);
        httpRequest.setContent(content);
        HttpResponse response = httpRequest.execute();
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
}
