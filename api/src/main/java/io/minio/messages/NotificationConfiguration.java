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


/**
 * Helper class to parse Amazon AWS S3 response XML containing notification configuration.
 */
public class NotificationConfiguration extends XmlEntity {
  @Key("CloudFunctionConfiguration")
  private List<CloudFunctionConfiguration> cloudFunctionConfigurationList = new LinkedList<>();
  @Key("QueueConfiguration")
  private List<QueueConfiguration> queueConfigurationList = new LinkedList<>();
  @Key("TopicConfiguration")
  private List<TopicConfiguration> topicConfigurationList = new LinkedList<>();

  /**
   * Constucts a new notification configuration with default namespace.
   */
  public NotificationConfiguration() throws XmlPullParserException {
    super();
    super.name = "NotificationConfiguration";
    super.namespaceDictionary.set("", "http://s3.amazonaws.com/doc/2006-03-01/");
  }


  /**
   * Returns cloud function configuration.
   */
  public List<CloudFunctionConfiguration> cloudFunctionConfigurationList() {
    return cloudFunctionConfigurationList;
  }


  /**
   * Sets cloud function configuration list.
   */
  public void setCloudFunctionConfigurationList(List<CloudFunctionConfiguration> cloudFunctionConfigurationList) {
    this.cloudFunctionConfigurationList = cloudFunctionConfigurationList;
  }


  /**
   * Returns queue configuration list.
   */
  public List<QueueConfiguration> queueConfigurationList() {
    return queueConfigurationList;
  }


  /**
   * Sets queue configuration list.
   */
  public void setQueueConfigurationList(List<QueueConfiguration> queueConfigurationList) {
    this.queueConfigurationList = queueConfigurationList;
  }


  /**
   * Returns topic configuration list.
   */
  public List<TopicConfiguration> topicConfigurationList() {
    return topicConfigurationList;
  }


  /**
   * Sets topic configuration list.
   */
  public void setTopicConfigurationList(List<TopicConfiguration> topicConfigurationList) {
    this.topicConfigurationList = topicConfigurationList;
  }
}
