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

public final class MakeBucketArgs {
  private final String bucketName;
  private final String region;
  private final boolean objectLock;
  private final BucketName bucket;
  private static final String NULL_STRING = "(null)";

  MakeBucketArgs(Builder builder) {
    this.bucket = new BucketName(builder.bucketName);
    this.bucketName = builder.bucketName;
    this.region = builder.region;
    this.objectLock = builder.objectLock;
  }

  public String bucketName() {
    return bucketName;
  }

  public String region() {
    return region;
  }

  public BucketName bucket() {
    return this.bucket;
  }

  public boolean objectLock() {
    return objectLock;
  }

  public static final class Builder {
    private String bucketName;
    private String region;
    private boolean objectLock;

    public Builder bucket(String bucketName) throws IllegalArgumentException {
      this.bucketName = bucketName;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder objectLock(boolean objectLock) {
      this.objectLock = objectLock;
      return this;
    }

    public MakeBucketArgs build() {
      return new MakeBucketArgs(this);
    }
  }
}
