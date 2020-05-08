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

public final class MakeBucketArgs extends BucketArgs {
  private final boolean objectLock;

  MakeBucketArgs(Builder builder) {
    super(builder.bucket, builder.region);
    this.objectLock = builder.objectLock;
  }

  public boolean objectLock() {
    return objectLock;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    public String bucket;
    public String region;
    private boolean objectLock;

    public Builder bucket(String name) {
      this.bucket = name;
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

    public MakeBucketArgs build() throws IllegalArgumentException {
      return new MakeBucketArgs(this);
    }
  }
}
