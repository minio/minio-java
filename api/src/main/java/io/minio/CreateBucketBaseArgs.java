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

import io.minio.messages.CreateBucketConfiguration;
import java.util.Objects;

/** Common arguments of {@link CreateBucketArgs} and {@link MakeBucketArgs}. */
public abstract class CreateBucketBaseArgs extends BucketArgs {
  protected boolean objectLock;
  protected CreateBucketConfiguration.Location locationConfig;
  protected CreateBucketConfiguration.Bucket bucket;

  protected CreateBucketBaseArgs() {}

  protected CreateBucketBaseArgs(CreateBucketBaseArgs args) {
    super(args);
    this.objectLock = args.objectLock;
    this.locationConfig = args.locationConfig;
    this.bucket = args.bucket;
  }

  public boolean objectLock() {
    return objectLock;
  }

  public CreateBucketConfiguration.Location locationConfig() {
    return locationConfig;
  }

  public CreateBucketConfiguration.Bucket bucketConfig() {
    return bucket;
  }

  /** Base argument builder of {@link CreateBucketBaseArgs}. */
  @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
  public abstract static class Builder<B extends Builder<B, A>, A extends CreateBucketBaseArgs>
      extends BucketArgs.Builder<B, A> {
    public B objectLock(boolean objectLock) {
      operations.add(args -> args.objectLock = objectLock);
      return (B) this;
    }

    public B locationConstraint(String region) {
      operations.add(args -> args.region = region);
      return (B) this;
    }

    public B locationConfig(CreateBucketConfiguration.Location locationConfig) {
      operations.add(args -> args.locationConfig = locationConfig);
      return (B) this;
    }

    public B bucketConfig(CreateBucketConfiguration.Bucket bucket) {
      operations.add(args -> args.bucket = bucket);
      return (B) this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CreateBucketBaseArgs)) return false;
    if (!super.equals(o)) return false;
    CreateBucketBaseArgs that = (CreateBucketBaseArgs) o;
    return objectLock == that.objectLock
        && Objects.equals(locationConfig, that.locationConfig)
        && Objects.equals(bucket, that.bucket);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), objectLock, locationConfig, bucket);
  }
}
