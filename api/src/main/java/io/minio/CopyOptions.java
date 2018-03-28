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

import javax.security.auth.DestroyFailedException;

import io.minio.errors.InvalidArgumentException;

public class CopyOptions extends Options {

  String contentType;
  ServerSideEncryption sourceEncryption;

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
    this.sourceEncryption = options.sourceEncryption;
  }

  @Override
  public void destroy() throws DestroyFailedException {
    if (encryption != null) {
      this.encryption.destroy();
    }
    if (sourceEncryption != null) {
      this.sourceEncryption.destroy();
    }
  }

  @Override
  public boolean isDestroyed() {
    if (this.encryption == null) {
      return false;
    }
    if (this.sourceEncryption == null) {
      return super.isDestroyed();
    }
    return super.isDestroyed() && this.sourceEncryption.isDestroyed();
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> headers = super.getHeaders();
    if (this.sourceEncryption != null) {
      this.sourceEncryption.marshal(headers);
    }
    if (contentType != null) {
      headers.put("Content-Type", this.contentType);
    }
    return headers;
  }
  
  @Override
  public CopyOptions setEncryption(ServerSideEncryption encryption) {
    super.setEncryption(encryption);
    return this;
  }

  public String getContentType() {
    return this.contentType;
  }

  public CopyOptions setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  @Override
  public CopyOptions setMetadata(String key, String value) {
    super.setMetadata(key, value);
    return this;
  }

  @Override
  public CopyOptions setMetadata(Map<String, String> metadata) {
    super.setMetadata(metadata);
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
   * @throws InvalidArgumentException if the rcDecryption is no SSE-C copy.
   */
  public CopyOptions setCopyEncryption(ServerSideEncryption srcDecryption, ServerSideEncryption dstEncryption)
    throws InvalidArgumentException {
    if (srcDecryption != null && !ServerSideEncryption.Type.SSE_C.equals(srcDecryption.getType())) {
      throw new InvalidArgumentException("source decryption must be SSE-C");
    }
    this.sourceEncryption = srcDecryption;
    this.encryption = dstEncryption;
    return this;
  }
}