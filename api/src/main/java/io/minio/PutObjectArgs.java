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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Argument class of MinioClient.putObject(). */
public class PutObjectArgs extends PutObjectBaseArgs {
  private BufferedInputStream stream;

  public BufferedInputStream stream() {
    return stream;
  }

  /**
   * Gets content type. It returns if content type is set (or) value of "Content-Type" header (or)
   * default "application/octet-stream".
   */
  public String contentType() throws IOException {
    String contentType = super.contentType();
    return (contentType != null) ? contentType : "application/octet-stream";
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link PutObjectArgs}. */
  public static final class Builder extends ObjectWriteArgs.Builder<Builder, PutObjectArgs> {
    @Override
    protected void validate(PutObjectArgs args) {
      super.validate(args);
      validateNotNull(args.stream, "stream");
    }

    private void validateSizes(long objectSize, long partSize) {
      if (partSize > 0) {
        if (partSize < MIN_MULTIPART_SIZE) {
          throw new IllegalArgumentException(
              "part size " + partSize + " is not supported; minimum allowed 5MiB");
        }

        if (partSize > MAX_PART_SIZE) {
          throw new IllegalArgumentException(
              "part size " + partSize + " is not supported; maximum allowed 5GiB");
        }
      }

      if (objectSize >= 0) {
        if (objectSize > MAX_OBJECT_SIZE) {
          throw new IllegalArgumentException(
              "object size " + objectSize + " is not supported; maximum allowed 5TiB");
        }
      } else if (partSize <= 0) {
        throw new IllegalArgumentException(
            "valid part size must be provided when object size is unknown");
      }
    }

    private void validatePartCount(int partCount, long objectSize, long partSize) {
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
    }

    private long[] partInfo(long objectSize, long partSize) {
      if (objectSize < 0) {
        return new long[] {partSize, -1};
      }

      if (partSize > 0) {
        if (partSize > objectSize) {
          partSize = objectSize;
        }

        long partCount = (long) Math.ceil((double) objectSize / partSize);
        return new long[] {partSize, partCount};
      }

      double pSize = Math.ceil((double) objectSize / MAX_MULTIPART_COUNT);
      pSize = Math.ceil(pSize / MIN_MULTIPART_SIZE) * MIN_MULTIPART_SIZE;
      partSize = (long) pSize;
      long partCount = 1;
      if (pSize > 0) {
        partCount = (long) Math.ceil(objectSize / pSize);
      }

      return new long[] {partSize, partCount};
    }

    /**
     * Sets stream to upload. Two ways to provide object/part sizes.
     *
     * <ul>
     *   <li>If object size is unknown, pass -1 to objectSize and pass valid partSize.
     *   <li>If object size is known, pass -1 to partSize for auto detect; else pass valid partSize
     *       to control memory usage and no. of parts in upload.
     *   <li>If partSize is greater than objectSize, objectSize is used as partSize.
     * </ul>
     *
     * <p>A valid part size is between 5MiB to 5GiB (both limits inclusive).
     */
    public Builder stream(InputStream stream, long objectSize, long partSize) {
      validateNotNull(stream, "stream");
      validateSizes(objectSize, partSize);

      long[] partinfo = partInfo(objectSize, partSize);
      long pSize = partinfo[0];
      int pCount = (int) partinfo[1];
      validatePartCount(pCount, objectSize, partSize);

      final BufferedInputStream bis =
          (stream instanceof BufferedInputStream)
              ? (BufferedInputStream) stream
              : new BufferedInputStream(stream);
      return setStream(bis, objectSize, pSize, pCount);
    }

    private Builder setStream(
        BufferedInputStream stream, long objectSize, long partSize, int partCount) {
      operations.add(args -> args.stream = stream);
      operations.add(args -> args.objectSize = objectSize);
      operations.add(args -> args.partSize = partSize);
      operations.add(args -> args.partCount = partCount);
      return this;
    }

    public Builder contentType(String contentType) {
      validateNotEmptyString(contentType, "content type");
      operations.add(args -> args.contentType = contentType);
      return this;
    }
  }
}
