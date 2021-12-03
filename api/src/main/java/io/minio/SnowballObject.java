/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
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

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Single object entry for {@link UploadSnowballObjectsArgs#objects}. */
public class SnowballObject {
  private String name;
  private InputStream stream;
  private long size;
  private ZonedDateTime modificationTime;
  private String filename;

  public SnowballObject(
      @Nonnull String name,
      @Nonnull InputStream stream,
      long size,
      @Nullable ZonedDateTime modificationTime) {
    if (name == null || name.isEmpty()) throw new IllegalArgumentException("name must be provided");
    this.name = name.startsWith("/") ? name.substring(1) : name;
    this.stream = Objects.requireNonNull(stream, "stream must not be null");
    if (size < 0) throw new IllegalArgumentException("size cannot be negative value");
    this.size = size;
    this.modificationTime = modificationTime;
  }

  public SnowballObject(@Nonnull String name, @Nonnull String filename) {
    if (name == null || name.isEmpty()) throw new IllegalArgumentException("name must be provided");
    this.name = name.startsWith("/") ? name.substring(1) : name;
    if (filename == null || filename.isEmpty()) {
      throw new IllegalArgumentException("filename must be provided");
    }
    this.filename = filename;
  }

  public String name() {
    return this.name;
  }

  public InputStream stream() {
    return this.stream;
  }

  public long size() {
    return this.size;
  }

  public String filename() {
    return this.filename;
  }

  public ZonedDateTime modificationTime() {
    return this.modificationTime;
  }
}
