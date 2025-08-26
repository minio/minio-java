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

import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Objects;

/** Arguments of {@link BaseS3Client#putObject}. */
public abstract class PutObjectAPIBaseArgs extends ObjectArgs {
  protected RandomAccessFile file;
  protected ByteBuffer buffer;
  protected byte[] data;
  protected Long length;
  protected Http.Headers headers;

  protected PutObjectAPIBaseArgs() {}

  private PutObjectAPIBaseArgs(PutObjectBaseArgs args, Http.Headers headers) {
    super(args);
    this.headers = headers;
  }

  protected PutObjectAPIBaseArgs(PutObjectBaseArgs args, ByteBuffer buffer, Http.Headers headers) {
    this(args, headers);
    this.buffer = buffer;
  }

  protected PutObjectAPIBaseArgs(
      PutObjectBaseArgs args, RandomAccessFile file, long length, Http.Headers headers) {
    this(args, headers);
    this.file = file;
    this.length = length;
  }

  protected PutObjectAPIBaseArgs(
      PutObjectBaseArgs args, byte[] data, int length, Http.Headers headers) {
    this(args, headers);
    this.data = data;
    this.length = (long) length;
  }

  private PutObjectAPIBaseArgs(AppendObjectArgs args, Http.Headers headers) {
    super(args);
    this.headers = headers;
  }

  protected PutObjectAPIBaseArgs(AppendObjectArgs args, ByteBuffer buffer, Http.Headers headers) {
    this(args, headers);
    this.buffer = buffer;
  }

  protected PutObjectAPIBaseArgs(
      AppendObjectArgs args, RandomAccessFile file, long length, Http.Headers headers) {
    this(args, headers);
    this.file = file;
    this.length = length;
  }

  protected PutObjectAPIBaseArgs(
      AppendObjectArgs args, byte[] data, int length, Http.Headers headers) {
    this(args, headers);
    this.data = data;
    this.length = (long) length;
  }

  protected PutObjectAPIBaseArgs(
      UploadSnowballObjectsArgs args, byte[] data, int length, Http.Headers headers) {
    super(args);
    this.data = data;
    this.length = (long) length;
    this.headers = headers;
  }

  protected PutObjectAPIBaseArgs(
      UploadSnowballObjectsArgs args, RandomAccessFile file, long length, Http.Headers headers) {
    super(args);
    this.file = file;
    this.length = length;
    this.headers = headers;
  }

  public RandomAccessFile file() {
    return file;
  }

  public ByteBuffer buffer() {
    return buffer;
  }

  public byte[] data() {
    return data;
  }

  public Long length() {
    return length;
  }

  public Http.Headers headers() {
    return headers;
  }

  /** Base argument builder of {@link PutObjectAPIBaseArgs}. */
  @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
  public abstract static class Builder<B extends Builder<B, A>, A extends PutObjectAPIBaseArgs>
      extends ObjectArgs.Builder<B, A> {
    protected void validate(A args) {
      super.validate(args);
      if (!((args.file != null) != (args.buffer != null) != (args.data != null)
          && !(args.file != null && args.buffer != null && args.data != null))) {
        throw new IllegalArgumentException("only one of file, buffer or data must be provided");
      }
    }

    public B setData(RandomAccessFile file, ByteBuffer buffer, byte[] data, Long length) {
      operations.add(args -> args.file = file);
      operations.add(args -> args.buffer = buffer);
      operations.add(args -> args.data = data);
      operations.add(args -> args.length = length);
      return (B) this;
    }

    public B file(RandomAccessFile file, long length) {
      Utils.validateNotNull(file, "file");
      if (length < 0) throw new IllegalArgumentException("valid length must be provided");
      return setData(file, null, null, length);
    }

    public B buffer(ByteBuffer buffer) {
      Utils.validateNotNull(buffer, "buffer");
      return setData(null, buffer, null, null);
    }

    public B data(byte[] data, int length) {
      Utils.validateNotNull(data, "data");
      if (length < 0) throw new IllegalArgumentException("valid length must be provided");
      return setData(null, null, data, (long) length);
    }

    public B headers(Http.Headers headers) {
      operations.add(args -> args.headers = headers);
      return (B) this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PutObjectAPIBaseArgs)) return false;
    if (!super.equals(o)) return false;
    PutObjectAPIBaseArgs that = (PutObjectAPIBaseArgs) o;
    return Objects.equals(file, that.file)
        && Objects.equals(buffer, that.buffer)
        && Arrays.equals(data, that.data)
        && Objects.equals(length, that.length)
        && Objects.equals(headers, that.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), file, buffer, data, length, headers);
  }
}
