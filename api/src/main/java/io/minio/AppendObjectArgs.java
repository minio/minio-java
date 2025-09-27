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

import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

/**
 * Arguments of {@link MinioAsyncClient#appendObject(io.minio.AppendObjectArgs)} and {@link
 * MinioClient#appendObject}.
 */
public class AppendObjectArgs extends ObjectArgs {
  private String filename;
  private InputStream stream;
  private byte[] data;
  private Long length;
  private Long chunkSize;

  public String filename() {
    return filename;
  }

  public InputStream stream() {
    return stream;
  }

  public byte[] data() {
    return data;
  }

  public Long length() {
    return length;
  }

  public Long chunkSize() {
    return chunkSize;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link AppendObjectArgs}. */
  public static final class Builder extends ObjectArgs.Builder<Builder, AppendObjectArgs> {
    @Override
    protected void validate(AppendObjectArgs args) {
      super.validate(args);
      if (args.filename == null && args.stream == null && args.data == null) {
        throw new IllegalArgumentException("either filename, stream or data must be provided");
      }
    }

    private void validateFilename(String filename) {
      Utils.validateNotEmptyString(filename, "filename");
      if (!Files.isRegularFile(Paths.get(filename))) {
        throw new IllegalArgumentException(filename + " not a regular file");
      }
    }

    private Builder setStream(String filename, InputStream stream, byte[] data, Long length) {
      operations.add(args -> args.filename = filename);
      operations.add(args -> args.stream = stream);
      operations.add(args -> args.data = data);
      operations.add(args -> args.length = length);
      return this;
    }

    public Builder filename(String filename) throws MinioException {
      try {
        validateFilename(filename);
        long length = Files.size(Paths.get(filename));
        return setStream(filename, null, null, length);
      } catch (IOException e) {
        throw new MinioException(e);
      }
    }

    public Builder stream(InputStream stream, Long length) {
      Utils.validateNotNull(stream, "stream");
      return setStream(null, stream, null, length);
    }

    public Builder data(byte[] data, int length) {
      if (data != null && length <= 0) {
        throw new IllegalArgumentException("valid length must be provided");
      }
      return setStream(null, null, data, data == null ? null : (long) length);
    }

    public Builder chunkSize(Long chunkSize) {
      if (chunkSize != null) {
        if (chunkSize < ObjectWriteArgs.MIN_MULTIPART_SIZE) {
          throw new IllegalArgumentException("chunk size must be minimum of 5 MiB");
        }
        if (chunkSize > ObjectWriteArgs.MAX_PART_SIZE) {
          throw new IllegalArgumentException("chunk size must be less than 5 GiB");
        }
      }
      operations.add(args -> args.chunkSize = chunkSize);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AppendObjectArgs)) return false;
    if (!super.equals(o)) return false;
    AppendObjectArgs that = (AppendObjectArgs) o;
    return Objects.equals(filename, that.filename)
        && Objects.equals(stream, that.stream)
        && Arrays.equals(data, that.data)
        && Objects.equals(length, that.length)
        && Objects.equals(chunkSize, that.chunkSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), filename, stream, data, length, chunkSize);
  }
}
