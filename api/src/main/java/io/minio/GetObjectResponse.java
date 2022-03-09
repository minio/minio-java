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

import java.io.FilterInputStream;
import java.io.InputStream;
import okhttp3.Headers;

/**
 * Response class of {@link MinioAsyncClient#getObject} and {@link MinioClient#getObject}. This
 * class is {@link InputStream} interface compatible and it must be closed after use to release
 * underneath network resources.
 */
public class GetObjectResponse extends FilterInputStream {
  private GenericResponse response;

  public GetObjectResponse(
      Headers headers, String bucket, String region, String object, InputStream body) {
    super(body);
    this.response = new GenericResponse(headers, bucket, region, object);
  }

  public Headers headers() {
    return response.headers();
  }

  public String bucket() {
    return response.bucket();
  }

  public String region() {
    return response.region();
  }

  public String object() {
    return response.object();
  }
}
