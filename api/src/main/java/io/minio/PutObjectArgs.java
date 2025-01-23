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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import okhttp3.MediaType;

/**
 * Arguments of {@link MinioAsyncClient#putObject(io.minio.PutObjectArgs)} and {@link
 * MinioClient#putObject}.
 */
public class PutObjectArgs extends PutObjectBaseArgs {
  private InputStream stream;
  private byte[] data;

  public InputStream stream() {
    return stream;
  }

  public byte[] data() {
    return data;
  }

  public MediaType contentType() throws IOException {
    return super.contentType();
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link PutObjectArgs}. */
  public static final class Builder extends PutObjectBaseArgs.Builder<Builder, PutObjectArgs> {
    @Override
    protected void validate(PutObjectArgs args) {
      super.validate(args);
      if (args.stream == null && args.data == null) {
        throw new IllegalArgumentException("either stream or data must be provided");
      }
    }

    private Builder setStream(
        InputStream stream, byte[] data, Long objectSize, long partSize, int partCount) {
      operations.add(args -> args.stream = stream);
      operations.add(args -> args.data = data);
      operations.add(args -> args.objectSize = objectSize);
      operations.add(args -> args.partSize = partSize);
      operations.add(args -> args.partCount = partCount);
      return this;
    }

    /**
     * Sets stream to upload. Two ways to provide object/part sizes.
     *
     * <ul>
     *   <li>If object size is unknown, pass valid part size.
     *   <li>If object size is known, pass valid part size to control memory usage and no. of parts
     *       to upload.
     * </ul>
     *
     * <p>A valid part size is between 5MiB to 5GiB (both limits inclusive).
     */
    public Builder stream(InputStream stream, Long objectSize, Long partSize) {
      Utils.validateNotNull(stream, "stream");
      long[] partinfo = getPartInfo(objectSize, partSize);
      return setStream(stream, null, objectSize, partinfo[0], (int) partinfo[1]);
    }

    public Builder data(byte[] data, int length) {
      if (data != null && length < 0) {
        throw new IllegalArgumentException("valid length must be provided");
      }
      return setStream(
          null,
          data,
          data == null ? null : (long) length,
          (long) Math.max(MIN_MULTIPART_SIZE, data == null ? -1 : length),
          1);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PutObjectArgs)) return false;
    if (!super.equals(o)) return false;
    PutObjectArgs that = (PutObjectArgs) o;
    return Objects.equals(stream, that.stream) && Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), stream, data);
  }
}
