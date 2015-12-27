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

import java.util.LinkedList;
import java.util.List;


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


  public ListPartsResult() {
    super();
    this.name = "ListPartsResult";
  }


  public String bucketName() {
    return bucketName;
  }


  public String objectName() {
    return objectName;
  }


  public String storageClass() {
    return storageClass;
  }


  public Initiator initiator() {
    return initiator;
  }


  public Owner owner() {
    return owner;
  }


  public int maxParts() {
    return maxParts;
  }


  public boolean isTruncated() {
    return isTruncated;
  }


  public int partNumberMarker() {
    return partNumberMarker;
  }


  public int nextPartNumberMarker() {
    return nextPartNumberMarker;
  }


  /**
   * get part list.
   */
  public List<Part> partList() {
    if (partList == null) {
      return new LinkedList<Part>();
    }

    return partList;
  }
}
