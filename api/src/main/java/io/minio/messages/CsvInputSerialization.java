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

import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Helper class to generate Amazon AWS S3 request XML for
 * SelectObjectContentRequest/InputSerialization/CSV information.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class CsvInputSerialization extends XmlEntity {
  @Key("AllowQuotedRecordDelimiter")
  private boolean allowQuotedRecordDelimiter;

  @Key("Comments")
  private Character comments;

  @Key("FieldDelimiter")
  private Character fieldDelimiter;

  @Key("FileHeaderInfo")
  private String fileHeaderInfo;

  @Key("QuoteCharacter")
  private Character quoteCharacter;

  @Key("QuoteEscapeCharacter")
  private Character quoteEscapeCharacter;

  @Key("RecordDelimiter")
  private Character recordDelimiter;

  /** Constructs a new CsvInputSerialization object. */
  public CsvInputSerialization(
      boolean allowQuotedRecordDelimiter,
      Character comments,
      Character fieldDelimiter,
      FileHeaderInfo fileHeaderInfo,
      Character quoteCharacter,
      Character quoteEscapeCharacter,
      Character recordDelimiter)
      throws XmlPullParserException {
    super();
    this.name = "CSV";

    this.allowQuotedRecordDelimiter = allowQuotedRecordDelimiter;
    this.comments = comments;
    this.fieldDelimiter = fieldDelimiter;
    if (fileHeaderInfo != null) {
      this.fileHeaderInfo = fileHeaderInfo.toString();
    }
    this.quoteCharacter = quoteCharacter;
    this.quoteEscapeCharacter = quoteEscapeCharacter;
    this.recordDelimiter = recordDelimiter;
  }
}
