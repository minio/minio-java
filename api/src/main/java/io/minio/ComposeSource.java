/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2019 MinIO, Inc.
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

/** Source information to compose object. */
public class ComposeSource extends ObjectVersionArgs {
  private Long offset;
  private Long length;
  private long objectSize;
  private String matchETag;
  private String notMatchETag;
  private ZonedDateTime modifiedSince;
  private ZonedDateTime unmodifiedSince;
  private ServerSideEncryptionCustomerKey ssec;

  private Multimap<String, String> headers;

  public Long offset() {
    return offset;
  }

  public Long length() {
    return length;
  }

  public long objectSize() {
    return objectSize;
  }

  public String matchETag() {
    return matchETag;
  }

  public String notMatchETag() {
    return notMatchETag;
  }

  public ZonedDateTime modifiedSince() {
    return modifiedSince;
  }

  public ZonedDateTime unmodifiedSince() {
    return unmodifiedSince;
  }

  public Multimap<String, String> headers() {
    return headers;
  }

  public ServerSideEncryptionCustomerKey ssec() {
    return ssec;
  }

  public void validateSse(HttpUrl url) {
    checkSse(ssec, url);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Constructs header. */
  public void buildHeaders(long objectSize, String etag) throws IllegalArgumentException {
    validateSize(objectSize);
    Multimap<String, String> headers = HashMultimap.create();
    headers.put("x-amz-copy-source", S3Escaper.encodePath(bucketName + "/" + objectName));
    headers.put("x-amz-copy-source-if-match", etag);

    if (extraHeaders() != null) {
      headers.putAll(extraHeaders());
    }

    if (matchETag != null) {
      headers.put("x-amz-copy-source-if-match", matchETag);
    }

    if (ssec != null) {
      headers.putAll(Multimaps.forMap(ssec.copySourceHeaders()));
    }

    if (notMatchETag != null) {
      headers.put("x-amz-copy-source-if-none-match", notMatchETag);
    }

    if (modifiedSince != null) {
      headers.put(
          "x-amz-copy-source-if-modified-since",
          modifiedSince.format(Time.HTTP_HEADER_DATE_FORMAT));
    }

    if (unmodifiedSince != null) {
      headers.put(
          "x-amz-copy-source-if-unmodified-since",
          unmodifiedSince.format(Time.HTTP_HEADER_DATE_FORMAT));
    }

    this.objectSize = objectSize;
    this.headers = headers;
  }

  private void validateSize(long objectSize) throws IllegalArgumentException {
    if (offset != null && offset >= objectSize) {
      throw new IllegalArgumentException(
          "source "
              + bucketName
              + "/"
              + objectName
              + ": offset "
              + offset
              + " is beyond object size "
              + objectSize);
    }

    if (length != null) {
      if (length > objectSize) {
        throw new IllegalArgumentException(
            "source "
                + bucketName
                + "/"
                + objectName
                + ": length "
                + length
                + " is beyond object size "
                + objectSize);
      }

      if (offset + length > objectSize) {
        throw new IllegalArgumentException(
            "source "
                + bucketName
                + "/"
                + objectName
                + ": compose size "
                + (offset + length)
                + " is beyond object size "
                + objectSize);
      }
    }
  }

  /** Argument builder of {@link ComposeSource}. */
  public static final class Builder extends ObjectVersionArgs.Builder<Builder, ComposeSource> {

    public Builder offset(long offset) {
      validateNullOrPositive(offset, "offset");
      operations.add(args -> args.offset = offset);
      return this;
    }

    public Builder length(long length) {
      validateNullOrPositive(length, "length");
      operations.add(args -> args.length = length);
      return this;
    }

    public Builder ssec(ServerSideEncryptionCustomerKey ssec) {
      operations.add(args -> args.ssec = ssec);
      return this;
    }

    public Builder matchETag(String etag) {
      validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.matchETag = etag);
      return this;
    }

    public Builder notMatchETag(String etag) {
      validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.notMatchETag = etag);
      return this;
    }

    public Builder modifiedSince(ZonedDateTime modifiedTime) {
      operations.add(args -> args.modifiedSince = modifiedTime);
      return this;
    }

    public Builder unmodifiedSince(ZonedDateTime unmodifiedTime) {
      operations.add(args -> args.unmodifiedSince = unmodifiedTime);
      return this;
    }
  }
}
