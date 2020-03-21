/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2019 MinIO, Inc.
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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Helper class to denote Output Serialization information of {@link SelectObjectContentRequest}.
 */
@Root(name = "OutputSerialization")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class OutputSerialization {
  @Element(name = "CSV", required = false)
  private CsvOutputSerialization csv;

  @Element(name = "JSON", required = false)
  private JsonOutputSerialization json;

  /** Constructs a new OutputSerialization object with CSV. */
  public OutputSerialization(
      Character fieldDelimiter,
      Character quoteCharacter,
      Character quoteEscapeCharacter,
      QuoteFields quoteFields,
      Character recordDelimiter) {
    this.csv =
        new CsvOutputSerialization(
            fieldDelimiter, quoteCharacter, quoteEscapeCharacter, quoteFields, recordDelimiter);
  }

  /** Constructs a new OutputSerialization object with JSON. */
  public OutputSerialization(Character recordDelimiter) {
    this.json = new JsonOutputSerialization(recordDelimiter);
  }
}
