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

package io.minio;

import java.util.Date;

/**
 * Object stat information.
 */
@SuppressWarnings("unused")
public class ObjectStat {
  private final String bucketName;
  private final String name;
  private final Date createdTime;
  private final long length;
  private final String etag;
  private final String contentType;


  /**
   * Creates ObjectStat with given bucket name, object name, created time, object length, Etag and content type.
   */
  public ObjectStat(String bucketName, String name, Date createdTime, long length, String etag, String contentType) {
    this.bucketName = bucketName;
    this.name = name;
    this.contentType = contentType;
    this.createdTime = (Date) createdTime.clone();
    this.length = length;
    if (etag != null) {
      this.etag = etag.replaceAll("\"", "");
    } else {
      this.etag = "";
    }
  }


  /**
   * Checks whether given object is same as this ObjectStat.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ObjectStat that = (ObjectStat) o;

    if (length != that.length) {
      return false;
    }
    if (!bucketName.equals(that.bucketName)) {
      return false;
    }
    if (!name.equals(that.name)) {
      return false;
    }
    if (!createdTime.equals(that.createdTime)) {
      return false;
    }
    if (!etag.equals(that.etag)) {
      return false;
    }
    return contentType.equals(that.contentType);

  }


  /**
   * Returns hash of this ObjectStat.
   */
  @Override
  public int hashCode() {
    int result = bucketName.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + createdTime.hashCode();
    result = 31 * result + (int) (length ^ (length >>> 32));
    result = 31 * result + etag.hashCode();
    result = 31 * result + contentType.hashCode();
    return result;
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
  public String name() {
    return name;
  }


  /**
   * Returns created time.
   */
  public Date createdTime() {
    return (Date) createdTime.clone();
  }


  /**
   * Returns object length.
   */
  public long length() {
    return length;
  }

  /**
   * Returns ETag.
   */
  public String etag() {
    return etag;
  }

  /**
   * Returns content type of object.
   */
  public String contentType() {
    return contentType;
  }

  /**
   * Returns ObjectStat as string.
   */
  @Override
  public String toString() {
    return "ObjectStat{"
        + "bucket='"
        + bucketName
        + '\''
        + ", name='"
        + name + '\''
        + ", contentType='"
        + contentType
        + '\''
        + ", createdTime="
        + createdTime
        + ", length="
        + length
        + ", etag='"
        + etag
        + '\''
        + '}';
  }
}
