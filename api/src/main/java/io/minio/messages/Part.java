/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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

package io.minio.messages;

import java.time.ZonedDateTime;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Helper class to denote Part information of a multipart upload and used in {@link
 * CompleteMultipartUpload} and {@link ListPartsResult}.
 */
@Root(name = "Part", strict = false)
public class Part {
  @Element(name = "PartNumber")
  private int partNumber;

  @Element(name = "ETag")
  private String etag;

  @Element(name = "LastModified", required = false)
  private ResponseDate lastModified;

  @Element(name = "Size", required = false)
  private Long size;

  public Part() {}

  /** Constructs a new Part object with given part number and ETag. */
  public Part(int partNumber, String etag) {

    this.partNumber = partNumber;
    this.etag = etag;
  }

  /** Returns part number. */
  public int partNumber() {
    return partNumber;
  }

  /** Returns ETag. */
  public String etag() {
    return etag.replaceAll("\"", "");
  }

  /** Returns last modified time. */
  public ZonedDateTime lastModified() {
    return lastModified.zonedDateTime();
  }

  /** Returns part size. */
  public long partSize() {
    return size;
  }
}
