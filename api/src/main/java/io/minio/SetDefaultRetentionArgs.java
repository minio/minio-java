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

import io.minio.messages.ObjectLockConfiguration;

/** Argument class of MinioClient.setDefaultRetention(). */
public class SetDefaultRetentionArgs extends BucketArgs {
  private ObjectLockConfiguration config;

  public ObjectLockConfiguration config() {
    return config;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link SetDefaultRetentionArgs}. */
  public static final class Builder extends BucketArgs.Builder<Builder, SetDefaultRetentionArgs> {
    private void validateConfig(ObjectLockConfiguration config) {
      validateNotNull(config, "object-lock configuration");
    }

    @Override
    protected void validate(SetDefaultRetentionArgs args) {
      super.validate(args);
      validateConfig(args.config);
    }

    public Builder config(ObjectLockConfiguration config) {
      validateConfig(config);
      operations.add(args -> args.config = config);
      return this;
    }
  }
}
