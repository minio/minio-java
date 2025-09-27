/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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

import io.minio.errors.InternalException;
import java.util.Objects;

/** A source object definition for {@link ComposeObjectArgs} and {@link CopyObjectArgs}. */
public class SourceObject extends ObjectConditionalReadArgs {
  private Long objectSize = null;

  protected SourceObject() {}

  public SourceObject(SourceObject args, long objectSize, String etag) {
    super(args, etag);
    validateSize(objectSize);
    this.objectSize = objectSize;
  }

  private void throwException(long objectsize, long arg, String argName) {
    StringBuilder builder =
        new StringBuilder().append("source ").append(bucketName).append("/").append(objectName);

    if (versionId != null) builder.append("?versionId=").append(versionId);

    builder
        .append(": ")
        .append(argName)
        .append(" ")
        .append(arg)
        .append(" is beyond object size ")
        .append(objectSize);

    throw new IllegalArgumentException(builder.toString());
  }

  private void validateSize(long objectSize) {
    if (offset != null && offset >= objectSize) throwException(objectSize, offset, "offset");

    if (length != null) {
      if (length > objectSize) throwException(objectSize, length, "length");
      long composeSize = (offset == null ? 0 : offset) + length;
      if (composeSize > objectSize) throwException(objectSize, composeSize, "compose size");
    }
  }

  public Long objectSize() {
    return objectSize;
  }

  public Http.Headers headers() throws InternalException {
    if (this.objectSize == null) {
      throw new InternalException("SourceObject must be created with object size", null);
    }
    return makeCopyHeaders();
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link SourceObject}. */
  public static final class Builder
      extends ObjectConditionalReadArgs.Builder<Builder, SourceObject> {}

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SourceObject)) return false;
    if (!super.equals(o)) return false;
    SourceObject that = (SourceObject) o;
    return Objects.equals(objectSize, that.objectSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), objectSize);
  }
}
