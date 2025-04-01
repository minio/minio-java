/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Collection of utility functions. */
public class Utils {
  public static final String UTF_8 = StandardCharsets.UTF_8.toString();

  public static String urlDecode(String value, String type) {
    if (!"url".equals(type)) return value;
    try {
      return value == null ? null : URLDecoder.decode(value, UTF_8);
    } catch (UnsupportedEncodingException e) {
      // This never happens as 'enc' name comes from JDK's own StandardCharsets.
      throw new RuntimeException(e);
    }
  }

  public static <T> List<T> unmodifiableList(List<? extends T> value) {
    return Collections.unmodifiableList(value == null ? new LinkedList<T>() : value);
  }

  public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> value) {
    return Collections.unmodifiableMap(value == null ? new HashMap<K, V>() : value);
  }
}
