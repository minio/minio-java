/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage,
 * (C) 2017 Minio, Inc.
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

import org.joda.time.DateTime;
import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;
import io.minio.DateFormat;

/**
 * Helper class to parse Amazon AWS S3 response XML containing
 * object item information.
 */
@SuppressWarnings({ "SameParameterValue", "unused" })
public class CopyObjectResult extends XmlEntity {
  @Key("ETag")
  private String etag;
  @Key("LastModified")
  private String lastModified;

  public CopyObjectResult() throws XmlPullParserException {
    super();
    super.name = "CopyObjectResult";
  }

  /**
   * Returns last modified time of the object.
   */
  public DateTime lastModified() {
    return DateFormat.RESPONSE_DATE_FORMAT.parseDateTime(lastModified).toDateTime();
  }

  /**
   * Returns ETag of the object.
   */
  public String etag() {
    return etag;
  }
}
