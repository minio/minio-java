/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote bucket information for {@link ListAllMyBucketsResult}. */
@Root(name = "Bucket", strict = false)
public class Bucket {
  @Element(name = "Name")
  private String name;

  @Element(name = "CreationDate")
  private ResponseDate creationDate;

  public Bucket() {}

  /** Returns bucket name. */
  public String name() {
    return name;
  }

  /** Returns creation date. */
  public ZonedDateTime creationDate() {
    return creationDate.zonedDateTime();
  }
}
