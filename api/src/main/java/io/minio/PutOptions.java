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

/**
 * PUT object options for parameterizing a PUT request.
 */
public class PutOptions extends Options {

  /**
   * PutOptions default constructor.
   */
  public PutOptions() {}

  /**
   * Create a new PutOptions object from the provided options.
   * @param options The PUT objects. 
   */
  public PutOptions(PutOptions options) {
    super(options);
  }

  /**
   * Set the PUT headers for server-side-encryption.
   * 
   * @param encryption The server-side-encryption method (SSE-C, SSE-S3 or SSE-KMS).
   * @return the modified PutOptions instance.
   * @throws InvalidArgumentException if the encryption parameter is null.
   */
  public PutOptions setEncryption(ServerSideEncryption encryption) throws InvalidArgumentException {
    if (encryption == null) {
      throw new InvalidArgumentException("encryption cannot be null");
    }
    encryption.marshal(this.headers);
    return this;
  }

  /**
   * Set the content type of uploaded object.
   * @param contentType The content type.
   * @return the modified PutOptions instance.
   */
  public PutOptions setContentType(String contentType) {
    setHeader("Content-Type", contentType);
    return this;
  }

  /**
   * Set the metadata key-value pair. If the key does not start
   * with "X-Amz-" the prefix "X-Amz-Meta-" is added to the key. 
   * @param key   The metadata key.
   * @param value The metadata value.
   * @return the modified PutOptions instance.
   */
  public PutOptions setMetadata(String key, String value) {
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
   * @return the modified PutOptions instance.
   */
  public PutOptions setMetadata(Map<String, String> metadata) {
    for (Entry<String,String> entry : metadata.entrySet()) {
      setMetadata(entry.getKey(), entry.getValue());
    }
    return this;
  }
}