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

public abstract class ObjectArgs extends BucketArgs {
  private String name;
  private String versionId;

  ObjectArgs(Builder<?> b) {
    super(b);
    if ((b.name == null) || (b.name.isEmpty())) {
      throw new IllegalArgumentException("object name cannot be empty");
    }
    this.name = b.name;
    this.versionId = b.versionId;
  }

  public String objectName() {
    return this.name;
  }

  public String version() {
    return this.versionId;
  }

  public abstract static class Builder<T extends Builder<T>> extends BucketArgs.Builder<T> {
    private String name;
    private String versionId;

    @SuppressWarnings("unchecked")
    public T object(String name) {
      this.name = name;
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T version(String versionId) {
      this.versionId = versionId;
      return (T) this;
    }
  }
}
