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
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
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

  @Test(expected = IllegalArgumentException.class)
  public void testEndpoint1() throws MinioException {
    MinioClient.builder().endpoint((String) null).build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndpoint2() throws MinioException {
    MinioClient.builder().endpoint("http://play.min.io/mybucket").build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndpoint3() throws MinioException {
    MinioClient.builder().endpoint("minio-.example.com").build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndpoint4() throws MinioException {
    MinioClient.builder().endpoint("-minio.example.com").build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndpoint5() throws MinioException {
    MinioClient.builder().endpoint("minio..example.com").build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndpoint6() throws MinioException {
    MinioClient.builder().endpoint("minio._.com").build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndpoint7() throws MinioException {
    MinioClient.builder().endpoint("https://s3.amazonaws.com.cn").build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPort1() throws MinioException {
    MinioClient.builder().endpoint("play.min.io", 0, false).build();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPort2() throws MinioException {
    MinioClient.builder().endpoint("play.min.io", 70000, false).build();
    Assert.fail("exception should be thrown");
  }

  @Test
  public void testAwsEndpoints()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = null;
    String url = null;

    // virtual-style checks.
    client = MinioClient.builder().endpoint("https://s3.amazonaws.com").build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3.us-east-1.amazonaws.com/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3.us-east-2.amazonaws.com")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3.us-east-2.amazonaws.com/myobject", url);

    client = MinioClient.builder().endpoint("https://s3-accelerate.amazonaws.com").build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3-accelerate.amazonaws.com/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3.dualstack.ca-central-1.amazonaws.com")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3.dualstack.ca-central-1.amazonaws.com/myobject", url);

    client =
        MinioClient.builder().endpoint("https://s3-accelerate.dualstack.amazonaws.com").build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3-accelerate.dualstack.amazonaws.com/myobject", url);

    // path-style checks.
    client = MinioClient.builder().endpoint("https://s3.amazonaws.com").build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3.us-east-1.amazonaws.com/mybucket/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3.us-east-2.amazonaws.com")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3.us-east-2.amazonaws.com/mybucket/myobject", url);

    client = MinioClient.builder().endpoint("https://s3-accelerate.amazonaws.com").build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3-accelerate.amazonaws.com/mybucket/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3.dualstack.ca-central-1.amazonaws.com")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3.dualstack.ca-central-1.amazonaws.com/mybucket/myobject", url);

    client =
        MinioClient.builder().endpoint("https://s3-accelerate.dualstack.amazonaws.com").build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3-accelerate.dualstack.amazonaws.com/mybucket/myobject", url);

    // China region.
    // virtual-style checks.
    client =
        MinioClient.builder()
            .endpoint("https://s3.cn-north-1.amazonaws.com.cn")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3.cn-north-1.amazonaws.com.cn/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3-accelerate.amazonaws.com.cn")
            .region("cn-north-1")
            .build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3-accelerate.amazonaws.com.cn/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3.dualstack.cn-northwest-1.amazonaws.com.cn")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals(
        "https://mybucket.s3.dualstack.cn-northwest-1.amazonaws.com.cn/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3-accelerate.dualstack.amazonaws.com.cn")
            .region("cn-north-1")
            .build();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://mybucket.s3-accelerate.dualstack.amazonaws.com.cn/myobject", url);

    // path-style checks.
    client =
        MinioClient.builder()
            .endpoint("https://s3.cn-north-1.amazonaws.com.cn")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3.cn-north-1.amazonaws.com.cn/mybucket/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3-accelerate.amazonaws.com.cn")
            .region("cn-north-1")
            .build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3-accelerate.amazonaws.com.cn/mybucket/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3.dualstack.cn-northwest-1.amazonaws.com.cn")
            .credentials("myaccesskey", "mysecretkey")
            .build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals(
        "https://s3.dualstack.cn-northwest-1.amazonaws.com.cn/mybucket/myobject", url);

    client =
        MinioClient.builder()
            .endpoint("https://s3-accelerate.dualstack.amazonaws.com.cn")
            .region("cn-north-1")
            .build();
    client.disableVirtualStyleEndpoint();
    url = client.getObjectUrl("mybucket", "myobject");
    Assert.assertEquals("https://s3-accelerate.dualstack.amazonaws.com.cn/mybucket/myobject", url);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBucketName1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl(null, "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName3()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("a", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName4()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl(
        "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName5()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("a..b", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName6()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("a_b", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = InvalidBucketNameException.class)
  public void testBucketName7()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("a#b", "myobject");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("abc", null);
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("abc", "");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName3()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("abc", "a/./b");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testObjectName4()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObjectUrl("abc", "a/../b");
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReadSse1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    MinioClient client = MinioClient.builder().endpoint("http://play.min.io:9000").build();
    client.statObject(
        StatObjectArgs.builder()
            .bucket("mybucket")
            .object("myobject")
            .ssec(ServerSideEncryption.withCustomerKey(keyGen.generateKey()))
            .build());
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteSse1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("http://play.min.io:9000").build();
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    client.putObject(
        PutObjectArgs.builder().bucket("mybucket").object("myobject").stream(
                new ByteArrayInputStream(new byte[] {}), 0, -1)
            .sse(ServerSideEncryption.withCustomerKey(keyGen.generateKey()))
            .build());
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteSse2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("http://play.min.io:9000").build();
    Map<String, String> myContext = new HashMap<>();
    myContext.put("key1", "value1");
    client.putObject(
        PutObjectArgs.builder().bucket("mybucket").object("myobject").stream(
                new ByteArrayInputStream(new byte[] {}), 0, -1)
            .sse(ServerSideEncryption.withManagedKeys("keyId", myContext))
            .build());
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

    MinioClient client = MinioClient.builder().endpoint(server.url("")).build();
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

    MinioClient client = MinioClient.builder().endpoint(server.url("")).build();
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

    MinioClient client = MinioClient.builder().endpoint(server.url("")).build();
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

    MinioClient client = MinioClient.builder().endpoint(server.url("")).build();
    client.listBuckets();
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeBucketRegionConflicts()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client =
        MinioClient.builder()
            .endpoint("http://play.min.io:9000")
            .credentials("foo", "bar")
            .region("us-east-1")
            .build();
    client.makeBucket(MakeBucketArgs.builder().bucket("mybucket").region("us-west-2").build());
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidExpiresRange1()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket("mybucket")
            .object("myobject")
            .expiry(0)
            .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidExpiresRange2()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket("mybucket")
            .object("myobject")
            .expiry(8 * 24 * 3600)
            .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectNegativeOffset()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObject(
        GetObjectArgs.builder()
            .bucket("mybucket")
            .object("myobject")
            .offset(-1L)
            .length(5L)
            .build());
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectNegativeLength()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObject(
        GetObjectArgs.builder()
            .bucket("mybucket")
            .object("myobject")
            .offset(0L)
            .length(-5L)
            .build());
    Assert.fail("exception should be thrown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectZeroLength()
      throws NoSuchAlgorithmException, IOException, InvalidKeyException, MinioException {
    MinioClient client = MinioClient.builder().endpoint("https://play.min.io:9000").build();
    client.getObject(
        GetObjectArgs.builder()
            .bucket("mybucket")
            .object("myobject")
            .offset(0L)
            .length(0L)
            .build());
    Assert.fail("exception should be thrown");
  }
}
