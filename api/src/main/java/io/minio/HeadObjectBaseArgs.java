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

/** Base argument class holds HEAD object. */
public abstract class HeadObjectBaseArgs extends ObjectConditionalReadArgs {
  protected HeadObjectBaseArgs() {}

  protected HeadObjectBaseArgs(ObjectReadArgs args) {
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

  /** Base argument builder class for {@link HeadObjectBaseArgs}. */
  public abstract static class Builder<B extends Builder<B, A>, A extends HeadObjectBaseArgs>
      extends ObjectConditionalReadArgs.Builder<B, A> {}
}
