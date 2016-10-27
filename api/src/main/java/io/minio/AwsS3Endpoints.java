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
 * Amazon AWS S3 endpoints for various regions.
 */
enum AwsS3Endpoints {
  INSTANCE;
  private final Map<String, String> endpoints = new HashMap<>();

  AwsS3Endpoints() {
    // ap-northeast-1
    endpoints.put("ap-northeast-1", "s3-ap-northeast-1.amazonaws.com");
    // ap-northeast-2
    endpoints.put("ap-northeast-2", "s3-ap-northeast-2.amazonaws.com");
    //ap-south-1
    endpoints.put("ap-south-1", "s3-ap-south-1.amazonaws.com");
    // ap-southeast-1
    endpoints.put("ap-southeast-1", "s3-ap-southeast-1.amazonaws.com");
    // ap-southeast-2
    endpoints.put("ap-southeast-2", "s3-ap-southeast-2.amazonaws.com");
    // eu-central-1
    endpoints.put("eu-central-1", "s3-eu-central-1.amazonaws.com");
    // eu-west-1
    endpoints.put("eu-west-1", "s3-eu-west-1.amazonaws.com");
    // sa-east-1
    endpoints.put("sa-east-1", "s3-sa-east-1.amazonaws.com");
    // us-west-1
    endpoints.put("us-west-1", "s3-us-west-1.amazonaws.com");
    // us-west-2
    endpoints.put("us-west-2", "s3-us-west-2.amazonaws.com");
    // us-east-1
    endpoints.put("us-east-1", "s3.amazonaws.com");
    // use-east-2
    endpoints.put("us-east-2", "s3-us-east-2.amazonaws.com");
    // cn-north-1
    endpoints.put("cn-north-1", "s3.cn-north-1.amazonaws.com.cn");
  }

  /**
   * Gets Amazon S3 endpoint for the relevant region.
   */
  public String endpoint(String region) {
    String s = AwsS3Endpoints.INSTANCE.endpoints.get(region);
    if (s == null) {
      s = "s3.amazonaws.com";
    }
    return s;
  }
}
