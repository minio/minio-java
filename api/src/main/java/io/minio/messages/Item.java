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
import java.util.Map;
import org.simpleframework.xml.Element;

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
  private String storageClass; // except DeleteMarker

  @Element(name = "IsLatest", required = false)
  private boolean isLatest; // except ListObjects V1

  @Element(name = "VersionId", required = false)
  private String versionId; // except ListObjects V1

  @Element(name = "UserMetadata", required = false)
  private Metadata userMetadata;

  private boolean isDir = false;

  public Item() {}

  /** Constructs a new Item for prefix i.e. directory. */
  public Item(String prefix) {
    this.objectName = prefix;
    this.isDir = true;
  }

  /** Returns object name. */
  public String objectName() {
    return objectName;
  }

  /** Returns last modified time of the object. */
  public ZonedDateTime lastModified() {
    return lastModified.zonedDateTime();
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
    return (etag == null && size == 0 && storageClass == null && versionId != null);
  }
}
