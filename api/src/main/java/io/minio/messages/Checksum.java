/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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

import io.minio.Http;
import io.minio.Utils;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Object checksum information. */
@Root(name = "Checksum", strict = false)
public class Checksum {
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

  @Element(name = "ChecksumType", required = false)
  private String checksumType;

  protected Checksum() {}

  public Checksum(
      @Nullable @Element(name = "ChecksumCRC32", required = false) String checksumCRC32,
      @Nullable @Element(name = "ChecksumCRC32C", required = false) String checksumCRC32C,
      @Nullable @Element(name = "ChecksumCRC64NVME", required = false) String checksumCRC64NVME,
      @Nullable @Element(name = "ChecksumSHA1", required = false) String checksumSHA1,
      @Nullable @Element(name = "ChecksumSHA256", required = false) String checksumSHA256,
      @Nullable @Element(name = "ChecksumType", required = false) String checksumType) {
    this.checksumCRC32 = checksumCRC32;
    this.checksumCRC32C = checksumCRC32C;
    this.checksumCRC64NVME = checksumCRC64NVME;
    this.checksumSHA1 = checksumSHA1;
    this.checksumSHA256 = checksumSHA256;
    this.checksumType = checksumType;
  }

  public Checksum(Checksum checksum) {
    this.checksumCRC32 = checksum.checksumCRC32;
    this.checksumCRC32C = checksum.checksumCRC32C;
    this.checksumCRC64NVME = checksum.checksumCRC64NVME;
    this.checksumSHA1 = checksum.checksumSHA1;
    this.checksumSHA256 = checksum.checksumSHA256;
    this.checksumType = checksum.checksumType;
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

  public String checksumType() {
    return checksumType;
  }

  private void addHeader(Http.Headers headers, String algorithm, String value) {
    if (value == null || value.isEmpty()) return;
    headers.put("x-amz-checksum-algorithm-" + algorithm, value);
    headers.put("x-amz-checksum-algorithm", algorithm);
  }

  public Http.Headers headers() {
    Http.Headers headers = new Http.Headers();
    addHeader(headers, "crc32", checksumCRC32);
    addHeader(headers, "crc32c", checksumCRC32C);
    addHeader(headers, "crc64nvme", checksumCRC64NVME);
    addHeader(headers, "sha1", checksumSHA1);
    addHeader(headers, "sha256", checksumSHA256);
    return headers;
  }

  protected String stringify() {
    return String.format(
        "checksumCRC32=%s, checksumCRC32C=%s, checksumCRC64NVME=%s, checksumSHA1=%s,"
            + " checksumSHA256=%s, checksumType=%s",
        Utils.stringify(checksumCRC32),
        Utils.stringify(checksumCRC32C),
        Utils.stringify(checksumCRC64NVME),
        Utils.stringify(checksumSHA1),
        Utils.stringify(checksumSHA256),
        Utils.stringify(checksumType));
  }

  @Override
  public String toString() {
    return String.format("Checksum{%s}", stringify());
  }
}
