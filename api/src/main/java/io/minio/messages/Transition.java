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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote transition information for {@link LifecycleRule}. */
@Root(name = "Transition")
public class Transition extends DateDays {
  @Element(name = "StorageClass")
  private String storageClass;

  public Transition(
      @Nullable @Element(name = "Date", required = false) ResponseDate date,
      @Nullable @Element(name = "Days", required = false) Integer days,
      @Nonnull @Element(name = "StorageClass", required = false) String storageClass) {
    if (date != null ^ days != null) {
      this.date = date;
      this.days = days;
    } else {
      throw new IllegalArgumentException("Only one of date or days must be set");
    }
    if (storageClass == null || storageClass.isEmpty()) {
      throw new IllegalArgumentException("StorageClass must be provided");
    }
    this.storageClass = storageClass;
  }

  public Transition(ZonedDateTime date, Integer days, String storageClass) {
    this(date == null ? null : new ResponseDate(date), days, storageClass);
  }

  public String storageClass() {
    return storageClass;
  }
}
