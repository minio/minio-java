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

/** Arguments of {@link BaseS3Client#uploadPart}. */
public class UploadPartArgs extends PutObjectAPIBaseArgs {
  private String uploadId;
  private int partNumber;

  protected UploadPartArgs() {}

  public UploadPartArgs(
      PutObjectBaseArgs args,
      String uploadId,
      int partNumber,
      ByteBuffer buffer,
      Http.Headers checksumHeaders) {
    super(args, buffer, checksumHeaders);
    this.uploadId = uploadId;
    this.partNumber = partNumber;
    this.buffer = buffer;
    if (args.sse() != null && args.sse() instanceof ServerSideEncryption.CustomerKey) {
      this.headers.putAll(args.sse().headers());
    }
  }

  public String uploadId() {
    return uploadId;
  }

  public int partNumber() {
    return partNumber;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link UploadPartArgs}. */
  public static final class Builder extends PutObjectAPIBaseArgs.Builder<Builder, UploadPartArgs> {
    @Override
    protected void validate(UploadPartArgs args) {
      super.validate(args);
      Utils.validateNotEmptyString(args.uploadId, "upload ID");
      if (args.partNumber <= 0) {
        throw new IllegalArgumentException("valid part number must be provided");
      }
    }

    public Builder uploadId(String uploadId) {
      Utils.validateNotEmptyString(uploadId, "upload ID");
      operations.add(args -> args.uploadId = uploadId);
      return this;
    }

    public Builder partNumber(int partNumber) {
      if (partNumber <= 0) throw new IllegalArgumentException("valid part number must be provided");
      operations.add(args -> args.partNumber = partNumber);
      return this;
    }
  }

  /** Wrapper of {@link UploadPartArgs} to be used in parallel part uploads. */
  public static class Wrapper {
    final UploadPartArgs args;

    public Wrapper(UploadPartArgs args) {
      this.args = args;
    }

    public UploadPartArgs args() {
      return args;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UploadPartArgs)) return false;
    if (!super.equals(o)) return false;
    UploadPartArgs that = (UploadPartArgs) o;
    return Objects.equals(uploadId, that.uploadId) && partNumber == that.partNumber;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), uploadId, partNumber);
  }
}
