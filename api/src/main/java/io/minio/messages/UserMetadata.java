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
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/** Helper class to denote user metadata information of {@link S3OutputLocation}. */
@Root(name = "UserMetadata", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class UserMetadata {
  @ElementMap(
      attribute = false,
      entry = "MetadataEntry",
      inline = true,
      key = "Name",
      value = "Value",
      required = false)
  Map<String, String> metadataEntries;

  private UserMetadata(@Nonnull Map<String, String> metadataEntries) {
    Objects.requireNonNull(metadataEntries, "Metadata entries must not be null");
    if (metadataEntries.size() == 0) {
      throw new IllegalArgumentException("Metadata entries must not be empty");
    }
    this.metadataEntries = Collections.unmodifiableMap(metadataEntries);
  }
}
