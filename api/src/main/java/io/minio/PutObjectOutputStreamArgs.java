/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2022 MinIO, Inc.
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

/** Argument class of {@link MinioClient#putObject}. */
public class PutObjectOutputStreamArgs extends PutObjectBaseArgs {

  private int maxParallelRequests = -1;

  public int maxParallelRequests() {
    return maxParallelRequests;
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

  /** Argument builder of {@link PutObjectOutputStreamArgs}. */
  public static final class Builder
      extends PutObjectBaseArgs.Builder<Builder, PutObjectOutputStreamArgs> {
    @Override
    protected void validate(PutObjectOutputStreamArgs args) {
      super.validate(args);
      if (args.partSize == 0) {
        throw new IllegalArgumentException("Part size cannot be 0");
      }
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
    public Builder size(long objectSize, long partSize) {
      long[] partinfo = getPartInfo(objectSize, partSize);
      long pSize = partinfo[0];
      int pCount = (int) partinfo[1];
      return setSize(objectSize, pSize, pCount);
    }

    private Builder setSize(long objectSize, long partSize, int partCount) {
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

    /**
     * Set the maximum of parallel PUT requests.
     * If current parallel requests is reached and a new byte[] should be allocated, client blocks the <code>PutObjectOutputStream.write()/code> until
     * a response is received.
     * Note: The MultipartUpload POST request and last PUT (as no array is allocated) does not count in current requests.
     * Setting max is is useful to prevent <code>OutOfMemoryError</code> as too much byte[] are created and cannot be released.
     * @param max maximum of parallel requests.
     */
    public Builder maxParallelRequests(int max) {
      operations.add(args -> args.maxParallelRequests = max);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PutObjectOutputStreamArgs)) return false;
    if (!super.equals(o)) return false;
    PutObjectOutputStreamArgs that = (PutObjectOutputStreamArgs) o;
    return maxParallelRequests == that.maxParallelRequests;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), maxParallelRequests);
  }
}
