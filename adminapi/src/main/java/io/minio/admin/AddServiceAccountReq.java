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
import javax.annotation.Nullable;

/**
 * add service account request info.
 *
 * <p>* @see <a href=
 * "https://github.com/minio/madmin-go/blob/main/user-commands.go#L336">user-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddServiceAccountReq {
  @JsonProperty("secretKey")
  private String secretKey;

  @JsonProperty("policy")
  private byte[] policy;

  @JsonProperty("targetUser")
  private String targetUser;

  @JsonProperty("accessKey")
  private String accessKey;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("expiration")
  private String expiration;

  public AddServiceAccountReq() {}

  public AddServiceAccountReq(
      @Nullable @JsonProperty("secretKey") String secretKey,
      @Nullable @JsonProperty("policy") byte[] policy,
      @Nullable @JsonProperty("targetUser") String targetUser,
      @Nullable @JsonProperty("accessKey") String accessKey,
      @Nullable @JsonProperty("name") String name,
      @Nullable @JsonProperty("description") String description,
      @Nullable @JsonProperty("expiration") String expiration) {
    this.secretKey = secretKey;
    this.policy = policy;
    this.targetUser = targetUser;
    this.accessKey = accessKey;
    this.name = name;
    this.description = description;
    this.expiration = expiration;
  }

  public String secretKey() {
    return secretKey;
  }

  public byte[] policy() {
    return policy;
  }

  public String targetUser() {
    return targetUser;
  }

  public String accessKey() {
    return accessKey;
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
