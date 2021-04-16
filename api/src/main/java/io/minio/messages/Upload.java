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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Helper class to denote Upload information of a multipart upload and used in {@link
 * ListMultipartUploadsResult}.
 */
@Root(name = "Upload", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class Upload {
  @Element(name = "Key")
  private String objectName;

  @Element(name = "UploadId")
  private String uploadId;

  @Element(name = "Initiator")
  private Initiator initiator;

  @Element(name = "Owner")
  private Owner owner;

  @Element(name = "StorageClass")
  private String storageClass;

  @Element(name = "Initiated")
  private ResponseDate initiated;

  private long aggregatedPartSize;
  private String encodingType = null;

  public Upload() {}

  /** Returns object name. */
  public String objectName() {
    try {
      return "url".equals(encodingType)
          ? URLDecoder.decode(objectName, StandardCharsets.UTF_8.name())
          : objectName;
    } catch (UnsupportedEncodingException e) {
      // This never happens as 'enc' name comes from JDK's own StandardCharsets.
      throw new RuntimeException(e);
    }
  }

  /** Returns upload ID. */
  public String uploadId() {
    return uploadId;
  }

  /** Returns initator information. */
  public Initiator initiator() {
    return initiator;
  }

  /** Returns owner information. */
  public Owner owner() {
    return owner;
  }

  /** Returns storage class. */
  public String storageClass() {
    return storageClass;
  }

  /** Returns initated time. */
  public ZonedDateTime initiated() {
    return initiated.zonedDateTime();
  }

  /** Returns aggregated part size. */
  public long aggregatedPartSize() {
    return aggregatedPartSize;
  }

  /** Sets given aggregated part size. */
  public void setAggregatedPartSize(long size) {
    this.aggregatedPartSize = size;
  }

  public void setEncodingType(String encodingType) {
    this.encodingType = encodingType;
  }
}
