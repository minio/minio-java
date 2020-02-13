/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2020 MinIO, Inc.
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
import java.util.Map;

public class PutObjectOptions {
  // allowed maximum object size is 5TiB.
  public static final long MAX_OBJECT_SIZE = 5L * 1024 * 1024 * 1024 * 1024;
  // allowed minimum part size is 5MiB in multipart upload.
  public static final int MIN_MULTIPART_SIZE = 5 * 1024 * 1024;
  // allowed minimum part size is 5GiB in multipart upload.
  public static final long MAX_PART_SIZE = 5L * 1024 * 1024 * 1024;
  public static final int MAX_MULTIPART_COUNT = 10000;

  private long objectSize = -1;
  private long partSize = -1;
  private int partCount = -1;
  private String contentType = "application/octet-stream";
  private Map<String, String> headers = null;
  private ServerSideEncryption sse = null;

  /**
   * Creates new PutObjectOptions object. Two ways to use PutObjectOptions when object size is concerned.
   * * If object size is unknown, pass -1 to objectSize and pass valid partSize.
   * * If object size is known, pass -1 to partSize for auto detect; else pass valid partSize to control
   *   memory usage and no. of parts in upload.
   * * If partSize is greater than objectSize, objectSize is used as partSize.
   *
   * <p>A valid part size is between 5MiB to 5GiB (both limits inclusive).
   */
  public PutObjectOptions(long objectSize, long partSize) throws InvalidArgumentException {
    if (partSize > 0) {
      if (partSize < MIN_MULTIPART_SIZE) {
        throw new InvalidArgumentException("part size " + partSize + " is not supported; minimum allowed 5MiB");
      }

      if (partSize > MAX_PART_SIZE) {
        throw new InvalidArgumentException("part size " + partSize + " is not supported; maximum allowed 5GiB");
      }
    }

    if (objectSize >= 0) {
      if (objectSize > MAX_OBJECT_SIZE) {
        throw new InvalidArgumentException("object size " + objectSize + " is not supported; maximum allowed 5TiB");
      }

      this.objectSize = objectSize;

      if (partSize > 0) {
        if (partSize > objectSize) {
          partSize = objectSize;
        }

        this.partSize = partSize;
        this.partCount = (int) Math.ceil((double) objectSize / partSize);
        if (this.partCount > MAX_MULTIPART_COUNT) {
          throw new InvalidArgumentException("object size " + this.objectSize + " and part size " + this.partSize
                                             + " make more than " + MAX_MULTIPART_COUNT + "parts for upload");
        }
      } else {
        double pSize = Math.ceil((double) objectSize / MAX_MULTIPART_COUNT);
        pSize = Math.ceil(pSize / MIN_MULTIPART_SIZE) * MIN_MULTIPART_SIZE;

        this.partSize = (long) pSize;

        if (pSize > 0) {
          this.partCount = (int) Math.ceil(objectSize / pSize);
        } else {
          this.partCount = 1;
        }
      }

      return;
    }

    if (partSize <= 0) {
      throw new InvalidArgumentException("valid part size must be provided when object size is unknown");
    }

    this.objectSize = -1;
    this.partSize = partSize;
    this.partCount = -1;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public void setSse(ServerSideEncryption sse) {
    this.sse = sse;
  }

  public long objectSize() {
    return this.objectSize;
  }

  public long partSize() {
    return this.partSize;
  }

  public int partCount() {
    return this.partCount;
  }

  public String contentType() {
    return this.contentType;
  }

  public Map<String, String> headers() {
    return this.headers;
  }

  public ServerSideEncryption sse() {
    return this.sse;
  }
}
