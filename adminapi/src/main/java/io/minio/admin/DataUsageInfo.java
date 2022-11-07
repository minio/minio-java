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
import java.util.Map;

/** Represents Data usage of the current object APi. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataUsageInfo {
  @JsonProperty("objectsCount")
  private long objectsCount;

  @JsonProperty("versionsCount")
  private long versionsCount;

  @JsonProperty("objectsTotalSize")
  private long objectsTotalSize;

  @JsonProperty("bucketsCount")
  private long bucketsCount;

  @JsonProperty("bucketsUsageInfo")
  private Map<String, BucketUsageInfo> bucketsUsageInfo;

  public long getObjectsCount() {
    return objectsCount;
  }

  public long getVersionsCount() {
    return versionsCount;
  }

  public long getObjectsTotalSize() {
    return objectsTotalSize;
  }

  public long getBucketsCount() {
    return bucketsCount;
  }

  public Map<String, BucketUsageInfo> getBucketsUsageInfo() {
    return bucketsUsageInfo;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BucketUsageInfo {
    @JsonProperty("size")
    private long size;

    @JsonProperty("objectsCount")
    private long objectsCount;

    @JsonProperty("versionsCount")
    private long versionsCount;

    public long getSize() {
      return size;
    }

    public long getObjectsCount() {
      return objectsCount;
    }

    public long getVersionsCount() {
      return versionsCount;
    }
  }
}
