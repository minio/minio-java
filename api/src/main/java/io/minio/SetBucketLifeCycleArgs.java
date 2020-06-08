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

/** Argument class of MinioClient.setBucketLifeCycle(). */
public class SetBucketLifeCycleArgs extends BucketArgs {
  private String config;

  public String config() {
    return config;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link SetBucketLifeCycleArgs}. */
  public static final class Builder extends BucketArgs.Builder<Builder, SetBucketLifeCycleArgs> {
    private void validateConfig(String config) {
      validateNotEmptyString(config, "life-cycle configuration");
    }

    protected void validate(SetBucketLifeCycleArgs args) {
      super.validate(args);
      validateConfig(args.config);
    }

    public Builder config(String config) {
      validateConfig(config);
      operations.add(args -> args.config = config);
      return this;
    }
  }
}
