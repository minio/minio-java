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

public class GetObjectArgs extends SsecObjectArgs {
  private Long offset;
  private Long length;

  public Long length() {
    return length;
  }

  public Long offset() {
    return offset;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends SsecObjectArgs.Builder<Builder, GetObjectArgs> {
    public Builder offset(Long offset) {
      validateOffset(offset);
      operations.add(args -> args.offset = offset);
      return this;
    }

    public Builder length(Long length) {
      validateLength(length);
      operations.add(args -> args.length = length);
      return this;
    }

    private void validateLength(Long length) {
      if (length != null && length <= 0) {
        throw new IllegalArgumentException("length should be greater than zero");
      }
    }

    private void validateOffset(Long offset) {
      if (offset != null && offset < 0) {
        throw new IllegalArgumentException("offset should be zero or greater");
      }
    }
  }
}
