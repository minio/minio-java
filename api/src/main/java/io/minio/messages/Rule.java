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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote Rule information for {@link ObjectLockConfiguration}. */
@Root(name = "Rule", strict = false)
public class Rule {
  @Element(name = "DefaultRetention", required = false)
  private DefaultRetention defaultRetention;

  public Rule() {}

  /** Constructs a new Rule object with given retention. */
  public Rule(RetentionMode mode, RetentionDuration duration) {
    if (mode != null && duration != null) {
      this.defaultRetention = new DefaultRetention(mode, duration);
    }
  }

  /** Returns retention mode. */
  public RetentionMode mode() {
    if (defaultRetention == null) {
      return null;
    }

    return defaultRetention.mode();
  }

  /** Returns retention duration. */
  public RetentionDuration duration() {
    if (defaultRetention == null) {
      return null;
    }

    return defaultRetention.duration();
  }
}
