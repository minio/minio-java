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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * InfoMessage container to hold server admin related information.
 *
 * @see <a href=
 *     "https://github.com/minio/madmin-go/blob/main/info-commands.go#L238">heal-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetServerInfoResponse {
  @JsonProperty("mode")
  private String mode;

  @JsonProperty("deploymentID")
  private String deploymentID;

  @JsonProperty("buckets")
  private Buckets buckets;

  @JsonProperty("objects")
  private Objects objects;

  @JsonProperty("versions")
  private Versions versions;

  @JsonProperty("usage")
  private Usage usage;

  @JsonProperty("backend")
  private Backend backend;

  @JsonProperty("servers")
  private List<ServerProperties> servers;

  @JsonProperty("pools")
  private Map<Integer, Map<Integer, ErasureSetInfo>> pools;

  public String mode() {
    return mode;
  }

  public String deploymentID() {
    return deploymentID;
  }

  public Buckets buckets() {
    return buckets;
  }

  public Objects objects() {
    return objects;
  }

  public Versions versions() {
    return versions;
  }

  public Usage usage() {
    return usage;
  }

  public Backend backend() {
    return backend;
  }

  public List<ServerProperties> servers() {
    return Collections.unmodifiableList(servers == null ? new ArrayList<>() : servers);
  }

  public Map<Integer, Map<Integer, ErasureSetInfo>> pools() {
    return pools;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Buckets {
    @JsonProperty("count")
    private Integer count;

    @JsonProperty("error")
    private String error;

    public Integer count() {
      return count;
    }

    public String error() {
      return error;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Objects {
    @JsonProperty("count")
    private Integer count;

    @JsonProperty("error")
    private String error;

    public Integer count() {
      return count;
    }

    public String error() {
      return error;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Versions {
    @JsonProperty("count")
    private Integer count;

    @JsonProperty("error")
    private String error;

    public Integer count() {
      return count;
    }

    public String error() {
      return error;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Usage {
    @JsonProperty("size")
    private Long size;

    @JsonProperty("error")
    private String error;

    public Long size() {
      return size;
    }

    public String error() {
      return error;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Backend {
    @JsonProperty("backendType")
    private String backendType;

    @JsonProperty("onlineDisks")
    private Integer onlineDisks;

    @JsonProperty("offlineDisks")
    private Integer offlineDisks;

    @JsonProperty("standardSCParity")
    private Integer standardSCParity;

    @JsonProperty("rrSCParity")
    private Integer rrSCParity;

    @JsonProperty("totalSets")
    private List<Integer> totalSets;

    @JsonProperty("totalDrivesPerSet")
    private List<Integer> totalDrivesPerSet;

    public String backendType() {
      return backendType;
    }

    public Integer onlineDisks() {
      return onlineDisks;
    }

    public Integer offlineDisks() {
      return offlineDisks;
    }

    public Integer standardSCParity() {
      return standardSCParity;
    }

    public Integer rrSCParity() {
      return rrSCParity;
    }

    public List<Integer> totalSets() {
      return Collections.unmodifiableList(totalSets == null ? new ArrayList<>() : totalSets);
    }

    public List<Integer> totalDrivesPerSet() {
      return Collections.unmodifiableList(
          totalDrivesPerSet == null ? new ArrayList<>() : totalDrivesPerSet);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ServerProperties {
    @JsonProperty("state")
    private String state;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("scheme")
    private String scheme;

    @JsonProperty("uptime")
    private Integer uptime;

    @JsonProperty("version")
    private String version;

    @JsonProperty("commitID")
    private String commitID;

    @JsonProperty("network")
    private Map<String, String> network;

    @JsonProperty("drives")
    private List<Disk> disks;

    @JsonProperty("poolNumber")
    private Integer poolNumber;

    @JsonProperty("mem_stats")
    private MemStats memStats;

    @JsonProperty("go_max_procs")
    private Integer goMaxProcs;

    @JsonProperty("num_cpu")
    private Integer numCPU;

    @JsonProperty("runtime_version")
    private String runtimeVersion;

    @JsonProperty("gc_stats")
    private GCStats gCStats;

    @JsonProperty("minio_env_vars")
    private Map<String, String> minioEnvVars;

    public String state() {
      return state;
    }

    public String endpoint() {
      return endpoint;
    }

    public String scheme() {
      return scheme;
    }

    public Integer uptime() {
      return uptime;
    }

    public String version() {
      return version;
    }

    public String commitID() {
      return commitID;
    }

    public Map<String, String> network() {
      return Collections.unmodifiableMap(this.network);
    }

    public List<Disk> disks() {
      return Collections.unmodifiableList(disks == null ? new ArrayList<>() : disks);
    }

    public Integer poolNumber() {
      return poolNumber;
    }

    public MemStats memStats() {
      return memStats;
    }

    public Integer goMaxProcs() {
      return goMaxProcs;
    }

    public Integer numCPU() {
      return numCPU;
    }

    public String runtimeVersion() {
      return runtimeVersion;
    }

    public GCStats gCStats() {
      return gCStats;
    }

    public Map<String, String> minioEnvVars() {
      return Collections.unmodifiableMap(this.minioEnvVars);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Disk {
      @JsonProperty("endpoint")
      private String endpoint;

      @JsonProperty("rootDisk")
      private boolean rootDisk;

      @JsonProperty("path")
      private String path;

      @JsonProperty("healing")
      private boolean healing;

      @JsonProperty("scanning")
      private boolean scanning;

      @JsonProperty("state")
      private String state;

      @JsonProperty("uuid")
      private String uuid;

      @JsonProperty("major")
      private BigDecimal major;

      @JsonProperty("minor")
      private BigDecimal minor;

      @JsonProperty("model")
      private String model;

      @JsonProperty("totalspace")
      private BigDecimal totalspace;

      @JsonProperty("usedspace")
      private BigDecimal usedspace;

      @JsonProperty("availspace")
      private BigDecimal availspace;

      @JsonProperty("readthroughput")
      private BigDecimal readthroughput;

      @JsonProperty("writethroughput")
      private BigDecimal writethroughput;

      @JsonProperty("readlatency")
      private BigDecimal readlatency;

      @JsonProperty("writelatency")
      private BigDecimal writelatency;

      @JsonProperty("utilization")
      private BigDecimal utilization;

      @JsonProperty("metrics")
      private DiskMetrics metrics;

      @JsonProperty("heal_info")
      private HealingDisk healInfo;

      @JsonProperty("used_inodes")
      private BigDecimal usedInodes;

      @JsonProperty("free_inodes")
      private BigDecimal freeInodes;

      @JsonProperty("pool_index")
      private Integer poolIndex;

      @JsonProperty("set_index")
      private Integer setIndex;

      @JsonProperty("disk_index")
      private Integer diskIndex;

      public String endpoint() {
        return endpoint;
      }

      public boolean isRootDisk() {
        return rootDisk;
      }

      public String path() {
        return path;
      }

      public boolean isHealing() {
        return healing;
      }

      public boolean isScanning() {
        return scanning;
      }

      public String state() {
        return state;
      }

      public String uuid() {
        return uuid;
      }

      public BigDecimal major() {
        return major;
      }

      public BigDecimal minor() {
        return minor;
      }

      public String model() {
        return model;
      }

      public BigDecimal totalspace() {
        return totalspace;
      }

      public BigDecimal usedspace() {
        return usedspace;
      }

      public BigDecimal availspace() {
        return availspace;
      }

      public BigDecimal readthroughput() {
        return readthroughput;
      }

      public BigDecimal writethroughput() {
        return writethroughput;
      }

      public BigDecimal readlatency() {
        return readlatency;
      }

      public BigDecimal writelatency() {
        return writelatency;
      }

      public BigDecimal utilization() {
        return utilization;
      }

      public DiskMetrics metrics() {
        return metrics;
      }

      public HealingDisk healInfo() {
        return healInfo;
      }

      public BigDecimal usedInodes() {
        return usedInodes;
      }

      public BigDecimal freeInodes() {
        return freeInodes;
      }

      public Integer poolIndex() {
        return poolIndex;
      }

      public Integer setIndex() {
        return setIndex;
      }

      public Integer diskIndex() {
        return diskIndex;
      }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemStats {
      @JsonProperty("Alloc")
      private BigDecimal alloc;

      @JsonProperty("TotalAlloc")
      private BigDecimal totalAlloc;

      @JsonProperty("Mallocs")
      private BigDecimal mallocs;

      @JsonProperty("Frees")
      private BigDecimal frees;

      @JsonProperty("HeapAlloc")
      private BigDecimal heapAlloc;

      public BigDecimal alloc() {
        return alloc;
      }

      public BigDecimal totalAlloc() {
        return totalAlloc;
      }

      public BigDecimal mallocs() {
        return mallocs;
      }

      public BigDecimal frees() {
        return frees;
      }

      public BigDecimal heapAlloc() {
        return heapAlloc;
      }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCStats {
      @JsonProperty("last_gc")
      private String lastGC;

      @JsonProperty("num_gc")
      private Integer numGC;

      @JsonProperty("pause_total")
      private Long pauseTotal;

      @JsonProperty("pause")
      private List<Integer> pause;

      @JsonProperty("pause_end")
      private List<String> pauseEnd;

      public String lastGC() {
        return lastGC;
      }

      public Integer numGC() {
        return numGC;
      }

      public Long pauseTotal() {
        return pauseTotal;
      }

      public List<Integer> pause() {
        return Collections.unmodifiableList(pause == null ? new ArrayList<>() : pause);
      }

      public List<String> pauseEnd() {
        return Collections.unmodifiableList(pauseEnd == null ? new ArrayList<>() : pauseEnd);
      }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiskMetrics {
      @JsonProperty("lastMinute")
      private Map<String, TimedAction> lastMinute;

      @JsonProperty("apiCalls")
      private Map<String, String> apiCalls;

      @JsonProperty("totalErrorsAvailability")
      private Integer totalErrorsAvailability;

      @JsonProperty("totalErrorsTimeout")
      private Integer totalErrorsTimeout;

      @JsonProperty("totalTokens")
      private Long totalTokens;

      @JsonProperty("totalWaiting")
      private Long totalWaiting;

      @JsonProperty("totalWrites")
      private Long totalWrites;

      @JsonProperty("totalDeletes")
      private Long totalDeletes;

      public Integer totalErrorsAvailability() {
        return totalErrorsAvailability;
      }

      public Integer totalErrorsTimeout() {
        return totalErrorsTimeout;
      }

      public Map<String, TimedAction> lastMinute() {
        return Collections.unmodifiableMap(lastMinute);
      }

      public Map<String, String> apiCalls() {
        return Collections.unmodifiableMap(apiCalls);
      }

      public Long totalTokens() {
        return totalTokens;
      }

      public Long totalWaiting() {
        return totalWaiting;
      }

      public Long totalWrites() {
        return totalWrites;
      }

      public Long totalDeletes() {
        return totalDeletes;
      }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HealingDisk {
      @JsonProperty("id")
      private String id;

      @JsonProperty("heal_id")
      private String healID;

      @JsonProperty("pool_index")
      private Integer poolIndex;

      @JsonProperty("set_index")
      private Integer setIndex;

      @JsonProperty("disk_index")
      private Integer diskIndex;

      @JsonProperty("endpoint")
      private String endpoint;

      @JsonProperty("path")
      private String path;

      @JsonProperty("started")
      private String started;

      @JsonProperty("last_update")
      private String lastUpdate;

      @JsonProperty("objects_total_count")
      private BigDecimal objectsTotalCount;

      @JsonProperty("objects_total_size")
      private BigDecimal objectsTotalSize;

      @JsonProperty("items_healed")
      private BigDecimal itemsHealed;

      @JsonProperty("items_failed")
      private BigDecimal itemsFailed;

      @JsonProperty("bytes_done")
      private BigDecimal bytesDone;

      @JsonProperty("bytes_failed")
      private BigDecimal bytesFailed;

      @JsonProperty("objects_healed")
      private BigDecimal objectsHealed;

      @JsonProperty("objects_failed")
      private BigDecimal objectsFailed;

      @JsonProperty("current_bucket")
      private String bucket;

      @JsonProperty("current_object")
      private String object;

      @JsonProperty("queued_buckets")
      private List<String> queuedBuckets;

      @JsonProperty("healed_buckets")
      private List<String> healedBuckets;

      public String id() {
        return id;
      }

      public String healID() {
        return healID;
      }

      public Integer poolIndex() {
        return poolIndex;
      }

      public Integer setIndex() {
        return setIndex;
      }

      public Integer diskIndex() {
        return diskIndex;
      }

      public String endpoint() {
        return endpoint;
      }

      public String path() {
        return path;
      }

      public String started() {
        return started;
      }

      public String lastUpdate() {
        return lastUpdate;
      }

      public BigDecimal objectsTotalCount() {
        return objectsTotalCount;
      }

      public BigDecimal objectsTotalSize() {
        return objectsTotalSize;
      }

      public BigDecimal itemsHealed() {
        return itemsHealed;
      }

      public BigDecimal itemsFailed() {
        return itemsFailed;
      }

      public BigDecimal bytesDone() {
        return bytesDone;
      }

      public BigDecimal bytesFailed() {
        return bytesFailed;
      }

      public BigDecimal objectsHealed() {
        return objectsHealed;
      }

      public BigDecimal objectsFailed() {
        return objectsFailed;
      }

      public String bucket() {
        return bucket;
      }

      public String object() {
        return object;
      }

      public List<String> queuedBuckets() {
        return Collections.unmodifiableList(
            queuedBuckets == null ? new ArrayList<>() : queuedBuckets);
      }

      public List<String> healedBuckets() {
        return Collections.unmodifiableList(
            healedBuckets == null ? new ArrayList<>() : healedBuckets);
      }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimedAction {
      @JsonProperty("count")
      private BigDecimal count;

      @JsonProperty("acc_time_ns")
      private BigDecimal accTime;

      @JsonProperty("bytes")
      private BigDecimal bytes;

      public BigDecimal count() {
        return count;
      }

      public BigDecimal accTime() {
        return accTime;
      }

      public BigDecimal bytes() {
        return bytes;
      }
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ErasureSetInfo {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("rawUsage")
    private BigDecimal rawUsage;

    @JsonProperty("rawCapacity")
    private BigDecimal rawCapacity;

    @JsonProperty("usage")
    private BigDecimal usage;

    @JsonProperty("objectsCount")
    private BigDecimal objectsCount;

    @JsonProperty("versionsCount")
    private BigDecimal versionsCount;

    @JsonProperty("healDisks")
    private Integer healDisks;

    public Integer id() {
      return id;
    }

    public BigDecimal rawUsage() {
      return rawUsage;
    }

    public BigDecimal rawCapacity() {
      return rawCapacity;
    }

    public BigDecimal usage() {
      return usage;
    }

    public BigDecimal objectsCount() {
      return objectsCount;
    }

    public BigDecimal versionsCount() {
      return versionsCount;
    }

    public Integer healDisks() {
      return healDisks;
    }
  }
}
