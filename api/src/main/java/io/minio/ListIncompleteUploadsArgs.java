/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio;

/** Argument class of @see #listIncompleteUploads(ListIncompleteUploadsArgs args). */
public class ListIncompleteUploadsArgs extends BucketArgs {
  private String prefix;
  private String delimiter;
  private String keyMarker;
  private Integer maxUploads;
  private String uploadIdMarker;
  private boolean recursive;

  public String prefix() {
    return prefix;
  }

  public String delimiter() {
    return delimiter;
  }

  public String keyMarker() {
    return keyMarker;
  }

  public Integer maxUploads() {
    return maxUploads;
  }

  public String uploadIdMarker() {
    return uploadIdMarker;
  }

  public boolean recursive() {
    return recursive;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of @see #listIncompleteUploads(ListIncompleteUploadsArgs args). */
  public static final class Builder extends BucketArgs.Builder<Builder, ListIncompleteUploadsArgs> {
    public Builder prefix(String prefix) {
      operations.add(args -> args.prefix = prefix);
      return this;
    }

    public Builder delimitter(String delimiter) {
      operations.add(args -> args.delimiter = delimiter);
      return this;
    }

    public Builder keyMarker(String keyMarker) {
      validateNullOrNotEmptyString(keyMarker, "keyMarker");
      operations.add(args -> args.keyMarker = keyMarker);
      return this;
    }

    public Builder maxUploads(int maxUploads) {
      if (maxUploads < 1 || maxUploads > 1000) {
        throw new IllegalArgumentException("maxUploads must be minimum 1 to maximum 1000");
      }
      operations.add(args -> args.maxUploads = maxUploads);
      return this;
    }

    public Builder uploadIdMarker(String uploadIdMarker) {
      validateNullOrNotEmptyString(uploadIdMarker, "uploadIdMarker");
      operations.add(args -> args.uploadIdMarker = uploadIdMarker);
      return this;
    }

    public Builder recursive(boolean recursive) {
      operations.add(args -> args.recursive = recursive);
      return this;
    }
  }
}
