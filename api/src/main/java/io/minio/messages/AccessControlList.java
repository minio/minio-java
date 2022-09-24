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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/** Helper class to denote access control list of {@link S3OutputLocation}. */
@Root(name = "AccessControlList")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class AccessControlList {
  @ElementList(name = "Grant", inline = true)
  private List<Grant> grants;

  public AccessControlList(@Nonnull List<Grant> grants) {
    Objects.requireNonNull(grants, "Grants must not be null");
    if (grants.size() == 0) {
      throw new IllegalArgumentException("Grants must not be empty");
    }
    this.grants = Collections.unmodifiableList(grants);
  }
}
