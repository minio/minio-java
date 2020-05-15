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

import io.minio.messages.Retention;

public class SetObjectRetentionArgs extends ObjectArgs {
  private Retention config;
  private boolean bypassGovernanceRetention;

  public Retention config() {
    return config;
  }

  public boolean bypassGovernanceRetention() {
    return bypassGovernanceRetention;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link SetObjectRetentionArgs}. */
  public static final class Builder extends ObjectArgs.Builder<Builder, SetObjectRetentionArgs> {
    public Builder config(Retention config) {
      if (config == null) {
        throw new IllegalArgumentException("null object retention configuration");
      }

      operations.add(args -> args.config = config);
      return this;
    }

    public Builder bypassGovernanceRetention(boolean bypassGovernanceRetention) {
      operations.add(args -> args.bypassGovernanceRetention = bypassGovernanceRetention);
      return this;
    }
  }
}
