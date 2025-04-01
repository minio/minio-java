/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015-2021 MinIO, Inc.
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

import io.minio.Utils;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Helper class to denote Object information in {@link ListBucketResultV1}, {@link
 * ListBucketResultV2} and {@link ListVersionsResult}.
 */
public abstract class Item {
  @Element(name = "ETag", required = false)
  private String etag; // except DeleteMarker

  @Element(name = "Key")
  private String objectName;

  @Element(name = "LastModified")
  private ResponseDate lastModified;

  @Element(name = "Owner", required = false)
  private Owner owner;

  @Element(name = "Size", required = false)
  private long size; // except DeleteMarker

  @Element(name = "StorageClass", required = false)
  private String storageClass; // except DeleteMarker, not in case of MinIO server.

  @Element(name = "IsLatest", required = false)
  private boolean isLatest; // except ListObjects V1

  @Element(name = "VersionId", required = false)
  private String versionId; // except ListObjects V1

  @Element(name = "UserMetadata", required = false)
  private Metadata userMetadata;

  @Element(name = "UserTags", required = false)
  private String userTags;

  @ElementList(name = "ChecksumAlgorithm", inline = true, required = false)
  private List<String> checksumAlgorithm;

  @Element(name = "ChecksumType", required = false)
  private String checksumType;

  @Element(name = "RestoreStatus", required = false)
  private RestoreStatus restoreStatus;

  private boolean isDir = false;
  private String encodingType = null;

  public Item() {}

  /** Constructs a new Item for prefix i.e. directory. */
  public Item(String prefix) {
    this.objectName = prefix;
    this.isDir = true;
  }

  public void setEncodingType(String encodingType) {
    this.encodingType = encodingType;
  }

  /** Returns object name. */
  public String objectName() {
    return Utils.urlDecode(objectName, encodingType);
  }

  /** Returns last modified time of the object. */
  public ZonedDateTime lastModified() {
    return (lastModified == null) ? null : lastModified.zonedDateTime();
  }

  /** Returns ETag of the object. */
  public String etag() {
    return etag;
  }

  /** Returns object size. */
  public long size() {
    return size;
  }

  /** Returns storage class of the object. */
  public String storageClass() {
    return storageClass;
  }

  /** Returns owner object of given the object. */
  public Owner owner() {
    return owner;
  }

  /** Returns user metadata. This is MinIO specific extension to ListObjectsV2. */
  public Map<String, String> userMetadata() {
    return (userMetadata == null) ? null : userMetadata.get();
  }

  public String userTags() {
    return userTags;
  }

  /** Returns whether this version ID is latest. */
  public boolean isLatest() {
    return isLatest;
  }

  /** Returns version ID. */
  public String versionId() {
    return versionId;
  }

  /** Returns whether this item is a directory or not. */
  public boolean isDir() {
    return isDir;
  }

  /** Returns whether this item is a delete marker or not. */
  public boolean isDeleteMarker() {
    return this instanceof DeleteMarker;
  }

  public List<String> checksumAlgorithm() {
    return Utils.unmodifiableList(checksumAlgorithm);
  }

  public String checksumType() {
    return checksumType;
  }

  public RestoreStatus restoreStatus() {
    return restoreStatus;
  }

  @Root(name = "RestoreStatus", strict = false)
  public static class RestoreStatus {
    @Element(name = "IsRestoreInProgress", required = false)
    private Boolean isRestoreInProgress;

    @Element(name = "RestoreExpiryDate", required = false)
    private ResponseDate restoreExpiryDate;

    public RestoreStatus(
        @Element(name = "IsRestoreInProgress", required = false) Boolean isRestoreInProgress,
        @Element(name = "RestoreExpiryDate", required = false) ResponseDate restoreExpiryDate) {
      this.isRestoreInProgress = isRestoreInProgress;
      this.restoreExpiryDate = restoreExpiryDate;
    }

    public Boolean isRestoreInProgress() {
      return isRestoreInProgress;
    }

    public ZonedDateTime restoreExpiryDate() {
      return restoreExpiryDate == null ? null : restoreExpiryDate.zonedDateTime();
    }
  }
}
