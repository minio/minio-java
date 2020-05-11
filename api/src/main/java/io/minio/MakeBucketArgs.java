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

public class MakeBucketArgs extends BucketArgs {
  private final boolean objectLock;

  MakeBucketArgs(Builder builder) {
    super(builder);
    this.objectLock = builder.objectLock;
  }

  public boolean objectLock() {
    return objectLock;
  }

  public MakeBucketArgs() {
    this(new Builder());
  }

  public Builder builder() {
    return new Builder(this);
  }

  public static final class Builder extends BucketArgs.Builder<Builder> {
    private boolean objectLock;

    public Builder() {}

    public Builder(MakeBucketArgs args) {
      this.name = args.bucketName();
      this.region = args.region();
      this.objectLock = args.objectLock();
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
