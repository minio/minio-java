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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Arguments of {@link MinioAsyncClient#composeObject(io.minio.ComposeObjectArgs)} and {@link
 * MinioClient#composeObject}.
 */
public class ComposeObjectArgs extends ObjectWriteArgs {
  List<SourceObject> sources;

  protected ComposeObjectArgs() {}

  public ComposeObjectArgs(ComposeObjectArgs args, List<SourceObject> sources) {
    super(args);
    this.sources = sources;
  }

  public ComposeObjectArgs(CopyObjectArgs args) {
    super(args);
    this.sources = Collections.singletonList(args.source());
  }

  public List<SourceObject> sources() {
    return sources;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void validateSse(boolean isHttps) {
    super.validateSse(isHttps);
    for (SourceObject source : sources) {
      source.validateSsec(isHttps);
    }
  }

  /** Builder of {@link ComposeObjectArgs}. */
  public static final class Builder extends ObjectWriteArgs.Builder<Builder, ComposeObjectArgs> {
    private void validateSources(List<SourceObject> sources) {
      if (sources == null || sources.isEmpty()) {
        throw new IllegalArgumentException("source objects cannot be empty");
      }
    }

    @Override
    protected void validate(ComposeObjectArgs args) {
      super.validate(args);
      validateSources(args.sources);
    }

    public Builder sources(List<SourceObject> sources) {
      validateSources(sources);
      operations.add(args -> args.sources = sources);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ComposeObjectArgs)) return false;
    if (!super.equals(o)) return false;
    ComposeObjectArgs that = (ComposeObjectArgs) o;
    return Objects.equals(sources, that.sources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), sources);
  }
}
