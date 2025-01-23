/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

/** Status type of {@link LifecycleConfiguration.Rule} and {@link ReplicationConfiguration.Rule}. */
@Root(name = "Status")
@Convert(Status.StatusConverter.class)
public enum Status {
  DISABLED("Disabled"),
  ENABLED("Enabled");

  private final String value;

  private Status(String value) {
    this.value = value;
  }

  public String toString() {
    return this.value;
  }

  /** Returns Status of given string. */
  @JsonCreator
  public static Status fromString(String statusString) {
    for (Status status : Status.values()) {
      if (statusString.equals(status.value)) {
        return status;
      }
    }

    throw new IllegalArgumentException("Unknown status '" + statusString + "'");
  }

  /** XML converter of {@link Status}. */
  public static class StatusConverter implements Converter<Status> {
    @Override
    public Status read(InputNode node) throws Exception {
      return Status.fromString(node.getValue());
    }

    @Override
    public void write(OutputNode node, Status status) throws Exception {
      node.setValue(status.toString());
    }
  }
}
