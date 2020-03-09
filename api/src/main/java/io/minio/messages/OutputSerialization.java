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
 * SelectObjectContentRequest/OutputSerialization information.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class OutputSerialization extends XmlEntity {
  @Key("CSV")
  private CsvOutputSerialization csv;

  @Key("JSON")
  private JsonOutputSerialization json;

  public OutputSerialization() throws XmlPullParserException {
    super();
    this.name = "OutputSerialization";
  }

  /** Constructs a new OutputSerialization object with CSV. */
  public static OutputSerialization csv(
      Character fieldDelimiter,
      Character quoteCharacter,
      Character quoteEscapeCharacter,
      QuoteFields quoteFields,
      Character recordDelimiter)
      throws XmlPullParserException {
    OutputSerialization os = new OutputSerialization();
    os.csv =
        new CsvOutputSerialization(
            fieldDelimiter, quoteCharacter, quoteEscapeCharacter, quoteFields, recordDelimiter);
    return os;
  }

  /** Constructs a new OutputSerialization object with JSON. */
  public static OutputSerialization json(Character recordDelimiter) throws XmlPullParserException {
    OutputSerialization os = new OutputSerialization();
    os.json = new JsonOutputSerialization(recordDelimiter);
    return os;
  }
}
