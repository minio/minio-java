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

/** Bucket Quota QuotaUnit. */
public enum QuotaUnit {
  KB(1024),
  MB(1024 * KB.unit),
  GB(1024 * MB.unit),
  TB(1024 * GB.unit);

  private final long unit;

  QuotaUnit(long unit) {
    this.unit = unit;
  }

  public long toBytes(long size) {
    long totalSize = size * this.unit;
    if (totalSize < 0) {
      throw new IllegalArgumentException(
          "Quota size must be greater than zero.But actual is " + totalSize);
    }
    if (totalSize / this.unit != size) {
      throw new IllegalArgumentException("Quota size overflow");
    }
    return totalSize;
  }
}
