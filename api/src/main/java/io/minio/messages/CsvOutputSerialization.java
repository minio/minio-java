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

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;


/**
 * Helper class to generate Amazon AWS S3 request XML for SelectObjectContentRequest/OutputSerialization/CSV
 * information.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class CsvOutputSerialization extends XmlEntity {
  @Key("FieldDelimiter")
  private Character fieldDelimiter;
  @Key("QuoteCharacter")
  private Character quoteCharacter;
  @Key("QuoteEscapeCharacter")
  private Character quoteEscapeCharacter;
  @Key("QuoteFields")
  private String quoteFields;
  @Key("RecordDelimiter")
  private Character recordDelimiter;


  /**
   * Constructs a new CsvOutputSerialization object.
   */
  public CsvOutputSerialization(Character fieldDelimiter, Character quoteCharacter, Character quoteEscapeCharacter,
                                QuoteFields quoteFields, Character recordDelimiter) throws XmlPullParserException {
    super();
    this.name = "CSV";

    this.fieldDelimiter = fieldDelimiter;
    this.quoteCharacter = quoteCharacter;
    this.quoteEscapeCharacter = quoteEscapeCharacter;
    if (quoteFields != null) {
      this.quoteFields = quoteFields.toString();
    }
    this.recordDelimiter = recordDelimiter;
  }
}
