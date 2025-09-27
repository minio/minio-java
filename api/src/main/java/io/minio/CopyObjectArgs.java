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

import java.util.Objects;

/**
 * Arguments of {@link BaseS3Client#copyObject}, {@link MinioAsyncClient#copyObject} and {@link
 * MinioClient#copyObject}.
 */
public class CopyObjectArgs extends ObjectWriteArgs {
  private SourceObject source;
  private Directive metadataDirective;
  private Directive taggingDirective;

  protected CopyObjectArgs() {}

  public CopyObjectArgs(CopyObjectArgs args, SourceObject source) {
    super(args);
    this.metadataDirective = args.metadataDirective;
    this.taggingDirective = args.taggingDirective;
    this.source = source;
  }

  public CopyObjectArgs(ComposeObjectArgs args) {
    super(args);
    this.source = args.sources().get(0);
  }

  public SourceObject source() {
    return source;
  }

  public Directive metadataDirective() {
    return metadataDirective;
  }

  public Directive taggingDirective() {
    return taggingDirective;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void validateSse(boolean isHttps) {
    super.validateSse(isHttps);
    source.validateSsec(isHttps);
  }

  /** Builder of {@link CopyObjectArgs}. */
  public static final class Builder extends ObjectWriteArgs.Builder<Builder, CopyObjectArgs> {
    @Override
    protected void validate(CopyObjectArgs args) {
      super.validate(args);
      Utils.validateNotNull(args.source, "source object");
      if (args.source.offset() != null || args.source.length() != null) {
        if (args.metadataDirective == Directive.COPY) {
          throw new IllegalArgumentException(
              "COPY metadata directive is not applicable to source object with range");
        }
        if (args.taggingDirective == Directive.COPY) {
          throw new IllegalArgumentException(
              "COPY tagging directive is not applicable to source object with range");
        }
      }
    }

    public Builder source(SourceObject source) {
      Utils.validateNotNull(source, "source object");
      operations.add(args -> args.source = source);
      return this;
    }

    public Builder metadataDirective(Directive directive) {
      operations.add(args -> args.metadataDirective = directive);
      return this;
    }

    public Builder taggingDirective(Directive directive) {
      operations.add(args -> args.taggingDirective = directive);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CopyObjectArgs)) return false;
    if (!super.equals(o)) return false;
    CopyObjectArgs that = (CopyObjectArgs) o;
    return Objects.equals(source, that.source)
        && metadataDirective == that.metadataDirective
        && taggingDirective == that.taggingDirective;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), source, metadataDirective, taggingDirective);
  }
}
