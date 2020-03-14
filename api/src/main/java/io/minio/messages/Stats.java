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
import com.google.api.client.xml.XmlNamespaceDictionary;
import java.io.IOException;
import java.io.Reader;
import org.xmlpull.v1.XmlPullParserException;

/** Helper class to parse Stats/Progress message in S3 select response message. */
public class Stats extends XmlEntity {
  @Key("BytesScanned")
  private long bytesScanned = -1;

  @Key("BytesProcessed")
  private long bytesProcessed = -1;

  @Key("BytesReturned")
  private long bytesReturned = -1;

  /** Constructs a new Stats object. */
  public Stats(String name) throws XmlPullParserException {
    super();
    super.name = name;
  }

  /**
   * Fills up this Stats object's fields by reading/parsing values from given Reader input stream.
   */
  @Override
  public void parseXml(Reader reader) throws IOException, XmlPullParserException {
    XmlNamespaceDictionary namespaceDictionary = new XmlNamespaceDictionary();
    namespaceDictionary.set("s3", "http://s3.amazonaws.com/doc/2006-03-01/");
    namespaceDictionary.set("", "");
    super.parseXml(reader, namespaceDictionary);
  }

  /** Returns bytes scanned. */
  public long bytesScanned() {
    return this.bytesScanned;
  }

  /** Returns bytes processed. */
  public long bytesProcessed() {
    return this.bytesProcessed;
  }

  /** Returns bytes returned. */
  public long bytesReturned() {
    return this.bytesReturned;
  }
}
