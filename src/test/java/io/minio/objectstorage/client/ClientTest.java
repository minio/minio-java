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

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import io.minio.objectstorage.client.messages.Item;
import io.minio.objectstorage.client.messages.ListBucketResult;
import io.minio.objectstorage.client.messages.Owner;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ClientTest {
    @Test()
    public void InstantiateNewClient() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Clients.getClient("http://" + expectedHost);

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test()
    public void InstantiateNewClientWithTrailingSlash() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Clients.getClient("http://" + expectedHost + "/");

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test(expected = MalformedURLException.class)
    public void NewClientWithPathFails() throws MalformedURLException {
        Clients.getClient("http://example.com/path");
    }

    @Test(expected = NullPointerException.class)
    public void NewClientWithNullURLFails() throws MalformedURLException {
        Clients.getClient((URL) null);
    }

    @Test(expected = NullPointerException.class)
    public void NewClientWithNullURLStringFails() throws MalformedURLException {
        Clients.getClient((String) null);
    }

    @Test(expected = IOException.class)
    public void getMissingObjectHeaders() throws IOException {
        // Set up mock
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
//                        response.addHeader("custom_header", "value");
                        response.setStatusCode(404);
//                        response.setContentType(Xml.MEDIA_TYPE);
                        //response.setContent("{\"error\":\"not found\"}");
                        return response;
                    }
                };
            }
        };

        HttpClient client = (HttpClient) Clients.getClient("http://example.com:9000");
        client.setTransport(transport);
        ObjectMetadata objectMetadata = client.getObjectMetadata("bucket", "key");
        assertEquals("bucket", objectMetadata.getBucket());
        assertEquals("key", objectMetadata.getKey());
    }

    @Test
    public void testGetObjectHeaders() throws IOException {
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Content-Length", "5080");
                        response.addHeader("Content-Type", "application/octet-stream");
                        response.addHeader("ETag", "a670520d9d36833b3e28d1e4b73cbe22");
                        response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        // build expected request
        Calendar expectedDate = Calendar.getInstance();
        expectedDate.clear();
        expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        expectedDate.set(2015, Calendar.MAY, 4, 7, 58, 51);
        ObjectMetadata expectedMetadata = new ObjectMetadata("bucket", "key", expectedDate.getTime(), 5080, "a670520d9d36833b3e28d1e4b73cbe22");

        // get request
        HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000");
        client.setTransport(transport);
        ObjectMetadata objectMetadata = client.getObjectMetadata("bucket", "key");

        assertEquals(expectedMetadata, objectMetadata);
    }

    @Test
    public void testGetObject() throws IOException {
        final String expectedObject = "hello world";

        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Content-Length", "5080");
                        response.addHeader("Content-Type", "application/octet-stream");
                        response.addHeader("ETag", "5eb63bbbe01eeed093cb22bb8f5acdc3");
                        response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
                        response.setStatusCode(200);
                        response.setContent(expectedObject.getBytes("UTF-8"));
                        return response;
                    }
                };
            }
        };

        // get request
        HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000");
        client.setTransport(transport);
        InputStream object = client.getObject("bucket", "key");
        byte[] result = new byte[20];
        int read = object.read(result);
        result = Arrays.copyOf(result, read);
        assertEquals(expectedObject, new String(result, "UTF-8"));
    }

    @Test
    public void testListObjects() throws IOException, XmlPullParserException, ParseException {
        final String body = "<ListBucketResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Name>bucket</Name><Prefix></Prefix><Marker></Marker><MaxKeys>1000</MaxKeys><Delimiter></Delimiter><IsTruncated>false</IsTruncated><Contents><Key>key</Key><LastModified>2015-05-05T02:21:15.716Z</LastModified><ETag>5eb63bbbe01eeed093cb22bb8f5acdc3</ETag><Size>11</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents></ListBucketResult>";
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Content-Length", "414");
                        response.addHeader("Content-Type", "application/xml");
                        response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
                        response.setContent(body.getBytes("UTF-8"));
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        HttpClient client = (HttpClient) Clients.getClient("http://localhost:9000");
        client.setTransport(transport);
        ListBucketResult bucket = client.listObjectsInBucket("bucket");

        assertEquals("bucket", bucket.getName());
        assertEquals(null, bucket.getPrefix());
        assertEquals(null, bucket.getMarker());
        assertEquals(1000, bucket.getMaxKeys());
        assertEquals(null, bucket.getDelimiter());
        assertEquals(false, bucket.isTruncated());
        assertEquals(1, bucket.getContents().size());

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
        assertEquals(expectedDate.getTime(), item.getLastModifiedDate());

        Owner owner = item.getOwner();
        assertEquals("minio", owner.getID());
        assertEquals("minio", owner.getDisplayName());
    }
}