/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage,
 * (C) 2017 Minio,Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import org.joda.time.DateTime;

import io.minio.errors.InvalidArgumentException;

/**
 * A container class to hold all the Conditions to be checked
 * before copying an object.
 */
public class CopyConditions {

  private Map<String, String> copyConditions = new HashMap<>();

  /**
   * Set modified condition, copy object modified since given time.
   *
   * @throws InvalidArgumentException
   *           When date is null
   */
  public void setModified(DateTime date) throws InvalidArgumentException {
    if (date == null) {
      throw new InvalidArgumentException("Date cannot be empty");
    }
    copyConditions.put("x-amz-copy-source-if-modified-since", date.toString(DateFormat.HTTP_HEADER_DATE_FORMAT));
  }

  /**
   * Sets object unmodified condition, copy object unmodified since given time.
   *
   * @throws InvalidArgumentException
   *           When date is null
   */
  public void setUnmodified(DateTime date) throws InvalidArgumentException {
    if (date == null) {
      throw new InvalidArgumentException("Date can not be null");
    }

    copyConditions.put("x-amz-copy-source-if-unmodified-since", date.toString(DateFormat.HTTP_HEADER_DATE_FORMAT));
  }

  /**
   * Set matching ETag condition, copy object which matches
   * the following ETag.
   *
   * @throws InvalidArgumentException
   *           When etag is null
   */
  public void setMatchETag(String etag) throws InvalidArgumentException {
    if (etag == null) {
      throw new InvalidArgumentException("ETag cannot be empty");
    }
    copyConditions.put("x-amz-copy-source-if-match", etag);
  }

  /**
   * Set matching ETag none condition, copy object which does not
   * match the following ETag.
   *
   * @throws InvalidArgumentException
   *           When etag is null
   */
  public void setMatchETagNone(String etag) throws InvalidArgumentException {
    if (etag == null) {
      throw new InvalidArgumentException("ETag cannot be empty");
    }
    copyConditions.put("x-amz-copy-source-if-none-match", etag);
  }

  /**
   * Get all the set copy conditions map.
   *
   */
  public Map<String, String> getConditions() {
    return Collections.unmodifiableMap(copyConditions);
  }
}
