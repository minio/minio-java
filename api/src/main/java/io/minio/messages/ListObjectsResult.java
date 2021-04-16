/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

import com.google.common.base.MoreObjects;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Base class of {@link ListBucketResultV1}, {@link ListBucketResultV2} and {@link
 * ListVersionsResult}.
 */
public abstract class ListObjectsResult {
  @Element(name = "Name")
  private String name;

  @Element(name = "EncodingType", required = false)
  private String encodingType;

  @Element(name = "Prefix", required = false)
  private String prefix;

  @Element(name = "Delimiter", required = false)
  private String delimiter;

  @Element(name = "IsTruncated", required = false)
  private boolean isTruncated;

  @Element(name = "MaxKeys", required = false)
  private int maxKeys;

  @ElementList(name = "CommonPrefixes", inline = true, required = false)
  private List<Prefix> commonPrefixes;

  private static final List<DeleteMarker> deleteMarkers =
      Collections.unmodifiableList(new LinkedList<>());

  public ListObjectsResult() {}

  protected String decodeIfNeeded(String value) {
    try {
      return (value != null && "url".equals(encodingType))
          ? URLDecoder.decode(value, StandardCharsets.UTF_8.name())
          : value;
    } catch (UnsupportedEncodingException e) {
      // This never happens as 'enc' name comes from JDK's own StandardCharsets.
      throw new RuntimeException(e);
    }
  }

  /** Returns bucket name. */
  public String name() {
    return name;
  }

  public String encodingType() {
    return encodingType;
  }

  /** Returns prefix. */
  public String prefix() {
    return decodeIfNeeded(prefix);
  }

  /** Returns delimiter. */
  public String delimiter() {
    return delimiter;
  }

  /** Returns whether the result is truncated or not. */
  public boolean isTruncated() {
    return isTruncated;
  }

  /** Returns max keys. */
  public int maxKeys() {
    return maxKeys;
  }

  /** Returns List of Prefix. */
  public List<Prefix> commonPrefixes() {
    return Collections.unmodifiableList(
        (commonPrefixes == null) ? new LinkedList<>() : commonPrefixes);
  }

  public List<DeleteMarker> deleteMarkers() {
    return deleteMarkers;
  }

  protected <T extends Item> List<T> emptyIfNull(List<T> lst) {
    return Collections.unmodifiableList(MoreObjects.firstNonNull(lst, new LinkedList<T>()));
  }

  public abstract List<? extends Item> contents();
}
