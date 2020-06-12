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
import java.nio.file.Files;
import java.nio.file.Paths;

/** Argument class of MinioClient.uploadObject(). */
public class UploadObjectArgs extends PutObjectBaseArgs {
  private String filename;

  public String filename() {
    return filename;
  }

  /**
   * Gets content type. It returns if content type is set (or) value of "Content-Type" header (or)
   * probed content type of file (or) default "application/octet-stream".
   */
  public String contentType() throws IOException {
    String contentType = super.contentType();
    if (contentType != null) {
      return contentType;
    }

    contentType = Files.probeContentType(Paths.get(filename));
    return (contentType != null && !contentType.isEmpty())
        ? contentType
        : "application/octet-stream";
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link UploadObjectArgs}. */
  public static final class Builder extends ObjectWriteArgs.Builder<Builder, UploadObjectArgs> {
    @Override
    protected void validate(UploadObjectArgs args) {
      super.validate(args);
      validateFilename(args.filename);
    }

    private void validateFilename(String filename) {
      validateNotEmptyString(filename, "filename");
      if (!Files.isRegularFile(Paths.get(filename))) {
        throw new IllegalArgumentException(filename + " not a regular file");
      }
    }

    public Builder filename(String filename) throws IOException {
      validateFilename(filename);
      final long objectSize = Files.size(Paths.get(filename));
      if (objectSize > MAX_OBJECT_SIZE) {
        throw new IllegalArgumentException(
            "object size " + objectSize + " is not supported; maximum allowed 5TiB");
      }

      double pSize = Math.ceil((double) objectSize / MAX_MULTIPART_COUNT);
      pSize = Math.ceil(pSize / MIN_MULTIPART_SIZE) * MIN_MULTIPART_SIZE;

      final long partSize = (long) pSize;
      final int partCount = (pSize > 0) ? (int) Math.ceil(objectSize / pSize) : 1;

      operations.add(args -> args.filename = filename);
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
