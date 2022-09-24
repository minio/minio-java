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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
  ENABLED("enabled"),
  DISABLED("disabled");

  private final String value;

  Status(String value) {
    this.value = value;
  }

  @JsonValue
  public String toString() {
    return this.value;
  }

  @JsonCreator
  public static Status fromString(String statusString) {
    if ("enabled".equals(statusString)) {
      return ENABLED;
    }

    if ("disabled".equals(statusString)) {
      return DISABLED;
    }

    if (statusString.isEmpty()) {
      return null;
    }

    throw new IllegalArgumentException("Unknown status " + statusString);
  }
}
