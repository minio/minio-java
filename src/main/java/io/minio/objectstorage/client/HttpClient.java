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
import io.minio.objectstorage.client.messages.ListAllMyBucketsResult;
import io.minio.objectstorage.client.messages.ListBucketResult;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class HttpClient implements Client {
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
        HttpHeaders headers = httpRequest.getHeaders().setRange(offset + "-" + offset+length);
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

        XmlPullParser parser = Xml.createParser();
        InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
        parser.setInput(reader);

        ListBucketResult result = new ListBucketResult();

        Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);

        return result;
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
}
