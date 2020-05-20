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
  private boolean recursive;

  /** Returns prefix. */
  public String prefix() {
    return prefix;
  }

  /** Returns recursive flag. */
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

    public Builder recursive(boolean recursive) {
      operations.add(args -> args.recursive = recursive);
      return this;
    }
  }
}
