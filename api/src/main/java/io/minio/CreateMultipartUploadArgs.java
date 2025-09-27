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

import java.util.Objects;
import okhttp3.MediaType;

/** Arguments of {@link BaseS3Client#createMultipartUpload}. */
public class CreateMultipartUploadArgs extends ObjectArgs {
  private Http.Headers headers;

  protected CreateMultipartUploadArgs() {}

  public CreateMultipartUploadArgs(
      PutObjectBaseArgs args, MediaType contentType, Checksum.Algorithm algorithm) {
    super(args);
    this.headers =
        args.makeHeaders(
            contentType,
            algorithm == null
                ? null
                : new Http.Headers("x-amz-checksum-algorithm", algorithm.toString()));
  }

  public CreateMultipartUploadArgs(ComposeObjectArgs args) {
    super(args);
    this.headers = args.makeHeaders();
  }

  public Http.Headers headers() {
    return headers;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link CreateMultipartUploadArgs}. */
  public static final class Builder extends ObjectArgs.Builder<Builder, CreateMultipartUploadArgs> {
    @Override
    protected void validate(CreateMultipartUploadArgs args) {
      super.validate(args);
    }

    public Builder headers(Http.Headers headers) {
      operations.add(args -> args.headers = headers);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CreateMultipartUploadArgs)) return false;
    if (!super.equals(o)) return false;
    CreateMultipartUploadArgs that = (CreateMultipartUploadArgs) o;
    return Objects.equals(headers, that.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), headers);
  }
}
