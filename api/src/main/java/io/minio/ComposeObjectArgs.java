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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import okhttp3.HttpUrl;

/**
 * Argument class of {@link MinioAsyncClient#composeObject} and {@link MinioClient#composeObject}.
 */
public class ComposeObjectArgs extends ObjectWriteArgs {
  List<ComposeSource> sources;

  protected ComposeObjectArgs() {}

  public ComposeObjectArgs(CopyObjectArgs args) {
    this.extraHeaders = args.extraHeaders;
    this.extraQueryParams = args.extraQueryParams;
    this.bucketName = args.bucketName;
    this.region = args.region;
    this.objectName = args.objectName;
    this.headers = args.headers;
    this.userMetadata = args.userMetadata;
    this.sse = args.sse;
    this.tags = args.tags;
    this.retention = args.retention;
    this.legalHold = args.legalHold;
    this.sources = new LinkedList<>();
    this.sources.add(new ComposeSource(args.source()));
  }

  public List<ComposeSource> sources() {
    return sources;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void validateSse(HttpUrl url) {
    super.validateSse(url);
    for (ComposeSource source : sources) {
      source.validateSsec(url);
    }
  }

  /** Argument builder of {@link ComposeObjectArgs}. */
  public static final class Builder extends ObjectWriteArgs.Builder<Builder, ComposeObjectArgs> {
    private void validateSources(List<ComposeSource> sources) {
      if (sources == null || sources.isEmpty()) {
        throw new IllegalArgumentException("compose sources cannot be empty");
      }
    }

    @Override
    protected void validate(ComposeObjectArgs args) {
      super.validate(args);
      validateSources(args.sources);
    }

    public Builder sources(List<ComposeSource> sources) {
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
