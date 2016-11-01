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
import java.util.HashMap;
import java.util.Map;

import io.minio.errors.InvalidArgumentException;

/**
 * A container class to hold all the Conditions to be checked before copying an object.
 */
public class CopyConditions {

  private Map<String, String> copyConditions = new HashMap<>();

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
    copyConditions.put("x-amz-copy-source-if-unmodified-since", date.toString());
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
    copyConditions.put("x-amz-copy-source-if-modified-since", date.toString());
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
    copyConditions.put("x-amz-copy-source-if-match", etag);
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
    copyConditions.put("x-amz-copy-source-if-none-match", etag);
  }

  /**
   * Get copy conditions HashMap.
   * 
   */
  public Map<String, String> getCopyConditions() {
    return copyConditions;
  }
}
