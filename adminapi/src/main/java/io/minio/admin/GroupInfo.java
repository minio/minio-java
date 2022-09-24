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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/** Represents group information. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupInfo {
  @JsonProperty("name")
  private String name;

  @JsonProperty("status")
  private Status status;

  @JsonProperty("members")
  private List<String> members;

  @JsonProperty("policy")
  private String policy;

  public String name() {
    return name;
  }

  public Status status() {
    return status;
  }

  public List<String> members() {
    return Collections.unmodifiableList(members == null ? new LinkedList<>() : members);
  }

  public String policy() {
    return policy;
  }
}
