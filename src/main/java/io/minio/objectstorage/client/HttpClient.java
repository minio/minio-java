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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.xml.Xml;
import com.google.api.client.xml.XmlNamespaceDictionary;
import io.minio.objectstorage.client.messages.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpClient implements Client, Closeable {
    private static final int PART_SIZE = 5 * 1024 * 1024;
    private final URL url;
    private CloseableHttpClient client = HttpClients.createDefault();

    HttpClient(URL url) {
        this.url = url;
    }

    void setTransport(CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException, ParseException, URISyntaxException {
        URL url = new URL(this.url, bucket + "/" + key);
        HttpGet get = new HttpGet(url.toURI());
        try (CloseableHttpResponse response = client.execute(get)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                String lengthString = response.getFirstHeader("Content-Length").getValue();
                long length = Long.parseLong(lengthString);
                String dateString = response.getFirstHeader("Last-Modified").getValue();
                SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                Date lastModified = formatter.parse(dateString);
                String etag = response.getFirstHeader("ETag").getValue();
                ObjectMetadata metadata = new ObjectMetadata(bucket, key, lastModified, length, etag);
                return metadata;
            } else {
                // TODO make a better exception
                throw new IOException();
            }
        }
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
    public InputStream getObject(String bucket, String key) throws IOException, ParseException, URISyntaxException {
        URL url = new URL(this.url, bucket + "/" + key);
        HttpGet get = new HttpGet(url.toURI());
        CloseableHttpResponse response = client.execute(get);
        int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            return response.getEntity().getContent();
        } else {
            // TODO make a better exception
            throw new IOException();
        }
    }

    @Override
    public InputStream getObject(String bucket, String key, long offset, long length) throws IOException, URISyntaxException {
        URL url = new URL(this.url, bucket + "/" + key);
        HttpGet get = new HttpGet(url.toURI());
        String range = offset + "-" + offset + length;
        get.setHeader("Range", range);
        try (CloseableHttpResponse response = client.execute(get)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 206) {
                return response.getEntity().getContent();
            } else {
                // TODO make a better exception
                throw new IOException();
            }
        }
    }

    @Override
    public ListBucketResult listObjectsInBucket(String bucket) throws IOException, XmlPullParserException, URISyntaxException {
        URL url = new URL(this.url, bucket);
        HttpGet get = new HttpGet(url.toURI());
        try (CloseableHttpResponse response = client.execute(get)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                XmlPullParser parser = Xml.createParser();
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
                parser.setInput(reader);

                ListBucketResult result = new ListBucketResult();

                Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
                return result;
            }
        }
        throw new IOException();
    }

    @Override
    public ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException, URISyntaxException {
        HttpGet get = new HttpGet(this.url.toURI());
        try (CloseableHttpResponse response = client.execute(get)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                XmlPullParser parser = Xml.createParser();
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
                parser.setInput(reader);

                ListAllMyBucketsResult result = new ListAllMyBucketsResult();

                Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);

                return result;
            }
        }
        throw new IOException();
    }

    @Override
    public boolean testBucketAccess(String bucket) throws IOException, URISyntaxException {
        URL url = new URL(this.url, bucket);
        HttpHead head = new HttpHead(url.toURI());
        try (CloseableHttpResponse response = client.execute(head)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean createBucket(String bucket, String acl) throws IOException, URISyntaxException {
        URL url = new URL(this.url, bucket);
        HttpPut put = new HttpPut(url.toURI());
        put.addHeader("x-amz-acl", acl);
        try (CloseableHttpResponse response = client.execute(put)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void createObject(String bucket, String key, String contentType, long size, InputStream data) throws IOException, XmlPullParserException, URISyntaxException {
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


    private String newMultipartUpload(String bucket, String key) throws IOException, XmlPullParserException, URISyntaxException {
        URI uri = new URL(this.url, bucket + "/" + key + "?uploads").toURI();
        HttpPost post = new HttpPost(uri);
        try (CloseableHttpResponse response = client.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                XmlPullParser parser = Xml.createParser();
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
                parser.setInput(reader);

                InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();

                Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
                return result.getUploadId();
            }
        }
        throw new IOException();
    }

    private void completeMultipart(String bucket, String key, String uploadID, List<String> eTags) throws IOException, URISyntaxException {
        URI uri = new URL(this.url, bucket + "/" + key).toURI();
        URIBuilder builder = new URIBuilder(uri);
        builder.addParameter("uploadId", uploadID);
        uri = builder.build();

        HttpPost post = new HttpPost(uri);

        List<Part> parts = new LinkedList<>();
        for (int i = 0; i < eTags.size(); i++) {
            Part part = new Part();
            part.setPartNumber(i + 1);
            part.seteTag(eTags.get(i));
            parts.add(part);
        }

        CompleteMultipartUpload completeManifest = new CompleteMultipartUpload();
        completeManifest.setParts(parts);

        byte[] data = completeManifest.toString().getBytes("UTF-8");

        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(data));
        entity.setContentLength(data.length);
        entity.setContentType("application/xml");
        post.setEntity(entity);

        post.setEntity(entity);

        try (CloseableHttpResponse response = client.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                return;
            }
        }
        throw new IOException();

    }

    private int computePartSize(long size) {
        int minimumPartSize = this.PART_SIZE; // 5MB
        int partSize = (int) (size / 9999);
        return Math.max(minimumPartSize, partSize);
    }

    private void putObject(String bucket, String key, String contentType, byte[] data) throws IOException, URISyntaxException {
        putObject(bucket, key, contentType, data, "", 0);
    }

    private String putObject(String bucket, String key, String contentType, byte[] data, String uploadId, int partID) throws IOException, URISyntaxException {
        URIBuilder builder = new URIBuilder(this.url.toURI() + bucket + "/" + key);

        if (partID > 0) {
            builder.addParameter("partNumber", "" + partID);
            builder.addParameter("uploadId", uploadId);
        }

        HttpPut put = new HttpPut(builder.build());

        byte[] md5sum = null;
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            md5sum = md5Digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (md5sum != null) {
            String base64md5sum = Base64.getEncoder().encodeToString(md5sum);
            put.setHeader("Content-MD5", base64md5sum);
        }

        ByteArrayInputStream content = new ByteArrayInputStream(data);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(content);
        entity.setContentLength(data.length);
        entity.setContentType(contentType);
        put.setEntity(entity);
        try (CloseableHttpResponse response = client.execute(put)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                return response.getFirstHeader("ETag").getValue();
            }
        }
        throw new IOException();
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

    @Override
    public void close() throws IOException {
        this.client.close();
    }
}
