/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015, 2016, 2017 MinIO, Inc.
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

import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.RegionConflictException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.KeyGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class MinioClientTest {
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String CONTENT_LENGTH = "Content-Length";

  @Test(expected = InvalidEndpointException.class)
  public void testEndpoint1() throws MinioException {
    new MinioClient((String) null);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidEndpointException.class)
  public void testEndpoint2() throws MinioException {
    new MinioClient("http://play.min.io/mybucket");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidEndpointException.class)
  public void testEndpoint3() throws MinioException {
    new MinioClient("minio-.example.com");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidEndpointException.class)
  public void testEndpoint4() throws MinioException {
    new MinioClient("-minio.example.com");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidEndpointException.class)
  public void testEndpoint5() throws MinioException {
    new MinioClient("minio..example.com");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidEndpointException.class)
  public void testEndpoint6() throws MinioException {
    new MinioClient("minio._.com");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidPortException.class)
  public void testPort1() throws MinioException {
    new MinioClient("play.min.io", -1, "", "", false);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidPortException.class)
  public void testPort2() throws MinioException {
    new MinioClient("play.min.io", 70000, "", "", false);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl(null, "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl(null, "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName3()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName4()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("a", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName5()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl(
        "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName6()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("a..b", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName7()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("a_b", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName8()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("a#b", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("abc", null);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("abc", "");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName3()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("abc", "a/./b");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName4()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObjectUrl("abc", "a/../b");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReadSse1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.statObject("mybucket", "myobject", ServerSideEncryption.atRest());
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReadSse2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    MinioClient client = new MinioClient("http://play.min.io:9000");
    client.statObject(
        "mybucket", "myobject", ServerSideEncryption.withCustomerKey(keyGen.generateKey()));
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteSse1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    PutObjectOptions options = new PutObjectOptions(0, -1);
    options.setSse(ServerSideEncryption.withCustomerKey(keyGen.generateKey()));
    MinioClient client = new MinioClient("http://play.min.io:9000");
    client.putObject("mybucket", "myobject", new ByteArrayInputStream(new byte[] {}), options);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteSse2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    Map<String, String> myContext = new HashMap<>();
    myContext.put("key1", "value1");
    PutObjectOptions options = new PutObjectOptions(0, -1);
    options.setSse(ServerSideEncryption.withManagedKeys("keyId", myContext));
    MinioClient client = new MinioClient("http://play.min.io:9000");
    client.putObject("mybucket", "myobject", new ByteArrayInputStream(new byte[] {}), options);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidResponseException.class)
  public void testInvalidResponse1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setResponseCode(403);
    response.setHeader(CONTENT_LENGTH, "13");
    response.setBody(new Buffer().writeUtf8("<html></html>"));
    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.listBuckets();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidResponseException.class)
  public void testInvalidResponse2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setResponseCode(403);
    response.setHeader(CONTENT_LENGTH, "13");
    response.setHeader(CONTENT_TYPE, "application/html");
    response.setBody(new Buffer().writeUtf8("<html></html>"));
    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.listBuckets();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidResponseException.class)
  public void testInvalidResponse3()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setResponseCode(403);
    response.setHeader(CONTENT_LENGTH, "13");
    response.setHeader(CONTENT_TYPE, "application/html;utf-8");
    response.setBody(new Buffer().writeUtf8("<html></html>"));
    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.listBuckets();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidResponseException.class)
  public void testInvalidResponse4()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setResponseCode(403);
    response.setHeader(CONTENT_LENGTH, "0");
    response.setHeader(CONTENT_TYPE, "application/xml;utf-8");
    response.setBody(new Buffer().writeUtf8(""));
    server.enqueue(response);
    server.start();

    MinioClient client = new MinioClient(server.url(""));
    client.listBuckets();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = RegionConflictException.class)
  public void testMakeBucketRegionConflicts()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("http://play.min.io:9000", "foo", "bar", "us-east-1");
    client.makeBucket("mybucket", "us-west-2");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidExpiresRangeException.class)
  public void testInvalidExpiresRange1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getPresignedObjectUrl(Method.GET, "mybucket", "myobject", 0, null);
  }

  @Test(expected = InvalidExpiresRangeException.class)
  public void testInvalidExpiresRange2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getPresignedObjectUrl(Method.GET, "mybucket", "myobject", 8 * 24 * 3600, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectNegativeOffset()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObject("mybucket", "myobject", -1L, 5L);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectNegativeLength()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObject("mybucket", "myobject", 0L, -5L);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectZeroLength()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = new MinioClient("https://play.min.io:9000");
    client.getObject("mybucket", "myobject", 0L, 0L);
    Assert.fail("exception should be thrown");
  }
}
