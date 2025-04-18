/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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

import io.minio.messages.ListAllMyBucketsResult;
import okhttp3.Headers;

/** Response class of {@link S3Base#listBucketsAsync}. */
public class ListBucketsResponse extends GenericResponse {
  private ListAllMyBucketsResult result;

  public ListBucketsResponse(Headers headers, ListAllMyBucketsResult result) {
    super(headers, null, null, null);
    this.result = result;
  }

  public ListAllMyBucketsResult result() {
    return result;
  }
}
