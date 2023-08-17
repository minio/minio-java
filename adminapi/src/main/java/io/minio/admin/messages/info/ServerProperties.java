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

package io.minio.admin.messages.info;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * ServerProperties holds server information
 *
 * @see <a href=
 *     "https://github.com/minio/madmin-go/blob/main/info-commands.go#L374">info-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerProperties {
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
    return Collections.unmodifiableList(disks == null ? new LinkedList<>() : disks);
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
}
