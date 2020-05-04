/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

public class ObjectWithVersion {
  private String objectName;
  private String versionId;

  public String name() {
    return objectName;
  }

  public String versionId() {
    return versionId;
  }

  public ObjectWithVersion(String objectName) {
    if (objectName == null || objectName.isEmpty()) {
      throw new IllegalArgumentException("null or empty Object name");
    }
    this.objectName = objectName;
  }

  public ObjectWithVersion(String objectName, String versionId) {
    if (objectName == null || objectName.isEmpty()) {
      throw new IllegalArgumentException("null or empty Object name");
    }
    if (versionId == null || versionId.isEmpty()) {
      throw new IllegalArgumentException("null or empty version");
    }
    this.objectName = objectName;
    this.versionId = versionId;
  }

  @Override
  public String toString() {
    return "objectName='" + this.objectName + ", versionId='" + this.versionId;
  }
}
