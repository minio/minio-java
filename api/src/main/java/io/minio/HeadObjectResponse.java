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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Response of {@link BaseS3Client#headObject}. */
public class HeadObjectResponse extends GenericResponse {
  private String etag;
  private long size;
  private ZonedDateTime lastModified;
  private RetentionMode lockMode;
  private ZonedDateTime lockRetainUntilDate;
  private LegalHold lockLegalHold;
  private boolean deleteMarker;
  private Http.Headers userMetadata;
  private Map<Checksum.Algorithm, String> checksums;
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
    this.lockMode = (value != null ? RetentionMode.valueOf(value) : null);

    value = headers.get("x-amz-object-lock-retain-until-date");
    this.lockRetainUntilDate =
        value == null ? null : Time.S3Time.fromString(value).toZonedDateTime();

    this.lockLegalHold = new LegalHold("ON".equals(headers.get("x-amz-object-lock-legal-hold")));

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

    Map<Checksum.Algorithm, String> checksums = new HashMap<>();
    value = headers.get("x-amz-checksum-crc32");
    if (value != null && !value.isEmpty()) checksums.put(Checksum.Algorithm.CRC32, value);
    value = headers.get("x-amz-checksum-crc32c");
    if (value != null && !value.isEmpty()) checksums.put(Checksum.Algorithm.CRC32C, value);
    value = headers.get("x-amz-checksum-crc64nvme");
    if (value != null && !value.isEmpty()) checksums.put(Checksum.Algorithm.CRC64NVME, value);
    value = headers.get("x-amz-checksum-sha1");
    if (value != null && !value.isEmpty()) checksums.put(Checksum.Algorithm.SHA1, value);
    value = headers.get("x-amz-checksum-sha256");
    if (value != null && !value.isEmpty()) checksums.put(Checksum.Algorithm.SHA256, value);
    if (!checksums.isEmpty()) this.checksums = checksums;
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

  public RetentionMode lockMode() {
    return lockMode;
  }

  public ZonedDateTime lockRetainUntilDate() {
    return lockRetainUntilDate;
  }

  public LegalHold lockLegalHold() {
    return lockLegalHold;
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

  public Map<Checksum.Algorithm, String> checksums() {
    return checksums;
  }

  @Override
  public String toString() {
    return "HeadObjectResponse{"
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
