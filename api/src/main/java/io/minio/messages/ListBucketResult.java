/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015, 2016, 2017 MinIO, Inc.
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public abstract class ListBucketResult {
  @Element(name = "Name")
  private String name;

  @Element(name = "Prefix", required = false)
  private String prefix;

  @Element(name = "StartAfter", required = false)
  private String startAfter;

  @Element(name = "KeyCount", required = false)
  private int keyCount;

  @Element(name = "MaxKeys")
  private int maxKeys;

  @Element(name = "Delimiter", required = false)
  private String delimiter;

  @Element(name = "IsTruncated", required = false)
  private boolean isTruncated;

  @ElementList(name = "Contents", inline = true, required = false)
  private List<Item> contents;

  @ElementList(name = "CommonPrefixes", inline = true, required = false)
  private List<Prefix> commonPrefixes;

  public ListBucketResult() {}

  /** Returns bucket name. */
  public String name() {
    return name;
  }

  /** Returns prefix. */
  public String prefix() {
    return prefix;
  }

  /** Returns start after. */
  public String startAfter() {
    return startAfter;
  }

  /** Returns key count. */
  public int keyCount() {
    return keyCount;
  }

  /** Returns max keys. */
  public int maxKeys() {
    return maxKeys;
  }

  /** Returns delimiter. */
  public String delimiter() {
    return delimiter;
  }

  /** Returns whether the result is truncated or not. */
  public boolean isTruncated() {
    return isTruncated;
  }

  /** Returns List of Items. */
  public List<Item> contents() {
    if (contents == null) {
      return Collections.unmodifiableList(new LinkedList<>());
    }

    return Collections.unmodifiableList(contents);
  }

  /** Returns List of Prefix. */
  public List<Prefix> commonPrefixes() {
    if (commonPrefixes == null) {
      return Collections.unmodifiableList(new LinkedList<>());
    }

    return Collections.unmodifiableList(commonPrefixes);
  }

  /** Returns continuation token. */
  public abstract String continuationToken();

  public abstract String nextContinuationToken();
}
