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

import java.util.List;

public class ComposeObjectArgs extends ObjectWriteArgs {
  List<ComposeSourceArgs> sources;

  public List<ComposeSourceArgs> sources() {
    return sources;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectWriteArgs.Builder<Builder, ComposeObjectArgs> {
    public Builder sources(List<ComposeSourceArgs> sources) {
      validateSources(sources);
      operations.add(args -> args.sources = sources);
      return this;
    }

    private void validateSources(List<ComposeSourceArgs> sources) {
      if (sources.isEmpty()) {
        throw new IllegalArgumentException("compose sources cannot be empty");
      }
    }
  }
}
