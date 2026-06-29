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

import io.minio.messages.Part;
import java.util.Arrays;
import java.util.Objects;

/** Arguments of {@link BaseS3Client#completeMultipartUpload}. */
public class CompleteMultipartUploadArgs extends ObjectArgs {
  private String uploadId;
  private Part[] parts;
  private ServerSideEncryption.CustomerKey ssec;
  private long delayMs = 200L;
  private int maxRetries = 5;

  protected CompleteMultipartUploadArgs() {}

  public CompleteMultipartUploadArgs(ComposeObjectArgs args, String uploadId, Part[] parts) {
    super(args);
    this.uploadId = uploadId;
    this.parts = parts;
    this.delayMs = args.delayMs();
    this.maxRetries = args.maxRetries();
  }

  public CompleteMultipartUploadArgs(PutObjectBaseArgs args, String uploadId, Part[] parts) {
    super(args);
    this.uploadId = uploadId;
    this.parts = parts;
    if (args.sse() != null && args.sse() instanceof ServerSideEncryption.CustomerKey) {
      this.ssec = (ServerSideEncryption.CustomerKey) args.sse();
    }
    this.delayMs = args.delayMs();
    this.maxRetries = args.maxRetries();
  }

  public String uploadId() {
    return uploadId;
  }

  public Part[] parts() {
    return parts;
  }

  public ServerSideEncryption.CustomerKey ssec() {
    return ssec;
  }

  public long delayMs() {
    return delayMs;
  }

  public int maxRetries() {
    return maxRetries;
  }

  public void validateSsec(boolean isHttps) {
    checkSse(ssec, isHttps);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link CompleteMultipartUploadArgs}. */
  public static final class Builder
      extends ObjectArgs.Builder<Builder, CompleteMultipartUploadArgs> {
    @Override
    protected void validate(CompleteMultipartUploadArgs args) {
      super.validate(args);
      Utils.validateNotEmptyString(args.uploadId, "upload ID");
      Utils.validateNotNull(args.parts, "parts");
    }

    public Builder uploadId(String uploadId) {
      Utils.validateNotEmptyString(uploadId, "upload ID");
      operations.add(args -> args.uploadId = uploadId);
      return this;
    }

    public Builder parts(Part[] parts) {
      Utils.validateNotNull(parts, "parts");
      operations.add(args -> args.parts = parts);
      return this;
    }

    public Builder ssec(ServerSideEncryption.CustomerKey ssec) {
      operations.add(args -> args.ssec = ssec);
      return this;
    }

    /** Set delay between retries. Value &lt;= 0 makes no delay (default 200ms). */
    public Builder delayMs(long delayMs) {
      operations.add(args -> args.delayMs = delayMs);
      return this;
    }

    /** Set maximum retry between failure. Value &lt;= 0 disables retry (default 5). */
    public Builder maxRetries(int maxRetries) {
      operations.add(args -> args.maxRetries = maxRetries);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CompleteMultipartUploadArgs)) return false;
    if (!super.equals(o)) return false;
    CompleteMultipartUploadArgs that = (CompleteMultipartUploadArgs) o;
    return Objects.equals(uploadId, that.uploadId)
        && Arrays.equals(parts, that.parts)
        && Objects.equals(ssec, that.ssec)
        && delayMs == that.delayMs
        && maxRetries == that.maxRetries;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), uploadId, Arrays.hashCode(parts), ssec, delayMs, maxRetries);
  }
}
