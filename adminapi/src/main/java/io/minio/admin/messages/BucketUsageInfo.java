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
import java.util.Collections;
import java.util.Map;

/**
 * Represents bucket usage stats of the current object APi.
 *
 * @see <a
 *     href="https://github.com/minio/minio/blob/master/cmd/data-usage-utils.go#L47">data-usage-utils.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BucketUsageInfo {
  @JsonProperty("size")
  private long size;

  @JsonProperty("objectsPendingReplicationTotalSize")
  private long objectsPendingReplicationTotalSize;

  @JsonProperty("objectsFailedReplicationTotalSize")
  private long objectsFailedReplicationTotalSize;

  @JsonProperty("objectsReplicatedTotalSize")
  private long objectsReplicatedTotalSize;

  @JsonProperty("objectsPendingReplicationCount")
  private long objectsPendingReplicationCount;

  @JsonProperty("objectsFailedReplicationCount")
  private long objectsFailedReplicationCount;

  @JsonProperty("objectsCount")
  private long objectsCount;

  @JsonProperty("objectsSizesHistogram")
  private Map<String, Long> objectsSizesHistogram;

  @JsonProperty("versionsCount")
  private long versionsCount;

  @JsonProperty("objectReplicaTotalSize")
  private long objectReplicaTotalSize;

  @JsonProperty("objectsReplicationInfo")
  private Map<String, BucketTargetUsageInfo> objectsReplicationInfo;

  public long size() {
    return size;
  }

  public long objectsPendingReplicationTotalSize() {
    return objectsPendingReplicationTotalSize;
  }

  public long objectsFailedReplicationTotalSize() {
    return objectsFailedReplicationTotalSize;
  }

  public long objectsReplicatedTotalSize() {
    return objectsReplicatedTotalSize;
  }

  public long objectsPendingReplicationCount() {
    return objectsPendingReplicationCount;
  }

  public long objectsFailedReplicationCount() {
    return objectsFailedReplicationCount;
  }

  public long objectsCount() {
    return objectsCount;
  }

  public Map<String, Long> objectsSizesHistogram() {
    return Collections.unmodifiableMap(this.objectsSizesHistogram);
  }

  public long versionsCount() {
    return versionsCount;
  }

  public long objectReplicaTotalSize() {
    return objectReplicaTotalSize;
  }

  public Map<String, BucketTargetUsageInfo> objectsReplicationInfo() {
    return Collections.unmodifiableMap(this.objectsReplicationInfo);
  }
}
