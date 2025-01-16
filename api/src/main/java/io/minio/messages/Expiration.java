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

import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote expiration information for {@link LifecycleRule}. */
@Root(name = "Expiration")
public class Expiration extends DateDays {
  @Element(name = "ExpiredObjectDeleteMarker", required = false)
  private Boolean expiredObjectDeleteMarker;

  @Element(name = "ExpiredObjectAllVersions", required = false)
  private Boolean expiredObjectAllVersions;

  public Expiration(
      @Nullable @Element(name = "Date", required = false) ResponseDate date,
      @Nullable @Element(name = "Days", required = false) Integer days,
      @Nullable @Element(name = "ExpiredObjectDeleteMarker", required = false)
          Boolean expiredObjectDeleteMarker,
      @Nullable @Element(name = "ExpiredObjectAllVersions", required = false)
          Boolean expiredObjectAllVersions) {
    if (expiredObjectDeleteMarker != null) {
      if (date != null || days != null) {
        throw new IllegalArgumentException(
            "ExpiredObjectDeleteMarker must not be provided along with Date and Days");
      }
    } else if (date != null ^ days != null) {
      this.date = date;
      this.days = days;
    } else {
      throw new IllegalArgumentException("Only one of date or days must be set");
    }

    this.expiredObjectDeleteMarker = expiredObjectDeleteMarker;
    this.expiredObjectAllVersions = expiredObjectAllVersions;
  }

  public Expiration(ZonedDateTime date, Integer days, Boolean expiredObjectDeleteMarker) {
    this(date == null ? null : new ResponseDate(date), days, expiredObjectDeleteMarker, null);
  }

  public Expiration(
      ZonedDateTime date,
      Integer days,
      Boolean expiredObjectDeleteMarker,
      Boolean expiredObjectAllVersions) {
    this(
        date == null ? null : new ResponseDate(date),
        days,
        expiredObjectDeleteMarker,
        expiredObjectAllVersions);
  }

  public Boolean expiredObjectDeleteMarker() {
    return expiredObjectDeleteMarker;
  }

  /** Allow setting ILM rule for removing all versions of expired objects from command line. */
  public Boolean expiredObjectAllVersions() {
    return expiredObjectAllVersions;
  }
}
