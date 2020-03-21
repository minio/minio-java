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
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Root;

/** Helper class to denote DefaultRetention information for {@link ObjectLockConfiguration}. */
@Root(name = "DefaultRetention", strict = false)
public class DefaultRetention {
  @Element(name = "Mode", required = false)
  private RetentionMode mode;

  @ElementUnion({
    @Element(name = "Days", type = RetentionDurationDays.class, required = false),
    @Element(name = "Years", type = RetentionDurationYears.class, required = false)
  })
  private RetentionDuration duration;

  public DefaultRetention() {}

  public DefaultRetention(RetentionMode mode, RetentionDuration duration) {
    this.mode = mode;
    this.duration = duration;
  }

  public RetentionMode mode() {
    return mode;
  }

  public RetentionDuration duration() {
    return duration;
  }
}
