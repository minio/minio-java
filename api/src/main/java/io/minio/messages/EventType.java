/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 Minio, Inc.
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

import java.util.LinkedList;
import java.util.List;

import io.minio.errors.InvalidArgumentException;


/**
 * Amazon AWS S3 event types for notifications.
 */
public enum EventType {
  OBJECT_CREATED_ANY("s3:ObjectCreated:*"),
  OBJECT_CREATED_PUT("s3:ObjectCreated:Put"),
  OBJECT_CREATED_POST("s3:ObjectCreated:Post"),
  OBJECT_CREATED_COPY("s3:ObjectCreated:Copy"),
  OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD("s3:ObjectCreated:CompleteMultipartUpload"),
  OBJECT_REMOVED_ANY("s3:ObjectRemoved:*"),
  OBJECT_REMOVED_DELETE("s3:ObjectRemoved:Delete"),
  OBJECT_REMOVED_DELETED_MARKER_CREATED("s3:ObjectRemoved:DeleteMarkerCreated"),
  REDUCED_REDUNDANCY_LOST_OBJECT("s3:ReducedRedundancyLostObject");

  private final String value;


  private EventType(String value) {
    this.value = value;
  }


  public String toString() {
    return this.value;
  }


  /**
   * Returns EventType of given string.
   */
  public static EventType fromString(String eventTypeString) throws InvalidArgumentException {
    for (EventType et : EventType.values()) {
      if (eventTypeString.equals(et.value)) {
        return et;
      }
    }


    throw new InvalidArgumentException("unknown event '" + eventTypeString + "'");
  }


  /**
   * Returns List&lt;EventType&gt; of given List&lt;String&gt;.
   */
  public static List<EventType> fromStringList(List<String> eventList) throws InvalidArgumentException {
    List<EventType> eventTypeList = new LinkedList<EventType>();
    for (String event: eventList) {
      eventTypeList.add(EventType.fromString(event));
    }

    return eventTypeList;
  }


  /**
   * Returns List&lt;String&gt; of given List&lt;EventType&gt;.
   */
  public static List<String> toStringList(List<EventType> eventTypeList) {
    List<String> events = new LinkedList<String>();
    for (EventType eventType: eventTypeList) {
      events.add(eventType.toString());
    }

    return events;
  }
}
