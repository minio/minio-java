/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2020 MinIO, Inc.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Helper class to denote object information for {@link EventMetadata}. */
public class ObjectMetadata {
  @JsonProperty private String key;
  @JsonProperty private long size;
  @JsonProperty private String eTag;
  @JsonProperty private String versionId;
  @JsonProperty private String sequencer;
  @JsonProperty private Map<String, String> userMetadata; // MinIO specific extension.

  public String key() {
    return key;
  }

  public long size() {
    return size;
  }

  public String etag() {
    return eTag;
  }

  public String versionId() {
    return versionId;
  }

  public String sequencer() {
    return sequencer;
  }

  public Map<String, String> userMetadata() {
    return Collections.unmodifiableMap(userMetadata == null ? new HashMap<>() : userMetadata);
  }
}
