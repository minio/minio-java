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
  private String bucket;
  @Key("Key")
  private String key;
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
  private List<Part> parts;

  public ListPartsResult() {
    super();
    this.name = "ListPartsResult";
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public Initiator getInitiator() {
    return initiator;
  }

  public void setInitiator(Initiator initiator) {
    this.initiator = initiator;
  }

  public Owner getOwner() {
    return owner;
  }

  public void setOwner(Owner owner) {
    this.owner = owner;
  }

  public int getMaxParts() {
    return maxParts;
  }

  public void setMaxParts(int maxParts) {
    this.maxParts = maxParts;
  }

  public boolean isTruncated() {
    return isTruncated;
  }

  public void setIsTruncated(boolean isTruncated) {
    this.isTruncated = isTruncated;
  }

  public int getPartNumberMarker() {
    return partNumberMarker;
  }

  public void setPartNumberMarker(int partNumberMarker) {
    this.partNumberMarker = partNumberMarker;
  }

  public int getNextPartNumberMarker() {
    return nextPartNumberMarker;
  }

  public void setNextPartNumberMarker(int nextPartNumberMarker) {
    this.nextPartNumberMarker = nextPartNumberMarker;
  }

  /**
   * get parts.
   */
  public List<Part> getParts() {
    if (parts == null) {
      return new LinkedList<Part>();
    }
    return parts;
  }

  public void setParts(List<Part> parts) {
    this.parts = parts;
  }

}
