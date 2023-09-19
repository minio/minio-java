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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * HealingDisk contains information about
 *
 * @see <a href=
 *     "https://github.com/minio/madmin-go/blob/main/heal-commands.go#L344">heal-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealingDisk {
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
    return Collections.unmodifiableList(queuedBuckets == null ? new LinkedList<>() : queuedBuckets);
  }

  public List<String> healedBuckets() {
    return Collections.unmodifiableList(healedBuckets == null ? new LinkedList<>() : healedBuckets);
  }
}
