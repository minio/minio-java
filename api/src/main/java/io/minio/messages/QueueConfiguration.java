/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
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

/** Helper class to denote Queue configuration of {@link NotificationConfiguration}. */
@Root(name = "QueueConfiguration", strict = false)
public class QueueConfiguration extends NotificationCommonConfiguration {
  @Element(name = "Queue")
  private String queue;

  public QueueConfiguration() {
    super();
  }

  /** Returns queue. */
  public String queue() {
    return queue;
  }

  /** Sets queue. */
  public void setQueue(String queue) {
    this.queue = queue;
  }
}
