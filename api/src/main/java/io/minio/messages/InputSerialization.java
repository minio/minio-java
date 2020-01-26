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
 * Helper class to generate Amazon AWS S3 request XML for SelectObjectContentRequest/InputSerialization information.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class InputSerialization extends XmlEntity {
  @Key("CompressionType")
  private String compressionType;
  @Key("CSV")
  private CsvInputSerialization csv;
  @Key("JSON")
  private JsonInputSerialization json;
  @Key("Parquet")
  private ParquetInputSerialization parquet;

  
  public InputSerialization() throws XmlPullParserException {
    super();
    this.name = "InputSerialization";
  }

  
  /**
   * Constructs a new InputSerialization object with CSV.
   */
  public static InputSerialization csv(CompressionType compressionType, boolean allowQuotedRecordDelimiter,
                                       Character comments, Character fieldDelimiter, FileHeaderInfo fileHeaderInfo,
                                       Character quoteCharacter, Character quoteEscapeCharacter,
                                       Character recordDelimiter) throws XmlPullParserException {
    InputSerialization is = new InputSerialization();
    if (compressionType != null) {
      is.compressionType = compressionType.toString();
    }
    is.csv = new CsvInputSerialization(allowQuotedRecordDelimiter, comments, fieldDelimiter, fileHeaderInfo,
                                       quoteCharacter, quoteEscapeCharacter, recordDelimiter);
    return is;
  }


  /**
   * Constructs a new InputSerialization object with JSON.
   */
  public static InputSerialization json(CompressionType compressionType, JsonType type) throws XmlPullParserException {
    InputSerialization is = new InputSerialization();
    if (compressionType != null) {
      is.compressionType = compressionType.toString();
    }
    is.json = new JsonInputSerialization(type);
    return is;
  }


  /**
   * Constructs a new InputSerialization object with Parquet.
   */
  public static InputSerialization parquet() throws XmlPullParserException {
    InputSerialization is = new InputSerialization();
    is.parquet = new ParquetInputSerialization();
    return is;
  }
}
