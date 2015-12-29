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


  public String nextMarker() {
    return nextMarker;
  }


  public String name() {
    return name;
  }


  public String prefix() {
    return prefix;
  }


  public String marker() {
    return marker;
  }


  public int maxKeys() {
    return maxKeys;
  }


  public String delimiter() {
    return delimiter;
  }


  public boolean isTruncated() {
    return isTruncated;
  }


  public List<Item> contents() {
    return contents;
  }


  /**
   * get common prefixes.
   */
  public List<Prefix> commonPrefixes() {
    if (commonPrefixes == null) {
      return new LinkedList<Prefix>();
    }
    return commonPrefixes;
  }
}
