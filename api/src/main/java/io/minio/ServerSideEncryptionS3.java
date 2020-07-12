/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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
import java.util.HashMap;
import java.util.Map;

/** S3 type of Server-side encryption. */
public class ServerSideEncryptionS3 extends ServerSideEncryption {
  private static final Map<String, String> headers;

  static {
    Map<String, String> map = new HashMap<>();
    map.put("X-Amz-Server-Side-Encryption", "AES256");
    headers = Collections.unmodifiableMap(map);
  }

  @Override
  public final Map<String, String> headers() {
    return headers;
  }

  @Override
  public final boolean tlsRequired() {
    return false;
  }

  @Override
  public String toString() {
    return "SSE-S3";
  }
}
