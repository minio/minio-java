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
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/** Helper class to denote Stats information of S3 select response message. */
@Root(name = "Stats", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class Stats {
  @Element(name = "BytesScanned")
  private long bytesScanned = -1;

  @Element(name = "BytesProcessed")
  private long bytesProcessed = -1;

  @Element(name = "BytesReturned")
  private long bytesReturned = -1;

  /** Constructs a new Stats object. */
  public Stats() {}

  /** Returns bytes scanned. */
  public long bytesScanned() {
    return this.bytesScanned;
  }

  /** Returns bytes processed. */
  public long bytesProcessed() {
    return this.bytesProcessed;
  }

  /** Returns bytes returned. */
  public long bytesReturned() {
    return this.bytesReturned;
  }
}
