/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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
import io.minio.messages.ErrorResponse;
import okhttp3.Request;
import okhttp3.Response;

/** Thrown to indicate that error response is received when executing Amazon S3 operation. */
@SuppressWarnings("WeakerAccess")
public class ErrorResponseException extends MinioException {
  private static final long serialVersionUID = -2933211538346902928L;

  private final ErrorResponse errorResponse;

  @SuppressFBWarnings(
      value = "Se",
      justification = "There's really no excuse except that nobody has complained")
  private final Response response;

  /** Constructs a new ErrorResponseException with error response and HTTP response object. */
  public ErrorResponseException(ErrorResponse errorResponse, Response response, String httpTrace) {
    super(errorResponse.message(), httpTrace);
    this.errorResponse = errorResponse;
    this.response = response;
  }

  /** Returns ErrorResponse contains detail of what error occurred. */
  public ErrorResponse errorResponse() {
    return this.errorResponse;
  }

  public Response response() {
    return this.response;
  }

  @Override
  public String toString() {
    Request request = response.request();
    return "error occurred\n"
        + errorResponse.toString()
        + "\n"
        + "request={"
        + "method="
        + request.method()
        + ", "
        + "url="
        + request.url()
        + ", "
        + "headers="
        + request
            .headers()
            .toString()
            .replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*")
            .replaceAll("Credential=([^/]+)", "Credential=*REDACTED*")
        + "}\n"
        + "response={"
        + "code="
        + response.code()
        + ", "
        + "headers="
        + response.headers()
        + "}\n";
  }
}
