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

import io.minio.objectstorage.client.messages.*;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ClientTest {
    @Test()
    public void instantiateNewClient() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Clients.getClient("http://" + expectedHost);

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test()
    public void instantiateNewClientWithTrailingSlash() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Clients.getClient("http://" + expectedHost + "/");

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test(expected = MalformedURLException.class)
    public void newClientWithPathFails() throws MalformedURLException {
        Clients.getClient("http://example.com/path");
    }

    @Test(expected = NullPointerException.class)
    public void newClientWithNullURLFails() throws MalformedURLException {
        Clients.getClient((URL) null);
    }

    @Test(expected = NullPointerException.class)
    public void newClientWithNullURLStringFails() throws MalformedURLException {
        Clients.getClient((String) null);
    }

    @Test(expected = IOException.class)
    public void getMissingObjectHeaders() throws IOException, ParseException, URISyntaxException {
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.setStatusCode(404);
//            transport.addHeader("Content-Type", Xml.MEDIA_TYPE);
//            transport.addHeader("Content-Length", "11");
//            transport.addHeader("Last-Modified", "Sun, 10 May 2015 06:53:42 GMT");
//            transport.addHeader("Etag", "foo-1");
            transport.setContent("{\"error\":\"not found\"}");
            client.setTransport(transport);
            ObjectMetadata objectMetadata = client.getObjectMetadata("bucket", "key");
            assertEquals("bucket", objectMetadata.getBucket());
            assertEquals("key", objectMetadata.getKey());
        }
    }

    @Test
    public void testGetObjectHeaders() throws IOException, ParseException, URISyntaxException {
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Content-Length", "5080");
            transport.addHeader("Content-Type", "application/octet-stream");
            transport.addHeader("Etag", "a670520d9d36833b3e28d1e4b73cbe22");
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.setStatusCode(200);

            // build expected request
            Calendar expectedDate = Calendar.getInstance();
            expectedDate.clear();
            expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
            expectedDate.set(2015, Calendar.MAY, 4, 7, 58, 51);
            ObjectMetadata expectedMetadata = new ObjectMetadata("bucket", "key", expectedDate.getTime(), 5080, "a670520d9d36833b3e28d1e4b73cbe22");

            // get request
            client.setTransport(transport);
            ObjectMetadata objectMetadata = client.getObjectMetadata("bucket", "key");

            assertEquals(expectedMetadata, objectMetadata);
        }
    }

    @Test
    public void testGetObject() throws IOException, ParseException, URISyntaxException {
        final String expectedObject = "hello world";

        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Content-Length", "5080");
            transport.addHeader("Content-Type", "application/octet-stream");
            transport.addHeader("ETag", "5eb63bbbe01eeed093cb22bb8f5acdc3");
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.setStatusCode(200);
            transport.setContent(expectedObject.getBytes("UTF-8"));

            // get request
            client.setTransport(transport);
            InputStream object = client.getObject("bucket", "key");
            byte[] result = new byte[20];
            int read = object.read(result);
            result = Arrays.copyOf(result, read);
            assertEquals(expectedObject, new String(result, "UTF-8"));
        }
    }

    @Test
    public void testPartialObject() throws IOException, URISyntaxException {
        final String expectedObject = "hello";

        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Content-Length", "5");
            transport.addHeader("Content-Type", "application/octet-stream");
            transport.addHeader("ETag", "5eb63bbbe01eeed093cb22bb8f5acdc3");
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.addHeader("Range", "0-4/11");
            transport.setStatusCode(206);
            transport.setContent(expectedObject.getBytes("UTF-8"));

            // get request
            client.setTransport(transport);
            InputStream object = client.getObject("bucket", "key", 0, 5);
            byte[] result = new byte[20];
            int read = object.read(result);
            result = Arrays.copyOf(result, read);
            assertEquals(expectedObject, new String(result, "UTF-8"));
        }
    }

    @Test
    public void testListObjects() throws IOException, XmlPullParserException, ParseException, URISyntaxException {
        final String body = "<ListBucketResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Name>bucket</Name><Prefix></Prefix><Marker></Marker><MaxKeys>1000</MaxKeys><Delimiter></Delimiter><IsTruncated>false</IsTruncated><Contents><Key>key</Key><LastModified>2015-05-05T02:21:15.716Z</LastModified><ETag>5eb63bbbe01eeed093cb22bb8f5acdc3</ETag><Size>11</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents><Contents><Key>key2</Key><LastModified>2015-05-05T20:36:17.498Z</LastModified><ETag>2a60eaffa7a82804bdc682ce1df6c2d4</ETag><Size>1661</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents></ListBucketResult>";
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Content-Length", "414");
            transport.addHeader("Content-Type", "application/xml");
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.setContent(body.getBytes("UTF-8"));
            transport.setStatusCode(200);

            client.setTransport(transport);
            ListBucketResult bucket = client.listObjectsInBucket("bucket");

            assertEquals("bucket", bucket.getName());
            assertEquals(null, bucket.getPrefix());
            assertEquals(null, bucket.getMarker());
            assertEquals(1000, bucket.getMaxKeys());
            assertEquals(null, bucket.getDelimiter());
            assertEquals(false, bucket.isTruncated());
            assertEquals(2, bucket.getContents().size());

            Item item = bucket.getContents().get(0);
            assertEquals("key", item.getKey());
            assertEquals("2015-05-05T02:21:15.716Z", item.getLastModified());
            assertEquals(11, item.getSize());
            assertEquals("STANDARD", item.getStorageClass());

            Calendar expectedDate = Calendar.getInstance();
            expectedDate.clear();
            expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
            expectedDate.set(2015, Calendar.MAY, 5, 2, 21, 15);
            expectedDate.set(Calendar.MILLISECOND, 716);
            assertEquals(expectedDate.getTime(), item.getParsedLastModified());

            Owner owner = item.getOwner();
            assertEquals("minio", owner.getID());
            assertEquals("minio", owner.getDisplayName());
        }
    }

    @Test
    public void testListBuckets() throws IOException, XmlPullParserException, ParseException, URISyntaxException {
        final String body = "<ListAllMyBucketsResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner><Buckets><Bucket><Name>bucket</Name><CreationDate>2015-05-05T20:35:51.410Z</CreationDate></Bucket><Bucket><Name>foo</Name><CreationDate>2015-05-05T20:35:47.170Z</CreationDate></Bucket></Buckets></ListAllMyBucketsResult>";
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Content-Length", "351");
            transport.addHeader("Content-Type", "application/xml");
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.setContent(body.getBytes("UTF-8"));
            transport.setStatusCode(200);
            client.setTransport(transport);
            ListAllMyBucketsResult buckets = client.listBuckets();

            assertEquals(2, buckets.getBuckets().size());
            Bucket bucket = buckets.getBuckets().get(0);
            assertEquals("bucket", bucket.getName());
            assertEquals("2015-05-05T20:35:51.410Z", bucket.getCreationDate());

            Calendar expectedDate = Calendar.getInstance();
            expectedDate.clear();
            expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
            expectedDate.set(2015, Calendar.MAY, 5, 20, 35, 51);
            expectedDate.set(Calendar.MILLISECOND, 410);
            assertEquals(expectedDate.getTime(), bucket.getParsedCreationDate());

            Owner owner = buckets.getOwner();
            assertEquals("minio", owner.getDisplayName());
            assertEquals("minio", owner.getID());
        }
    }

    @Test
    public void testBucketAccess() throws IOException, XmlPullParserException, URISyntaxException {
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.addHeader("Host", "localhost");
            transport.setStatusCode(200);

            client.setTransport(transport);
            boolean result = client.testBucketAccess("bucket");

            assertEquals(true, result);
        }
    }

    @Test
    public void testBucketAccessFails() throws IOException, XmlPullParserException, URISyntaxException {
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.addHeader("Host", "localhost");
            transport.setStatusCode(404);

            client.setTransport(transport);
            boolean result = client.testBucketAccess("bucket");

            assertEquals(false, result);
        }
    }

    @Test
    public void testCreateBucket() throws IOException, XmlPullParserException, URISyntaxException {
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.addHeader("Host", "localhost");
            transport.setStatusCode(200);
            client.setTransport(transport);
            boolean result = client.createBucket("bucket", Client.ACL_PUBLIC_READ);

            assertEquals(true, result);
        }
    }

    @Test
    public void testCreateBucketFails() throws IOException, XmlPullParserException, URISyntaxException {
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.addHeader("Host", "localhost");
            transport.setStatusCode(403);

            client.setTransport(transport);
            boolean result = client.createBucket("bucket", Client.ACL_PUBLIC_READ);

            assertEquals(false, result);
        }
    }

    @Test
    public void testCreateObject() throws IOException, NoSuchAlgorithmException, XmlPullParserException, URISyntaxException {
        try (HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000")) {
            MockServer transport = new MockServer();
            transport.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
            transport.addHeader("Host", "localhost");
            transport.addHeader("ETag", "5eb63bbbe01eeed093cb22bb8f5acdc3");
            transport.setStatusCode(200);

            client.setTransport(transport);

            String inputString = "hello world";
            ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

            client.createObject("bucket", "key", "application/octet-stream", 11, data);
        }

    }
}