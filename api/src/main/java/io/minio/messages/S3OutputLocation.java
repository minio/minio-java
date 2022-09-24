/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
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

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote S3 output location information of {@link OutputLocation}. */
@Root(name = "S3OutputLocation")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class S3OutputLocation {
  @Element(name = "AccessControlList", required = false)
  private AccessControlList accessControlList;

  @Element(name = "BucketName")
  private String bucketName;

  @Element(name = "CannedACL", required = false)
  private CannedAcl cannedAcl;

  @Element(name = "Encryption", required = false)
  private Encryption encryption;

  @Element(name = "Prefix")
  private String prefix;

  @Element(name = "StorageClass", required = false)
  private String storageClass;

  @Element(name = "Tagging", required = false)
  private Tags tagging;

  @Element(name = "UserMetadata", required = false)
  private UserMetadata userMetadata;

  public S3OutputLocation(
      @Nonnull String bucketName,
      @Nonnull String prefix,
      @Nullable AccessControlList accessControlList,
      @Nullable CannedAcl cannedAcl,
      @Nullable Encryption encryption,
      @Nullable String storageClass,
      @Nullable Tags tagging,
      @Nullable UserMetadata userMetadata) {
    this.bucketName = Objects.requireNonNull(bucketName, "Bucket name must not be null");
    this.prefix = Objects.requireNonNull(prefix, "Prefix must not be null");
    this.accessControlList = accessControlList;
    this.cannedAcl = cannedAcl;
    this.encryption = encryption;
    this.storageClass = storageClass;
    this.tagging = tagging;
    this.userMetadata = userMetadata;
  }
}
