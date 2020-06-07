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

import java.time.ZonedDateTime;
import okhttp3.HttpUrl;

/** Argument class of MinioClient.copyObject(). */
public class CopyObjectArgs extends ObjectWriteArgs {
  private String srcBucket;
  private String srcObject;
  private String srcVersionId;
  private ServerSideEncryptionCustomerKey srcSsec;
  private String srcMatchETag;
  private String srcNotMatchETag;
  private ZonedDateTime srcModifiedSince;
  private ZonedDateTime srcUnmodifiedSince;
  private Directive metadataDirective;
  private Directive taggingDirective;

  public String srcBucket() {
    return srcBucket;
  }

  public String srcObject() {
    return srcObject;
  }

  public String srcVersionId() {
    return srcVersionId;
  }

  public ServerSideEncryptionCustomerKey srcSsec() {
    return srcSsec;
  }

  public String srcMatchETag() {
    return srcMatchETag;
  }

  public String srcNotMatchETag() {
    return srcNotMatchETag;
  }

  public ZonedDateTime srcModifiedSince() {
    return srcModifiedSince;
  }

  public ZonedDateTime srcUnmodifiedSince() {
    return srcUnmodifiedSince;
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
  public void validateSse(HttpUrl url) {
    super.validateSse(url);
    checkSse(srcSsec, url);
  }

  /** Argument builder of {@link CopyObjectArgs}. */
  public static final class Builder extends ObjectWriteArgs.Builder<Builder, CopyObjectArgs> {
    @Override
    protected void validate(CopyObjectArgs args) {
      super.validate(args);
      validateBucketName(args.srcBucket);
    }

    public Builder srcBucket(String srcBucket) {
      validateBucketName(srcBucket);
      operations.add(args -> args.srcBucket = srcBucket);
      return this;
    }

    public Builder srcObject(String srcObject) {
      validateNullOrNotEmptyString(srcObject, "source object");
      operations.add(args -> args.srcObject = srcObject);
      return this;
    }

    public Builder srcVersionId(String srcVersionId) {
      validateNullOrNotEmptyString(srcVersionId, "source version ID");
      operations.add(args -> args.srcVersionId = srcVersionId);
      return this;
    }

    public Builder srcSsec(ServerSideEncryptionCustomerKey srcSsec) {
      operations.add(args -> args.srcSsec = srcSsec);
      return this;
    }

    public Builder srcMatchETag(String etag) {
      validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.srcMatchETag = etag);
      return this;
    }

    public Builder srcNotMatchETag(String etag) {
      validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.srcNotMatchETag = etag);
      return this;
    }

    public Builder srcModifiedSince(ZonedDateTime modifiedTime) {
      operations.add(args -> args.srcModifiedSince = modifiedTime);
      return this;
    }

    public Builder srcUnmodifiedSince(ZonedDateTime modifiedTime) {
      operations.add(args -> args.srcUnmodifiedSince = modifiedTime);
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
}
