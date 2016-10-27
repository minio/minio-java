/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2016 Minio, Inc.
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

package io.minio.policy;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Helper class to parse Amazon AWS S3 policy declarations.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Principal {
  @JsonProperty("AWS")
  private Set<String> aws;
  
  @JsonProperty("CanonicalUser")
  private Set<String> canonicalUser;


  public Principal() {
  }


  public Principal(String aws) {
    this.aws = new HashSet<String>();
    this.aws.add(aws);
  }


  /**
   * Returns AWS value.
   */
  public Set<String> aws() {
    if (this.aws == null) {
      return null;
    }

    return new HashSet<String>(this.aws);
  }
}
