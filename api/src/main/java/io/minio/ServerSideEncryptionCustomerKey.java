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

import com.google.common.io.BaseEncoding;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

public class ServerSideEncryptionCustomerKey extends ServerSideEncryption {
  final SecretKey secretKey;
  final Map<String, String> headers;
  final Map<String, String> copySourceHeaders;

  public ServerSideEncryptionCustomerKey(SecretKey key)
      throws InvalidKeyException, NoSuchAlgorithmException {
    this.secretKey = key;

    byte[] keyBytes = key.getEncoded();
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    md5.update(keyBytes);
    String customerKey = BaseEncoding.base64().encode(keyBytes);
    String customerKeyMd5 = BaseEncoding.base64().encode(md5.digest());

    Map<String, String> map = new HashMap<>();
    map.put("X-Amz-Server-Side-Encryption-Customer-Algorithm", "AES256");
    map.put("X-Amz-Server-Side-Encryption-Customer-Key", customerKey);
    map.put("X-Amz-Server-Side-Encryption-Customer-Key-Md5", customerKeyMd5);
    this.headers = Collections.unmodifiableMap(map);

    map = new HashMap<>();
    map.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Algorithm", "AES256");
    map.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key", customerKey);
    map.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key-Md5", customerKeyMd5);
    this.copySourceHeaders = Collections.unmodifiableMap(map);
  }

  @Override
  public final Type type() {
    return Type.SSE_C;
  }

  @Override
  public final Map<String, String> headers() {
    if (this.isDestroyed()) {
      throw new IllegalStateException("object is already destroyed");
    }

    return headers;
  }

  @Override
  public final Map<String, String> copySourceHeaders() {
    if (this.isDestroyed()) {
      throw new IllegalStateException("object is already destroyed");
    }

    return copySourceHeaders;
  }

  @Override
  public final void destroy() throws DestroyFailedException {
    secretKey.destroy();
    this.destroyed = true;
  }
}
