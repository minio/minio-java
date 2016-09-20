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
import com.google.gson.Gson;
import com.squareup.okhttp.mockwebserver.MockResponse;

import okio.Buffer;

import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;
import io.minio.messages.Owner;
import io.minio.policy.*;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class MinioClientTest {
  private static final String EXPECTED_EXCEPTION_DID_NOT_FIRE = "Expected exception did not fire";
  private static final String BUCKET = "bucket";
  private static final String CONTENT_LENGTH = "Content-Length";
  private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String MON_04_MAY_2015_07_58_51_GMT = "Mon, 04 May 2015 07:58:51 GMT";
  private static final String LAST_MODIFIED = "Last-Modified";
  private static final String HELLO_WORLD = "hello world";
  private static final String HELLO = "hello";
  private static final String BYTES = "bytes";
  private static final String ACCEPT_RANGES = "Accept-Ranges";
  private static final String CONTENT_RANGE = "Content-Range";
  private static final String SUN_29_JUN_2015_22_01_10_GMT = "Sun, 29 Jun 2015 22:01:10 GMT";
  private static final String MON_04_MAY_2015_07_58_51_UTC = "Mon, 04 May 2015 07:58:51 UTC";
  private static final String BUCKET_KEY = "/bucket/key";
  private static final String MD5_HASH_STRING = "\"5eb63bbbe01eeed093cb22bb8f5acdc3\"";
  private static final Gson gson = new Gson();

  @Test()
  public void setUserAgentOnceSet() throws IOException, MinioException {
    String expectedHost = "example.com";
    MinioClient client = new MinioClient("http://" + expectedHost + "/");
    client.setAppInfo("testApp", "2.0.3");
  }

  @Test(expected = MinioException.class)
  public void newClientWithPathFails() throws MinioException {
    new MinioClient("http://example.com/path");
    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test(expected = NullPointerException.class)
  public void newClientWithNullUrlFails() throws NullPointerException, MinioException {
    URL url = null;
    new MinioClient(url);
    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test(expected = MinioException.class)
  public void newClientWithNullStringFails() throws InvalidArgumentException, MinioException {
    String url = null;
    new MinioClient(url);
    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test(expected = ErrorResponseException.class)
  public void testForbidden()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(403));

    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.statObject(BUCKET, "key");

    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test(expected = ErrorResponseException.class)
  public void getMissingObjectHeaders()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(404));

    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.statObject(BUCKET, "key");

    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test
  public void testGetObjectHeaders()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setResponseCode(200);
    response.setHeader("Date", "Sun, 05 Jun 2015 22:01:10 GMT");
    response.setHeader(CONTENT_LENGTH, "5080");
    response.setHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    response.setHeader("ETag", "\"a670520d9d36833b3e28d1e4b73cbe22\"");
    response.setHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_GMT);

    server.enqueue(response);
    server.start();

    // build expected request
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.clear();
    expectedDate.setTimeZone(TimeZone.getTimeZone("GMT"));
    expectedDate.set(2015, Calendar.MAY, 4, 7, 58, 51);
    ObjectStat expectedStatInfo = new ObjectStat(BUCKET, "key",
                                                 expectedDate.getTime(),
                                                 5080,
                                                 "a670520d9d36833b3e28d1e4b73cbe22",
            APPLICATION_OCTET_STREAM);

    // get request
    MinioClient client = new MinioClient(server.url(""));
    ObjectStat objectStatInfo = client.statObject(BUCKET, "key");

    assertEquals(expectedStatInfo, objectStatInfo);
  }

  @Test(expected = InvalidExpiresRangeException.class)
  public void testPresignGetObjectFail()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    server.start();

    // get request
    MinioClient client = new MinioClient(server.url(""));
    client.presignedGetObject(BUCKET, "key", 604801);
  }

  @Test
  public void testPresignGetObject()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    server.start();

    // get request
    MinioClient client = new MinioClient(server.url(""));
    String presignedObjectUrl = client.presignedGetObject(BUCKET, "key");
    assertEquals(presignedObjectUrl.isEmpty(), false);
  }

  @Test
  public void testGetObject()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    final String expectedObject = HELLO_WORLD;

    response.addHeader("Date", "Sun, 05 Jun 2015 22:01:10 GMT");
    response.addHeader(CONTENT_LENGTH, "5080");
    response.addHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_GMT);
    response.setResponseCode(200);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.url(""));
    InputStream object = client.getObject(BUCKET, "key");
    byte[] result = new byte[20];
    int read = object.read(result);
    result = Arrays.copyOf(result, read);
    assertEquals(expectedObject, new String(result, StandardCharsets.UTF_8));
  }

  @Test
  public void testPartialObject()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    final String expectedObject = HELLO;
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader(CONTENT_LENGTH, "5");
    response.addHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_GMT);
    response.addHeader(ACCEPT_RANGES, BYTES);
    response.addHeader(CONTENT_RANGE, "0-4/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.url(""));
    InputStream object = client.getObject(BUCKET, "key", 0L, 5L);
    byte[] result = new byte[20];
    int read = object.read(result);
    result = Arrays.copyOf(result, read);
    assertEquals(expectedObject, new String(result, StandardCharsets.UTF_8));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testGetObjectOffsetIsNegativeReturnsError()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    final String expectedObject = HELLO;
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.addHeader(CONTENT_LENGTH, "5");
    response.addHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_GMT);
    response.addHeader(ACCEPT_RANGES, BYTES);
    response.addHeader(CONTENT_RANGE, "0-4/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.url(""));
    client.getObject(BUCKET, "key", -1L, 5L);
    Assert.fail("Should of thrown an exception");
  }

  @Test(expected = InvalidArgumentException.class)
  public void testGetObjectLengthIsZeroReturnsError()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    final String expectedObject = HELLO;
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader(CONTENT_LENGTH, "5");
    response.addHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_GMT);
    response.addHeader(ACCEPT_RANGES, BYTES);
    response.addHeader(CONTENT_RANGE, "0-4/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.url(""));
    client.getObject(BUCKET, "key", 0L, 0L);
    Assert.fail("Should of thrown an exception");
  }

  /**
   * test GetObjectWithOffset.
   */
  public void testGetObjectWithOffset()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    final String expectedObject = "world";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader(CONTENT_LENGTH, "6");
    response.addHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_GMT);
    response.addHeader(ACCEPT_RANGES, BYTES);
    response.addHeader(CONTENT_RANGE, "5-10/11");
    response.setResponseCode(206);
    response.setBody(new Buffer().writeUtf8(expectedObject));

    server.enqueue(response);
    server.start();

    // get request
    MinioClient client = new MinioClient(server.url(""));
    InputStream object = client.getObject(BUCKET, "key", 6);
    byte[] result = new byte[5];
    int read = object.read(result);
    result = Arrays.copyOf(result, read);
    assertEquals(expectedObject, new String(result, StandardCharsets.UTF_8));
  }

  @Test
  public void testListObjects()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    final String body = "<ListBucketResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Name>bucket</Name><Prefix></Prefix><Marker></Marker><MaxKeys>1000</MaxKeys><Delimiter></Delimiter><IsTruncated>false</IsTruncated><Contents><Key>key</Key><LastModified>2015-05-05T02:21:15.716Z</LastModified><ETag>\"5eb63bbbe01eeed093cb22bb8f5acdc3\"</ETag><Size>11</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents><Contents><Key>key2</Key><LastModified>2015-05-05T20:36:17.498Z</LastModified><ETag>\"2a60eaffa7a82804bdc682ce1df6c2d4\"</ETag><Size>1661</Size><StorageClass>STANDARD</StorageClass><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner></Contents></ListBucketResult>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.addHeader(CONTENT_LENGTH, "414");
    response.addHeader(CONTENT_TYPE, "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    Iterator<Result<Item>> objectsInBucket = client.listObjects(BUCKET).iterator();

    Item item = objectsInBucket.next().get();
    assertEquals("key", item.objectName());
    assertEquals(11, item.objectSize());
    assertEquals("STANDARD", item.storageClass());

    Calendar expectedDate = Calendar.getInstance();
    expectedDate.clear();
    expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
    expectedDate.set(2015, Calendar.MAY, 5, 2, 21, 15);
    expectedDate.set(Calendar.MILLISECOND, 716);
    assertEquals(expectedDate.getTime(), item.lastModified());

    Owner owner = item.owner();
    assertEquals("minio", owner.id());
    assertEquals("minio", owner.displayName());
  }

  @Test
  public void testListBuckets()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException,
           ParseException {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    final String body = "<ListAllMyBucketsResult xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><Owner><ID>minio</ID><DisplayName>minio</DisplayName></Owner><Buckets><Bucket><Name>bucket</Name><CreationDate>2015-05-05T20:35:51.410Z</CreationDate></Bucket><Bucket><Name>foo</Name><CreationDate>2015-05-05T20:35:47.170Z</CreationDate></Bucket></Buckets></ListAllMyBucketsResult>";
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.addHeader(CONTENT_LENGTH, "351");
    response.addHeader(CONTENT_TYPE, "application/xml");
    response.setBody(new Buffer().writeUtf8(body));
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    Iterator<Bucket> buckets = client.listBuckets().iterator();

    Bucket bucket = buckets.next();
    assertEquals(BUCKET, bucket.name());
    assertEquals(dateFormat.parse("2015-05-05T20:35:51.410Z"), bucket.creationDate());

    bucket = buckets.next();
    assertEquals("foo", bucket.name());
    assertEquals(dateFormat.parse("2015-05-05T20:35:47.170Z"), bucket.creationDate());

    Calendar expectedDate = Calendar.getInstance();
    expectedDate.clear();
    expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
    expectedDate.set(2015, Calendar.MAY, 5, 20, 35, 47);
    expectedDate.set(Calendar.MILLISECOND, 170);
    assertEquals(expectedDate.getTime(), bucket.creationDate());
  }

  @Test
  public void testBucketExists()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    boolean result = client.bucketExists(BUCKET);

    assertEquals(true, result);
  }

  @Test
  public void testBucketExistsFails()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(404);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    boolean result = client.bucketExists(BUCKET);

    assertEquals(false, result);
  }

  @Test
  public void testMakeBucket()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response1 = new MockResponse();
    MockResponse response2 = new MockResponse();

    response1.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response1.setResponseCode(200);

    response2.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response2.setResponseCode(200);

    server.enqueue(response1);
    server.enqueue(response2);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.makeBucket(BUCKET);
  }


  @Test(expected = ErrorResponseException.class)
  public void testMakeBucketFails()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse(ErrorCode.BUCKET_ALREADY_EXISTS, null, null, "/bucket", "1",
                                                        null);

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(409); // status conflict
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.makeBucket(BUCKET);

    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test
  public void testPutSmallObject()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_UTC);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    String inputString = HELLO_WORLD;
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));

    client.putObject(BUCKET, "key", data, 11, APPLICATION_OCTET_STREAM);
  }

  // this case only occurs for minio cloud storage
  @Test(expected = ErrorResponseException.class)
  public void testPutSmallObjectFails()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse(ErrorCode.METHOD_NOT_ALLOWED, null, null, BUCKET_KEY, "1",
                                                        null);

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(405); // method not allowed set by minio cloud storage
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    String inputString = HELLO_WORLD;
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));

    client.putObject(BUCKET, "key", data, 11, APPLICATION_OCTET_STREAM);
    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test(expected = EOFException.class)
  public void testPutIncompleteSmallPut()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse(ErrorCode.METHOD_NOT_ALLOWED, null, null, BUCKET_KEY, "1",
                                                        null);

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(405); // method not allowed set by minio cloud storage
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    String inputString = "hello worl";
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));

    client.putObject(BUCKET, "key", data, 11, APPLICATION_OCTET_STREAM);
    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test(expected = ErrorResponseException.class)
  public void testPutOversizedSmallPut()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    final ErrorResponse errResponse = new ErrorResponse(ErrorCode.METHOD_NOT_ALLOWED, null, null, BUCKET_KEY, "1",
                                                        null);

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(405); // method not allowed set by minio cloud storage
    response.setBody(new Buffer().writeUtf8(errResponse.toString()));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    String inputString = "how long is a piece of string? too long!";
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));

    client.putObject(BUCKET, "key", data, 11, APPLICATION_OCTET_STREAM);
    throw new RuntimeException(EXPECTED_EXCEPTION_DID_NOT_FIRE);
  }

  @Test
  public void testSpecialCharsNameWorks()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_UTC);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    String inputString = HELLO_WORLD;
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));

    byte[] ascii = new byte[255];
    for (int i = 1; i < 256; i++) {
      ascii[i - 1] = (byte) i;
    }
    client.putObject(BUCKET, "世界" + new String(ascii, "UTF-8"), data, 11, null);
  }

  @Test
  public void testNullContentTypeWorks()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_UTC);
    response.addHeader("ETag", MD5_HASH_STRING);
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    String inputString = HELLO_WORLD;
    ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));

    client.putObject(BUCKET, "key", data, 11, null);
  }

  @Test
  public void testSigningKey()
    throws NoSuchAlgorithmException, InvalidKeyException, IOException, XmlPullParserException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.addHeader(CONTENT_LENGTH, "5080");
    response.addHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    response.addHeader("ETag", "\"a670520d9d36833b3e28d1e4b73cbe22\"");
    response.addHeader(LAST_MODIFIED, MON_04_MAY_2015_07_58_51_UTC);
    response.setResponseCode(200);

    server.enqueue(response);
    server.start();

    // build expected request
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.clear();
    expectedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
    expectedDate.set(2015, Calendar.MAY, 4, 7, 58, 51);
    String contentType = APPLICATION_OCTET_STREAM;
    ObjectStat expectedStatInfo = new ObjectStat(BUCKET, "key", expectedDate.getTime(), 5080,
                                                 "a670520d9d36833b3e28d1e4b73cbe22", contentType);

    // get request
    MinioClient client = new MinioClient(server.url(""), "foo", "bar");

    ObjectStat objectStatInfo = client.statObject(BUCKET, "key");
    assertEquals(expectedStatInfo, objectStatInfo);
  }

  @Test
  public void testSetBucketPolicyReadOnly()
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException,
           NoSuchBucketPolicyException, MinioException {

    // Create Mock web server and mocked responses
    MockWebServer server = new MockWebServer();
    MockResponse response1 = new MockResponse();
    MockResponse response2 = new MockResponse();

    response1.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response1.setResponseCode(200);
    /* Second response is expected to return a blank policy as
     * we are creating a new bucket, so create a blank policy */
    BucketAccessPolicy none = BucketAccessPolicy.none();
    /* serialize the object into its equivalent Json representation and set it in the
     * response body */
    response1.setBody(gson.toJson(none));

    response2.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response2.setResponseCode(200);

    server.enqueue(response1);
    server.enqueue(response2);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    // Set the bucket policy for a bucket
    client.setBucketPolicy(BUCKET, "uploads", BucketPolicy.ReadOnly);
  }

  @Test
  public void testSetBucketPolicyReadWrite()
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException,
           NoSuchBucketPolicyException, MinioException {

    // Create Mock web server and mocked responses
    MockWebServer server = new MockWebServer();
    MockResponse response1 = new MockResponse();
    MockResponse response2 = new MockResponse();

    response1.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response1.setResponseCode(200);
    /* Second response is expected to return a blank policy as
     * we are creating a new bucket, so create a blank policy */
    BucketAccessPolicy none = BucketAccessPolicy.none();
    /* serialize the object into its equivalent Json representation and set it in the
     * response body */
    response1.setBody(gson.toJson(none));

    response2.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response2.setResponseCode(200);

    server.enqueue(response1);
    server.enqueue(response2);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    // Set the bucket policy for a bucket
    client.setBucketPolicy(BUCKET, "uploads", BucketPolicy.ReadWrite);
  }

  @Test
  public void testSetBucketPolicyWriteOnly()
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException,
           NoSuchBucketPolicyException, MinioException {

    // Create Mock web server and mocked responses
    MockWebServer server = new MockWebServer();
    MockResponse response1 = new MockResponse();
    MockResponse response2 = new MockResponse();

    response1.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response1.setResponseCode(200);
    /* Second response is expected to return a blank policy as
     * we are creating a new bucket, so create a blank policy */
    BucketAccessPolicy none = BucketAccessPolicy.none();
    /* serialize the object into its equivalent Json representation and set it in the
     * response body */
    response1.setBody(gson.toJson(none));

    response2.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response2.setResponseCode(200);

    server.enqueue(response1);
    server.enqueue(response2);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    // Set the bucket policy for a bucket
    client.setBucketPolicy(BUCKET, "uploads", BucketPolicy.WriteOnly);
  }

  @Test
  public void testGetBucketPolicyReadOnly()
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException,
           NoSuchBucketPolicyException, MinioException {

    // Create Mock web server and mocked responses
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(200);
    /* Second response is expected to return a blank policy as
     * we are creating a new bucket, so create a blank policy */
    BucketAccessPolicy none = BucketAccessPolicy.none();
    // Generate statements
    List<Statement> generatedStatements = BucketAccessPolicy.generatePolicyStatements(BucketPolicy.ReadOnly,
                                                                                      BUCKET, "uploads");
    // Add statements to the BucketAccessPolicy
    none.setStatements(generatedStatements);
    /* serialize the object into its equivalent Json representation and set it in the
     * response body */
    response.setBody(gson.toJson(none));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    // Get the bucket policy for the new bucket and check
    BucketPolicy responseBucketPolicy = client.getBucketPolicy(BUCKET, "uploads");
    assertEquals(BucketPolicy.ReadOnly, responseBucketPolicy);
  }

  @Test
  public void testGetBucketPolicyReadWrite()
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException,
           NoSuchBucketPolicyException, MinioException {

    // Create Mock web server and mocked responses
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(200);
    /* Second response is expected to return a blank policy as
     * we are creating a new bucket, so create a blank policy */
    BucketAccessPolicy none = BucketAccessPolicy.none();
    // Generate statements
    List<Statement> generatedStatements = BucketAccessPolicy.generatePolicyStatements(BucketPolicy.ReadWrite,
                                                                                      BUCKET, "uploads");
    // Add statements to the BucketAccessPolicy
    none.setStatements(generatedStatements);
    /* serialize the object into its equivalent Json representation and set it in the
     * response body */
    response.setBody(gson.toJson(none));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    // Get the bucket policy for the new bucket and check
    BucketPolicy responseBucketPolicy = client.getBucketPolicy(BUCKET, "uploads");
    assertEquals(BucketPolicy.ReadWrite, responseBucketPolicy);
  }

  @Test
  public void testGetBucketPolicyWriteOnly()
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException,
           NoSuchBucketPolicyException, MinioException {

    // Create Mock web server and mocked responses
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();

    response.addHeader("Date", SUN_29_JUN_2015_22_01_10_GMT);
    response.setResponseCode(200);
    /* Second response is expected to return a blank policy as
     * we are creating a new bucket, so create a blank policy */
    BucketAccessPolicy none = BucketAccessPolicy.none();
    // Generate statements
    List<Statement> generatedStatements = BucketAccessPolicy.generatePolicyStatements(BucketPolicy.WriteOnly,
                                                                                      BUCKET, "uploads");
    // Add statements to the BucketAccessPolicy
    none.setStatements(generatedStatements);
    /* serialize the object into its equivalent Json representation and set it in the
     * response body */
    response.setBody(gson.toJson(none));

    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));

    // Get the bucket policy for the new bucket and check
    BucketPolicy responseBucketPolicy = client.getBucketPolicy(BUCKET, "uploads");
    assertEquals(BucketPolicy.WriteOnly, responseBucketPolicy);
  }
}
