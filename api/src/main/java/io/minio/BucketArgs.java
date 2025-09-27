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
import java.util.regex.Pattern;

/**
 * Common arguments of {@link BucketExistsArgs}, {@link CreateBucketBaseArgs}, {@link
 * DeleteBucketCorsArgs}, {@link DeleteBucketEncryptionArgs}, {@link DeleteBucketLifecycleArgs},
 * {@link DeleteBucketNotificationArgs}, {@link DeleteBucketPolicyArgs}, {@link
 * DeleteBucketReplicationArgs}, {@link DeleteBucketTagsArgs}, {@link
 * DeleteObjectLockConfigurationArgs}, {@link DeleteObjectsArgs}, {@link GetBucketCorsArgs}, {@link
 * GetBucketEncryptionArgs}, {@link GetBucketLifecycleArgs}, {@link GetBucketLocationArgs}, {@link
 * GetBucketNotificationArgs}, {@link GetBucketPolicyArgs}, {@link GetBucketReplicationArgs}, {@link
 * GetBucketTagsArgs}, {@link GetBucketVersioningArgs}, {@link GetObjectLockConfigurationArgs},
 * {@link ListenBucketNotificationArgs}, {@link ListMultipartUploadsArgs}, {@link ListObjectsArgs},
 * {@link ListObjectsV1Args}, {@link ListObjectsV2Args}, {@link ListObjectVersionsArgs}, {@link
 * ListPartsArgs}, {@link ObjectArgs}, {@link PutObjectFanOutArgs}, {@link RemoveBucketArgs}, {@link
 * RemoveObjectsArgs}, {@link SetBucketCorsArgs}, {@link SetBucketEncryptionArgs}, {@link
 * SetBucketLifecycleArgs}, {@link SetBucketNotificationArgs}, {@link SetBucketPolicyArgs}, {@link
 * SetBucketReplicationArgs}, {@link SetBucketTagsArgs}, {@link SetBucketVersioningArgs} and {@link
 * SetObjectLockConfigurationArgs}.
 */
public abstract class BucketArgs extends BaseArgs {
  protected String bucketName;
  protected String region;

  protected BucketArgs() {}

  protected BucketArgs(BucketArgs args) {
    super(args);
    this.bucketName = args.bucketName;
    this.region = args.region;
  }

  public String bucket() {
    return bucketName;
  }

  public String region() {
    return region;
  }

  /** Builder of {@link BucketArgs}. */
  public abstract static class Builder<B extends Builder<B, A>, A extends BucketArgs>
      extends BaseArgs.Builder<B, A> {
    private static final Pattern BUCKET_NAME_REGEX =
        Pattern.compile("^[a-z0-9][a-z0-9\\.\\-]{1,61}[a-z0-9]$");
    protected boolean skipValidation = false;

    protected void validateBucketName(String name) {
      Utils.validateNotNull(name, "bucket name");
      if (skipValidation) {
        return;
      }

      if (!BUCKET_NAME_REGEX.matcher(name).find()) {
        throw new IllegalArgumentException(
            "bucket name '"
                + name
                + "' does not follow Amazon S3 standards. For more information refer "
                + "https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html");
      }

      if (Utils.isValidIPv4(name)) {
        throw new IllegalArgumentException(
            "bucket name '" + name + "' must not be formatted as an IP address");
      }

      if (name.contains("..") || name.contains(".-") || name.contains("-.")) {
        throw new IllegalArgumentException(
            "bucket name '" + name + "' cannot contain successive characters '..', '.-' and '-.'");
      }
    }

    private void validateRegion(String region) {
      if (!skipValidation && region != null && !Utils.REGION_REGEX.matcher(region).find()) {
        throw new IllegalArgumentException("invalid region " + region);
      }
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
    public B skipValidation(boolean skipValidation) {
      this.skipValidation = skipValidation;
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
