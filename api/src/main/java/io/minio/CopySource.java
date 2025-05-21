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

/** A source object definition for {@link CopyObjectArgs}. */
public class CopySource extends ObjectConditionalReadArgs {
  protected CopySource() {}

  public CopySource(ObjectConditionalReadArgs args) {
    this.extraHeaders = args.extraHeaders;
    this.extraQueryParams = args.extraQueryParams;
    this.bucketName = args.bucketName;
    this.region = args.region;
    this.objectName = args.objectName;
    this.versionId = args.versionId;
    this.ssec = args.ssec;
    this.offset = args.offset;
    this.length = args.length;
    this.matchETag = args.matchETag;
    this.notMatchETag = args.notMatchETag;
    this.modifiedSince = args.modifiedSince;
    this.unmodifiedSince = args.unmodifiedSince;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link CopySource}. */
  public static final class Builder
      extends ObjectConditionalReadArgs.Builder<Builder, CopySource> {}
}
