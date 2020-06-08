/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote deleted object for {@link DeleteResult}. */
@Root(name = "Deleted", strict = false)
public class DeletedObject {
  @Element(name = "Key")
  private String name;

  @Element(name = "VersionId", required = false)
  private String versionId;

  @Element(name = "DeleteMarker", required = false)
  private boolean deleteMarker;

  @Element(name = "DeleteMarkerVersionId", required = false)
  private String deleteMarkerVersionId;

  public DeletedObject() {}

  public String name() {
    return name;
  }

  public String versionId() {
    return versionId;
  }

  public boolean deleteMarker() {
    return deleteMarker;
  }

  public String deleteMarkerVersionId() {
    return deleteMarkerVersionId;
  }
}
