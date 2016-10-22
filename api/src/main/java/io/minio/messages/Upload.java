/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

import java.util.Date;
import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;
import io.minio.DateFormat;


/**
 * Helper class to parse Amazon AWS S3 response XML containing Upload information.
 */
@SuppressWarnings("unused")
public class Upload extends XmlEntity {
  @Key("Key")
  private String objectName;
  @Key("UploadId")
  private String uploadId;
  @Key("Initiator")
  private Initiator initiator;
  @Key("Owner")
  private Owner owner;
  @Key("StorageClass")
  private String storageClass;
  @Key("Initiated")
  private String initiated;
  private long aggregatedPartSize;


  public Upload() throws XmlPullParserException {
    super();
    super.name = "Upload";
  }


  /**
   * Returns object name.
   */
  public String objectName() {
    return objectName;
  }


  /**
   * Returns upload ID.
   */
  public String uploadId() {
    return uploadId;
  }


  /**
   * Returns initator information.
   */
  public Initiator initiator() {
    return initiator;
  }


  /**
   * Returns owner information.
   */
  public Owner owner() {
    return owner;
  }


  /**
   * Returns storage class.
   */
  public String storageClass() {
    return storageClass;
  }


  /**
   * Returns initated time.
   */
  public Date initiated() {
    return DateFormat.RESPONSE_DATE_FORMAT.parseDateTime(initiated).toDate();
  }


  /**
   * Returns aggregated part size.
   */
  public long aggregatedPartSize() {
    return aggregatedPartSize;
  }


  /**
   * Sets given aggregated part size.
   */
  public void setAggregatedPartSize(long size) {
    this.aggregatedPartSize = size;
  }
}
