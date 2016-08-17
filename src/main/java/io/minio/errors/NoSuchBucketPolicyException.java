/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

package io.minio.errors;

import io.minio.policy.BucketPolicy;

/**
 * Thrown to indicate that given bucket name is not valid.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class NoSuchBucketPolicyException extends MinioException {
  private final String bucketName;
  private final String objectPrefix;
  private final BucketPolicy bucketPolicy;


  /**
   * Constructs a new NoSuchBucketPolicyException with bucket name caused the error and error message.
   */
  public NoSuchBucketPolicyException(String bucketName, String objectPrefix, BucketPolicy bucketPolicy) {
    super();
    this.bucketName = bucketName;
    this.objectPrefix = objectPrefix;
    this.bucketPolicy = bucketPolicy;
  }


  @Override
  public String toString() {
    return String.format("No policy exists on %s/%s", this.bucketName, this.objectPrefix) + ": " + super.toString();
  }
}
