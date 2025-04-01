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

import io.minio.Time;
import java.time.ZonedDateTime;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/** Helper class to denote Object information for {@link DeleteRequest}. */
@Root(name = "Object")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class DeleteObject {
  @Element(name = "Key")
  private String name;

  @Element(name = "VersionId", required = false)
  private String versionId;

  @Element(name = "ETag", required = false)
  private String etag;

  @Element(name = "LastModifiedTime", required = false)
  private HttpHeaderDate lastModifiedTime;

  @Element(name = "Size", required = false)
  private Long size;

  public DeleteObject(String name) {
    this.name = name;
  }

  public DeleteObject(String name, String versionId) {
    this(name);
    this.versionId = versionId;
  }

  public DeleteObject(
      String name, String versionId, String etag, ZonedDateTime lastModifiedTime, Long size) {
    this(name, versionId);
    this.etag = etag;
    this.lastModifiedTime = lastModifiedTime == null ? null : new HttpHeaderDate(lastModifiedTime);
    this.size = size;
  }

  /** HTTP header date wrapping {@link ZonedDateTime}. */
  @Root
  @Convert(HttpHeaderDate.HttpHeaderDateConverter.class)
  public static class HttpHeaderDate {
    private ZonedDateTime zonedDateTime;

    public HttpHeaderDate(ZonedDateTime zonedDateTime) {
      this.zonedDateTime = zonedDateTime;
    }

    public String toString() {
      return zonedDateTime.format(Time.HTTP_HEADER_DATE_FORMAT);
    }

    public static HttpHeaderDate fromString(String dateString) {
      return new HttpHeaderDate(ZonedDateTime.parse(dateString, Time.HTTP_HEADER_DATE_FORMAT));
    }

    /** XML converter class. */
    public static class HttpHeaderDateConverter implements Converter<HttpHeaderDate> {
      @Override
      public HttpHeaderDate read(InputNode node) throws Exception {
        return HttpHeaderDate.fromString(node.getValue());
      }

      @Override
      public void write(OutputNode node, HttpHeaderDate date) {
        node.setValue(date.toString());
      }
    }
  }
}
