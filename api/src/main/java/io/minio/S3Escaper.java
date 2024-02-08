/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2016 MinIO, Inc.
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

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

public class S3Escaper {
  private static final Escaper ESCAPER = UrlEscapers.urlPathSegmentEscaper();

  /** Returns S3 encoded string. */
  public static String encode(String str) {
    if (str == null) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    for (char ch : ESCAPER.escape(str).toCharArray()) {
      switch (ch) {
        case '!':
          builder.append("%21");
          break;
        case '$':
          builder.append("%24");
          break;
        case '&':
          builder.append("%26");
          break;
        case '\'':
          builder.append("%27");
          break;
        case '(':
          builder.append("%28");
          break;
        case ')':
          builder.append("%29");
          break;
        case '*':
          builder.append("%2A");
          break;
        case '+':
          builder.append("%2B");
          break;
        case ',':
          builder.append("%2C");
          break;
        case '/':
          builder.append("%2F");
          break;
        case ':':
          builder.append("%3A");
          break;
        case ';':
          builder.append("%3B");
          break;
        case '=':
          builder.append("%3D");
          break;
        case '@':
          builder.append("%40");
          break;
        case '[':
          builder.append("%5B");
          break;
        case ']':
          builder.append("%5D");
          break;
        default:
          builder.append(ch);
      }
    }
    return builder.toString();
  }

  /** Returns S3 encoded string of given path where multiple '/' are trimmed. */
  public static String encodePath(String path) {
    final StringBuilder encodedPath = new StringBuilder();
    for (String pathSegment : path.split("/")) {
      if (!pathSegment.isEmpty()) {
        if (encodedPath.length() > 0) {
          encodedPath.append("/");
        }
        encodedPath.append(S3Escaper.encode(pathSegment));
      }
    }

    if (path.startsWith("/")) {
      encodedPath.insert(0, "/");
    }
    if (path.endsWith("/")) {
      encodedPath.append("/");
    }

    return encodedPath.toString();
  }
}
