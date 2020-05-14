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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of request XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketNotificationConfiguration.html">PutBucketNotificationConfiguration
 * API</a> and response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketNotificationConfiguration.html">GetBucketNotificationConfiguration
 * API</a>.
 */
@Root(name = "NotificationConfiguration", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class NotificationConfiguration {
  @ElementList(name = "CloudFunctionConfiguration", inline = true, required = false)
  private List<CloudFunctionConfiguration> cloudFunctionConfigurationList;

  @ElementList(name = "QueueConfiguration", inline = true, required = false)
  private List<QueueConfiguration> queueConfigurationList;

  @ElementList(name = "TopicConfiguration", inline = true, required = false)
  private List<TopicConfiguration> topicConfigurationList;

  public NotificationConfiguration() {}

  /** Returns cloud function configuration. */
  public List<CloudFunctionConfiguration> cloudFunctionConfigurationList() {
    return Collections.unmodifiableList(
        cloudFunctionConfigurationList == null
            ? new LinkedList<>()
            : cloudFunctionConfigurationList);
  }

  /** Sets cloud function configuration list. */
  public void setCloudFunctionConfigurationList(
      List<CloudFunctionConfiguration> cloudFunctionConfigurationList) {
    this.cloudFunctionConfigurationList =
        Collections.unmodifiableList(cloudFunctionConfigurationList);
  }

  /** Returns queue configuration list. */
  public List<QueueConfiguration> queueConfigurationList() {
    return Collections.unmodifiableList(
        queueConfigurationList == null ? new LinkedList<>() : queueConfigurationList);
  }

  /** Sets queue configuration list. */
  public void setQueueConfigurationList(List<QueueConfiguration> queueConfigurationList) {
    this.queueConfigurationList = Collections.unmodifiableList(queueConfigurationList);
  }

  /** Returns topic configuration list. */
  public List<TopicConfiguration> topicConfigurationList() {
    return Collections.unmodifiableList(
        topicConfigurationList == null ? new LinkedList<>() : topicConfigurationList);
  }

  /** Sets topic configuration list. */
  public void setTopicConfigurationList(List<TopicConfiguration> topicConfigurationList) {
    this.topicConfigurationList = Collections.unmodifiableList(topicConfigurationList);
  }
}
