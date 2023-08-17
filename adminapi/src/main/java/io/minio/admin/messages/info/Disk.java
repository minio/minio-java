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
import java.math.BigDecimal;

/**
 * Disk holds Disk information
 *
 * @see <a href=
 *     "https://github.com/minio/madmin-go/blob/main/info-commands.go#L404">info-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Disk {
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
