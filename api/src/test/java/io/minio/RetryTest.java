/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2026 MinIO, Inc.
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

import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.messages.ErrorResponse;
import java.io.IOException;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.Assert;
import org.junit.Test;

/** Unit and integration tests for the automatic retry mechanism. */
public class RetryTest {

  // ---------------------------------------------------------------------------
  // Retry.isRetryableS3Code tests
  // ---------------------------------------------------------------------------

  @Test
  public void testIsRetryableS3Code_retryable() {
    Assert.assertTrue(Retry.isRetryableS3Code("RequestError"));
    Assert.assertTrue(Retry.isRetryableS3Code("RequestTimeout"));
    Assert.assertTrue(Retry.isRetryableS3Code("Throttling"));
    Assert.assertTrue(Retry.isRetryableS3Code("ThrottlingException"));
    Assert.assertTrue(Retry.isRetryableS3Code("RequestLimitExceeded"));
    Assert.assertTrue(Retry.isRetryableS3Code("RequestThrottled"));
    Assert.assertTrue(Retry.isRetryableS3Code("InternalError"));
    Assert.assertTrue(Retry.isRetryableS3Code("ExpiredToken"));
    Assert.assertTrue(Retry.isRetryableS3Code("ExpiredTokenException"));
    Assert.assertTrue(Retry.isRetryableS3Code("SlowDown"));
    Assert.assertTrue(Retry.isRetryableS3Code("SlowDownWrite"));
    Assert.assertTrue(Retry.isRetryableS3Code("SlowDownRead"));
  }

  @Test
  public void testIsRetryableS3Code_notRetryable() {
    Assert.assertFalse(Retry.isRetryableS3Code("NoSuchKey"));
    Assert.assertFalse(Retry.isRetryableS3Code("NoSuchBucket"));
    Assert.assertFalse(Retry.isRetryableS3Code("AccessDenied"));
    Assert.assertFalse(Retry.isRetryableS3Code("InvalidBucketName"));
    Assert.assertFalse(Retry.isRetryableS3Code("RetryHead"));
    Assert.assertFalse(Retry.isRetryableS3Code("MethodNotAllowed"));
    Assert.assertFalse(Retry.isRetryableS3Code("PreconditionFailed"));
    Assert.assertFalse(Retry.isRetryableS3Code(""));
  }

  @Test
  public void testIsRetryableS3Code_null() {
    Assert.assertFalse(Retry.isRetryableS3Code(null));
  }

  // ---------------------------------------------------------------------------
  // Retry.isRetryableHttpCode tests
  // ---------------------------------------------------------------------------

  @Test
  public void testIsRetryableHttpCode_retryable() {
    Assert.assertTrue(Retry.isRetryableHttpCode(408));
    Assert.assertTrue(Retry.isRetryableHttpCode(429));
    Assert.assertTrue(Retry.isRetryableHttpCode(499));
    Assert.assertTrue(Retry.isRetryableHttpCode(500));
    Assert.assertTrue(Retry.isRetryableHttpCode(502));
    Assert.assertTrue(Retry.isRetryableHttpCode(503));
    Assert.assertTrue(Retry.isRetryableHttpCode(504));
    Assert.assertTrue(Retry.isRetryableHttpCode(520));
  }

  @Test
  public void testIsRetryableHttpCode_notRetryable() {
    Assert.assertFalse(Retry.isRetryableHttpCode(200));
    Assert.assertFalse(Retry.isRetryableHttpCode(201));
    Assert.assertFalse(Retry.isRetryableHttpCode(400));
    Assert.assertFalse(Retry.isRetryableHttpCode(403));
    Assert.assertFalse(Retry.isRetryableHttpCode(404));
    Assert.assertFalse(Retry.isRetryableHttpCode(409));
    Assert.assertFalse(Retry.isRetryableHttpCode(412));
  }

  // ---------------------------------------------------------------------------
  // Retry.isRetryableIOException tests
  // ---------------------------------------------------------------------------

  @Test
  public void testIsRetryableIOException_retryable() {
    Assert.assertTrue(Retry.isRetryableIOException(new IOException("connection reset")));
    Assert.assertTrue(Retry.isRetryableIOException(new IOException("EOF")));
    Assert.assertTrue(
        Retry.isRetryableIOException(new IOException("http: server closed idle connection")));
    Assert.assertTrue(Retry.isRetryableIOException(new SocketException("Connection timed out")));
    Assert.assertTrue(Retry.isRetryableIOException(new java.net.SocketTimeoutException("read")));
  }

  @Test
  public void testIsRetryableIOException_sslHandshakeNotRetryable() {
    Assert.assertFalse(Retry.isRetryableIOException(new SSLHandshakeException("cert not trusted")));
  }

  @Test
  public void testIsRetryableIOException_protocolMismatchNotRetryable() {
    Assert.assertFalse(
        Retry.isRetryableIOException(new IOException("server gave HTTP response to HTTPS client")));
  }

  // ---------------------------------------------------------------------------
  // Retry.isRetryable(Throwable) tests
  // ---------------------------------------------------------------------------

  @Test
  public void testIsRetryable_ioException() {
    Assert.assertTrue(Retry.isRetryable(new IOException("connection reset")));
  }

  @Test
  public void testIsRetryable_ioExceptionWrappedInCompletion() {
    Assert.assertTrue(
        Retry.isRetryable(new CompletionException(new IOException("connection reset"))));
  }

  @Test
  public void testIsRetryable_sslHandshakeNotRetryable() {
    Assert.assertFalse(Retry.isRetryable(new SSLHandshakeException("bad cert")));
  }

  @Test
  public void testIsRetryable_serverException_retryable() throws Exception {
    Assert.assertTrue(Retry.isRetryable(new ServerException("Server Error", 500, "")));
    Assert.assertTrue(Retry.isRetryable(new ServerException("Bad Gateway", 502, "")));
    Assert.assertTrue(Retry.isRetryable(new ServerException("Service Unavailable", 503, "")));
  }

  @Test
  public void testIsRetryable_serverException_notRetryable() throws Exception {
    Assert.assertFalse(Retry.isRetryable(new ServerException("Not Modified", 304, "")));
  }

  @Test
  public void testIsRetryable_invalidResponseException_retryable() throws Exception {
    Assert.assertTrue(Retry.isRetryable(new InvalidResponseException(500, "text/html", "", "")));
    Assert.assertTrue(Retry.isRetryable(new InvalidResponseException(503, "text/html", "", "")));
  }

  @Test
  public void testIsRetryable_invalidResponseException_notRetryable() throws Exception {
    Assert.assertFalse(Retry.isRetryable(new InvalidResponseException(403, "text/html", "", "")));
    Assert.assertFalse(Retry.isRetryable(new InvalidResponseException(404, "text/html", "", "")));
  }

  @Test
  public void testIsRetryable_null() {
    Assert.assertFalse(Retry.isRetryable(null));
  }

  @Test
  public void testIsRetryable_retryHeadNotRetryable() throws Exception {
    // The explicit guard in isRetryable() must fire even if "RetryHead" were in the codes set.
    ErrorResponse errorResponse =
        new ErrorResponse("RetryHead", null, null, null, null, null, null);
    ErrorResponseException e = new ErrorResponseException(errorResponse, null, "");
    Assert.assertFalse(Retry.isRetryable(e));
  }

  // ---------------------------------------------------------------------------
  // Retry.computeBackoffMs tests
  // ---------------------------------------------------------------------------

  @Test
  public void testComputeBackoffMs_attemptZeroIsZero() {
    Random random = new Random(42);
    Assert.assertEquals(0L, Retry.computeBackoffMs(0, random));
  }

  @Test
  public void testComputeBackoffMs_negativeAttemptIsZero() {
    Random random = new Random(42);
    Assert.assertEquals(0L, Retry.computeBackoffMs(-1, random));
  }

  @Test
  public void testComputeBackoffMs_firstRetryWithinCap() {
    // attempt=1 (first retry after one failure): cap = min(1000, 200*2^0) = 200ms
    for (int seed = 0; seed < 20; seed++) {
      Random random = new Random(seed);
      long delay = Retry.computeBackoffMs(1, random);
      Assert.assertTrue("first retry delay must be in [0, 200ms]", delay >= 0 && delay <= 200);
    }
  }

  @Test
  public void testComputeBackoffMs_secondRetryWithinCap() {
    // attempt=2 (second retry): cap = min(1000, 200*2^1) = 400ms
    for (int seed = 0; seed < 20; seed++) {
      Random random = new Random(seed);
      long delay = Retry.computeBackoffMs(2, random);
      Assert.assertTrue("second retry delay must be in [0, 400ms]", delay >= 0 && delay <= 400);
    }
  }

  @Test
  public void testComputeBackoffMs_cappedAtRetryCapMs() {
    // With attempt=5, uncapped = 200 * 32 = 6400ms, but should be capped at 1000ms
    for (int seed = 0; seed < 20; seed++) {
      Random random = new Random(seed);
      long delay = Retry.computeBackoffMs(5, random);
      Assert.assertTrue(
          "delay must be <= RETRY_CAP_MS (" + Retry.RETRY_CAP_MS + ")",
          delay <= Retry.RETRY_CAP_MS);
      Assert.assertTrue("delay must be >= 0", delay >= 0);
    }
  }

  @Test
  public void testComputeBackoffMs_highAttemptCapped() {
    for (int seed = 0; seed < 20; seed++) {
      Random random = new Random(seed);
      long delay = Retry.computeBackoffMs(100, random);
      Assert.assertTrue("delay must be <= RETRY_CAP_MS", delay <= Retry.RETRY_CAP_MS);
    }
  }

  // ---------------------------------------------------------------------------
  // Integration tests via MockWebServer
  // ---------------------------------------------------------------------------

  private static final String LIST_BUCKETS_OK =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<ListAllMyBucketsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
          + "<Owner><ID>test</ID><DisplayName>test</DisplayName></Owner>"
          + "<Buckets/></ListAllMyBucketsResult>";

  private static final String INTERNAL_ERROR_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<Error><Code>InternalError</Code>"
          + "<Message>We encountered an internal error. Please try again.</Message>"
          + "<Resource>/</Resource><RequestId>abc</RequestId></Error>";

  private static final String THROTTLE_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<Error><Code>SlowDown</Code>"
          + "<Message>Please reduce your request rate.</Message>"
          + "<Resource>/</Resource><RequestId>abc</RequestId></Error>";

  /** Returns a 200 OK response with valid ListAllMyBucketsResult body. */
  private MockResponse successResponse() {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/xml")
        .setBody(new Buffer().writeUtf8(LIST_BUCKETS_OK));
  }

  /** Returns a 500 server error response with HTML body (non-XML). */
  private MockResponse serverError500Html() {
    return new MockResponse()
        .setResponseCode(500)
        .setHeader("Content-Type", "text/html")
        .setBody(new Buffer().writeUtf8("<html><body>Internal Server Error</body></html>"));
  }

  /** Returns a 503 error with HTML body. */
  private MockResponse serviceUnavailable503() {
    return new MockResponse()
        .setResponseCode(503)
        .setHeader("Content-Type", "text/html")
        .setBody(new Buffer().writeUtf8("<html>Service Unavailable</html>"));
  }

  /** Returns a 500 S3 InternalError XML response. */
  private MockResponse s3InternalErrorResponse() {
    return new MockResponse()
        .setResponseCode(500)
        .setHeader("Content-Type", "application/xml")
        .setBody(new Buffer().writeUtf8(INTERNAL_ERROR_XML));
  }

  /** Returns a 429 TooManyRequests with S3 SlowDown XML. */
  private MockResponse s3ThrottleResponse() {
    return new MockResponse()
        .setResponseCode(429)
        .setHeader("Content-Type", "application/xml")
        .setBody(new Buffer().writeUtf8(THROTTLE_XML));
  }

  @Test
  public void testRetryOn500HtmlThenSuccess() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      // First request → 500 HTML (non-XML) → InvalidResponseException(500) → retryable
      server.enqueue(serverError500Html());
      // Second request (retry) → 200 OK
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      // Should succeed after one retry
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testRetryOn503ThenSuccess() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(serviceUnavailable503());
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testRetryOnS3InternalErrorThenSuccess() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(s3InternalErrorResponse());
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testRetryOnS3ThrottleThenSuccess() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(s3ThrottleResponse());
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testNoRetryOn404() throws Exception {
    String notFoundXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Error><Code>NoSuchBucket</Code><Message>not found</Message>"
            + "<Resource>/test</Resource><RequestId>abc</RequestId></Error>";

    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(404)
              .setHeader("Content-Type", "application/xml")
              .setBody(new Buffer().writeUtf8(notFoundXml)));
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        client.listBuckets();
        Assert.fail("expected exception");
      } catch (Exception e) {
        Assert.assertNotNull("expected non-null exception", e);
      }
      // Must not retry — only 1 request should have been made
      Assert.assertEquals(1, server.getRequestCount());
    }
  }

  @Test
  public void testNoRetryOn403() throws Exception {
    String accessDeniedXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Error><Code>AccessDenied</Code><Message>Access Denied</Message>"
            + "<Resource>/</Resource><RequestId>abc</RequestId></Error>";

    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(403)
              .setHeader("Content-Type", "application/xml")
              .setBody(new Buffer().writeUtf8(accessDeniedXml)));
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        client.listBuckets();
        Assert.fail("expected exception");
      } catch (Exception e) {
        Assert.assertNotNull("expected non-null exception", e);
      }
      Assert.assertEquals(1, server.getRequestCount());
    }
  }

  @Test
  public void testRetryExhaustedThrowsLastError() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      // All 3 attempts fail with 500
      server.enqueue(serverError500Html());
      server.enqueue(serverError500Html());
      server.enqueue(serverError500Html());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        client.listBuckets();
        Assert.fail("expected exception after exhausted retries");
      } catch (Exception e) {
        Assert.assertNotNull("expected non-null exception after exhausted retries", e);
      }
      Assert.assertEquals(3, server.getRequestCount());
    }
  }

  @Test
  public void testMaxRetriesOneDisablesRetry() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(serverError500Html());
      // Second response should never be reached
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(1).build();
      try {
        client.listBuckets();
        Assert.fail("expected exception");
      } catch (Exception e) {
        Assert.assertNotNull("expected non-null exception", e);
      }
      // maxRetries=1 means a single attempt; must not retry
      Assert.assertEquals(1, server.getRequestCount());
    }
  }

  @Test
  public void testSetMaxRetriesPostConstruction() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      // Two failures enqueued, but maxRetries will be set to 1 after construction
      server.enqueue(serverError500Html());
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.setMaxRetries(1); // override to disable retries
      try {
        client.listBuckets();
        Assert.fail("expected exception");
      } catch (Exception e) {
        Assert.assertNotNull("expected non-null exception", e);
      }
      Assert.assertEquals(1, server.getRequestCount());
    }
  }

  @Test
  public void testMultipleRetrySucceedsOnThirdAttempt() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(serverError500Html());
      server.enqueue(serviceUnavailable503());
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      client.listBuckets();

      Assert.assertEquals(3, server.getRequestCount());
    }
  }

  @Test
  public void testRetry429ThenSuccess() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(429)
              .setHeader("Content-Type", "text/html")
              .setBody(new Buffer().writeUtf8("Too Many Requests")));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testRetry502ThenSuccess() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(502)
              .setHeader("Content-Type", "text/html")
              .setBody(new Buffer().writeUtf8("Bad Gateway")));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testRetry504ThenSuccess() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(504)
              .setHeader("Content-Type", "text/html")
              .setBody(new Buffer().writeUtf8("Gateway Timeout")));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxRetriesBuilderValidation() {
    MinioClient.builder().endpoint("http://localhost:9000").maxRetries(0).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxRetriesValidation() throws Exception {
    MinioClient client = MinioClient.builder().endpoint("http://localhost:9000").build();
    client.setMaxRetries(0);
  }

  @Test
  public void testDefaultMaxRetriesIsConfigurable() throws Exception {
    // Use maxRetries=4 to keep test fast (delays ≤ 200+400+800ms ≈ 1.4s max)
    int retries = 4;
    try (MockWebServer server = new MockWebServer()) {
      for (int i = 0; i < retries - 1; i++) {
        server.enqueue(serverError500Html());
      }
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(retries).build();
      client.listBuckets();

      Assert.assertEquals(retries, server.getRequestCount());
    }
  }

  @Test
  public void testRequestBodyRetried() throws Exception {
    // Byte array body (seekable) should be retried
    try (MockWebServer server = new MockWebServer()) {
      // First putObject fails, second succeeds with ETag
      server.enqueue(serverError500Html());
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("ETag", "\"abc123\"")
              .setHeader("Content-Length", "0"));
      server.start();

      // Specify region so the SDK doesn't need a separate GetBucketLocation request
      MinioClient client =
          MinioClient.builder()
              .endpoint(server.url("").toString())
              .credentials("access", "secret")
              .region("us-east-1")
              .maxRetries(2)
              .build();

      client.putObject(
          PutObjectArgs.builder()
              .bucket("test-bucket")
              .object("test-object")
              .data(new byte[0], 0)
              .build());

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testRecordedRequestHeaders() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(serverError500Html());
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.listBuckets();

      // Both requests should be to the same path
      RecordedRequest first = server.takeRequest();
      RecordedRequest second = server.takeRequest();
      Assert.assertEquals(first.getPath(), second.getPath());
    }
  }

  @Test
  public void testInvalidResponseExceptionHasResponseCode() {
    InvalidResponseException e = new InvalidResponseException(503, "text/html", "body", "trace");
    Assert.assertEquals(503, e.responseCode());
  }
}
