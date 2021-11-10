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

/** Base argument class holds bucket name and region. */
public abstract class BucketArgs extends BaseArgs {
  protected String bucketName;
  protected String region;

  public String bucket() {
    return bucketName;
  }

  public String region() {
    return region;
  }

  /** Base argument builder class for {@link BucketArgs}. */
  public abstract static class Builder<B extends Builder<B, A>, A extends BucketArgs>
      extends BaseArgs.Builder<B, A> {
    protected void validateBucketName(String name) {
      validateNotNull(name, "bucket name");

      // Bucket names cannot be no less than 3 and no more than 63 characters long.
      if (name.length() < 3 || name.length() > 63) {
        throw new IllegalArgumentException(
            name + " : " + "bucket name must be at least 3 and no more than 63 characters long");
      }
      // Successive periods in bucket names are not allowed.
      if (name.contains("..")) {
        String msg =
            "bucket name cannot contain successive periods. For more information refer "
                + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
        throw new IllegalArgumentException(name + " : " + msg);
      }
      // Bucket names should be dns compatible.
      if (!name.matches("^[a-z0-9][a-z0-9\\.\\-]+[a-z0-9]$")) {
        String msg =
            "bucket name does not follow Amazon S3 standards. For more information refer "
                + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
        throw new IllegalArgumentException(name + " : " + msg);
      }
    }

    private void validateRegion(String region) {
      validateNullOrNotEmptyString(region, "region");
    }

    @Override
    protected void validate(A args) {
      validateBucketName(args.bucketName);
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B extends this class.
    public B bucket(String name) {
      validateBucketName(name);
      operations.add(args -> args.bucketName = name);
      return (B) this;
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B extends this class.
    public B region(String region) {
      validateRegion(region);
      operations.add(args -> args.region = region);
      return (B) this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BucketArgs)) return false;
    if (!super.equals(o)) return false;
    BucketArgs that = (BucketArgs) o;
    return Objects.equals(bucketName, that.bucketName) && Objects.equals(region, that.region);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), bucketName, region);
  }
}
