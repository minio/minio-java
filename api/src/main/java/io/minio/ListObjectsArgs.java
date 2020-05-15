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

/** Argument class of @see #listObjects(ListObjectsArgs args). */
public class ListObjectsArgs extends BucketArgs {
  private String continuationToken;
  private String delimiter;
  private boolean fetchOwner;
  private Integer maxKeys;
  private String prefix;
  private String startAfter;
  private boolean includeUserMetadata;
  private String marker;
  private boolean recursive;
  private boolean useVersion1;

  public String continuationToken() {
    return continuationToken;
  }

  public boolean includeUserMetadata() {
    return includeUserMetadata;
  }

  public String startAfter() {
    return startAfter;
  }

  public String prefix() {
    return prefix;
  }

  public Integer maxKeys() {
    return maxKeys;
  }

  public boolean fetchOwner() {
    return fetchOwner;
  }

  public String delimiter() {
    return delimiter;
  }

  public String marker() {
    return marker;
  }

  public boolean recursive() {
    return recursive;
  }

  public boolean useVersion1() {
    return useVersion1;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of @see MinioClient#listObjects(ListObjectArgs args). */
  public static final class Builder extends BucketArgs.Builder<Builder, ListObjectsArgs> {
    public Builder continuationToken(String continuationToken) {
      operations.add(args -> args.continuationToken = continuationToken);
      return this;
    }

    public Builder includeUserMetadata(boolean includeUserMetadata) {
      operations.add(args -> args.includeUserMetadata = includeUserMetadata);
      return this;
    }

    public Builder startAfter(String startAfter) {
      operations.add(args -> args.startAfter = startAfter);
      return this;
    }

    public Builder prefix(String prefix) {
      operations.add(args -> args.prefix = prefix);
      return this;
    }

    public Builder maxKeys(Integer maxKeys) {
      operations.add(args -> args.maxKeys = maxKeys);
      return this;
    }

    public Builder fetchOwner(boolean fetchOwner) {
      operations.add(args -> args.fetchOwner = fetchOwner);
      return this;
    }

    public Builder delimiter(String delimiter) {
      operations.add(args -> args.delimiter = delimiter);
      return this;
    }

    public Builder marker(String marker) {
      operations.add(args -> args.marker = marker);
      return this;
    }

    public Builder recursive(boolean recursive) {
      operations.add(args -> args.recursive = recursive);
      return this;
    }

    public Builder useVersion1(boolean useVersion1) {
      operations.add(args -> args.useVersion1 = useVersion1);
      return this;
    }
  }
}
