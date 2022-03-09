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

import java.util.Objects;

/** Argument class of {@link MinioAsyncClient#makeBucket} and {@link MinioClient#makeBucket}. */
public class MakeBucketArgs extends BucketArgs {
  private boolean objectLock;

  public boolean objectLock() {
    return objectLock;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link MakeBucketArgs}. */
  public static final class Builder extends BucketArgs.Builder<Builder, MakeBucketArgs> {
    public Builder objectLock(boolean objectLock) {
      operations.add(args -> args.objectLock = objectLock);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MakeBucketArgs)) return false;
    if (!super.equals(o)) return false;
    MakeBucketArgs that = (MakeBucketArgs) o;
    return objectLock == that.objectLock;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), objectLock);
  }
}
