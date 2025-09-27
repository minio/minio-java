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
import java.util.Objects;
import okhttp3.MediaType;

/** Arguments of {@link BaseS3Client#putObject}. */
public class PutObjectAPIArgs extends PutObjectAPIBaseArgs {
  private MediaType contentType;

  protected PutObjectAPIArgs() {}

  public PutObjectAPIArgs(
      PutObjectBaseArgs args,
      ByteBuffer buffer,
      MediaType contentType,
      Http.Headers checksumHeaders) {
    super(args, buffer, args.makeHeaders(contentType, checksumHeaders));
    this.contentType = contentType;
  }

  public PutObjectAPIArgs(
      PutObjectBaseArgs args,
      RandomAccessFile file,
      long length,
      MediaType contentType,
      Http.Headers checksumHeaders) {
    super(args, file, length, args.makeHeaders(contentType, checksumHeaders));
    this.contentType = contentType;
  }

  public PutObjectAPIArgs(
      PutObjectBaseArgs args,
      byte[] data,
      int length,
      MediaType contentType,
      Http.Headers checksumHeaders) {
    super(args, data, length, args.makeHeaders(contentType, checksumHeaders));
    this.contentType = contentType;
  }

  public PutObjectAPIArgs(AppendObjectArgs args, ByteBuffer buffer, Http.Headers headers) {
    super(args, buffer, headers);
  }

  public PutObjectAPIArgs(
      AppendObjectArgs args, RandomAccessFile file, long length, Http.Headers headers) {
    super(args, file, length, headers);
  }

  public PutObjectAPIArgs(AppendObjectArgs args, byte[] data, int length, Http.Headers headers) {
    super(args, data, length, headers);
  }

  public PutObjectAPIArgs(
      UploadSnowballObjectsArgs args, byte[] data, int length, Http.Headers headers) {
    super(args, data, length, headers);
  }

  public PutObjectAPIArgs(
      UploadSnowballObjectsArgs args, RandomAccessFile file, long length, Http.Headers headers) {
    super(args, file, length, headers);
  }

  public MediaType contentType() {
    return contentType;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link PutObjectAPIArgs}. */
  public static final class Builder
      extends PutObjectAPIBaseArgs.Builder<Builder, PutObjectAPIArgs> {
    public Builder contentType(String value) {
      MediaType contentType = MediaType.parse(value);
      if (value != null && contentType == null) {
        throw new IllegalArgumentException("invalid content type '" + value + "' as per RFC 2045");
      }
      return contentType(contentType);
    }

    public Builder contentType(MediaType contentType) {
      operations.add(args -> args.contentType = contentType);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PutObjectAPIArgs)) return false;
    if (!super.equals(o)) return false;
    PutObjectAPIArgs that = (PutObjectAPIArgs) o;
    return Objects.equals(contentType, that.contentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), contentType);
  }
}
