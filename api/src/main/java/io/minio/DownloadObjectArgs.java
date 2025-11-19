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

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Arguments of {@link MinioAsyncClient#downloadObject(io.minio.DownloadObjectArgs)} and {@link
 * MinioClient#downloadObject}.
 */
public class DownloadObjectArgs extends ObjectReadArgs {
  private String filename;
  private boolean overwrite;
  protected String matchETag;
  protected String notMatchETag;
  protected ZonedDateTime modifiedSince;
  protected ZonedDateTime unmodifiedSince;

  public String filename() {
    return filename;
  }

  public boolean overwrite() {
    return overwrite;
  }

  public String matchETag() {
    return matchETag;
  }

  public String notMatchETag() {
    return notMatchETag;
  }

  public ZonedDateTime modifiedSince() {
    return modifiedSince;
  }

  public ZonedDateTime unmodifiedSince() {
    return unmodifiedSince;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Arguments of {@link DownloadObjectArgs}. */
  public static final class Builder extends ObjectReadArgs.Builder<Builder, DownloadObjectArgs> {
    private void validateFilename(String filename) {
      Utils.validateNotEmptyString(filename, "filename");
    }

    public Builder filename(String filename) {
      validateFilename(filename);
      operations.add(args -> args.filename = filename);
      return this;
    }

    public Builder overwrite(boolean flag) {
      operations.add(args -> args.overwrite = flag);
      return this;
    }

    public Builder matchETag(String etag) {
      Utils.validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.matchETag = etag);
      return this;
    }

    public Builder notMatchETag(String etag) {
      Utils.validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.notMatchETag = etag);
      return this;
    }

    public Builder modifiedSince(ZonedDateTime modifiedTime) {
      operations.add(args -> args.modifiedSince = modifiedTime);
      return this;
    }

    public Builder unmodifiedSince(ZonedDateTime unmodifiedTime) {
      operations.add(args -> args.unmodifiedSince = unmodifiedTime);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DownloadObjectArgs)) return false;
    if (!super.equals(o)) return false;
    DownloadObjectArgs that = (DownloadObjectArgs) o;
    return Objects.equals(filename, that.filename)
        && overwrite == that.overwrite
        && Objects.equals(matchETag, that.matchETag)
        && Objects.equals(notMatchETag, that.notMatchETag)
        && Objects.equals(modifiedSince, that.modifiedSince)
        && Objects.equals(unmodifiedSince, that.unmodifiedSince);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        filename,
        overwrite,
        matchETag,
        notMatchETag,
        modifiedSince,
        unmodifiedSince);
  }
}
