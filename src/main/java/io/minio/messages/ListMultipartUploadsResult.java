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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ListMultipartUploadsResult extends XmlEntity {
  @Key("Upload")
  List<Upload> uploads;
  @Key("Bucket")
  private String bucket;
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

  public ListMultipartUploadsResult() {
    super();
    super.name = "ListMultipartUploadsResult";
  }

  public boolean isTruncated() {
    return isTruncated;
  }

  public void setIsTruncated(boolean isTruncated) {
    this.isTruncated = isTruncated;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getKeyMarker() {
    return keyMarker;
  }

  public void setKeyMarker(String keyMarker) {
    this.keyMarker = keyMarker;
  }

  public String getUploadIdMarker() {
    return uploadIdMarker;
  }

  public void setUploadIdMarker(String uploadIdMarker) {
    this.uploadIdMarker = uploadIdMarker;
  }

  public String getNextKeyMarker() {
    return nextKeyMarker;
  }

  public void setNextKeyMarker(String nextKeyMarker) {
    this.nextKeyMarker = nextKeyMarker;
  }

  public String getNextUploadIdMarker() {
    return nextUploadIdMarker;
  }

  public void setNextUploadIdMarker(String nextUploadIdMarker) {
    this.nextUploadIdMarker = nextUploadIdMarker;
  }

  public int getMaxUploads() {
    return maxUploads;
  }

  public void setMaxUploads(int maxUploads) {
    this.maxUploads = maxUploads;
  }

  /**
   * get uploads.
   */
  public List<Upload> getUploads() {
    if (uploads == null) {
      return new ArrayList<Upload>();
    }
    return uploads;
  }

  public void setUploads(List<Upload> uploads) {
    this.uploads = uploads;
  }
}
