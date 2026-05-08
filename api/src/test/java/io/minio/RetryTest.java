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
import io.minio.errors.MinioException;
import java.io.IOException;
import java.net.SocketException;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import org.junit.Assert;
import org.junit.Test;

/** Unit + integration tests for {@link Retry} and {@link Http.RetryInterceptor}. */
public class RetryTest {

  private static void closeQuietly(MinioClient client) {
    if (client == null) return;
    try {
      client.close();
    } catch (Exception ignored) {
      // Tests do not rely on close(); swallowing keeps the throws clauses narrow.
    }
  }

  // ---------------------------------------------------------------------------
  // Retry.isHttpStatusRetryable
  // ---------------------------------------------------------------------------

  @Test
  public void testIsHttpStatusRetryable_retryable() {
    Assert.assertTrue(Retry.isHttpStatusRetryable(408));
    Assert.assertTrue(Retry.isHttpStatusRetryable(429));
    Assert.assertTrue(Retry.isHttpStatusRetryable(499));
    Assert.assertTrue(Retry.isHttpStatusRetryable(500));
    Assert.assertTrue(Retry.isHttpStatusRetryable(502));
    Assert.assertTrue(Retry.isHttpStatusRetryable(503));
    Assert.assertTrue(Retry.isHttpStatusRetryable(504));
    Assert.assertTrue(Retry.isHttpStatusRetryable(520));
  }

  @Test
  public void testIsHttpStatusRetryable_notRetryable() {
    for (int code : new int[] {200, 201, 204, 301, 304, 400, 401, 403, 404, 409, 412, 416, 501}) {
      Assert.assertFalse(
          "status " + code + " must not be retryable", Retry.isHttpStatusRetryable(code));
    }
  }

  // ---------------------------------------------------------------------------
  // Retry.isS3CodeRetryable
  // ---------------------------------------------------------------------------

  @Test
  public void testIsS3CodeRetryable_retryable() {
    String[] codes = {
      "RequestError",
      "RequestTimeout",
      "Throttling",
      "ThrottlingException",
      "RequestLimitExceeded",
      "RequestThrottled",
      "InternalError",
      "ExpiredToken",
      "ExpiredTokenException",
      "SlowDown",
      "SlowDownWrite",
      "SlowDownRead"
    };
    for (String c : codes) {
      Assert.assertTrue("S3 code " + c + " must be retryable", Retry.isS3CodeRetryable(c));
    }
  }

  @Test
  public void testIsS3CodeRetryable_notRetryable() {
    Assert.assertFalse(Retry.isS3CodeRetryable("NoSuchKey"));
    Assert.assertFalse(Retry.isS3CodeRetryable("NoSuchBucket"));
    Assert.assertFalse(Retry.isS3CodeRetryable("AccessDenied"));
    Assert.assertFalse(Retry.isS3CodeRetryable("RetryHead"));
    Assert.assertFalse(Retry.isS3CodeRetryable(""));
    Assert.assertFalse(Retry.isS3CodeRetryable(null));
  }

  // ---------------------------------------------------------------------------
  // Retry.isRequestErrorRetryable
  // ---------------------------------------------------------------------------

  @Test
  public void testIsRequestErrorRetryable_retryable() {
    Assert.assertTrue(Retry.isRequestErrorRetryable(new IOException("connection reset")));
    Assert.assertTrue(Retry.isRequestErrorRetryable(new IOException("EOF")));
    Assert.assertTrue(
        Retry.isRequestErrorRetryable(new IOException("http: server closed idle connection")));
    Assert.assertTrue(Retry.isRequestErrorRetryable(new SocketException("Connection timed out")));
    Assert.assertTrue(Retry.isRequestErrorRetryable(new java.net.SocketTimeoutException("read")));
  }

  @Test
  public void testIsRequestErrorRetryable_sslHandshakeNotRetryable() {
    Assert.assertFalse(
        Retry.isRequestErrorRetryable(new SSLHandshakeException("cert not trusted")));
  }

  @Test
  public void testIsRequestErrorRetryable_protocolMismatchNotRetryable() {
    Assert.assertFalse(
        Retry.isRequestErrorRetryable(
            new IOException("server gave HTTP response to HTTPS client")));
  }

  // ---------------------------------------------------------------------------
  // Retry.exponentialBackoffMs
  // ---------------------------------------------------------------------------

  @Test
  public void testExponentialBackoffMs_attempt0WithinFirstUnit() {
    // attempt=0: cap = min(1000, 200*2^0) = 200ms; with full jitter, result in [0, 200].
    for (int i = 0; i < 100; i++) {
      long delay = Retry.exponentialBackoffMs(0);
      Assert.assertTrue(
          "attempt 0 delay must be in [0, 200ms], got " + delay, delay >= 0 && delay <= 200);
    }
  }

  @Test
  public void testExponentialBackoffMs_attempt2WithinSecondCap() {
    // attempt=2: cap = min(1000, 200*2^2) = 800ms.
    for (int i = 0; i < 100; i++) {
      long delay = Retry.exponentialBackoffMs(2);
      Assert.assertTrue(
          "attempt 2 delay must be in [0, 800ms], got " + delay, delay >= 0 && delay <= 800);
    }
  }

  @Test
  public void testExponentialBackoffMs_cappedAtRetryCap() {
    // attempt=10: uncapped 200*2^10 = 204800ms; capped at 1000ms.
    for (int i = 0; i < 100; i++) {
      long delay = Retry.exponentialBackoffMs(10);
      Assert.assertTrue("delay must be <= cap, got " + delay, delay <= Retry.DEFAULT_RETRY_CAP_MS);
      Assert.assertTrue(delay >= 0);
    }
  }

  @Test
  public void testExponentialBackoffMs_negativeAttemptIsClamped() {
    // Negative attempt clamps to 0; cap = 200ms.
    long delay = Retry.exponentialBackoffMs(-5);
    Assert.assertTrue(delay >= 0 && delay <= 200);
  }

  @Test
  public void testExponentialBackoffMs_highAttemptDoesNotOverflow() {
    // High attempts must not bit-shift overflow; cap saturates.
    for (int attempt : new int[] {30, 31, 60, 100, 1000}) {
      long delay = Retry.exponentialBackoffMs(attempt);
      Assert.assertTrue(
          "attempt=" + attempt + " delay must be <= cap, got " + delay,
          delay <= Retry.DEFAULT_RETRY_CAP_MS);
      Assert.assertTrue("attempt=" + attempt + " delay must be >= 0", delay >= 0);
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

  private MockResponse successResponse() {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/xml")
        .setBody(new Buffer().writeUtf8(LIST_BUCKETS_OK));
  }

  private MockResponse htmlServerError(int code) {
    return new MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "text/html")
        .setBody(new Buffer().writeUtf8("<html>" + code + "</html>"));
  }

  private MockResponse xmlError(int code, String s3Code) {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Error><Code>"
            + s3Code
            + "</Code><Message>m</Message><Resource>/</Resource><RequestId>id</RequestId></Error>";
    return new MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/xml")
        .setBody(new Buffer().writeUtf8(xml));
  }

  @Test
  public void testRetryOn503ThenSuccess() throws IOException, MinioException {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(htmlServerError(503));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      try {
        client.listBuckets();
        Assert.assertEquals(2, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testRetryOnEachRetryableHttpCode() throws IOException, MinioException {
    for (int code : new int[] {408, 429, 499, 500, 502, 503, 504, 520}) {
      try (MockWebServer server = new MockWebServer()) {
        server.enqueue(htmlServerError(code));
        server.enqueue(successResponse());
        server.start();

        MinioClient client =
            MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
        try {
          client.listBuckets();
          Assert.assertEquals("status " + code + " expected to retry", 2, server.getRequestCount());
        } finally {
          closeQuietly(client);
        }
      }
    }
  }

  @Test
  public void testRetryOnRetryableS3CodeIn400Body() throws IOException, MinioException {
    // 400 is NOT a retryable HTTP status, but body has retryable S3 code → retry.
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(xmlError(400, "ExpiredToken"));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      try {
        client.listBuckets();
        Assert.assertEquals(2, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testNoRetryOn404() throws IOException, MinioException {
    // Bucket-scoped operation returning 404 NoSuchBucket — a wire-realistic shape
    // (NoSuchBucket only ever applies to bucket-scoped calls). Verifies that 404
    // does not trigger retry and that the parsed error surfaces both code and status.
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(xmlError(404, "NoSuchBucket"));
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        try {
          client.removeBucket(
              RemoveBucketArgs.builder().bucket("missing-bucket-for-no-retry").build());
          Assert.fail("expected ErrorResponseException");
        } catch (ErrorResponseException e) {
          Assert.assertEquals("NoSuchBucket", e.errorResponse().code());
          Assert.assertEquals(404, e.response().code());
        }
        Assert.assertEquals(1, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testNoRetryOn403() throws IOException, MinioException {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(xmlError(403, "AccessDenied"));
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        try {
          client.listBuckets();
          Assert.fail("expected ErrorResponseException");
        } catch (ErrorResponseException e) {
          Assert.assertEquals("AccessDenied", e.errorResponse().code());
        }
        Assert.assertEquals(1, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testRetryExhaustedReturnsLastResponse() throws IOException, MinioException {
    // 3 attempts, all 500 (HTML so server-side response dispatches to InvalidResponseException).
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(htmlServerError(500));
      server.enqueue(htmlServerError(500));
      server.enqueue(htmlServerError(500));
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        try {
          client.listBuckets();
          Assert.fail("expected exception after exhausted retries");
        } catch (InvalidResponseException e) {
          // Terminal exception must surface the underlying 500 status so callers can distinguish
          // exhausted retries from unrelated failures (e.g. NPE, parser bugs).
          Assert.assertTrue(
              "exhausted-retries exception must reflect HTTP 500, got: " + e.getMessage(),
              e.getMessage().contains("Response code: 500"));
        }
        Assert.assertEquals(3, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testRetryExhaustedSurfacesXmlErrorResponse() throws IOException, MinioException {
    // 3 attempts, all 503 with XML <Error><Code>InternalError</Code> — confirms the terminal
    // ErrorResponseException carries both the 503 status and the parsed S3 code after the retry
    // loop gives up.
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(xmlError(503, "InternalError"));
      server.enqueue(xmlError(503, "InternalError"));
      server.enqueue(xmlError(503, "InternalError"));
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        try {
          client.listBuckets();
          Assert.fail("expected ErrorResponseException after exhausted retries");
        } catch (ErrorResponseException e) {
          Assert.assertEquals(503, e.response().code());
          Assert.assertEquals("InternalError", e.errorResponse().code());
        }
        Assert.assertEquals(3, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testMaxRetriesOneDisablesRetry() throws IOException, MinioException {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(htmlServerError(503));
      // Second response should never be reached.
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(1).build();
      try {
        try {
          client.listBuckets();
          Assert.fail("expected exception");
        } catch (InvalidResponseException e) {
          // expected
        }
        Assert.assertEquals(1, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testSetMaxRetriesPostConstruction() throws IOException, MinioException {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(htmlServerError(503));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(2).build();
      client.setMaxRetries(1); // disable
      try {
        try {
          client.listBuckets();
          Assert.fail("expected exception");
        } catch (InvalidResponseException e) {
          // expected
        }
        Assert.assertEquals(1, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxRetriesBuilderValidation() {
    MinioClient.builder().endpoint("http://localhost:9000").maxRetries(0).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxRetriesValidation() {
    MinioClient client = MinioClient.builder().endpoint("http://localhost:9000").build();
    client.setMaxRetries(0);
  }

  @Test
  public void testMultipleRetrySucceedsOnThirdAttempt() throws IOException, MinioException {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(htmlServerError(500));
      server.enqueue(htmlServerError(503));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        client.listBuckets();
        Assert.assertEquals(3, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Additional coverage: SSL cert-path classifier, negative-validation, IOException
  // path, user-supplied OkHttpClient, and cancellation short-circuit.
  // ---------------------------------------------------------------------------

  @Test
  public void testIsRequestErrorRetryable_certPathErrorsNotRetryable() {
    // SSLException whose cause is one of the cert-path error types must NOT retry.
    SSLException certPathBuilder = new SSLException("x", new CertPathBuilderException("bad"));
    SSLException certPathValidator = new SSLException("x", new CertPathValidatorException("bad"));
    SSLException certError = new SSLException("x", new CertificateException("bad"));
    Assert.assertFalse(Retry.isRequestErrorRetryable(certPathBuilder));
    Assert.assertFalse(Retry.isRequestErrorRetryable(certPathValidator));
    Assert.assertFalse(Retry.isRequestErrorRetryable(certError));

    // SSLPeerUnverifiedException must NOT retry.
    Assert.assertFalse(Retry.isRequestErrorRetryable(new SSLPeerUnverifiedException("untrusted")));

    // SSLException with an unrelated cause is still retryable (transient TLS hiccup).
    Assert.assertTrue(
        Retry.isRequestErrorRetryable(new SSLException("read", new IOException("boom"))));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxRetriesBuilderValidation_negative() {
    MinioClient.builder().endpoint("http://localhost:9000").maxRetries(-1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxRetriesValidation_negative() {
    MinioClient client = MinioClient.builder().endpoint("http://localhost:9000").build();
    try {
      client.setMaxRetries(-1);
    } finally {
      closeQuietly(client);
    }
  }

  @Test
  public void testRetryOnConnectionDropThenSuccess() throws IOException, MinioException {
    // Simulate a transient transport failure: server drops the socket on first attempt, returns
    // the success body on the second. Verifies the IOException retry path end-to-end (only the
    // predicate was previously tested in isolation).
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
      server.enqueue(successResponse());
      server.start();

      MinioClient client =
          MinioClient.builder().endpoint(server.url("").toString()).maxRetries(3).build();
      try {
        client.listBuckets();
        Assert.assertEquals(2, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test
  public void testUserSuppliedClientStillRetries() throws IOException, MinioException {
    // Caller supplies a custom OkHttpClient with no RetryInterceptor and
    // retryOnConnectionFailure(true). wrapWithRetry must still install the SDK interceptor and
    // honour maxRetries — proves the integration path for caller-supplied clients.
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(htmlServerError(503));
      server.enqueue(htmlServerError(503));
      server.enqueue(successResponse());
      server.start();

      OkHttpClient custom = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();
      MinioClient client =
          MinioClient.builder()
              .endpoint(server.url("").toString())
              .httpClient(custom, false)
              .maxRetries(3)
              .build();
      try {
        client.listBuckets();
        Assert.assertEquals(3, server.getRequestCount());
      } finally {
        closeQuietly(client);
      }
    }
  }

  @Test(timeout = 10_000)
  public void testCancelDuringRetryStopsLoop() throws IOException, InterruptedException {
    // Without cancellation handling the interceptor would loop maxRetries=10 times with backoff
    // (~6s of sleep). With the chain.call().isCanceled() check, cancel mid-loop must short-circuit
    // and surface IOException quickly — the timeout guards against regression.
    try (MockWebServer server = new MockWebServer()) {
      for (int i = 0; i < 20; i++) {
        server.enqueue(htmlServerError(503));
      }
      server.start();

      OkHttpClient client =
          new OkHttpClient.Builder()
              .retryOnConnectionFailure(false)
              .addInterceptor(new Http.RetryInterceptor())
              .build();
      try {
        okhttp3.Request request = new okhttp3.Request.Builder().url(server.url("/x")).get().build();
        Call call = client.newCall(request);

        Thread canceler =
            new Thread(
                () -> {
                  try {
                    Thread.sleep(150);
                  } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                  }
                  call.cancel();
                });
        canceler.start();

        long start = System.currentTimeMillis();
        try {
          call.execute().close();
          Assert.fail("expected IOException after cancel");
        } catch (IOException e) {
          // expected: cancel surfaces as IOException
        }
        long elapsed = System.currentTimeMillis() - start;
        canceler.join();

        Assert.assertTrue(
            "cancel must short-circuit the retry loop, took " + elapsed + "ms", elapsed < 5_000);
      } finally {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
      }
    }
  }
}
