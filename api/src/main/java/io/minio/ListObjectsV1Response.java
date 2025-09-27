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

import io.minio.messages.ListBucketResultV1;
import okhttp3.Headers;

/** Response of {@link BaseS3Client#listObjectsV1}. */
public class ListObjectsV1Response extends GenericResponse {
  private ListBucketResultV1 result;

  public ListObjectsV1Response(
      Headers headers, String bucket, String region, ListBucketResultV1 result) {
    super(headers, bucket, region, null);
    this.result = result;
  }

  public ListBucketResultV1 result() {
    return result;
  }
}
