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

import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;


/**
 * Helper class to parse Amazon AWS S3 response XML containing initiate multipart upload result.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class InitiateMultipartUploadResult extends XmlEntity {
  @Key("Bucket")
  private String bucketName;
  @Key("Key")
  private String objectName;
  @Key("UploadId")
  private String uploadId;


  public InitiateMultipartUploadResult() throws XmlPullParserException {
    super();
    this.name = "InitiateMultipartUploadResult";
  }


  /**
   * Returns bucket name.
   */
  public String bucketName() {
    return bucketName;
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
}
