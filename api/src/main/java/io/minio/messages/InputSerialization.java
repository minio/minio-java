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

/** Helper class to denote Input Serialization information of {@link SelectObjectContentRequest}. */
@Root(name = "InputSerialization")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class InputSerialization {
  @Element(name = "CompressionType", required = false)
  private CompressionType compressionType;

  @Element(name = "CSV", required = false)
  private CsvInputSerialization csv;

  @Element(name = "JSON", required = false)
  private JsonInputSerialization json;

  @Element(name = "Parquet", required = false)
  private ParquetInputSerialization parquet;

  /** Constructs a new InputSerialization object with CSV. */
  public InputSerialization(
      CompressionType compressionType,
      boolean allowQuotedRecordDelimiter,
      Character comments,
      Character fieldDelimiter,
      FileHeaderInfo fileHeaderInfo,
      Character quoteCharacter,
      Character quoteEscapeCharacter,
      Character recordDelimiter) {
    this.csv =
        new CsvInputSerialization(
            allowQuotedRecordDelimiter,
            comments,
            fieldDelimiter,
            fileHeaderInfo,
            quoteCharacter,
            quoteEscapeCharacter,
            recordDelimiter);
  }

  /** Constructs a new InputSerialization object with JSON. */
  public InputSerialization(CompressionType compressionType, JsonType type) {
    this.json = new JsonInputSerialization(type);
  }

  /** Constructs a new InputSerialization object with Parquet. */
  public InputSerialization() {
    this.parquet = new ParquetInputSerialization();
  }
}
