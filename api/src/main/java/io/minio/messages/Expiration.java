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

  public Expiration(
      @Nullable @Element(name = "Date", required = false) ResponseDate date,
      @Nullable @Element(name = "Days", required = false) Integer days,
      @Nullable @Element(name = "ExpiredObjectDeleteMarker", required = false)
          Boolean expiredObjectDeleteMarker) {
    super(date, days);
    this.expiredObjectDeleteMarker = expiredObjectDeleteMarker;
  }

  public Expiration(ZonedDateTime date, Integer days, Boolean expiredObjectDeleteMarker) {
    this(date == null ? null : new ResponseDate(date), days, expiredObjectDeleteMarker);
  }

  public Boolean expiredObjectDeleteMarker() {
    return expiredObjectDeleteMarker;
  }
}
