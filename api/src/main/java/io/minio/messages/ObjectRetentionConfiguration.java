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

import com.google.api.client.util.Key;
import io.minio.Time;
import java.time.ZonedDateTime;
import org.xmlpull.v1.XmlPullParserException;

/** Helper class to parse Amazon AWS S3 response XML containing ObjectLockRetention information. */
@SuppressWarnings("SameParameterValue")
public class ObjectRetentionConfiguration extends XmlEntity {
  @Key("Mode")
  private String mode;

  @Key("RetainUntilDate")
  private String retainUntilDate;

  /** Constructs a new ObjectRetentionConfiguration object. */
  public ObjectRetentionConfiguration() throws XmlPullParserException {
    super();
    super.name = "Retention";
  }

  /**
   * Constructs a new ObjectRetentionConfiguration object with given retention until date and mode.
   */
  public ObjectRetentionConfiguration(RetentionMode mode, ZonedDateTime retainUntilDate)
      throws XmlPullParserException, IllegalArgumentException {
    this();

    if (mode == null) {
      throw new IllegalArgumentException("null mode is not allowed");
    }

    if (retainUntilDate == null) {
      throw new IllegalArgumentException("null retainUntilDate is not allowed");
    }

    this.mode = mode.toString();
    this.retainUntilDate = retainUntilDate.format(Time.EXPIRATION_DATE_FORMAT);
  }

  /** Returns mode. */
  public RetentionMode mode() {
    return this.mode == null ? null : RetentionMode.fromString(this.mode);
  }

  /** Returns retain until date. */
  public ZonedDateTime retainUntilDate() {
    return retainUntilDate == null
        ? null
        : ZonedDateTime.parse(retainUntilDate, Time.EXPIRATION_DATE_FORMAT);
  }
}
