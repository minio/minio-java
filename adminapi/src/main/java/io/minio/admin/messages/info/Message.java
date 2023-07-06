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
 * InfoMessage container to hold server admin related information.
 *
 * @see <a href=
 *     "https://github.com/minio/madmin-go/blob/main/info-commands.go#L238">heal-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
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
    return Collections.unmodifiableList(servers == null ? new LinkedList<>() : servers);
  }

  public Map<Integer, Map<Integer, ErasureSetInfo>> pools() {
    return pools;
  }
}
