/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2022 MinIO, Inc.
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

package io.minio.admin.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-tier stats of a remote tier.
 *
 * @see <a
 *     href="https://github.com/minio/minio/blob/master/cmd/data-usage-cache.go#L102">data-usage-cache.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TierStats {
  @JsonProperty("TotalSize")
  private long totalSize;

  @JsonProperty("NumVersions")
  private int numVersions;

  @JsonProperty("NumObjects")
  private int numObjects;

  public long totalSize() {
    return totalSize;
  }

  public int numVersions() {
    return numVersions;
  }

  public int numObjects() {
    return numObjects;
  }
}
