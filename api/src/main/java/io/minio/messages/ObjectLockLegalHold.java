/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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
 * Helper class to construct create bucket configuration request XML for Amazon AWS S3.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class ObjectLockLegalHold extends XmlEntity{
  @Key("Status")
  private String status;

  /**
   * Constructs a new CustomRetention object with given retention.
   */
  public ObjectLockLegalHold() throws XmlPullParserException {
    super();
    super.name = "LegalHold";
  }

  /**
   * Constructs a new CustomRetention object with given retention.
   */
  public ObjectLockLegalHold(boolean legalHold) throws  XmlPullParserException {
    super();
    super.name = "LegalHold";
    if (legalHold) {
      this.status = "ON";
    } else {
      this.status = "OFF";
    }
  }

  /**
   * Indicates whether the specified object has a Legal Hold in place.
   */
  public String getStatus() {
    return status;
  }
}
