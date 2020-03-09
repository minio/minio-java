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

/** Helper class to parse Amazon AWS S3 response XML containing DefaultRetention information. */
@SuppressWarnings("SameParameterValue")
public class DefaultRetention extends XmlEntity {
  @Key("Mode")
  private String mode;

  @Key("Days")
  private Integer days;

  @Key("Years")
  private Integer years;

  public DefaultRetention() throws XmlPullParserException {
    super();
    this.name = "DefaultRetention";
  }

  /** Constructs a new DefaultRetention object with given retention. */
  public DefaultRetention(RetentionMode mode, int duration, DurationUnit unit)
      throws XmlPullParserException {
    this();

    if (mode != null) {
      this.mode = mode.toString();
    }

    if (unit == DurationUnit.DAYS) {
      this.days = duration;
    } else {
      this.years = duration;
    }
  }

  /** Returns mode. */
  public RetentionMode mode() {
    return RetentionMode.fromString(mode);
  }

  /** Returns days. */
  public Integer days() {
    return days;
  }

  /** Returns years. */
  public Integer years() {
    return years;
  }
}
