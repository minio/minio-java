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

@SuppressWarnings({"SameParameterValue", "unused"})
public class ListBucketResult extends XmlEntity {
  @Key("Name")
  private String name;
  @Key("Prefix")
  private String prefix;
  @Key("Marker")
  private String marker;
  @Key("NextMarker")
  private String nextMarker;
  @Key("MaxKeys")
  private int maxKeys;
  @Key("Delimiter")
  private String delimiter;
  @Key("IsTruncated")
  private boolean isTruncated;
  @Key("Contents")
  private List<Item> contents;
  @Key("CommonPrefixes")
  private List<Prefix> commonPrefixes;

  public ListBucketResult() {
    super.name = "ListBucketResult";
  }

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(String nextMarker) {
    this.nextMarker = nextMarker;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(String marker) {
    this.marker = marker;
  }

  public int getMaxKeys() {
    return maxKeys;
  }

  public void setMaxKeys(int maxKeys) {
    this.maxKeys = maxKeys;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public boolean isTruncated() {
    return isTruncated;
  }

  public void setIsTruncated(boolean isTruncated) {
    this.isTruncated = isTruncated;
  }

  public List<Item> getContents() {
    return contents;
  }

  public void setContents(List<Item> contents) {
    this.contents = contents;
  }

  /**
   * get common prefixes.
   */
  public List<Prefix> getCommonPrefixes() {
    if (commonPrefixes == null) {
      return new LinkedList<Prefix>();
    }
    return commonPrefixes;
  }

  public void setCommonPrefixes(List<Prefix> commonPrefixes) {
    this.commonPrefixes = commonPrefixes;
  }
}
