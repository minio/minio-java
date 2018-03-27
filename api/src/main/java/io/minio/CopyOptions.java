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

import java.util.Map;
import java.util.Map.Entry;

import io.minio.errors.InvalidArgumentException;

public class CopyOptions extends Options {

  /**
   * CopyOptions default constructor.
   */
  public CopyOptions() {}

  /**
   * Create a new CopyOptions object from the provided options.
   * @param options The COPY options. 
   */
  public CopyOptions(CopyOptions options) {
    super(options);
  }

  /**
   * Set the COPY headers for server-side-encryption. If the encryption is
   * SSE-C only the source object is decrypt but the desitantion object is not
   * encrypted. 
   * 
   * @param encryption The server-side-encryption method (SSE-C, SSE-S3 or SSE-KMS).
   * @return the modified PutOptions instance.
   * @throws InvalidArgumentException if the encryption parameter is null.
   */
  @Override
  public CopyOptions setEncryption(ServerSideEncryption encryption) throws InvalidArgumentException {
    super.setEncryption(encryption);
    return this;
  }

  /**
   * Set the COPY headers for server-side encryption. The srcDecryption must be SSE-C copy encryption.
   * The dstEncryption can be any server-side encryption. SSE-C only the source object is decrypt but
   * the desitantion object is not encrypted.
   * 
   * @param srcDecryption The server-side-encryption method - only SSE-C copy
   * @param dstEncryption The server-side-encryption method (SSE-C, SSE-S3 or SSE-KMS).
   * @return the modified PutOptions instance.
   * @throws InvalidArgumentException if the encryption srcDecryption / dstEncryption is null or 
   *                                  the srcDecryption is no SSE-C copy.
   */
  public CopyOptions setEncryption(ServerSideEncryption srcDecryption, ServerSideEncryption dstEncryption)
    throws InvalidArgumentException {
    if (srcDecryption == null) {
      throw new InvalidArgumentException("source decryption cannot be null");
    }
    if (dstEncryption == null) {
      throw new InvalidArgumentException("destination encryption cannot be null");
    }
    if (!ServerSideEncryption.Type.SSE_C.equals(srcDecryption.getType())) {
      throw new InvalidArgumentException("source decryption must be SSE-C");
    }

    srcDecryption.marshal(this.headers);
    dstEncryption.marshal(this.headers);
    return this;
  }

  /**
   * Set the content type of uploaded object.
   * @param contentType The content type.
   * @return the modified CopyOptions instance.
   */
  public CopyOptions setContentType(String contentType) {
    setHeader("Content-Type", contentType);
    return this;
  }

  /**
   * Set the metadata key-value pair. If the key does not start
   * with "X-Amz-" the prefix "X-Amz-Meta-" is added to the key. 
   * @param key   The metadata key.
   * @param value The metadata value.
   * @return the modified CopyOptions instance.
   */
  public CopyOptions setMetadata(String key, String value) {
    String normKey = key.toLowerCase();
    if (!normKey.startsWith("x-amz-meta-") && !normKey.startsWith("x-amz-")) {
      key = "X-Amz-Meta-" + key;
    }
    setHeader(key, value);
    return this;
  }

  /**
   * Set the metadata key-value pairs. If the one key does not start
   * with "X-Amz-" the prefix "X-Amz-Meta-" is added to that key. 
   * @param metadata The metadata key-value map.
   * @return the modified CopyOptions instance.
   */
  public CopyOptions setMetadata(Map<String, String> metadata) {
    for (Entry<String,String> entry : metadata.entrySet()) {
      setMetadata(entry.getKey(), entry.getValue());
    }
    return this;
  }
}