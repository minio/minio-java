/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2018 MinIO, Inc.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.errors.MinioException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

/** Server-side encryption support. */
public abstract class ServerSideEncryption {
  private static final Http.Headers emptyHeaders = new Http.Headers();

  public abstract Http.Headers headers();

  public boolean tlsRequired() {
    return true;
  }

  public Http.Headers copySourceHeaders() {
    return emptyHeaders;
  }

  /** S3 type of Server-side encryption. */
  public static class S3 extends ServerSideEncryption {
    private static final Http.Headers headers =
        new Http.Headers("X-Amz-Server-Side-Encryption", "AES256");

    @Override
    public final Http.Headers headers() {
      return headers;
    }

    @Override
    public final boolean tlsRequired() {
      return false;
    }

    @Override
    public String toString() {
      return "SSE-S3";
    }
  }

  /** KMS type of Server-side encryption. */
  public static class KMS extends ServerSideEncryption {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Http.Headers headers;

    public KMS(String keyId, Map<String, String> context) throws MinioException {
      if (keyId == null) throw new IllegalArgumentException("Key ID cannot be null");

      this.headers =
          new Http.Headers(
              "X-Amz-Server-Side-Encryption",
              "aws:kms",
              "X-Amz-Server-Side-Encryption-Aws-Kms-Key-Id",
              keyId);
      if (context != null) {
        try {
          this.headers.put(
              "X-Amz-Server-Side-Encryption-Context",
              Checksum.base64String(
                  objectMapper.writeValueAsString(context).getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException e) {
          throw new MinioException(e);
        }
      }
    }

    @Override
    public final Http.Headers headers() {
      return headers;
    }

    @Override
    public String toString() {
      return "SSE-KMS";
    }
  }

  /** Customer-key type of Server-side encryption. */
  public static class CustomerKey extends ServerSideEncryption {
    private boolean isDestroyed = false;
    private final SecretKey secretKey;
    private final Http.Headers headers;
    private final Http.Headers copySourceHeaders;

    public CustomerKey(SecretKey key) throws MinioException {
      if (key == null || !key.getAlgorithm().equals("AES") || key.getEncoded().length != 32) {
        throw new IllegalArgumentException("Secret key must be 256 bit AES key");
      }

      if (key.isDestroyed()) {
        throw new IllegalArgumentException("Secret key already destroyed");
      }

      this.secretKey = key;

      byte[] keyBytes = key.getEncoded();
      String customerKey = Checksum.base64String(keyBytes);
      String customerKeyMd5 = Checksum.base64String(Checksum.MD5.sum(keyBytes));

      this.headers =
          new Http.Headers(
              "X-Amz-Server-Side-Encryption-Customer-Algorithm", "AES256",
              "X-Amz-Server-Side-Encryption-Customer-Key", customerKey,
              "X-Amz-Server-Side-Encryption-Customer-Key-Md5", customerKeyMd5);

      this.copySourceHeaders =
          new Http.Headers(
              "X-Amz-Copy-Source-Server-Side-Encryption-Customer-Algorithm", "AES256",
              "X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key", customerKey,
              "X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key-Md5", customerKeyMd5);
    }

    @Override
    public final Http.Headers headers() {
      if (isDestroyed) {
        throw new IllegalStateException("Secret key was destroyed");
      }

      return headers;
    }

    @Override
    public final Http.Headers copySourceHeaders() {
      if (isDestroyed) {
        throw new IllegalStateException("Secret key was destroyed");
      }

      return copySourceHeaders;
    }

    public final void destroy() throws DestroyFailedException {
      secretKey.destroy();
      isDestroyed = true;
    }

    @Override
    public String toString() {
      return "SSE-C";
    }
  }
}
