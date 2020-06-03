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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.time.ZonedDateTime;
import okhttp3.HttpUrl;

/** Argument class of MinioClient.composeObject(). */
public class ComposeSourceArgs extends ObjectWriteArgs {
  private String srcVersionId;
  private Long srcOffset;
  private Long srcLength;
  private long srcObjectSize;
  private ServerSideEncryptionCustomerKey srcSsec;
  private Multimap<String, String> headers;

  private String srcMatchETag;
  private String srcNotMatchETag;
  private ZonedDateTime srcModifiedSince;
  private ZonedDateTime srcUnmodifiedSince;
  private Directive metadataDirective;
  private Directive taggingDirective;

  public String srcBucket() {
    return bucketName;
  }

  public String srcObject() {
    return objectName;
  }

  public String srcVersionId() {
    return srcVersionId;
  }

  public Long srcOffset() {
    return srcOffset;
  }

  public Long srcLength() {
    return srcLength;
  }

  public long objectSize() {
    return srcObjectSize;
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

  public Multimap<String, String> headers() {
    return headers;
  }

  /** Constructs header . */
  public void buildHeaders(long objectSize, String etag) throws IllegalArgumentException {
    if (srcOffset != null && srcOffset >= objectSize) {
      throw new IllegalArgumentException(
          "source "
              + bucketName
              + "/"
              + objectName
              + ": offset "
              + srcOffset
              + " is beyond object size "
              + objectSize);
    }

    if (srcLength != null) {
      if (srcLength > objectSize) {
        throw new IllegalArgumentException(
            "source "
                + bucketName
                + "/"
                + objectName
                + ": length "
                + srcLength
                + " is beyond object size "
                + objectSize);
      }

      if (srcOffset + srcLength > objectSize) {
        throw new IllegalArgumentException(
            "source "
                + bucketName
                + "/"
                + objectName
                + ": compose size "
                + (srcOffset + srcLength)
                + " is beyond object size "
                + objectSize);
      }
    }

    Multimap<String, String> headers = HashMultimap.create();
    headers.put("x-amz-copy-source", S3Escaper.encodePath(bucketName + "/" + objectName));
    headers.put("x-amz-copy-source-if-match", etag);

    if (extraHeaders() != null) {
      headers.putAll(extraHeaders());
    }

    if (srcMatchETag != null) {
      headers.put("x-amz-copy-source-if-match", srcMatchETag);
    }

    if (srcSsec != null) {
      headers.putAll(Multimaps.forMap(srcSsec.copySourceHeaders()));
    }

    if (srcNotMatchETag != null) {
      headers.put("x-amz-copy-source-if-none-match", srcNotMatchETag);
    }

    if (srcModifiedSince != null) {
      headers.put(
          "x-amz-copy-source-if-modified-since",
          srcModifiedSince.format(Time.HTTP_HEADER_DATE_FORMAT));
    }

    if (srcUnmodifiedSince != null) {
      headers.put(
          "x-amz-copy-source-if-unmodified-since",
          srcUnmodifiedSince.format(Time.HTTP_HEADER_DATE_FORMAT));
    }

    if (metadataDirective != null) {
      headers.put("x-amz-metadata-directive", metadataDirective.name());
    }

    if (taggingDirective != null) {
      headers.put("x-amz-tagging-directive", taggingDirective.name());
    }

    this.srcObjectSize = objectSize;
    this.headers = headers;
  }

  /** Argument builder of {@link ComposeSourceArgs}. */
  public static final class Builder extends ObjectWriteArgs.Builder<Builder, ComposeSourceArgs> {
    @Override
    protected void validate(ComposeSourceArgs args) {
      super.validate(args);
      validateBucketName(args.bucket());
    }

    public Builder srcBucket(String srcBucket) {
      validateBucketName(srcBucket);
      operations.add(args -> args.bucketName = srcBucket);
      return this;
    }

    public Builder srcObject(String srcObject) {
      validateNullOrNotEmptyString(srcObject, "source object");
      operations.add(args -> args.objectName = srcObject);
      return this;
    }

    public Builder srcVersionId(String srcVersionId) {
      validateNullOrNotEmptyString(srcVersionId, "source version ID");
      operations.add(args -> args.srcVersionId = srcVersionId);
      return this;
    }

    public Builder srcOffset(long offset) {
      validateNullOrEmpty(offset, "offset");
      operations.add(args -> args.srcOffset = offset);
      return this;
    }

    public Builder srcLength(long length) {
      validateNullOrEmpty(length, "length");
      operations.add(args -> args.srcLength = length);
      return this;
    }

    public Builder srcSsec(ServerSideEncryptionCustomerKey ssec) {
      operations.add(args -> args.srcSsec = ssec);
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
