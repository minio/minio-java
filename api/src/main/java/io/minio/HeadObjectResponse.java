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

import io.minio.messages.LegalHold;
import io.minio.messages.RetentionMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Response of {@link BaseS3Client#headObject}. */
public class HeadObjectResponse extends GenericResponse {
  private String etag;
  private long size;
  private ZonedDateTime lastModified;
  private RetentionMode retentionMode;
  private ZonedDateTime retentionRetainUntilDate;
  private LegalHold legalHold;
  private boolean deleteMarker;
  private Http.Headers userMetadata;
  private Checksum.Type checksumType;

  public HeadObjectResponse(okhttp3.Headers headers, String bucket, String region, String object) {
    super(headers, bucket, region, object);
    String value;

    value = headers.get("ETag");
    this.etag = (value != null ? value.replaceAll("\"", "") : "");

    value = headers.get(Http.Headers.CONTENT_LENGTH);
    this.size = (value != null ? Long.parseLong(value) : -1);

    this.lastModified =
        ZonedDateTime.parse(headers.get("Last-Modified"), Time.HTTP_HEADER_DATE_FORMAT);

    value = headers.get("x-amz-object-lock-mode");
    this.retentionMode = (value != null ? RetentionMode.valueOf(value) : null);

    value = headers.get("x-amz-object-lock-retain-until-date");
    this.retentionRetainUntilDate =
        value == null ? null : Time.S3Time.fromString(value).toZonedDateTime();

    this.legalHold = new LegalHold("ON".equals(headers.get("x-amz-object-lock-legal-hold")));

    this.deleteMarker = Boolean.parseBoolean(headers.get("x-amz-delete-marker"));

    Http.Headers userMetadata = new Http.Headers();
    for (Map.Entry<String, List<String>> entry : headers.toMultimap().entrySet()) {
      String lowerName = entry.getKey().toLowerCase(Locale.US);
      if (lowerName.startsWith("x-amz-meta-")) {
        userMetadata.put(
            lowerName.substring("x-amz-meta-".length(), lowerName.length()), entry.getValue());
      }
    }
    this.userMetadata = userMetadata;

    value = headers.get("x-amz-checksum-type");
    this.checksumType = value == null ? null : Checksum.Type.valueOf(value);
  }

  public String etag() {
    return etag;
  }

  public long size() {
    return size;
  }

  public ZonedDateTime lastModified() {
    return lastModified;
  }

  public RetentionMode retentionMode() {
    return retentionMode;
  }

  public ZonedDateTime retentionRetainUntilDate() {
    return retentionRetainUntilDate;
  }

  public LegalHold legalHold() {
    return legalHold;
  }

  public boolean deleteMarker() {
    return deleteMarker;
  }

  public String versionId() {
    return this.headers().get("x-amz-version-id");
  }

  public String contentType() {
    return this.headers().get(Http.Headers.CONTENT_TYPE);
  }

  public Http.Headers userMetadata() {
    return userMetadata;
  }

  public Checksum.Type checksumType() {
    return checksumType;
  }

  public List<Checksum.Algorithm> algorithms() {
    okhttp3.Headers headers = headers();
    List<Checksum.Algorithm> algorithms = new ArrayList<>();
    String value;

    value = headers.get("x-amz-checksum-crc32");
    if (value != null && !value.isEmpty()) algorithms.add(Checksum.Algorithm.CRC32);

    value = headers.get("x-amz-checksum-crc32c");
    if (value != null && !value.isEmpty()) algorithms.add(Checksum.Algorithm.CRC32C);

    value = headers.get("x-amz-checksum-crc64nvme");
    if (value != null && !value.isEmpty()) algorithms.add(Checksum.Algorithm.CRC64NVME);

    value = headers.get("x-amz-checksum-sha1");
    if (value != null && !value.isEmpty()) algorithms.add(Checksum.Algorithm.SHA1);

    value = headers.get("x-amz-checksum-sha256");
    if (value != null && !value.isEmpty()) algorithms.add(Checksum.Algorithm.SHA256);

    return algorithms.size() == 0 ? null : algorithms;
  }

  @Override
  public String toString() {
    return "ObjectHead{"
        + "bucket="
        + bucket()
        + ", object="
        + object()
        + ", last-modified="
        + lastModified
        + ", size="
        + size
        + "}";
  }
}
