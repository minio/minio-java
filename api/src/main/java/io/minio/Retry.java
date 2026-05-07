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
import java.util.Random;
import java.util.Set;
import javax.net.ssl.SSLHandshakeException;

/** Retry configuration and helpers for S3 request execution. */
class Retry {
  /** Default maximum number of retry attempts per request. */
  static final int MAX_RETRY = 10;

  /** Base sleep unit for exponential backoff (milliseconds). */
  static final long RETRY_BASE_MS = 200L;

  /** Maximum sleep cap for exponential backoff (milliseconds). */
  static final long RETRY_CAP_MS = 1_000L;

  /** HTTP status codes that should trigger a retry. */
  static final Set<Integer> RETRYABLE_HTTP_CODES =
      ImmutableSet.of(
          408, // Request Timeout
          429, // Too Many Requests
          499, // Client Closed Request (nginx)
          500, // Internal Server Error
          502, // Bad Gateway
          503, // Service Unavailable
          504, // Gateway Timeout
          520); // Cloudflare unknown error

  static boolean isRetryableHttpCode(int code) {
    return RETRYABLE_HTTP_CODES.contains(code);
  }

  /**
   * Returns true if the IOException is retryable. Non-retryable: TLS handshake failures, HTTP/HTTPS
   * protocol mismatch. Everything else (connection reset, EOF, server closed idle connection) is
   * retried.
   */
  static boolean isRetryableIOException(IOException e) {
    if (e instanceof SSLHandshakeException) return false;
    String msg = e.getMessage();
    if (msg != null && msg.contains("server gave HTTP response to HTTPS client")) return false;
    return true;
  }

  /**
   * Computes the full-jitter exponential backoff delay for retry {@code attempt} (1-indexed: 1 =
   * first retry).
   *
   * <pre>
   * attempt=1 → [0, 200 ms]
   * attempt=2 → [0, 400 ms]
   * attempt=3 → [0, 800 ms]
   * attempt=4+→ [0, 1000 ms] (capped)
   * </pre>
   *
   * Pass {@code attempt <= 0} to get 0 (no delay).
   */
  static long computeBackoffMs(int attempt, Random random) {
    if (attempt <= 0) return 0L;
    int exp = Math.min(attempt - 1, 30);
    long cap = Math.min(RETRY_CAP_MS, RETRY_BASE_MS * (1L << exp));
    return (long) (random.nextDouble() * cap);
  }

  private Retry() {}
}
