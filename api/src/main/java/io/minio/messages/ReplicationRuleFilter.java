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

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote filter information for {@link ReplicationRule}. */
@Root(name = "Filter")
public class ReplicationRuleFilter {
  @Element(name = "And", required = false)
  private AndOperator andOperator;

  @Element(name = "Prefix", required = false)
  private String prefix;

  @Element(name = "Tag", required = false)
  private Tag tag;

  public ReplicationRuleFilter(
      @Nullable @Element(name = "And", required = false) AndOperator andOperator,
      @Nullable @Element(name = "Prefix", required = false) String prefix,
      @Nullable @Element(name = "Tag", required = false) Tag tag) {
    if (andOperator != null ^ prefix != null ^ tag != null) {
      if (prefix != null && prefix.isEmpty()) {
        throw new IllegalArgumentException("Prefix must not be empty");
      }

      this.andOperator = andOperator;
      this.prefix = prefix;
      this.tag = tag;
    } else {
      throw new IllegalArgumentException("Only one of And, Prefix or Tag must be set");
    }
  }

  public ReplicationRuleFilter(@Nonnull AndOperator andOperator) {
    this.andOperator = Objects.requireNonNull(andOperator, "And operator must not be null");
  }

  public ReplicationRuleFilter(@Nonnull String prefix) {
    this.prefix = Objects.requireNonNull(prefix, "Prefix must not be null");
    if (prefix.isEmpty()) {
      throw new IllegalArgumentException("Prefix must not be empty");
    }
  }

  public ReplicationRuleFilter(@Nonnull Tag tag) {
    this.tag = Objects.requireNonNull(tag, "Tag must not be null");
  }

  public AndOperator andOperator() {
    return this.andOperator;
  }

  public String prefix() {
    return this.prefix;
  }

  public Tag tag() {
    return this.tag;
  }
}
