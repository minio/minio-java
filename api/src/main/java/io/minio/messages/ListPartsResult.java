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

import java.util.LinkedList;
import java.util.List;


/**
 * Helper class to parse Amazon AWS S3 response XML containing ListPartsResult information.
 */
@SuppressWarnings("unused")
public class ListPartsResult extends XmlEntity {
  @Key("Bucket")
  private String bucketName;
  @Key("Key")
  private String objectName;
  @Key("Initiator")
  private Initiator initiator;
  @Key("Owner")
  private Owner owner;
  @Key("StorageClass")
  private String storageClass;
  @Key("PartNumberMarker")
  private int partNumberMarker;
  @Key("NextPartNumberMarker")
  private int nextPartNumberMarker;
  @Key("MaxParts")
  private int maxParts;
  @Key("IsTruncated")
  private boolean isTruncated;
  @Key("Part")
  private List<Part> partList;


  public ListPartsResult() throws XmlPullParserException {
    super();
    this.name = "ListPartsResult";
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
   * Returns storage class.
   */
  public String storageClass() {
    return storageClass;
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
   * Returns maximum parts information received.
   */
  public int maxParts() {
    return maxParts;
  }


  /**
   * Returns whether the result is truncated or not.
   */
  public boolean isTruncated() {
    return isTruncated;
  }


  /**
   * Returns part number marker.
   */
  public int partNumberMarker() {
    return partNumberMarker;
  }


  /**
   * Returns next part number marker.
   */
  public int nextPartNumberMarker() {
    return nextPartNumberMarker;
  }


  /**
   * Returns List of Part.
   */
  public List<Part> partList() {
    if (partList == null) {
      return new LinkedList<>();
    }

    return partList;
  }
}
