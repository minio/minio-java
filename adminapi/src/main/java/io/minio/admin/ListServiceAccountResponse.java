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
import java.util.List;

/** list service account response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListServiceAccountResponse {
  @JsonProperty("accounts")
  private List<ListServiceAccountInfo> accounts;

  public List<ListServiceAccountInfo> accounts() {
    return accounts;
  }

  public static class ListServiceAccountInfo {
    @JsonProperty("accessKey")
    private String accessKey;

    @JsonProperty("expiration")
    private String expiration;

    @JsonProperty("parentUser")
    private String parentUser;

    @JsonProperty("accountStatus")
    private AccountStatus accountStatus;

    @JsonProperty("impliedPolicy")
    private boolean impliedPolicy;

    public String expiration() {
      return expiration;
    }

    public String accessKey() {
      return accessKey;
    }

    public String parentUser() {
      return parentUser;
    }

    public AccountStatus accountStatus() {
      return accountStatus;
    }

    public boolean impliedPolicy() {
      return impliedPolicy;
    }

    public enum AccountStatus {
      ON("on"),
      OFF("off");

      private final String value;

      AccountStatus(String value) {
        this.value = value;
      }

      @JsonValue
      public String value() {
        return value;
      }

      @JsonCreator
      public static AccountStatus fromValue(String value) {
        for (AccountStatus v : AccountStatus.values()) {
          if (v.value.equals(value)) {
            return v;
          }
        }

        throw new IllegalArgumentException("unknown account status '" + value + "'");
      }
    }
  }
}
