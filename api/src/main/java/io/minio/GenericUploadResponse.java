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

package io.minio;

import io.minio.messages.CompleteMultipartUploadResult;
import io.minio.messages.CopyObjectResult;
import okhttp3.Headers;

/** Common response of {@link ObjectWriteResponse} and {@link PutObjectFanOutResponse}. */
public class GenericUploadResponse extends GenericResponse {
  private String etag;
  private String checksumCRC32;
  private String checksumCRC32C;
  private String checksumCRC64NVME;
  private String checksumSHA1;
  private String checksumSHA256;
  private String checksumType;

  public GenericUploadResponse(
      Headers headers, String bucket, String region, String object, String etag) {
    super(headers, bucket, region, object);
    this.etag = etag;
    if (headers != null) {
      this.checksumCRC32 = headers.get("x-amz-checksum-crc32");
      this.checksumCRC32C = headers.get("x-amz-checksum-crc32c");
      this.checksumCRC64NVME = headers.get("x-amz-checksum-crc64nvme");
      this.checksumSHA1 = headers.get("x-amz-checksum-sha1");
      this.checksumSHA256 = headers.get("x-amz-checksum-sha256");
      this.checksumType = headers.get("x-amz-checksum-type");
    }
  }

  public GenericUploadResponse(
      Headers headers,
      String bucket,
      String region,
      String object,
      String etag,
      CopyObjectResult result) {
    super(headers, bucket, region, object);
    this.etag = etag;
    if (result != null) {
      this.checksumType = result.checksumType();
      this.checksumCRC32 = result.checksumCRC32();
      this.checksumCRC32C = result.checksumCRC32C();
      this.checksumCRC64NVME = result.checksumCRC64NVME();
      this.checksumSHA1 = result.checksumSHA1();
      this.checksumSHA256 = result.checksumSHA256();
    }
  }

  public GenericUploadResponse(
      Headers headers,
      String bucket,
      String region,
      String object,
      String etag,
      CompleteMultipartUploadResult result) {
    super(headers, bucket, region, object);
    this.etag = etag;
    if (result != null) {
      this.checksumType = result.checksumType();
      this.checksumCRC32 = result.checksumCRC32();
      this.checksumCRC32C = result.checksumCRC32C();
      this.checksumCRC64NVME = result.checksumCRC64NVME();
      this.checksumSHA1 = result.checksumSHA1();
      this.checksumSHA256 = result.checksumSHA256();
    }
  }

  public String etag() {
    return etag;
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
}
