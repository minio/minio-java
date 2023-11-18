/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2021 MinIO, Inc.
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

/**
 * service account info.
 *
 * <p>* @see <a href=
 * "https://github.com/minio/madmin-go/blob/main/user-commands.go#L535">user-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetServiceAccountInfoResp {
  @JsonProperty("parentUser")
  private String parentUser;

  @JsonProperty("accountStatus")
  private String accountStatus;

  @JsonProperty("impliedPolicy")
  private boolean impliedPolicy;

  @JsonProperty("policy")
  private String policy;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("expiration")
  private String expiration;

  public String parentUser() {
    return parentUser;
  }

  public String accountStatus() {
    return accountStatus;
  }

  public boolean impliedPolicy() {
    return impliedPolicy;
  }

  public String description() {
    return description;
  }

  public String name() {
    return name;
  }

  public String expiration() {
    return expiration;
  }
}
