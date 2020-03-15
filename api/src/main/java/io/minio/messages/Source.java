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

/**
 * Helper class to denote client information causes this event. This is MinIO extension to <a
 * href="http://docs.aws.amazon.com/AmazonS3/latest/dev/notification-content-structure.html">Event
 * Message Structure</a>
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "UwF",
    justification = "Everything in this class is initialized by JSON unmarshalling.")
public class Source {
  @JsonProperty private String host;
  @JsonProperty private String port;
  @JsonProperty private String userAgent;

  public String host() {
    return host;
  }

  public String port() {
    return port;
  }

  public String userAgent() {
    return userAgent;
  }
}
