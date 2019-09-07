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
 * Helper class to parse Amazon AWS S3 response XML containing Rule information.
 */
@SuppressWarnings("SameParameterValue")
public class Rule extends XmlEntity {
  @Key("DefaultRetention")
  private DefaultRetention defaultRetention;


  public Rule() throws XmlPullParserException {
    super();
    this.name = "Rule";
  }


  /**
   * Constructs a new Rule object with given retention.
   */
  public Rule(RetentionMode mode, int duration, DurationUnit unit) throws XmlPullParserException {
    this();

    this.defaultRetention = new DefaultRetention(mode, duration, unit);
  }


  /**
   * Returns mode.
   */
  public RetentionMode mode() {
    return defaultRetention.mode();
  }


  /**
   * Returns days.
   */
  public Integer days() {
    return defaultRetention.days();
  }


  /**
   * Returns years.
   */
  public Integer years() {
    return defaultRetention.years();
  }
}
