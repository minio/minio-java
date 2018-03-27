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
import java.util.HashMap;
import java.util.Optional;

import io.minio.errors.InvalidArgumentException;

abstract class Options {

  protected final Map<String,String> headers = new HashMap<>();

  public Options() {}

  public Options(Options options) {
    if (options != null) {
      this.headers.putAll(options.headers);
    }
  }

  protected Options setEncryption(ServerSideEncryption encryption) throws InvalidArgumentException {
    if (encryption == null) {
      throw new InvalidArgumentException("encryption cannot be null");
    }
    encryption.marshal(this.headers);
    return this;
  }

  protected void setHeader(String key, String value) {
    headers.put(key, value);
  }

  public Optional<String> valueOf(String key) {
    String value = headers.get(key);
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(value);
  } 
}