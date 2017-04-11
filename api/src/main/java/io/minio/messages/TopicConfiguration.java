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

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;

import io.minio.errors.InvalidArgumentException;


/**
 * Helper class to parse Amazon AWS S3 response XML containing topic configuration.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class TopicConfiguration extends XmlEntity {
  @Key("Id")
  private String id;
  @Key("Topic")
  private String topic;
  @Key("Event")
  private List<String> events = new LinkedList<>();
  @Key("Filter")
  private Filter filter;


  public TopicConfiguration() throws XmlPullParserException {
    super();
    super.name = "TopicConfiguration";
  }


  /**
   * Sets ID. This is used only in functional test.
   */
  public void setId(String id) {
    this.id = id;
  }


  /**
   * Returns topic.
   */
  public String topic() {
    return topic;
  }


  /**
   * Sets topic.
   */
  public void setTopic(String topic) {
    this.topic = topic;
  }


  /**
   * Returns events.
   */
  public List<EventType> events() throws InvalidArgumentException {
    return EventType.fromStringList(events);
  }


  /**
   * Sets event.
   */
  public void setEvents(List<EventType> events) {
    this.events = EventType.toStringList(events);
  }


  /**
   * Returns filter.
   */
  public Filter filter() {
    return filter;
  }


  /**
   * Sets filter.
   */
  public void setFilter(Filter filter) {
    this.filter = filter;
  }
}
