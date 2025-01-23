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

package io.minio.messages;

import io.minio.Utils;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;

/** Single tag information of {@link Tags} and {@link Filter}. */
@Root(name = "Tag")
public class Tag {
  @Element(name = "Key")
  private String key;

  @Element(name = "Value", required = false)
  @Convert(StringConverter.class)
  private String value;

  public Tag(
      @Nonnull @Element(name = "Key") String key,
      @Nullable @Element(name = "Value", required = false) String value) {
    Objects.requireNonNull(key, "Key must not be null");
    if (key.isEmpty()) throw new IllegalArgumentException("Key must not be empty");
    this.key = key;
    this.value = value;
  }

  public String key() {
    return this.key;
  }

  public String value() {
    return this.value;
  }

  @Override
  public String toString() {
    return String.format("Tag{key=%s, value=%s}", Utils.stringify(key), Utils.stringify(value));
  }
}
