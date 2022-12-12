/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2018 MinIO, Inc.
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
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Helper class to denote single event record for {@link NotificationRecords}. */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "UuF",
    justification = "eventVersion and eventSource are available for completeness")
public class Event {
  @JsonProperty private String eventVersion;
  @JsonProperty private String eventSource;
  @JsonProperty private String awsRegion;
  @JsonProperty private EventType eventName;
  @JsonProperty private Identity userIdentity;
  @JsonProperty private Map<String, String> requestParameters;
  @JsonProperty private Map<String, String> responseElements;
  @JsonProperty private EventMetadata s3;
  @JsonProperty private Source source;
  @JsonProperty private ResponseDate eventTime;

  public String region() {
    return awsRegion;
  }

  public ZonedDateTime eventTime() {
    return eventTime.zonedDateTime();
  }

  public EventType eventType() {
    return eventName;
  }

  public String userId() {
    if (userIdentity == null) {
      return null;
    }

    return userIdentity.principalId();
  }

  public Map<String, String> requestParameters() {
    return Collections.unmodifiableMap(
        requestParameters == null ? new HashMap<>() : requestParameters);
  }

  public Map<String, String> responseElements() {
    return Collections.unmodifiableMap(
        responseElements == null ? new HashMap<>() : responseElements);
  }

  public String bucketName() {
    if (s3 == null) {
      return null;
    }

    return s3.bucketName();
  }

  public String bucketOwner() {
    if (s3 == null) {
      return null;
    }

    return s3.bucketOwner();
  }

  public String bucketArn() {
    if (s3 == null) {
      return null;
    }

    return s3.bucketArn();
  }

  public String objectName() {
    if (s3 == null) {
      return null;
    }

    return s3.objectName();
  }

  public long objectSize() {
    if (s3 == null) {
      return -1;
    }

    return s3.objectSize();
  }

  public String etag() {
    if (s3 == null) {
      return null;
    }

    return s3.etag();
  }

  public String objectVersionId() {
    if (s3 == null) {
      return null;
    }

    return s3.objectVersionId();
  }

  public String sequencer() {
    if (s3 == null) {
      return null;
    }

    return s3.sequencer();
  }

  public Map<String, String> userMetadata() {
    if (s3 == null) {
      return null;
    }

    return s3.userMetadata();
  }

  public String host() {
    if (source == null) {
      return null;
    }

    return source.host();
  }

  public String port() {
    if (source == null) {
      return null;
    }

    return source.port();
  }

  public String userAgent() {
    if (source == null) {
      return null;
    }

    return source.userAgent();
  }
}
