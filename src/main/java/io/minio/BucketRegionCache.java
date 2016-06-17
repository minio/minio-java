/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

package io.minio;

import java.util.HashMap;
import java.util.Map;


/**
 * A singleton bucket/region cache map.
 */
enum BucketRegionCache {
  INSTANCE;
  private final Map<String, String> regionMap = new HashMap<>();

  /**
   * Returns AWS region for given bucket name.
   */
  public String region(String bucketName) {
    if (bucketName == null) {
      return "us-east-1";
    }

    String region = this.regionMap.get(bucketName);
    if (region == null) {
      return "us-east-1";
    } else {
      return region;
    }
  }


  /**
   * Adds bucket name and its region to BucketRegionCache.
   */
  public void add(String bucketName, String region) {
    this.regionMap.put(bucketName, region);
  }


  /**
   * Removes region cache of the bucket if any.
   */
  public void remove(String bucketName) {
    if (bucketName != null) {
      this.regionMap.remove(bucketName);
    }
  }


  /**
   * Returns true if given bucket name is in the map else false.
   */
  public boolean exists(String bucketName) {
    return this.regionMap.get(bucketName) != null;
  }
}
