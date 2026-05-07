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

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * Retry configuration and classification helpers used by {@link Http.RetryInterceptor}.
 *
 * <p>Defines the retryable HTTP status set, retryable S3 error code set, IOException filter, and
 * the full-jitter exponential backoff formula used for transient failure recovery.
 *
 * <p>Constants and predicates here mirror the contract in minio-go's {@code retry.go}
 * (https://github.com/minio/minio-go/blob/master/retry.go) so a Java caller experiences the same
 * retry semantics as a Go caller. Future drift in minio-go should be reflected here.
 */
class Retry {
  /** Default maximum number of attempts per request. */
  static final int MAX_RETRY = 10;

  /** Base unit per retry attempt, in milliseconds. */
  static final long DEFAULT_RETRY_UNIT_MS = 200L;

  /** Per-attempt sleep cap, in milliseconds. */
  static final long DEFAULT_RETRY_CAP_MS = 1_000L;

  /** Maximum jitter fraction in {@code [0.0, 1.0]}. {@code 1.0} = full jitter. */
  static final double MAX_JITTER = 1.0;

  /** Retryable AWS S3 error codes. */
  static final Set<String> RETRYABLE_S3_CODES =
      ImmutableSet.of(
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
          "SlowDownRead");

  /** Retryable HTTP status codes. */
  static final Set<Integer> RETRYABLE_HTTP_STATUS_CODES =
      ImmutableSet.of(
          408, // Request Timeout
          429, // Too Many Requests
          499, // Client Closed Request (nginx)
          500, // Internal Server Error
          502, // Bad Gateway
          503, // Service Unavailable
          504, // Gateway Timeout
          520); // Cloudflare unknown error

  static boolean isS3CodeRetryable(String code) {
    return code != null && RETRYABLE_S3_CODES.contains(code);
  }

  static boolean isHttpStatusRetryable(int code) {
    return RETRYABLE_HTTP_STATUS_CODES.contains(code);
  }

  /**
   * Returns true if {@code e} represents a transient transport failure that should be retried. TLS
   * handshake failure, unknown-CA / cert-path errors, and the "server gave HTTP response to HTTPS
   * client" protocol mismatch are NOT retryable; everything else (connection reset, EOF, server
   * closed idle connection, socket timeout, …) is.
   */
  static boolean isRequestErrorRetryable(IOException e) {
    if (e instanceof SSLHandshakeException) return false;
    if (e instanceof SSLPeerUnverifiedException) return false;
    if (e instanceof SSLException) {
      Throwable cause = e.getCause();
      if (cause instanceof CertPathBuilderException
          || cause instanceof CertPathValidatorException
          || cause instanceof CertificateException) {
        return false;
      }
    }
    String msg = e.getMessage();
    if (msg != null && msg.contains("server gave HTTP response to HTTPS client")) return false;
    return true;
  }

  /**
   * Computes the exponential-backoff-with-full-jitter delay for retry {@code attempt} (0-indexed:
   * {@code 0} = before the second attempt, {@code 1} = before the third, …):
   *
   * <pre>
   *   sleep = min(DEFAULT_RETRY_CAP_MS, DEFAULT_RETRY_UNIT_MS * 2^attempt)
   *   sleep -= (long)(random.nextDouble() * sleep * MAX_JITTER)   // full jitter when MAX_JITTER == 1.0
   * </pre>
   *
   * <p>With {@code MAX_JITTER == 1.0}, returns a value in {@code [1, min(cap, base * 2^attempt)]}.
   * The lower bound is {@code 1} rather than {@code 0} because {@link
   * java.util.concurrent.ThreadLocalRandom#nextDouble()} is in {@code [0.0, 1.0)} and the {@code
   * (long)} cast truncates {@code rand * sleep} to at most {@code sleep - 1}. This matches the
   * behaviour of minio-go's {@code exponentialBackoffWait}, which uses the same formula and
   * therefore the same bounds.
   */
  static long exponentialBackoffMs(int attempt) {
    int exp = Math.min(Math.max(attempt, 0), 30);
    long sleep = DEFAULT_RETRY_UNIT_MS * (1L << exp);
    if (sleep > DEFAULT_RETRY_CAP_MS) sleep = DEFAULT_RETRY_CAP_MS;
    sleep -= (long) (ThreadLocalRandom.current().nextDouble() * (double) sleep * MAX_JITTER);
    return Math.max(0L, sleep);
  }

  private Retry() {}
}
