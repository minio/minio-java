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

public class Bucket {
  private String name;

  public String name() {
    return name;
  }

  public Bucket(String bucketName) {

    if (bucketName == null) {
      throw new IllegalArgumentException("null bucket name");
    }

    // Bucket names cannot be no less than 3 and no more than 63 characters long.
    if (bucketName.length() < 3 || bucketName.length() > 63) {
      throw new IllegalArgumentException(
          bucketName
              + " : "
              + "bucket name must be at least 3 and no more than 63 characters long");
    }
    // Successive periods in bucket names are not allowed.
    if (bucketName.contains("..")) {
      String msg =
          "bucket name cannot contain successive periods. For more information refer "
              + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new IllegalArgumentException(bucketName + " : " + msg);
    }
    // Bucket names should be dns compatible.
    if (!bucketName.matches("^[a-z0-9][a-z0-9\\.\\-]+[a-z0-9]$")) {
      String msg =
          "bucket name does not follow Amazon S3 standards. For more information refer "
              + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new IllegalArgumentException(bucketName + " : " + msg);
    }
    this.name = bucketName;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
