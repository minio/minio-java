/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

package io.minio;


import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.MockResponse;

import okio.Buffer;

import io.minio.acl.Acl;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;
import io.minio.messages.Owner;

import org.junit.Assert;
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
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class MinioClientTest {
  @Test()
  public void instantiateNewClient() throws MalformedURLException, MinioException {
    String expectedHost = "example.com";
    MinioClient client = new MinioClient("http://" + expectedHost);

    URL url = client.getUrl();
    // check schema
    assertEquals("http", url.getProtocol());
    // check host
    assertEquals(expectedHost, url.getHost());
  }

  @Test()
  public void instantiateNewClientWithTrailingSlash() throws MalformedURLException, MinioException {
    String expectedHost = "example.com";
    MinioClient client = new MinioClient("http://" + expectedHost + "/");

    URL url = client.getUrl();
    // check schema
    assertEquals("http", url.getProtocol());
    // check host
    assertEquals(expectedHost, url.getHost());
  }

  @Test()
  public void setUserAgentOnceSet() throws IOException, MinioException {
    String expectedHost = "example.com";
    MinioClient client = new MinioClient("http://" + expectedHost + "/");
    client.setAppInfo("testApp", "1.0.0");
  }

  @Test(expected = MinioException.class)
  public void newClientWithPathFails() throws MinioException {
    new MinioClient("http://example.com/path");
    throw new RuntimeException("Expected exception did not fire");
  }

  @Test(expected = NullPointerException.class)
  public void newClientWithNullUrlFails() throws NullPointerException, MinioException {
    URL url = null;
    new MinioClient(url);
    throw new RuntimeException("Expected exception did not fire");
  }

  @Test(expected = MinioException.class)
  public void newClientWithNullStringFails() throws InvalidArgumentException, MinioException {
    String url = null;
    new MinioClient(url);
    throw new RuntimeException("Expected exception did not fire");
  }

  @Test(expected = ErrorResponseException.class)
  public void testForbidden() throws XmlPullParserException, IOException, MinioException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(403));

    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    client.statObject("bucket", "key");

    throw new RuntimeException("Expected exception did not fire");
  }

  @Test(expected = ErrorResponseException.class)
  public void getMissingObjectHeaders() throws XmlPullParserException, IOException, MinioException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(404));

    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    client.statObject("bucket", "key");

    throw new RuntimeException("Expected exception did not fire");
  }

  @Test
  public void testGetObjectHeaders()
    throws XmlPullParserException, IOException, NoSuchAlgorithmException, InvalidKeyException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setResponseCode(200);
    response.setHeader("Date", "Sun, 05 Jun 2015 22:01:10 GMT");
    response.setHeader("Content-Length", "5080");
    response.setHeader("Content-Type", "application/octet-stream");
    response.setHeader("ETag", "\"a670520d9d36833b3e28d1e4b73cbe22\"");
    response.setHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");

    server.enqueue(response);
    server.start();

    // build expected request
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.clear();
    expectedDate.setTimeZone(TimeZone.getTimeZone("GMT"));
    expectedDate.set(2015, Calendar.MAY, 4, 7, 58, 51);
    ObjectStat expectedStatInfo = new ObjectStat("bucket", "key",
                                                 expectedDate.getTime(),
                                                 5080,
                                                 "a670520d9d36833b3e28d1e4b73cbe22",
                                                 "application/octet-stream");

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    ObjectStat objectStatInfo = client.statObject("bucket", "key");

    assertEquals(expectedStatInfo, objectStatInfo);
  }

  @Test(expected = InvalidExpiresRangeException.class)
  public void testPresignGetObjectFail() throws IOException, InvalidKeyException, MinioException,
                                                NoSuchAlgorithmException, InvalidExpiresRangeException {
    MockWebServer server = new MockWebServer();
    server.start();

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    client.presignedGetObject("bucket", "key", 604801);
  }

  @Test
  public void testPresignGetObject() throws IOException, InvalidKeyException, MinioException, NoSuchAlgorithmException,
                                            InvalidExpiresRangeException {
    MockWebServer server = new MockWebServer();
    server.start();

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    String presignedObjectUrl = client.presignedGetObject("bucket", "key");
    assertEquals(presignedObjectUrl.isEmpty(), false);
  }

  @Test
  public void testGetObject() throws XmlPullParserException, IOException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    final String expectedObject = "hello world";

    response.addHeader("Date", "Sun, 05 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "5080");
    response.addHeader("Content-Type", "application/octet-stream");
    response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
    response.setResponseCode(200);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    InputStream object = client.getObject("bucket", "key");
    byte[] result = new byte[20];
    int read = object.read(result);
    result = Arrays.copyOf(result, read);
    assertEquals(expectedObject, new String(result, "UTF-8"));
  }

  @Test
  public void testPartialObject() throws XmlPullParserException, IOException, MinioException {
    final String expectedObject = "hello";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Content-Length", "5");
    response.addHeader("Content-Type", "application/octet-stream");
    response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
    response.addHeader("Accept-Ranges", "bytes");
    response.addHeader("Content-Range", "0-4/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    InputStream object = client.getPartialObject("bucket", "key", 0, 5);
    byte[] result = new byte[20];
    int read = object.read(result);
    result = Arrays.copyOf(result, read);
    assertEquals(expectedObject, new String(result, "UTF-8"));
  }

  @Test(expected = InvalidRangeException.class)
  public void testGetObjectOffsetIsNegativeReturnsError() throws XmlPullParserException, IOException, MinioException {
    final String expectedObject = "hello";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.addHeader("Content-Length", "5");
    response.addHeader("Content-Type", "application/octet-stream");
    response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
    response.addHeader("Accept-Ranges", "bytes");
    response.addHeader("Content-Range", "0-4/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    client.getPartialObject("bucket", "key", -1, 5);
    Assert.fail("Should of thrown an exception");
  }

  @Test(expected = InvalidRangeException.class)
  public void testGetObjectLengthIsZeroReturnsError() throws XmlPullParserException, IOException, MinioException {
    final String expectedObject = "hello";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Content-Length", "5");
    response.addHeader("Content-Type", "application/octet-stream");
    response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
    response.addHeader("Accept-Ranges", "bytes");
    response.addHeader("Content-Range", "0-4/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    client.getPartialObject("bucket", "key", 0, 0);
    Assert.fail("Should of thrown an exception");
  }

  /**
   * test GetObjectWithOffset.
   */
  public void testGetObjectWithOffset() throws XmlPullParserException, IOException, MinioException {
    final String expectedObject = "world";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Content-Length", "6");
    response.addHeader("Content-Type", "application/octet-stream");
    response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 GMT");
    response.addHeader("Accept-Ranges", "bytes");
    response.addHeader("Content-Range", "5-10/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.getUrl(""));
    InputStream object = client.getPartialObject("bucket", "key", 6);
    byte[] result = new byte[5];
    int read = object.read(result);
    result = Arrays.copyOf(result, read);
    assertEquals(expectedObject, new String(result, "UTF-8"));
  }

  @Test
  public void testListObjects() throws IOException, XmlPullParserException, ParseException, MinioException {
    final String body = "<ListBucketResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Name>bucket</Name><Prefix></Prefix><Marker></Marker><MaxKeys>1000</MaxKeys><Delimiter></Delimiter><IsTruncated>false</IsTruncated><Contents><Key>key</Key><LastModified>2015-05-05T02:21:15.716Z</LastModified><ETag>\"5eb63bbbe01eeed093cb22bb8f5acdc3\"</ETag><Size>11</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents><Contents><Key>key2</Key><LastModified>2015-05-05T20:36:17.498Z</LastModified><ETag>\"2a60eaffa7a82804bdc682ce1df6c2d4\"</ETag><Size>1661</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents></ListBucketResult>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "414");
    response.addHeader("Content-Type", "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
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
    assertEquals("minio", owner.getId());
    assertEquals("minio", owner.getDisplayName());
  }

  @Test
  public void testListBuckets() throws IOException, XmlPullParserException, ParseException, MinioException {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    final String body = "<ListAllMyBucketsResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner><Buckets><Bucket><Name>bucket</Name><CreationDate>2015-05-05T20:35:51.410Z</CreationDate></Bucket><Bucket><Name>foo</Name><CreationDate>2015-05-05T20:35:47.170Z</CreationDate></Bucket></Buckets></ListAllMyBucketsResult>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "351");
    response.addHeader("Content-Type", "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    Iterator<Bucket> buckets = client.listBuckets();

    Bucket bucket = buckets.next();
    assertEquals("bucket", bucket.getName());
    assertEquals(dateFormat.parse("2015-05-05T20:35:51.410Z"), bucket.getCreationDate());

    bucket = buckets.next();
    assertEquals("foo", bucket.getName());
    assertEquals(dateFormat.parse("2015-05-05T20:35:47.170Z"), bucket.getCreationDate());

    Calendar expectedDate = Calendar.getInstance();
    expectedDate.clear();
    expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
    expectedDate.set(2015, Calendar.MAY, 5, 20, 35, 47);
    expectedDate.set(Calendar.MILLISECOND, 170);
    assertEquals(expectedDate.getTime(), bucket.getCreationDate());
  }

  @Test
  public void testBucketExists() throws IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    boolean result = client.bucketExists("bucket");

    assertEquals(true, result);
  }

  @Test
  public void testBucketExistsFails() throws IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.setResponseCode(404);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    boolean result = client.bucketExists("bucket");

    assertEquals(false, result);
  }

  @Test
  public void testMakeBucket() throws IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response1 = new MockResponse();
    MockResponse response2 = new MockResponse();

    response1.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response1.setResponseCode(200);

    response2.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response2.setResponseCode(200);

    server.enqueue(response1);
    server.enqueue(response2);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    client.makeBucket("bucket", Acl.PUBLIC_READ);
    client.setBucketAcl("bucket", Acl.PRIVATE);
  }


  @Test
  public void testGetBucketAclPublicRw() throws IOException, XmlPullParserException, MinioException {
    final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AllUsers</URI></Grantee><Permission>WRITE</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AllUsers</URI></Grantee><Permission>READ</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "124");
    response.addHeader("Content-Type", "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    Acl acl = client.getBucketAcl("bucket");

    assertEquals(acl, Acl.PUBLIC_READ_WRITE);
  }

  @Test
  public void testGetBucketAclPublicRead() throws IOException, XmlPullParserException, MinioException {
    final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AllUsers</URI></Grantee><Permission>READ</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "124");
    response.addHeader("Content-Type", "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    Acl acl = client.getBucketAcl("bucket");

    assertEquals(acl, Acl.PUBLIC_READ);
  }

  @Test
  public void testGetBucketAclAuthenticatedRead() throws IOException, XmlPullParserException, MinioException {
    final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName><URI>http://acs.amazonaws.com/groups/global/AuthenticatedUsers</URI></Grantee><Permission>READ</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "124");
    response.addHeader("Content-Type", "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    Acl acl = client.getBucketAcl("bucket");

    assertEquals(acl, Acl.AUTHENTICATED_READ);
  }

  @Test
  public void testGetBucketAclPrivate() throws IOException, XmlPullParserException, MinioException {
    final String body = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\"><Owner><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID><DisplayName>CustomersName@amazon.com</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "124");
    response.addHeader("Content-Type", "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    Acl acl = client.getBucketAcl("bucket");

    assertEquals(acl, Acl.PRIVATE);
  }

  @Test(expected = InvalidAclNameException.class)
  public void testSetNullAclFails() throws XmlPullParserException, IOException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    client.makeBucket("bucket");
    client.setBucketAcl("bucket", null);

    throw new RuntimeException("Expected exception did not fire");
  }


  @Test(expected = ErrorResponseException.class)
  public void testMakeBucketFails() throws IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse();
    errResponse.setCode("BucketAlreadyExists");
    errResponse.setMessage("Bucket Already Exists");
    errResponse.setRequestId("1");
    errResponse.setResource("/bucket");

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.setResponseCode(409); // status conflict
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));
    client.makeBucket("bucket", Acl.PUBLIC_READ);

    throw new RuntimeException("Expected exception did not fire");
  }

  @Test
  public void testPutSmallObject() throws IOException, NoSuchAlgorithmException, XmlPullParserException,
                                          MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
    response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));

    String inputString = "hello world";
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

    client.putObject("bucket", "key", "application/octet-stream", 11, data);
  }

  // this case only occurs for minio cloud storage
  @Test(expected = ErrorResponseException.class)
  public void testPutSmallObjectFails() throws IOException, NoSuchAlgorithmException, XmlPullParserException,
                                               MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse();
    errResponse.setCode("MethodNotAllowed");
    errResponse.setMessage("The specified method is not allowed against this resource.");
    errResponse.setRequestId("1");
    errResponse.setResource("/bucket/key");

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.setResponseCode(405); // method not allowed set by minio cloud storage
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));

    String inputString = "hello world";
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

    client.putObject("bucket", "key", "application/octet-stream", 11, data);
    throw new RuntimeException("Expected exception did not fire");
  }

  @Test(expected = UnexpectedShortReadException.class)
  public void testPutIncompleteSmallPut() throws IOException, NoSuchAlgorithmException, XmlPullParserException,
                                                 MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse();
    errResponse.setCode("MethodNotAllowed");
    errResponse.setMessage("The specified method is not allowed against this resource.");
    errResponse.setRequestId("1");
    errResponse.setResource("/bucket/key");

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.setResponseCode(405); // method not allowed set by minio cloud storage
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));

    String inputString = "hello worl";
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

    client.putObject("bucket", "key", "application/octet-stream", 11, data);
    throw new RuntimeException("Expected exception did not fire");
  }

  @Test(expected = UnexpectedShortReadException.class)
  public void testPutOversizedSmallPut() throws IOException, NoSuchAlgorithmException, XmlPullParserException,
                                                MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse();
    errResponse.setCode("MethodNotAllowed");
    errResponse.setMessage("The specified method is not allowed against this resource.");
    errResponse.setRequestId("1");
    errResponse.setResource("/bucket/key");

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.setResponseCode(405); // method not allowed set by minio cloud storage
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));

    String inputString = "how long is a piece of string? too long!";
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

    client.putObject("bucket", "key", "application/octet-stream", 11, data);
    throw new RuntimeException("Expected exception did not fire");
  }

  @Test
  public void testNullContentTypeWorks() throws XmlPullParserException, IOException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
    response.addHeader("ETag", "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"");
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.getUrl(""));

    String inputString = "hello world";
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));

    client.putObject("bucket", "key", null, 11, data);
  }

  @Test
  public void testSigningKey()
    throws XmlPullParserException, IOException, NoSuchAlgorithmException, InvalidKeyException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", "Sun, 29 Jun 2015 22:01:10 GMT");
    response.addHeader("Content-Length", "5080");
    response.addHeader("Content-Type", "application/octet-stream");
    response.addHeader("ETag", "\"a670520d9d36833b3e28d1e4b73cbe22\"");
    response.addHeader("Last-Modified", "Mon, 04 May 2015 07:58:51 UTC");
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    // build expected request
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.clear();
    expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
    expectedDate.set(2015, Calendar.MAY, 4, 7, 58, 51);
    String contentType = "application/octet-stream";
    ObjectStat expectedStatInfo = new ObjectStat("bucket", "key", expectedDate.getTime(), 5080,
                                                 "a670520d9d36833b3e28d1e4b73cbe22", contentType);

    // get request
    MinioClient client = new MinioClient(server.getUrl(""), "foo", "bar");

    ObjectStat objectStatInfo = client.statObject("bucket", "key");
    assertEquals(expectedStatInfo, objectStatInfo);
  }
}
