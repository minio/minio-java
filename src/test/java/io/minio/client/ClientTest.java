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

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import io.minio.client.acl.Acl;
import io.minio.client.errors.*;
import io.minio.client.messages.Bucket;
import io.minio.client.messages.ErrorResponse;
import io.minio.client.messages.Item;
import io.minio.client.messages.Owner;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class ClientTest {
    @Test()
    public void instantiateNewClient() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Client.getClient("http://" + expectedHost);

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test()
    public void instantiateNewClientWithTrailingSlash() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Client.getClient("http://" + expectedHost + "/");

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test()
    public void setUserAgentOnceSet() throws IOException {
        String expectedHost = "example.com";
        Client client = Client.getClient("http://" + expectedHost + "/");
        client.setUserAgent("testApp", "1.0.0", "");
    }

    @Test(expected = IOException.class)
    public void setUserAgentTwiceSet() throws IOException {
        String expectedHost = "example.com";
        Client client = Client.getClient("http://" + expectedHost + "/");
        client.setUserAgent("testApp", "1.0.0", "");
        client.setUserAgent("testApp", "1.0.0", "");
    }

    @Test(expected = MalformedURLException.class)
    public void newClientWithPathFails() throws MalformedURLException {
        Client.getClient("http://example.com/path");
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test(expected = NullPointerException.class)
    public void newClientWithNullURLFails() throws MalformedURLException {
        Client.getClient((URL) null);
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test(expected = NullPointerException.class)
    public void newClientWithNullURLStringFails() throws MalformedURLException {
        Client.getClient((String) null);
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test(expected = ForbiddenException.class)
    public void testForbidden() throws IOException, ClientException {
        // Set up mock
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.setStatusCode(403);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://example.com:9000");
        client.setTransport(transport);
        client.statObject("bucket", "key");
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void getMissingObjectHeaders() throws IOException, ClientException {
        // Set up mock
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.setStatusCode(404);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://example.com:9000");
        client.setTransport(transport);
        client.statObject("bucket", "key");
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test
    public void testGetObjectHeaders() throws IOException, NoSuchAlgorithmException, InvalidKeyException, ClientException {
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 05 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "5080");
                        response.addHeader("Content-Type", "application/octet-stream");
                        response.addHeader("ETag", "\"a670520d9d36833b3e28d1e4b73cbe22\"");
                        response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        // build expected request
        Calendar expectedDate = Calendar.getInstance();
        expectedDate.clear();
        expectedDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        expectedDate.set(2015, Calendar.MAY, 4, 7, 58, 51);
        ObjectStat expectedStatInfo = new ObjectStat("bucket", "key", expectedDate.getTime(), 5080, "a670520d9d36833b3e28d1e4b73cbe22", "application/octet-stream");

        // get request
        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        ObjectStat objectStatInfo = client.statObject("bucket", "key");

        assertEquals(expectedStatInfo, objectStatInfo);
    }

    @Test
    public void testGetObject() throws IOException, ClientException {
        final String expectedObject = "hello world";

        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 05 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "5080");
                        response.addHeader("Content-Type", "application/octet-stream");
                        response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
                        response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
                        response.setStatusCode(200);
                        response.setContent(expectedObject.getBytes("UTF-8"));
                        return response;
                    }
                };
            }
        };

        // get request
        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        InputStream object = client.getObject("bucket", "key");
        byte[] result = new byte[20];
        int read = object.read(result);
        result = Arrays.copyOf(result, read);
        assertEquals(expectedObject, new String(result, "UTF-8"));
    }

    @Test
    public void testPartialObject() throws IOException, ClientException {
        final String expectedObject = "hello";

        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Content-Length", "5");
                        response.addHeader("Content-Type", "application/octet-stream");
                        response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
                        response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
                        response.addHeader("Accept-Ranges", "bytes");
                        response.addHeader("Content-Range", "0-4/11");
                        response.setStatusCode(206);
                        response.setContent(expectedObject.getBytes("UTF-8"));
                        return response;
                    }
                };
            }
        };

        // get request
        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        InputStream object = client.getObject("bucket", "key", 0, 5);
        byte[] result = new byte[20];
        int read = object.read(result);
        result = Arrays.copyOf(result, read);
        assertEquals(expectedObject, new String(result, "UTF-8"));
    }

    @Test
    public void testListObjects() throws IOException, XmlPullParserException, ParseException, ClientException {
        final String body = "<ListBucketResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Name>bucket</Name><Prefix></Prefix><Marker></Marker><MaxKeys>1000</MaxKeys><Delimiter></Delimiter><IsTruncated>false</IsTruncated><Contents><Key>key</Key><LastModified>2015-05-05T02:21:15.716Z</LastModified><ETag>\"5eb63bbbe01eeed093cb22bb8f5acdc3\"</ETag><Size>11</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents><Contents><Key>key2</Key><LastModified>2015-05-05T20:36:17.498Z</LastModified><ETag>\"2a60eaffa7a82804bdc682ce1df6c2d4\"</ETag><Size>1661</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents></ListBucketResult>";
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "414");
                        response.addHeader("Content-Type", "application/xml");
                        response.setContent(body.getBytes("UTF-8"));
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        Iterator<Result<Item>> objectsInBucket = client.listObjects("bucket");

        Item item = objectsInBucket.next().getResult();
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

    @Test
    public void testListBuckets() throws IOException, XmlPullParserException, ParseException, ClientException {
        final String body = "<ListAllMyBucketsResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner><Buckets><Bucket><Name>bucket</Name><CreationDate>2015-05-05T20:35:51.410Z</CreationDate></Bucket><Bucket><Name>foo</Name><CreationDate>2015-05-05T20:35:47.170Z</CreationDate></Bucket></Buckets></ListAllMyBucketsResult>";
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "351");
                        response.addHeader("Content-Type", "application/xml");
                        response.setContent(body.getBytes("UTF-8"));
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        Iterator<Bucket> buckets = client.listBuckets();

        Bucket bucket = buckets.next();
        assertEquals("bucket", bucket.getName());
        assertEquals("2015-05-05T20:35:51.410Z", bucket.getCreationDate());

        bucket = buckets.next();
        assertEquals("foo", bucket.getName());
        assertEquals("2015-05-05T20:35:47.170Z", bucket.getCreationDate());

        Calendar expectedDate = Calendar.getInstance();
        expectedDate.clear();
        expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        expectedDate.set(2015, Calendar.MAY, 5, 20, 35, 47);
        expectedDate.set(Calendar.MILLISECOND, 170);
        assertEquals(expectedDate.getTime(), bucket.getParsedCreationDate());
    }

    @Test
    public void testBucketExists() throws IOException, XmlPullParserException, ClientException {
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        boolean result = client.bucketExists("bucket");

        assertEquals(true, result);
    }

    @Test
    public void testBucketExistsFails() throws IOException, XmlPullParserException, ClientException {
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(404);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        boolean result = client.bucketExists("bucket");

        assertEquals(false, result);
    }

    @Test
    public void testMakeBucket() throws IOException, XmlPullParserException, ClientException {
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        client.makeBucket("bucket", Acl.PUBLIC_READ);
        client.setBucketACL("bucket", Acl.PRIVATE);
    }


    @Test
    public void testGetBucketACLPublicRW() throws IOException, XmlPullParserException, ClientException {
        final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AllUsers</URI></Grantee><Permission>WRITE</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AllUsers</URI></Grantee><Permission>READ</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "124");
                        response.addHeader("Content-Type", "application/xml");
                        response.setContent(body.getBytes("UTF-8"));
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        Acl acl = client.getBucketACL("bucket");

        assertEquals(acl, Acl.PUBLIC_READ_WRITE);
    }

    @Test
    public void testGetBucketACLPublicRead() throws IOException, XmlPullParserException, ClientException {
        final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AllUsers</URI></Grantee><Permission>READ</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "124");
                        response.addHeader("Content-Type", "application/xml");
                        response.setContent(body.getBytes("UTF-8"));
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        Acl acl = client.getBucketACL("bucket");

        assertEquals(acl, Acl.PUBLIC_READ);
    }

    @Test
    public void testGetBucketACLAuthenticatedRead() throws IOException, XmlPullParserException, ClientException {
        final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AuthenticatedUsers</URI></Grantee><Permission>READ</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "124");
                        response.addHeader("Content-Type", "application/xml");
                        response.setContent(body.getBytes("UTF-8"));
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        Acl acl = client.getBucketACL("bucket");

        assertEquals(acl, Acl.AUTHENTICATED_READ);
    }

    @Test
    public void testGetBucketACLPrivate() throws IOException, XmlPullParserException, ClientException {
        final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "124");
                        response.addHeader("Content-Type", "application/xml");
                        response.setContent(body.getBytes("UTF-8"));
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        Acl acl = client.getBucketACL("bucket");

        assertEquals(acl, Acl.PRIVATE);
    }

    @Test(expected = InvalidAclNameException.class)
    public void testSetNullAclFails() throws IOException, ClientException {
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        client.makeBucket("bucket");
        client.setBucketACL("bucket", null);
        throw new RuntimeException("Expected exception did not fire");
    }


    @Test(expected = BucketExistsException.class)
    public void testMakeBucketFails() throws IOException, XmlPullParserException, ClientException {
        final ErrorResponse errResponse = new ErrorResponse();
        errResponse.setCode("BucketAlreadyExists");
        errResponse.setMessage("Bucket Already Exists");
        errResponse.setRequestID("1");
        errResponse.setResource("/bucket");
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(409); // status conflict
                        response.setContent(errResponse.toString());
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        client.makeBucket("bucket", Acl.PUBLIC_READ);
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test
    public void testPutSmallObject() throws IOException, NoSuchAlgorithmException, XmlPullParserException, ClientException {
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
                        response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
                        response.setStatusCode(200);
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);

        String inputString = "hello world";
        ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

        client.putObject("bucket", "key", "application/octet-stream", 11, data);
    }

    // this case only occurs for minio object storage
    @Test(expected = ObjectExistsException.class)
    public void testPutSmallObjectFails() throws IOException, NoSuchAlgorithmException, XmlPullParserException, ClientException {
        final ErrorResponse errResponse = new ErrorResponse();
        errResponse.setCode("MethodNotAllowed");
        errResponse.setMessage("The specified method is not allowed against this resource.");
        errResponse.setRequestID("1");
        errResponse.setResource("/bucket/key");
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(405); // method not allowed set by minio object storage
                        response.setContent(errResponse.toString());
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);

        String inputString = "hello world";
        ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

        client.putObject("bucket", "key", "application/octet-stream", 11, data);
        throw new RuntimeException("Expected exception did not fire");
    }

    // this case only occurs for minio object storage
//    @Test(expected = ObjectExistsException.class)
    @Test(expected = DataSizeMismatchException.class)
    public void testPutIncompleteSmallPut() throws IOException, NoSuchAlgorithmException, XmlPullParserException, ClientException {
        final ErrorResponse errResponse = new ErrorResponse();
        errResponse.setCode("MethodNotAllowed");
        errResponse.setMessage("The specified method is not allowed against this resource.");
        errResponse.setRequestID("1");
        errResponse.setResource("/bucket/key");
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(405); // method not allowed set by minio object storage
                        response.setContent(errResponse.toString());
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);

        String inputString = "hello worl";
        ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

        client.putObject("bucket", "key", "application/octet-stream", 11, data);
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test(expected = DataSizeMismatchException.class)
    public void testPutOversizedSmallPut() throws IOException, NoSuchAlgorithmException, XmlPullParserException, ClientException {
        final ErrorResponse errResponse = new ErrorResponse();
        errResponse.setCode("MethodNotAllowed");
        errResponse.setMessage("The specified method is not allowed against this resource.");
        errResponse.setRequestID("1");
        errResponse.setResource("/bucket/key");
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.setStatusCode(405); // method not allowed set by minio object storage
                        response.setContent(errResponse.toString());
                        return response;
                    }
                };
            }
        };

        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);

        String inputString = "how long is a piece of string? too long!";
        ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

        client.putObject("bucket", "key", "application/octet-stream", 11, data);
        throw new RuntimeException("Expected exception did not fire");
    }

    @Test
    public void testSigningKey() throws IOException, NoSuchAlgorithmException, InvalidKeyException, ClientException {
        MockHttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        Map<String, List<String>> headers = this.getHeaders();
                        for (String s : headers.keySet()) {
                            System.out.println("-" + s);
                        }
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
                        response.addHeader("Content-Length", "5080");
                        response.addHeader("Content-Type", "application/octet-stream");
                        response.addHeader("ETag", "\"a670520d9d36833b3e28d1e4b73cbe22\"");
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
        String contentType = "application/octet-stream";
        ObjectStat expectedStatInfo = new ObjectStat("bucket", "key", expectedDate.getTime(), 5080, "a670520d9d36833b3e28d1e4b73cbe22", contentType);

        // get request
        Client client = Client.getClient("http://localhost:9000");
        client.setTransport(transport);
        client.setKeys("foo", "bar");
        ObjectStat objectStatInfo = client.statObject("bucket", "key");

        assertEquals(expectedStatInfo, objectStatInfo);
    }
}
