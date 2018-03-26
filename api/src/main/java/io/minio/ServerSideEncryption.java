/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2018 Minio, Inc.
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

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import  com.google.common.io.BaseEncoding;


/**
 * ServerSideEncryption represents a form of server-side encryption.
 */
public abstract class ServerSideEncryption implements Destroyable {

  /**
   * Returns the type of server-side encryption.
   * @return the type of server-side encryption.
   */
  public abstract Type getType();

  /**
   * Set the server-side-encryption headers of this specific encryption.
   * @param headers The metadata key-value map.
   */
  public abstract void marshal(Map<String, String> headers);

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
      return this.equals(SSE_C);
    }

    /**
     * Returns true if the server-side encryption requires signature V4.
     * @return true if the type of server-side encryption requires signature V4.
     */
    public boolean requiresV4() {
      return this.equals(SSE_KMS);
    }
  }

  private static boolean isCustomerKeyValid(SecretKey key) {
    if (key == null) {
      return false;
    }
    return !key.isDestroyed() && key.getAlgorithm() == "AES" && key.getEncoded().length == 32;
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
  * Create a new server-side-encryption object for encryption with customer
  * provided keys (a.k.a. SSE-C).
  *
  * @param key The secret AES-256 key.
  * @return An instance of ServerSideEncryption implementing SSE-C.
  * @throws InvalidKeyException if the provided secret key is not a 256 bit AES key.
  * @throws NoSuchAlgorithmException if the crypto provider does not implement MD5.
  */
  public static ServerSideEncryption copyWithCustomerKey(SecretKey key) 
    throws InvalidKeyException, NoSuchAlgorithmException {
    if (!isCustomerKeyValid(key)) {
      throw new InvalidKeyException("The secret key is not a 256 bit AES key");
    }
    return new ServerSideEncryptionCopyWithCustomerKey(key, MessageDigest.getInstance(("MD5")));
  }

  /**
   * Create a new server-side-encryption object for encryption at rest (a.k.a. SSE-S3).
   *
   * @return an instance of ServerSideEncryption implementing SSE-S3  
   */
  public static ServerSideEncryption atRest() {
    return new ServerSideEncryptionS3(); 
  }

  static class ServerSideEncryptionWithCustomerKey extends ServerSideEncryption {

    protected final SecretKey secretKey;
    protected final MessageDigest md5;     
    
    public ServerSideEncryptionWithCustomerKey(SecretKey key, MessageDigest md5) {
      this.secretKey = key;
      this.md5 = md5;
    }

    @Override
    public final Type getType() { 
      return Type.SSE_C; 
    }
    
    @Override
    public void marshal(Map<String, String> headers) {
      if (this.isDestroyed()) {
        throw new IllegalStateException("object is already destroyed");
      }
      try {
        final byte[] key = secretKey.getEncoded();
        md5.update(key);
            
        headers.put("X-Amz-Server-Side-Encryption-Customer-Algorithm", "AES256");
        headers.put("X-Amz-Server-Side-Encryption-Customer-Key",  BaseEncoding.base64().encode(key));
        headers.put("X-Amz-Server-Side-Encryption-Customer-Key-Md5", BaseEncoding.base64().encode(md5.digest()));
      } finally {
        md5.reset();
      }    
    }
    
    @Override
    public final void destroy() throws DestroyFailedException { 
      secretKey.destroy(); 
    }
  
    @Override
    public final boolean isDestroyed() {
      return secretKey.isDestroyed(); 
    }
  }

  static final class ServerSideEncryptionCopyWithCustomerKey extends ServerSideEncryptionWithCustomerKey {
    
    public ServerSideEncryptionCopyWithCustomerKey(SecretKey key, MessageDigest md5) {
        super(key, md5);
    }

    @Override
    public final void marshal(Map<String, String> headers) {
      if (this.isDestroyed()) {
        throw new IllegalStateException("object is already destroyed");
      }
      try {
        final byte[] key = secretKey.getEncoded();
        md5.update(key);
        final String md5Sum = BaseEncoding.base64().encode(md5.digest());
        headers.put("X-Amz-Copy-Source​-Server-Side-Encryption-Customer-Algorithm", "AES256");
        headers.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key",  BaseEncoding.base64().encode(key));
        headers.put("X-Amz-Copy-Source-Server-Side-Encryption-Customer-Key-Md5", md5Sum);
      } finally {
        md5.reset();
      }    
    }
  }

  static final class ServerSideEncryptionS3 extends ServerSideEncryption {
    @Override
    public final Type getType() { 
      return Type.SSE_S3; 
    }

    @Override
    public final void marshal(Map<String, String> headers) { 
      headers.put("X-Amz-Server-Side-Encryption", "AES256"); 
    }

    @Override
    public final void destroy() throws DestroyFailedException {}
  }
}