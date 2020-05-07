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

public final class VersionBucketArgs extends BucketArgs {
  private final boolean bucketVersion;

  VersionBucketArgs(Builder builder) {
    this.name = builder.name;
    this.region = builder.region;
    this.bucketVersion = builder.bucketVersion;
  }

  public boolean bucketVersion() {
    return bucketVersion;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.BucketArgsBuilder {
    private boolean bucketVersion;

    public Builder bucket(String name) {
      this.name = name;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder bucketVersion(boolean bucketVersion) {
      this.bucketVersion = bucketVersion;
      return this;
    }

    public VersionBucketArgs build() throws IllegalArgumentException {
      return new VersionBucketArgs(this);
    }
  }
}
