/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2019 MinIO, Inc.
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

package io.minio;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Source information to compose object. */
public class ComposeSource {
  private String bucketName;
  private String objectName;
  private Long offset;
  private Long length;
  private Map<String, String> headerMap;
  private CopyConditions copyConditions;
  private ServerSideEncryptionCustomerKey ssec;
  private long objectSize;
  private Map<String, String> headers;

  /** Create new ComposeSource for given bucket and object. */
  public ComposeSource(String bucketName, String objectName) throws IllegalArgumentException {
    this(bucketName, objectName, null, null, null, null, null);
  }

  /** Create new ComposeSource for given bucket, object, offset and length. */
  public ComposeSource(String bucketName, String objectName, Long offset, Long length)
      throws IllegalArgumentException {
    this(bucketName, objectName, offset, length, null, null, null);
  }

  /** Create new ComposeSource for given bucket, object, offset, length and headerMap. */
  public ComposeSource(
      String bucketName, String objectName, Long offset, Long length, Map<String, String> headerMap)
      throws IllegalArgumentException {
    this(bucketName, objectName, offset, length, headerMap, null, null);
  }

  /**
   * Create new ComposeSource for given bucket, object, offset, length, headerMap and
   * CopyConditions.
   */
  public ComposeSource(
      String bucketName,
      String objectName,
      Long offset,
      Long length,
      Map<String, String> headerMap,
      CopyConditions copyConditions)
      throws IllegalArgumentException {
    this(bucketName, objectName, offset, length, headerMap, copyConditions, null);
  }

  /**
   * Creates new ComposeSource for given bucket, object, offset, length, headerMap, CopyConditions
   * and server side encryption.
   *
   * @throws IllegalArgumentException upon invalid value is passed to a method.
   */
  public ComposeSource(
      String bucketName,
      String objectName,
      Long offset,
      Long length,
      Map<String, String> headerMap,
      CopyConditions copyConditions,
      ServerSideEncryptionCustomerKey ssec)
      throws IllegalArgumentException {
    if (bucketName == null) {
      throw new IllegalArgumentException("Source bucket name cannot be empty");
    }

    if (objectName == null) {
      throw new IllegalArgumentException("Source object name cannot be empty");
    }

    if (offset != null && offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    if (length != null && length < 0) {
      throw new IllegalArgumentException("Length cannot be negative");
    }

    if (length != null && offset == null) {
      offset = 0L;
    }

    this.bucketName = bucketName;
    this.objectName = objectName;
    this.offset = offset;
    this.length = length;
    if (headerMap != null) {
      this.headerMap = Collections.unmodifiableMap(headerMap);
    } else {
      this.headerMap = null;
    }
    this.copyConditions = copyConditions;
    this.ssec = ssec;
  }

  /** Constructs header . */
  public void buildHeaders(long objectSize, String etag) throws IllegalArgumentException {
    if (offset != null && offset >= objectSize) {
      throw new IllegalArgumentException(
          "source "
              + bucketName
              + "/"
              + objectName
              + ": offset "
              + offset
              + " is beyond object size "
              + objectSize);
    }

    if (length != null) {
      if (length > objectSize) {
        throw new IllegalArgumentException(
            "source "
                + bucketName
                + "/"
                + objectName
                + ": length "
                + length
                + " is beyond object size "
                + objectSize);
      }

      if (offset + length > objectSize) {
        throw new IllegalArgumentException(
            "source "
                + bucketName
                + "/"
                + objectName
                + ": compose size "
                + (offset + length)
                + " is beyond object size "
                + objectSize);
      }
    }

    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.put("x-amz-copy-source", S3Escaper.encodePath(bucketName + "/" + objectName));
    headers.put("x-amz-copy-source-if-match", etag);

    if (headerMap != null) {
      headers.putAll(headerMap);
    }

    if (copyConditions != null) {
      headers.putAll(copyConditions.getConditions());
    }

    if (ssec != null) {
      headers.putAll(ssec.copySourceHeaders());
    }

    this.objectSize = objectSize;
    this.headers = Collections.unmodifiableMap(headers);
  }

  public String bucketName() {
    return bucketName;
  }

  public String objectName() {
    return objectName;
  }

  public Long offset() {
    return offset;
  }

  public Long length() {
    return length;
  }

  public CopyConditions copyConditions() {
    return copyConditions;
  }

  public ServerSideEncryptionCustomerKey ssec() {
    return ssec;
  }

  public Map<String, String> headers() {
    return headers;
  }

  public long objectSize() {
    return objectSize;
  }
}
