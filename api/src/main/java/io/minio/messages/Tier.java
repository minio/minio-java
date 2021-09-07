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

package io.minio.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/** Tier representing retrieval tier value. */
@Root(name = "Tier")
@Convert(Tier.TierConverter.class)
public enum Tier {
  STANDARD("Standard"),
  BULK("Bulk"),
  EXPEDITED("Expedited");

  private final String value;

  private Tier(String value) {
    this.value = value;
  }

  public String toString() {
    return this.value;
  }

  /** Returns Tier of given string. */
  @JsonCreator
  public static Tier fromString(String tierString) {
    for (Tier tier : Tier.values()) {
      if (tierString.equals(tier.value)) {
        return tier;
      }
    }

    throw new IllegalArgumentException("Unknown tier '" + tierString + "'");
  }

  /** XML converter class. */
  public static class TierConverter implements Converter<Tier> {
    @Override
    public Tier read(InputNode node) throws Exception {
      return Tier.fromString(node.getValue());
    }

    @Override
    public void write(OutputNode node, Tier tier) throws Exception {
      node.setValue(tier.toString());
    }
  }
}
