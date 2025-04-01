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

import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote grant information of {@link AccessControlList}. */
@Root(name = "Grant")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class Grant {
  @Element(name = "Grantee", required = false)
  private Grantee grantee;

  @Element(name = "Permission", required = false)
  private Permission permission;

  public Grant(
      @Nullable @Element(name = "Grantee", required = false) Grantee grantee,
      @Nullable @Element(name = "Permission", required = false) Permission permission) {
    if (grantee == null && permission == null) {
      throw new IllegalArgumentException("Either Grantee or Permission must be provided");
    }
    this.grantee = grantee;
    this.permission = permission;
  }

  public Grantee grantee() {
    return grantee;
  }

  public Permission permission() {
    return permission;
  }

  public String granteeUri() {
    return grantee == null ? null : grantee.uri();
  }

  public String granteeId() {
    return grantee == null ? null : grantee.id();
  }
}
