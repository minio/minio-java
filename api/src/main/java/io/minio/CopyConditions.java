/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2017 MinIO,Inc.
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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** A container class to hold all the Conditions to be checked before copying an object. */
public class CopyConditions {
  // Metadata directive "REPLACE" used to replace metadata on
  // destination object in copyObject().
  private static final String METADATA_DIRECTIVE_REPLACE = "REPLACE";

  private Map<String, String> copyConditions = new HashMap<>();

  /**
   * Set modified condition, copy object modified since given time.
   *
   * @throws IllegalArgumentException When date is null
   */
  public void setModified(ZonedDateTime time) throws IllegalArgumentException {
    if (time == null) {
      throw new IllegalArgumentException("modified time cannot be empty");
    }
    copyConditions.put(
        "x-amz-copy-source-if-modified-since", time.format(Time.HTTP_HEADER_DATE_FORMAT));
  }

  /**
   * Sets object unmodified condition, copy object unmodified since given time.
   *
   * @throws IllegalArgumentException When date is null
   */
  public void setUnmodified(ZonedDateTime time) throws IllegalArgumentException {
    if (time == null) {
      throw new IllegalArgumentException("unmodified time can not be null");
    }

    copyConditions.put(
        "x-amz-copy-source-if-unmodified-since", time.format(Time.HTTP_HEADER_DATE_FORMAT));
  }

  /**
   * Set matching ETag condition, copy object which matches the following ETag.
   *
   * @throws IllegalArgumentException When etag is null
   */
  public void setMatchETag(String etag) throws IllegalArgumentException {
    if (etag == null) {
      throw new IllegalArgumentException("ETag cannot be empty");
    }
    copyConditions.put("x-amz-copy-source-if-match", etag);
  }

  /**
   * Set matching ETag none condition, copy object which does not match the following ETag.
   *
   * @throws IllegalArgumentException When etag is null
   */
  public void setMatchETagNone(String etag) throws IllegalArgumentException {
    if (etag == null) {
      throw new IllegalArgumentException("ETag cannot be empty");
    }
    copyConditions.put("x-amz-copy-source-if-none-match", etag);
  }

  /**
   * Set replace metadata directive which specifies that destination object after copyObject() sets
   * new metadata provided in the request.
   */
  public void setReplaceMetadataDirective() {
    copyConditions.put("x-amz-metadata-directive", METADATA_DIRECTIVE_REPLACE);
  }

  /** Get all the set copy conditions map. */
  public Map<String, String> getConditions() {
    return Collections.unmodifiableMap(copyConditions);
  }
}
