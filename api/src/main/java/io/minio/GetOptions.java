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

import io.minio.errors.InvalidArgumentException;

/**
 * GET object options for parameterizing a GET request.
 */
public class GetOptions extends Options {

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
  }

  /**
   * Set the GET headers for server-side-encryption.
   * 
   * @param encryption The server-side-encryption method (SSE-C, SSE-S3 or SSE-KMS).
   * @return the modified GetOptions instance.
   * @throws InvalidArgumentException if the encryption parameter is null.
   */
  public GetOptions setEncryption(ServerSideEncryption encryption) throws InvalidArgumentException {
    if (encryption == null) {
      throw new InvalidArgumentException("encryption cannot be null");
    }
    encryption.marshal(this.headers);
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
    setHeader("Range", "bytes=" + offset + "-");
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
    setHeader("Range", "bytes=" + offset + "-" + (offset + length - 1));
    return this;
  }
}