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

  @Element(name = "ChecksumCRC32", required = false)
  private String checksumCRC32;

  @Element(name = "ChecksumCRC32C", required = false)
  private String checksumCRC32C;

  @Element(name = "ChecksumCRC64NVME", required = false)
  private String checksumCRC64NVME;

  @Element(name = "ChecksumSHA1", required = false)
  private String checksumSHA1;

  @Element(name = "ChecksumSHA256", required = false)
  private String checksumSHA256;

  public Part() {}

  /** Constructs a new Part object with given part number and ETag. */
  public Part(int partNumber, String etag) {

    this.partNumber = partNumber;
    this.etag = etag;
  }

  /** Constructs a new Part object with given values. */
  public Part(
      int partNumber,
      String etag,
      String checksumCRC32,
      String checksumCRC32C,
      String checksumCRC64NVME,
      String checksumSHA1,
      String checksumSHA256) {
    this.partNumber = partNumber;
    this.etag = etag;
    this.checksumCRC32 = checksumCRC32;
    this.checksumCRC32C = checksumCRC32C;
    this.checksumCRC64NVME = checksumCRC64NVME;
    this.checksumSHA1 = checksumSHA1;
    this.checksumSHA256 = checksumSHA256;
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

  public String checksumCRC32() {
    return checksumCRC32;
  }

  public String checksumCRC32C() {
    return checksumCRC32C;
  }

  public String checksumCRC64NVME() {
    return checksumCRC64NVME;
  }

  public String checksumSHA1() {
    return checksumSHA1;
  }

  public String checksumSHA256() {
    return checksumSHA256;
  }
}
