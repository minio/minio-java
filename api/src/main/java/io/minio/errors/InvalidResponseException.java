/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2019 MinIO, Inc.
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

package io.minio.errors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import okhttp3.Request;

/** Thrown to indicate that non-xml response thrown from server. */
public class InvalidResponseException extends MinioException {
  @SuppressFBWarnings(value = "Se", justification = "Non-XML response from server")
  private Request request;

  public InvalidResponseException() {
    super("Non-XML response from server");
  }

  /** Constructs a new InvalidResponseException with HTTP request object causes the error. */
  public InvalidResponseException(Request request) {
    this();
    this.request = request;
  }

  public Request request() {
    return this.request;
  }
}
