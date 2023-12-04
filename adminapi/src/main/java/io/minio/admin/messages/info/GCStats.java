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

/**
 * GCStats collect information about recent garbage collections.
 *
 * @see <a href= "https://github.com/minio/madmin-go/blob/main/health.go#L865">health.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GCStats {
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
    return Collections.unmodifiableList(pause == null ? new LinkedList<>() : pause);
  }

  public List<String> pauseEnd() {
    return Collections.unmodifiableList(pauseEnd == null ? new LinkedList<>() : pauseEnd);
  }
}
