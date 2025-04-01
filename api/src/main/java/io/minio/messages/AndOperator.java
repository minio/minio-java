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

package io.minio.messages;

import io.minio.Utils;
import java.util.Map;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;

/** Helper class to denote AND operator information for {@link RuleFilter}. */
@Root(name = "And")
public class AndOperator {
  @Element(name = "Prefix", required = false)
  @Convert(PrefixConverter.class)
  private String prefix;

  @Element(name = "ObjectSizeLessThan", required = false)
  private Long objectSizeLessThan;

  @Element(name = "ObjectSizeGreaterThan", required = false)
  private Long objectSizeGreaterThan;

  @ElementMap(
      attribute = false,
      entry = "Tag",
      inline = true,
      key = "Key",
      value = "Value",
      required = false)
  private Map<String, String> tags;

  public AndOperator(
      @Nullable @Element(name = "Prefix", required = false) String prefix,
      @Nullable
          @ElementMap(
              attribute = false,
              entry = "Tag",
              inline = true,
              key = "Key",
              value = "Value",
              required = false)
          Map<String, String> tags) {
    if (prefix == null && tags == null) {
      throw new IllegalArgumentException("At least Prefix or Tags must be set");
    }

    if (tags != null) {
      for (String key : tags.keySet()) {
        if (key.isEmpty()) {
          throw new IllegalArgumentException("Tags must not contain empty key");
        }
      }
    }

    this.prefix = prefix;
    this.tags = Utils.unmodifiableMap(tags);
  }

  public AndOperator(
      @Nullable @Element(name = "Prefix", required = false) String prefix,
      @Nullable
          @ElementMap(
              attribute = false,
              entry = "Tag",
              inline = true,
              key = "Key",
              value = "Value",
              required = false)
          Map<String, String> tags,
      @Nullable @Element(name = "ObjectSizeLessThan", required = false) Long objectSizeLessThan,
      @Nullable @Element(name = "ObjectSizeGreaterThan", required = false)
          Long objectSizeGreaterThan) {
    this(prefix, tags);
    this.objectSizeLessThan = objectSizeLessThan;
    this.objectSizeGreaterThan = objectSizeGreaterThan;
  }

  public String prefix() {
    return this.prefix;
  }

  public Long objectSizeLessThan() {
    return this.objectSizeLessThan;
  }

  public Long objectSizeGreaterThan() {
    return this.objectSizeGreaterThan;
  }

  public Map<String, String> tags() {
    return this.tags;
  }
}
