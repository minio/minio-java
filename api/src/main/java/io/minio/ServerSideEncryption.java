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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import com.google.common.io.BaseEncoding;


/**
* ServerSideEncryption represents a form of server-side encryption.
*/
public abstract class ServerSideEncryption implements Destroyable {
  /**
   * The types of server-side encryption.
   */
  public static enum Type {
    SSE_C, SSE_S3, SSE_KMS;

    /**
     * Returns true if the server-side encryption requires a TLS connection.
     * @return true if the type of server-side encryption requires TLS.
     */
    public boolean requiresTls() {
      return this.equals(SSE_C) || this.equals(SSE_KMS);
    }

    /**
     * Returns true if the server-side encryption requires signature V4.
     * @return true if the type of server-side encryption requires signature V4.
     *
     * @deprecated As of release 7.0
     */
    @Deprecated
    public boolean requiresV4() {
      return this.equals(SSE_KMS);
    }
  }

  protected boolean destroyed = false;

  /**
   * Returns server side encryption type.
   */
  public abstract Type type();

  /**
   * Returns server side encryption headers.
   */
  public abstract Map<String, String> headers();

  /**
   * Returns server side encryption headers for source object in Put Object - Copy.
   */
  public Map<String, String> copySourceHeaders() throws IllegalArgumentException {
    throw new IllegalArgumentException(this.type().name() + " is not supported in copy source");
  }

  /**
   * Returns the type of server-side encryption.
   * @return the type of server-side encryption.
   *
   * @deprecated As of release 7.0
   */
  @Deprecated
  public Type getType() {
    return this.type();
  }

  /**
   * Set the server-side-encryption headers of this specific encryption.
   * @param headers The metadata key-value map.
   *
   * @deprecated As of release 7.0
   */
  @Deprecated
  public void marshal(Map<String, String> headers) {
    headers.putAll(this.headers());
  }

  @Override
  public boolean isDestroyed() {
    return this.destroyed;
  }

  private static boolean isCustomerKeyValid(SecretKey key) {
    if (key == null) {
      return false;
    }
    return !key.isDestroyed() && key.getAlgorithm().equals("AES") && key.getEncoded().length == 32;
  }

  static class ServerSideEncryptionWithCustomerKey extends ServerSideEncryption {
    protected final SecretKey secretKey;
    protected final MessageDigest md5;

    public ServerSideEncryptionWithCustomerKey(SecretKey key, MessageDigest md5) {
      this.secretKey = key;
      this.md5 = md5;
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

      Map<String, String> headers = new HashMap<>();
      try {
        final byte[] key = secretKey.getEncoded();
        md5.update(key);

        headers.put("X-Amz-Server-Side-Encryption-Customer-Algorithm", "AES256");
        headers.put("X-Amz-Server-Side-Encryption-Customer-Key",  BaseEncoding.base64().encode(key));
        headers.put("X-Amz-Server-Side-Encryption-Customer-Key-Md5", BaseEncoding.base64().encode(md5.digest()));
      } finally {
        md5.reset();
      }

      return headers;
    }

    @Override
    public final Map<String, String> copySourceHeaders() {
      if (this.isDestroyed()) {
        throw new IllegalStateException("object is already destroyed");
      }

      Map<String, String> headers = new HashMap<>();
      try {
        final byte[] key = secretKey.getEncoded();
        md5.update(key);

        headers.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Algorithm", "AES256");
        headers.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key",  BaseEncoding.base64().encode(key));
        headers.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key-Md5",
                    BaseEncoding.base64().encode(md5.digest()));
      } finally {
        md5.reset();
      }

      return headers;
    }

    @Override
    public final void destroy() throws DestroyFailedException {
      secretKey.destroy();
      this.destroyed = true;
    }
  }

  /**
   * Create a new server-side-encryption object for encryption with customer
   * provided keys (a.k.a. SSE-C).
   *
   * @param key The secret AES-256 key.
   * @return An instance of ServerSideEncryption implementing SSE-C.
   * @throws InvalidKeyException if the provided secret key is not a 256 bit AES key.
   * @throws NoSuchAlgorithmException if the crypto provider does not implement MD5.
   */
  public static ServerSideEncryption withCustomerKey(SecretKey key)
    throws InvalidKeyException, NoSuchAlgorithmException {
    if (!isCustomerKeyValid(key)) {
      throw new InvalidKeyException("The secret key is not a 256 bit AES key");
    }
    return new ServerSideEncryptionWithCustomerKey(key, MessageDigest.getInstance(("MD5")));
  }

  /**
   * @deprecated As of release 7.0
   */
  @Deprecated
  static final class ServerSideEncryptionCopyWithCustomerKey extends ServerSideEncryptionWithCustomerKey {
    public ServerSideEncryptionCopyWithCustomerKey(SecretKey key, MessageDigest md5) {
      super(key, md5);
    }
  }

  /**
   * Create a new server-side-encryption object for encryption with customer
   * provided keys (a.k.a. SSE-C).
   *
   * @param key The secret AES-256 key.
   * @return An instance of ServerSideEncryption implementing SSE-C.
   * @throws InvalidKeyException if the provided secret key is not a 256 bit AES key.
   * @throws NoSuchAlgorithmException if the crypto provider does not implement MD5.
   *
   * @deprecated As of release 7.0
   */
  @Deprecated
  public static ServerSideEncryption copyWithCustomerKey(SecretKey key)
    throws InvalidKeyException, NoSuchAlgorithmException {
    if (!isCustomerKeyValid(key)) {
      throw new InvalidKeyException("The secret key is not a 256 bit AES key");
    }
    return new ServerSideEncryptionCopyWithCustomerKey(key, MessageDigest.getInstance(("MD5")));
  }

  static final class ServerSideEncryptionS3 extends ServerSideEncryption {
    @Override
    public final Type type() {
      return Type.SSE_S3;
    }

    @Override
    public final Map<String, String> headers() {
      Map<String, String> headers = new HashMap<>();
      headers.put("X-Amz-Server-Side-Encryption", "AES256");
      return headers;
    }

    @Override
    public final void destroy() throws DestroyFailedException {
      this.destroyed = true;
    }
  }

  /**
   * Create a new server-side-encryption object for encryption at rest (a.k.a. SSE-S3).
   *
   * @return an instance of ServerSideEncryption implementing SSE-S3
   */
  public static ServerSideEncryption atRest() {
    return new ServerSideEncryptionS3();
  }

  static final class ServerSideEncryptionKms extends ServerSideEncryption {
    final String keyId;
    final Optional<String> context;

    public ServerSideEncryptionKms(String keyId, Optional<String> context) {
      this.keyId = keyId;
      this.context = context;
    }

    @Override
    public final Type type() {
      return Type.SSE_KMS; 
    }

    @Override
    public final Map<String, String> headers() {
      if (this.isDestroyed()) {
        throw new IllegalStateException("object is already destroyed");
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("X-Amz-Server-Side-Encryption", "aws:kms");
      headers.put("X-Amz-Server-Side-Encryption-Aws-Kms-Key-Id", keyId);
      if (context.isPresent()) {
        headers.put("X-Amz-Server-Side-Encryption-Context", context.get());
      }

      return headers;
    }

    @Override
    public final void destroy() throws DestroyFailedException {
      this.destroyed = true;
    }
  }

  /**
   * Create a new server-side-encryption object for encryption using a KMS (a.k.a. SSE-KMS).
   *
   * @param keyId   specifies the customer-master-key (CMK) and must not be null.
   * @param context is the encryption context. If the context is null no context is used.
   *
   * @return an instance of ServerSideEncryption implementing SSE-KMS.
   */
  public static ServerSideEncryption withManagedKeys(String keyId, Map<String,String> context)
    throws IllegalArgumentException, UnsupportedEncodingException {
    if (keyId == null) {
      throw new IllegalArgumentException("The key-ID cannot be null");
    }
    if (context == null) {
      return new ServerSideEncryptionKms(keyId, Optional.empty());
    }

    StringBuilder builder = new StringBuilder();
    int i = 0;
    builder.append('{');
    for (Entry<String,String> entry : context.entrySet()) {
      builder.append('"');
      builder.append(entry.getKey());
      builder.append('"');
      builder.append(':');
      builder.append('"');
      builder.append(entry.getValue());
      builder.append('"');
      if (i < context.entrySet().size() - 1) {
        builder.append(',');
      }
    }
    builder.append('}');
    String contextString = BaseEncoding.base64().encode(builder.toString().getBytes("UTF-8"));
    return new ServerSideEncryptionKms(keyId, Optional.of(contextString));
  }
}
