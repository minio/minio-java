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

package io.minio.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Represents data usage stats of the current object API.
 *
 * @see <a
 *     href="https://github.com/minio/minio/blob/master/cmd/data-usage-utils.go#L69">data-usage-utils.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetDataUsageInfoResponse {
  @JsonProperty("lastUpdate")
  private ZonedDateTime lastUpdate;

  @JsonProperty("objectsCount")
  private long objectsCount;

  @JsonProperty("versionsCount")
  private long versionsCount;

  @JsonProperty("objectsTotalSize")
  private long objectsTotalSize;

  @JsonProperty("objectsReplicationInfo")
  private Map<String, BucketTargetUsageInfo> objectsReplicationInfo;

  @JsonProperty("bucketsCount")
  private long bucketsCount;

  @JsonProperty("bucketsUsageInfo")
  private Map<String, BucketUsageInfo> bucketsUsageInfo;

  @JsonProperty("bucketsSizes")
  private Map<String, Long> bucketsSizes;

  @JsonProperty("tierStats")
  private AllTierStats tierStats;

  public ZonedDateTime lastUpdate() {
    return lastUpdate;
  }

  public long objectsCount() {
    return objectsCount;
  }

  public long versionsCount() {
    return versionsCount;
  }

  public long objectsTotalSize() {
    return objectsTotalSize;
  }

  public Map<String, BucketTargetUsageInfo> objectsReplicationInfo() {
    return Collections.unmodifiableMap(this.objectsReplicationInfo);
  }

  public long bucketsCount() {
    return bucketsCount;
  }

  public Map<String, BucketUsageInfo> bucketsUsageInfo() {
    return Collections.unmodifiableMap(this.bucketsUsageInfo);
  }

  public Map<String, Long> bucketsSizes() {
    return Collections.unmodifiableMap(bucketsSizes);
  }

  public AllTierStats tierStats() {
    return tierStats;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BucketTargetUsageInfo {
    @JsonProperty("objectsPendingReplicationTotalSize")
    private long objectsPendingReplicationTotalSize;

    @JsonProperty("objectsFailedReplicationTotalSize")
    private long objectsFailedReplicationTotalSize;

    @JsonProperty("objectsReplicatedTotalSize")
    private long objectsReplicatedTotalSize;

    @JsonProperty("objectReplicaTotalSize")
    private long objectReplicaTotalSize;

    @JsonProperty("objectsPendingReplicationCount")
    private long objectsPendingReplicationCount;

    @JsonProperty("objectsFailedReplicationCount")
    private long objectsFailedReplicationCount;

    public long objectsPendingReplicationTotalSize() {
      return objectsPendingReplicationTotalSize;
    }

    public long objectsFailedReplicationTotalSize() {
      return objectsFailedReplicationTotalSize;
    }

    public long objectsReplicatedTotalSize() {
      return objectsReplicatedTotalSize;
    }

    public long objectReplicaTotalSize() {
      return objectReplicaTotalSize;
    }

    public long objectsPendingReplicationCount() {
      return objectsPendingReplicationCount;
    }

    public long objectsFailedReplicationCount() {
      return objectsFailedReplicationCount;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BucketUsageInfo {
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TierStats {
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AllTierStats {
    @JsonProperty("Tiers")
    private Map<String, TierStats> tiers;

    public Map<String, TierStats> tiers() {
      return Collections.unmodifiableMap(this.tiers);
    }
  }
}
