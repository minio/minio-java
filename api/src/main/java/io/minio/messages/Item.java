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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;
import org.simpleframework.xml.Element;

/**
 * Helper class to denote Object information in {@link ListBucketResultV1}, {@link
 * ListBucketResultV2} and {@link ListVersionsResult}.
 */
public abstract class Item {
  private static final String UTF_8 = StandardCharsets.UTF_8.toString();

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
    try {
      return "url".equals(encodingType) ? URLDecoder.decode(objectName, UTF_8) : objectName;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
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
}
