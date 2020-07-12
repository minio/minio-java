/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2018 MinIO, Inc.
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

/** Base class of server-side encryption. */
public abstract class ServerSideEncryption {
  private static final Map<String, String> emptyHeaders =
      Collections.unmodifiableMap(new HashMap<>());

  public abstract Map<String, String> headers();

  public boolean tlsRequired() {
    return true;
  }

  public Map<String, String> copySourceHeaders() {
    return emptyHeaders;
  }
}
