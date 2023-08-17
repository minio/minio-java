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
 * ErasureBackend contains specific erasure storage information
 *
 * @see <a href=
 *     "https://github.com/minio/madmin-go/blob/main/info-commands.go#L359">info-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Backend {
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
    return Collections.unmodifiableList(totalSets == null ? new LinkedList<>() : totalSets);
  }

  public List<Integer> totalDrivesPerSet() {
    return Collections.unmodifiableList(
        totalDrivesPerSet == null ? new LinkedList<>() : totalDrivesPerSet);
  }
}
