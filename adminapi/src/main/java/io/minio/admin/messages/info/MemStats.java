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
 * MemStats is strip down version of runtime.MemStats containing memory stats of MinIO server.
 *
 * @see <a href= "https://github.com/minio/madmin-go/blob/main/health.go#L856">health.go</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemStats {
  @JsonProperty("Alloc")
  private BigDecimal alloc;

  @JsonProperty("TotalAlloc")
  private BigDecimal totalAlloc;

  @JsonProperty("Mallocs")
  private BigDecimal mallocs;

  @JsonProperty("Frees")
  private BigDecimal frees;

  @JsonProperty("HeapAlloc")
  private BigDecimal heapAlloc;

  public BigDecimal alloc() {
    return alloc;
  }

  public BigDecimal totalAlloc() {
    return totalAlloc;
  }

  public BigDecimal mallocs() {
    return mallocs;
  }

  public BigDecimal frees() {
    return frees;
  }

  public BigDecimal heapAlloc() {
    return heapAlloc;
  }
}
