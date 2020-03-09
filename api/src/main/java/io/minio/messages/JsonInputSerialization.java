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
 * SelectObjectContentRequest/InputSerialization/JSON information.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class JsonInputSerialization extends XmlEntity {
  @Key("Type")
  private String type;

  /** Constructs a new JsonInputSerialization object. */
  public JsonInputSerialization(JsonType type) throws XmlPullParserException {
    super();
    this.name = "JSON";

    if (type != null) {
      this.type = type.toString();
    }
  }
}
