/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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
import io.minio.Time;
import java.time.ZonedDateTime;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/** S3 specified response time wrapping {@link ZonedDateTime}. */
@Root
@Convert(ResponseDate.ResponseDateConverter.class)
public class ResponseDate {
  private ZonedDateTime zonedDateTime;

  public ResponseDate() {}

  public ResponseDate(ZonedDateTime zonedDateTime) {
    this.zonedDateTime = zonedDateTime;
  }

  public ZonedDateTime zonedDateTime() {
    return zonedDateTime;
  }

  public String toString() {
    return zonedDateTime.format(Time.RESPONSE_DATE_FORMAT);
  }

  @JsonCreator
  public static ResponseDate fromString(String responseDateString) {
    return new ResponseDate(ZonedDateTime.parse(responseDateString));
  }

  /** XML converter class. */
  public static class ResponseDateConverter implements Converter<ResponseDate> {
    @Override
    public ResponseDate read(InputNode node) throws Exception {
      return ResponseDate.fromString(node.getValue());
    }

    @Override
    public void write(OutputNode node, ResponseDate amzDate) {
      node.setValue(amzDate.toString());
    }
  }
}
