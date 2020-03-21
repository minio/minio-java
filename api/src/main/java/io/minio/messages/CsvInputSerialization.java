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
 * Helper class to denote CSV input serialization request XML as per {@link
 * SelectObjectContentRequest}.
 */
@Root(name = "CSV")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class CsvInputSerialization {
  @Element(name = "AllowQuotedRecordDelimiter", required = false)
  private boolean allowQuotedRecordDelimiter;

  @Element(name = "Comments", required = false)
  private Character comments;

  @Element(name = "FieldDelimiter", required = false)
  private Character fieldDelimiter;

  @Element(name = "FileHeaderInfo", required = false)
  private FileHeaderInfo fileHeaderInfo;

  @Element(name = "QuoteCharacter", required = false)
  private Character quoteCharacter;

  @Element(name = "QuoteEscapeCharacter", required = false)
  private Character quoteEscapeCharacter;

  @Element(name = "RecordDelimiter", required = false)
  private Character recordDelimiter;

  /** Constructs a new CsvInputSerialization object. */
  public CsvInputSerialization(
      boolean allowQuotedRecordDelimiter,
      Character comments,
      Character fieldDelimiter,
      FileHeaderInfo fileHeaderInfo,
      Character quoteCharacter,
      Character quoteEscapeCharacter,
      Character recordDelimiter) {
    this.allowQuotedRecordDelimiter = allowQuotedRecordDelimiter;
    this.comments = comments;
    this.fieldDelimiter = fieldDelimiter;
    this.fileHeaderInfo = fileHeaderInfo;
    this.quoteCharacter = quoteCharacter;
    this.quoteEscapeCharacter = quoteEscapeCharacter;
    this.recordDelimiter = recordDelimiter;
  }
}
