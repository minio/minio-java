/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

import okhttp3.Headers;

/** Generic response of any APIs. */
public class GenericResponse {
  private Headers headers;
  private String bucket;
  private String region;
  private String object;

  public GenericResponse(Headers headers, String bucket, String region, String object) {
    this.headers = headers;
    this.bucket = bucket;
    this.region = region;
    this.object = object;
  }

  public Headers headers() {
    return headers;
  }

  public String bucket() {
    return bucket;
  }

  public String region() {
    return region;
  }

  public String object() {
    return object;
  }
}
