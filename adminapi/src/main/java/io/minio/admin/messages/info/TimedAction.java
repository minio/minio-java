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
import java.math.BigDecimal;

/**
 * TimedAction contains a number of actions and their accumulated duration in nanoseconds.
 *
 * @see <a href= "https://github.com/minio/madmin-go/blob/main/metrics.go#L244">metrics.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimedAction {
  @JsonProperty("count")
  private BigDecimal count;

  @JsonProperty("acc_time_ns")
  private BigDecimal accTime;

  @JsonProperty("bytes")
  private BigDecimal bytes;

  public BigDecimal count() {
    return count;
  }

  public BigDecimal accTime() {
    return accTime;
  }

  public BigDecimal bytes() {
    return bytes;
  }
}
