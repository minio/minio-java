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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.annotation.Nullable;

/**
 * add service account request info.
 *
 * <p>* @see <a href=
 * "https://github.com/minio/madmin-go/blob/main/user-commands.go#L437">user-commands.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateServiceAccountReq {
  @JsonProperty("newSecretKey")
  private String newSecretKey;

  @JsonProperty("newStatus")
  private Status newStatus;

  @JsonProperty("newPolicy")
  private byte[] newPolicy;

  @JsonProperty("newName")
  private String newName;

  @JsonProperty("newDescription")
  private String newDescription;

  @JsonProperty("newExpiration")
  private String newExpiration;

  public UpdateServiceAccountReq() {}

  public UpdateServiceAccountReq(
      @Nullable @JsonProperty("newSecretKey") String newSecretKey,
      @Nullable @JsonProperty("newPolicy") byte[] newPolicy,
      @Nullable @JsonProperty("newStatus") Status newStatus,
      @Nullable @JsonProperty("newName") String newName,
      @Nullable @JsonProperty("newDescription") String newDescription,
      @Nullable @JsonProperty("newExpiration") String newExpiration) {
    this.newSecretKey = newSecretKey;
    this.newPolicy = newPolicy;
    this.newStatus = newStatus;
    this.newName = newName;
    this.newDescription = newDescription;
    this.newExpiration = newExpiration;
  }

  public String newSecretKey() {
    return newSecretKey;
  }

  public byte[] newPolicy() {
    return newPolicy;
  }

  public Status newStatus() {
    return newStatus;
  }

  public String newName() {
    return newName;
  }

  public String newDescription() {
    return newDescription;
  }

  public String newExpiration() {
    return newExpiration;
  }

  public static enum Status {
    ENABLED("on"),
    DISABLED("off");

    private final String value;

    private Status(String value) {
      this.value = value;
    }

    @JsonValue
    public String toString() {
      return this.value;
    }

    @JsonCreator
    public static UpdateServiceAccountReq.Status fromString(String statusString) {
      if ("on".equals(statusString)) {
        return ENABLED;
      }

      if ("off".equals(statusString)) {
        return DISABLED;
      }

      if (statusString.isEmpty()) {
        return null;
      }

      throw new IllegalArgumentException("Unknown status " + statusString);
    }
  }
}
