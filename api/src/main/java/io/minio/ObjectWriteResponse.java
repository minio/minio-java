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

import io.minio.messages.CompleteMultipartUploadResult;
import io.minio.messages.CopyObjectResult;
import okhttp3.Headers;

/** Response class of any APIs doing object creation. */
public class ObjectWriteResponse extends GenericUploadResponse {
  private String versionId;

  public ObjectWriteResponse(
      Headers headers, String bucket, String region, String object, String etag, String versionId) {
    super(headers, bucket, region, object, etag);
    this.versionId = versionId;
  }

  public ObjectWriteResponse(
      Headers headers,
      String bucket,
      String region,
      String object,
      String etag,
      String versionId,
      CopyObjectResult result) {
    super(headers, bucket, region, object, etag, result);
    this.versionId = versionId;
  }

  public ObjectWriteResponse(
      Headers headers,
      String bucket,
      String region,
      String object,
      String etag,
      String versionId,
      CompleteMultipartUploadResult result) {
    super(headers, bucket, region, object, etag, result);
    this.versionId = versionId;
  }

  public String versionId() {
    return versionId;
  }
}
