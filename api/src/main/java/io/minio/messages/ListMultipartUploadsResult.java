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

import io.minio.Utils;
import java.util.List;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListMultipartUploads.html">ListMultipartUploads
 * API</a>.
 */
@Root(name = "ListMultipartUploadsResult", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class ListMultipartUploadsResult {
  @Element(name = "Bucket")
  private String bucketName;

  @Element(name = "EncodingType", required = false)
  private String encodingType;

  @Element(name = "KeyMarker", required = false)
  private String keyMarker;

  @Element(name = "UploadIdMarker", required = false)
  private String uploadIdMarker;

  @Element(name = "NextKeyMarker", required = false)
  private String nextKeyMarker;

  @Element(name = "NextUploadIdMarker", required = false)
  private String nextUploadIdMarker;

  @Element(name = "MaxUploads")
  private int maxUploads;

  @Element(name = "IsTruncated", required = false)
  private boolean isTruncated;

  @ElementList(name = "Upload", inline = true, required = false)
  List<Upload> uploads;

  public ListMultipartUploadsResult() {}

  /** Returns whether the result is truncated or not. */
  public boolean isTruncated() {
    return isTruncated;
  }

  /** Returns bucket name. */
  public String bucketName() {
    return bucketName;
  }

  /** Returns key marker. */
  public String keyMarker() {
    return Utils.urlDecode(keyMarker, encodingType);
  }

  /** Returns upload ID marker. */
  public String uploadIdMarker() {
    return uploadIdMarker;
  }

  /** Returns next key marker. */
  public String nextKeyMarker() {
    return Utils.urlDecode(nextKeyMarker, encodingType);
  }

  /** Returns next upload ID marker. */
  public String nextUploadIdMarker() {
    return nextUploadIdMarker;
  }

  /** Returns max uploads received. */
  public int maxUploads() {
    return maxUploads;
  }

  public String encodingType() {
    return encodingType;
  }

  /** Returns List of Upload. */
  public List<Upload> uploads() {
    return Utils.unmodifiableList(uploads);
  }
}
