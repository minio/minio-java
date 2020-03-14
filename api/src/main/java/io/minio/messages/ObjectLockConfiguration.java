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

/** Helper class to construct create bucket configuration request XML for Amazon AWS S3. */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class ObjectLockConfiguration extends XmlEntity {
  @Key("ObjectLockEnabled")
  private String objectLockEnabled = "Enabled";

  @Key("Rule")
  private Rule rule;

  /** Constructs a new ObjectLockConfiguration object. */
  public ObjectLockConfiguration() throws XmlPullParserException {
    super();
    super.name = "ObjectLockConfiguration";
    super.namespaceDictionary.set("", "http://s3.amazonaws.com/doc/2006-03-01/");
  }

  /** Constructs a new ObjectLockConfiguration object with given retention. */
  public ObjectLockConfiguration(RetentionMode mode, int duration, DurationUnit unit)
      throws XmlPullParserException {
    this();

    this.rule = new Rule(mode, duration, unit);
  }

  /** Returns mode. */
  public RetentionMode mode() {
    if (rule == null) {
      return null;
    }

    return rule.mode();
  }

  /** Returns days. */
  public Integer days() {
    if (rule == null) {
      return null;
    }

    return rule.days();
  }

  /** Returns years. */
  public Integer years() {
    if (rule == null) {
      return null;
    }

    return rule.years();
  }
}
