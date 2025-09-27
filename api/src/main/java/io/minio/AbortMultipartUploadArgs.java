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

/** Arguments of {@link BaseS3Client#abortMultipartUpload}. */
public class AbortMultipartUploadArgs extends ObjectArgs {
  private String uploadId;

  protected AbortMultipartUploadArgs() {}

  public AbortMultipartUploadArgs(ComposeObjectArgs args, String uploadId) {
    super(args);
    this.uploadId = uploadId;
  }

  public AbortMultipartUploadArgs(PutObjectBaseArgs args, String uploadId) {
    super(args);
    this.uploadId = uploadId;
  }

  public String uploadId() {
    return uploadId;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link AbortMultipartUploadArgs}. */
  public static final class Builder extends ObjectArgs.Builder<Builder, AbortMultipartUploadArgs> {
    @Override
    protected void validate(AbortMultipartUploadArgs args) {
      super.validate(args);
      Utils.validateNotEmptyString(args.uploadId, "upload ID");
    }

    public Builder uploadId(String uploadId) {
      Utils.validateNotEmptyString(uploadId, "upload ID");
      operations.add(args -> args.uploadId = uploadId);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbortMultipartUploadArgs)) return false;
    if (!super.equals(o)) return false;
    AbortMultipartUploadArgs that = (AbortMultipartUploadArgs) o;
    return Objects.equals(uploadId, that.uploadId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), uploadId);
  }
}
