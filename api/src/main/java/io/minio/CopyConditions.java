/*Minio Java Library for Amazon S3 Compatible Cloud Storage,(C)2015 Minio,Inc.
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

import io.minio.errors.InvalidArgumentException;

/**
 * A container class to hold all the Conditions to be checked before copying an object.
 */
public class CopyConditions {

  Date dateUnmodifiedSince;
  Date dateModifiedAfter;
  String matchETag;
  String exceptETag;

  /**
   * Sets object unmodified since condition.
   * 
   * @throws InvalidArgumentException
   *           When invalid arguement is passed
   */
  public void setUnmodified(Date date) throws InvalidArgumentException {
    if (date == null) {
      throw new InvalidArgumentException("Date can not be null");
    }
    dateUnmodifiedSince = (Date) date.clone();
  }

  /**
   * Sets object modified after condition.
   * 
   * @throws InvalidArgumentException
   *           When invalid arguement is passed
   */
  public void setModified(Date date) throws InvalidArgumentException {
    if (date == null) {
      throw new InvalidArgumentException("Date can not be null");
    }
    dateModifiedAfter = (Date) date.clone();
  }

  /**
   * Set matching ETag condition.
   * 
   * @throws InvalidArgumentException
   *           When invalid arguement is passed
   */
  public void setMatchETag(String etag) throws InvalidArgumentException {
    if (etag == null) {
      throw new InvalidArgumentException("ETag can not be null");
    }
    matchETag = etag;
  }

  /**
   * Set matching ETag except condition.
   * 
   * @throws InvalidArgumentException
   *           When invalid arguement is passed
   */
  public void setMatchETagExcept(String etag) throws InvalidArgumentException {
    if (etag == null) {
      throw new InvalidArgumentException("ETag can not be null");
    }
    exceptETag = etag;
  }

  /**
   * Returns true if the object is modified after the "modified after condition" or modified condition is not set. Else
   * returns false.
   */
  boolean isModifiedAfter(Date lastModifiedDate) {
    if (dateModifiedAfter != null) {
      return dateModifiedAfter.before(lastModifiedDate);
    }

    return true;
  }

  /**
   * Returns true if the object is not modified after the unmodified since condition or unmodified condition is not set.
   * Else returns false.
   */
  boolean isUnmodifiedSince(Date lastModifiedDate) {
    if (dateUnmodifiedSince != null) {
      return dateUnmodifiedSince.after(lastModifiedDate);
    }

    return true;
  }

  /**
   * Returns true if the object etag matches the etag to match condition, or etag to match condition is not set. Else
   * returns false.
   */
  boolean etagMatches(String eTagToMatch) {
    if (matchETag != null) {
      return matchETag.equals(eTagToMatch);
    }

    return true;
  }

  /**
   * Returns true if the object etag doesn't match the etag not to match condition, or etag not to match condition is
   * not set. Else returns false.
   */
  boolean etagDoesNotMatch(String eTagToMatch) {
    if (exceptETag != null) {
      return !(exceptETag.equals(eTagToMatch));
    }

    return true;
  }
}
