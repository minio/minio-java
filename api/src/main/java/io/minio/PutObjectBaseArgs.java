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

import java.io.IOException;

public abstract class PutObjectBaseArgs extends ObjectWriteArgs {
  protected long objectSize;
  protected long partSize;
  protected int partCount;
  protected String contentType;

  public long objectSize() {
    return objectSize;
  }

  public long partSize() {
    return partSize;
  }

  public int partCount() {
    return partCount;
  }

  /** Gets content type. It returns if content type is set (or) value of "Content-Type" header. */
  public String contentType() throws IOException {
    if (contentType != null) {
      return contentType;
    }

    if (this.headers().containsKey("Content-Type")) {
      return this.headers().get("Content-Type").iterator().next();
    }

    return null;
  }

  public abstract static class Builder<B extends Builder<B, A>, A extends PutObjectBaseArgs>
      extends ObjectWriteArgs.Builder<B, A> {}
}
