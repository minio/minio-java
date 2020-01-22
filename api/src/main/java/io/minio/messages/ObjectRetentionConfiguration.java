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

import io.minio.DateFormat;
import java.util.Date;
import org.joda.time.DateTime;
import com.google.api.client.util.Key;
import io.minio.errors.InvalidArgumentException;

/**
 * Helper class to parse Amazon AWS S3 response XML containing ObjectLockRetention information.
 */
@SuppressWarnings("SameParameterValue")
public class ObjectRetentionConfiguration extends XmlEntity {
  @Key("Mode")
  private String mode;
  @Key("RetainUntilDate")
  private String retainUntilDate;

 /**
   * Constructs a new ObjectLockRetention object.
   */
  public ObjectRetentionConfiguration() throws XmlPullParserException {
    super();
    super.name = "Retention";
  }

  /**
   * Constructs a new CustomRetention object with given retention.
   */
  public ObjectRetentionConfiguration(RetentionMode mode,  DateTime retainUntilDate) throws XmlPullParserException,
          InvalidArgumentException {
    super();
    super.name = "Retention";
    if (mode == null) {
      throw new InvalidArgumentException("null mode.");
    }
    this.mode = mode.toString();
    if (retainUntilDate == null) {
      throw new InvalidArgumentException("null retain until date.");
    }
    
    this.retainUntilDate = retainUntilDate.toString(DateFormat.RETENTION_DATE_FORMAT);  
  }

  /**
   * Returns mode.
   */
  public RetentionMode mode() {
    return RetentionMode.fromString(mode);
  }

  /**
   * Returns retain until date.
   */
  public Date retainUntil() {
    if (retainUntilDate == null ) {
      return null;
    }
    return DateFormat.RETENTION_DATE_FORMAT.parseDateTime(retainUntilDate).toDate();
  }
}

