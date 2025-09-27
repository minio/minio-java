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
import java.util.Objects;
import okhttp3.MediaType;

/** Common arguments of {@link PutObjectArgs} and {@link UploadObjectArgs}. */
public abstract class PutObjectBaseArgs extends ObjectWriteArgs {
  protected Long objectSize;
  protected long partSize;
  protected int partCount;
  protected MediaType contentType;
  protected Checksum.Algorithm checksum;
  protected int parallelUploads;

  public Long objectSize() {
    return objectSize;
  }

  public long partSize() {
    return partSize;
  }

  public int partCount() {
    return partCount;
  }

  public MediaType contentType() throws IOException {
    return contentType != null ? contentType : super.contentType();
  }

  public Checksum.Algorithm checksum() {
    return checksum;
  }

  public int parallelUploads() {
    return parallelUploads;
  }

  /** Builder of {@link PutObjectBaseArgs}. */
  @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
  public abstract static class Builder<B extends Builder<B, A>, A extends PutObjectBaseArgs>
      extends ObjectWriteArgs.Builder<B, A> {
    protected void validate(A args) {
      super.validate(args);
      if (args.checksum != null
          && args.partCount > 0
          && (!(args.partCount == 1 && args.checksum.fullObjectSupport()
              || args.partCount > 1 && args.checksum.compositeSupport()))) {
        throw new IllegalArgumentException(
            "unsupported checksum " + args.checksum + " for part count " + args.partCount);
      }
    }

    protected long[] getPartInfo(Long objectSize, Long partSize) {
      if (partSize != null && partSize > 0) {
        if (partSize < MIN_MULTIPART_SIZE) {
          throw new IllegalArgumentException(
              "part size " + partSize + " is not supported; minimum allowed 5MiB");
        }

        if (partSize > MAX_PART_SIZE) {
          throw new IllegalArgumentException(
              "part size " + partSize + " is not supported; maximum allowed 5GiB");
        }
      }

      if (objectSize == null || objectSize < 0) {
        if (partSize == null || partSize <= 0) {
          throw new IllegalArgumentException(
              "valid part size must be provided for unknown object size");
        }
        return new long[] {partSize, -1};
      }

      if (partSize == null || partSize <= 0) {
        // Calculate part size by multiple of MIN_MULTIPART_SIZE.
        double dPartSize = Math.ceil((double) objectSize / MAX_MULTIPART_COUNT);
        dPartSize = Math.ceil(dPartSize / MIN_MULTIPART_SIZE) * MIN_MULTIPART_SIZE;
        partSize = (long) dPartSize;
      }

      if (partSize > objectSize) return new long[] {partSize, 1};

      long partCount = (long) Math.ceil((double) objectSize / partSize);
      if (partCount > MAX_MULTIPART_COUNT) {
        throw new IllegalArgumentException(
            "object size "
                + objectSize
                + " and part size "
                + partSize
                + " make more than "
                + MAX_MULTIPART_COUNT
                + "parts for upload");
      }

      return new long[] {partSize, partCount};
    }

    public B contentType(String value) {
      MediaType contentType = MediaType.parse(value);
      if (value != null && contentType == null) {
        throw new IllegalArgumentException("invalid content type '" + value + "' as per RFC 2045");
      }
      return contentType(contentType);
    }

    public B contentType(MediaType contentType) {
      operations.add(args -> args.contentType = contentType);
      return (B) this;
    }

    public B checksum(Checksum.Algorithm algorithm) {
      if (algorithm == Checksum.Algorithm.MD5) {
        throw new IllegalArgumentException(Checksum.Algorithm.MD5 + " algorithm is not allowed");
      }
      operations.add(args -> args.checksum = algorithm);
      return (B) this;
    }

    public B parallelUploads(int parallelUploads) {
      operations.add(args -> args.parallelUploads = parallelUploads);
      return (B) this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PutObjectBaseArgs)) return false;
    if (!super.equals(o)) return false;
    PutObjectBaseArgs that = (PutObjectBaseArgs) o;
    return Objects.equals(objectSize, that.objectSize)
        && partSize == that.partSize
        && partCount == that.partCount
        && Objects.equals(contentType, that.contentType)
        && Objects.equals(checksum, that.checksum)
        && parallelUploads == that.parallelUploads;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), objectSize, partSize, partCount, contentType, checksum, parallelUploads);
  }
}
