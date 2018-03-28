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
import java.util.HashMap;

import javax.security.auth.Destroyable;
import javax.security.auth.DestroyFailedException;

abstract class Options implements Destroyable {

  protected final Map<String,String> metadata = new HashMap<>();
  
  protected ServerSideEncryption encryption;

  public Options() {}

  public Options(Options options) {
    if (options != null) {
      this.metadata.putAll(options.metadata);
      this.encryption = options.encryption;
    }
  }

  @Override
  public void destroy() throws DestroyFailedException {
    if (encryption != null) {
      this.encryption.destroy();
    }
  }

  @Override
  public boolean isDestroyed() {
    if (encryption == null) {
      return false;
    }
    return this.encryption.isDestroyed();
  }

  public Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    if (this.encryption != null) {
      this.encryption.marshal(headers);
    }
    headers.putAll(this.metadata);
    return headers;
  }

  public Options setEncryption(ServerSideEncryption encryption) {
    this.encryption = encryption;
    return this;
  }

  public ServerSideEncryption getEncryption() {
    return this.encryption;
  }

  public Options setMetadata(String key, String value) {
    String normKey = key.toLowerCase();
    if (!normKey.startsWith("x-amz-meta-")) {
      key = "X-Amz-Meta-" + key;
    }
    metadata.put(key, value);
    return this;
  }

  public Options setMetadata(Map<String, String> metadata) {
    for (Entry<String,String> entry : metadata.entrySet()) {
      setMetadata(entry.getKey(), entry.getValue());
    }
    return this;
  }
}