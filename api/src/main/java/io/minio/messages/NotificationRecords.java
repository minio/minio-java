/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2020 MinIO, Inc.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Object representation of JSON format of <a
 * href="http://docs.aws.amazon.com/AmazonS3/latest/dev/notification-content-structure.html">Event
 * Message Structure</a>.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "UwF",
    justification = "Everything in this class is initialized by JSON unmarshalling.")
public class NotificationRecords {
  @JsonProperty("Records")
  private List<Event> events;

  public List<Event> events() {
    return Collections.unmodifiableList(events == null ? new LinkedList<>() : events);
  }
}
