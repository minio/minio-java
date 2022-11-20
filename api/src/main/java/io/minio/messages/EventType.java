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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/** Amazon AWS S3 event types for notifications. */
@Root(name = "Event")
@Convert(EventType.EventTypeConverter.class)
public enum EventType {
  OBJECT_CREATED_ANY("s3:ObjectCreated:*"),
  OBJECT_CREATED_PUT("s3:ObjectCreated:Put"),
  OBJECT_CREATED_POST("s3:ObjectCreated:Post"),
  OBJECT_CREATED_COPY("s3:ObjectCreated:Copy"),
  OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD("s3:ObjectCreated:CompleteMultipartUpload"),
  OBJECT_ACCESSED_GET("s3:ObjectAccessed:Get"),
  OBJECT_ACCESSED_HEAD("s3:ObjectAccessed:Head"),
  OBJECT_ACCESSED_ANY("s3:ObjectAccessed:*"),
  OBJECT_REMOVED_ANY("s3:ObjectRemoved:*"),
  OBJECT_REMOVED_DELETE("s3:ObjectRemoved:Delete"),
  OBJECT_REMOVED_DELETED_MARKER_CREATED("s3:ObjectRemoved:DeleteMarkerCreated"),
  REDUCED_REDUNDANCY_LOST_OBJECT("s3:ReducedRedundancyLostObject"),
  BUCKET_CREATED("s3:BucketCreated"),
  BUCKET_REMOVED("s3:BucketRemoved");

  private final String value;

  private EventType(String value) {
    this.value = value;
  }

  public String toString() {
    return this.value;
  }

  /** Returns EventType of given string. */
  @JsonCreator
  public static EventType fromString(String eventTypeString) {
    String s3EventTypeString = "s3:" + eventTypeString;
    for (EventType et : EventType.values()) {
      if (eventTypeString.equals(et.value) || s3EventTypeString.equals(et.value)) {
        return et;
      }
    }

    throw new IllegalArgumentException("unknown event '" + eventTypeString + "'");
  }

  /** XML converter class. */
  public static class EventTypeConverter implements Converter<EventType> {
    @Override
    public EventType read(InputNode node) throws Exception {
      return EventType.fromString(node.getValue());
    }

    @Override
    public void write(OutputNode node, EventType eventType) throws Exception {
      node.setValue(eventType.toString());
    }
  }
}
