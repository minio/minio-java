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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;

/**
 * DiskMetrics has the information about XL Storage APIs
 *
 * @see <a href=
 *     "https://github.com/minio/madmin-go/blob/main/info-commands.go#L395">info-commands.go</a>
 */
public class DiskMetrics {
  @JsonProperty("lastMinute")
  private Map<String, TimedAction> lastMinute;

  @JsonProperty("apiCalls")
  private Map<String, String> apiCalls;

  @JsonProperty("totalErrorsAvailability")
  private Integer totalErrorsAvailability;

  @JsonProperty("totalErrorsTimeout")
  private Integer totalErrorsTimeout;

  public Integer totalErrorsAvailability() {
    return totalErrorsAvailability;
  }

  public Integer totalErrorsTimeout() {
    return totalErrorsTimeout;
  }

  public Map<String, TimedAction> lastMinute() {
    return Collections.unmodifiableMap(lastMinute);
  }

  public Map<String, String> apiCalls() {
    return Collections.unmodifiableMap(apiCalls);
  }
}
