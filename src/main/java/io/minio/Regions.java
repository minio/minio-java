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

public enum Regions {
  INSTANCE;
  private final Map<String, String> regions = new HashMap<String, String>();

  Regions() {
    // ap-northeast-1
    regions.put("s3-ap-northeast-1.amazonaws.com", "ap-northeast-1");
    // ap-southeast-1
    regions.put("s3-ap-southeast-1.amazonaws.com", "ap-southeast-1");
    // ap-southeast-2
    regions.put("s3-ap-southeast-2.amazonaws.com", "ap-southeast-2");
    // eu-central-1
    regions.put("s3-eu-central-1.amazonaws.com", "eu-central-1");
    // eu-west-1
    regions.put("s3-eu-west-1.amazonaws.com", "eu-west-1");
    // sa-east-1
    regions.put("s3-sa-east-1.amazonaws.com", "sa-east-1");
    // us-east-1
    regions.put("s3.amazonaws.com", "us-east-1");
    // us-west-1
    regions.put("s3-us-west-1.amazonaws.com", "us-west-1");
    // us-west-2
    regions.put("s3-us-west-2.amazonaws.com", "us-west-2");
  }

  /**
   * get region.
   */
  public String getRegion(String host) {
    String s = Regions.INSTANCE.regions.get(host);
    if (s == null) {
      s = "us-east-1";
    }
    return s;
  }
}
