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
import java.util.OptionalLong;

import io.minio.errors.InvalidArgumentException;

/**
 * GET object options for parameterizing a GET request.
 */
public class GetOptions extends Options {

  OptionalLong offset = OptionalLong.empty();
  OptionalLong length = OptionalLong.empty();

  /**
   * GetOptions default constructor.
   */
  public GetOptions() {}

  /**
   * Create a new GetOptions object from the provided options.
   * @param options The GET objects. 
   */
  public GetOptions(GetOptions options) {
    super(options);
    this.offset = options.offset;
    this.length = options.length;
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> headers = super.getHeaders();
    if (this.offset.isPresent() && this.length.isPresent()) {
      headers.put("Range", "bytes=" + this.offset.getAsLong() + "-" 
          + (this.offset.getAsLong() + this.length.getAsLong() - 1));
    } else if (this.offset.isPresent()) {
      headers.put("Range", "bytes=" + this.offset.getAsLong() + "-");
    }
    return headers;
  }

  @Override
  public GetOptions setEncryption(ServerSideEncryption encryption) {
    super.setEncryption(encryption);
    return this;
  }

  @Override
  public GetOptions setMetadata(String key, String value) {
    super.setMetadata(key, value);
    return this;
  }
  
  @Override
  public GetOptions setMetadata(Map<String, String> metadata) {
    super.setMetadata(metadata);
    return this;
  }

  /**
   * Set the HTTP range header using the povided offset.
   * 
   * @param offset The offset from which the GET should read.
   * @return the modified GetOptions instance.
   * @throws InvalidArgumentException if the offset is smaller than 0.
   */
  public GetOptions setRange(long offset) throws InvalidArgumentException {
    if (offset < 0) {
      throw new InvalidArgumentException("offset must not be negative");
    }
    this.offset = OptionalLong.of(offset);
    this.length = OptionalLong.empty();
    
    return this;
  }

  /**
   * Set the HTTP range header using the povided offset and length.
   * 
   * @param offset The offset from which the GET should read.
   * @param length The number of bytes which should be read.
   * @return the modified GetOptions instance.
   * @throws InvalidArgumentException if the offset is smaller than 0, 
   *                                  the length is smaller than 1 or the
   *                                  length is smaller than the offset.
   */
  public GetOptions setRange(long offset, long length) throws InvalidArgumentException {
    if (offset < 0) {
      throw new InvalidArgumentException("offset must not be negative");
    }
    if (length <= 0) {
      throw new InvalidArgumentException("length should be greater than zero");
    }
    if (length < offset) {
      throw new InvalidArgumentException("length should be greater than offset");
    }
    this.offset = OptionalLong.of(offset);
    this.length = OptionalLong.of(length);
    return this;
  }
}