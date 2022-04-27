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

/** Argument class of {@link MinioAsyncClient#getObject} and {@link MinioClient#getObject}. */
public class GetObjectArgs extends ObjectConditionalReadArgs {
  protected GetObjectArgs() {}

  public GetObjectArgs(DownloadObjectArgs args) {
    this.extraHeaders = args.extraHeaders;
    this.extraQueryParams = args.extraQueryParams;
    this.bucketName = args.bucketName;
    this.region = args.region;
    this.objectName = args.objectName;
    this.versionId = args.versionId;
    this.ssec = args.ssec;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link GetObjectArgs}. */
  public static final class Builder
      extends ObjectConditionalReadArgs.Builder<Builder, GetObjectArgs> {}
}
