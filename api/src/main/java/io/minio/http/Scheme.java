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

package io.minio.http;


/**
 * HTTP schemes.
 */
public enum Scheme {
  HTTP("http"), HTTPS("https");
  private final String value;


  private Scheme(String value) {
    this.value = value;
  }


  /**
   * Returns Scheme enum of given string.
   */
  public static Scheme fromString(String scheme) {
    if (scheme == null) {
      throw new IllegalArgumentException("null scheme");
    }

    for (Scheme s : Scheme.values()) {
      if (scheme.equalsIgnoreCase(s.value)) {
        return s;
      }
    }

    throw new IllegalArgumentException("invalid HTTP scheme '" + scheme + "'");
  }
}
