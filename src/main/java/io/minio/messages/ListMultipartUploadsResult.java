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

import java.util.ArrayList;
import java.util.List;


/**
 * Helper class to parse Amazon AWS S3 response XML containing ListMultipartUploadResult information.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ListMultipartUploadsResult extends XmlEntity {
  @Key("Upload")
  List<Upload> uploads;
  @Key("Bucket")
  private String bucketName;
  @Key("KeyMarker")
  private String keyMarker;
  @Key("UploadIdMarker")
  private String uploadIdMarker;
  @Key("NextKeyMarker")
  private String nextKeyMarker;
  @Key("NextUploadIdMarker")
  private String nextUploadIdMarker;
  @Key("MaxUploads")
  private int maxUploads;
  @Key("IsTruncated")
  private boolean isTruncated;


  public ListMultipartUploadsResult() throws XmlPullParserException {
    super();
    super.name = "ListMultipartUploadsResult";
  }


  /**
   * Returns whether the result is truncated or not.
   */
  public boolean isTruncated() {
    return isTruncated;
  }


  /**
   * Returns bucket name.
   */
  public String bucketName() {
    return bucketName;
  }


  /**
   * Returns key marker.
   */
  public String keyMarker() {
    return keyMarker;
  }


  /**
   * Returns upload ID marker.
   */
  public String uploadIdMarker() {
    return uploadIdMarker;
  }


  /**
   * Returns next key marker.
   */
  public String nextKeyMarker() {
    return nextKeyMarker;
  }


  /**
   * Returns next upload ID marker.
   */
  public String nextUploadIdMarker() {
    return nextUploadIdMarker;
  }


  /**
   * Returns max uploads received.
   */
  public int maxUploads() {
    return maxUploads;
  }


  /**
   * Returns List of Upload.
   */
  public List<Upload> uploads() {
    if (uploads == null) {
      return new ArrayList<>();
    }
    return uploads;
  }
}
