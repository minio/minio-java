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

package io.minio.errors;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import io.minio.messages.ErrorResponse;
import io.minio.ErrorCode;


@SuppressWarnings({"WeakerAccess", "unused"})
public class ErrorResponseException extends MinioException {
  private ErrorResponse errorResponse;
  private Response response;


  /**
   * constructor.
   */
  public ErrorResponseException(ErrorResponse errorResponse, Response response) {
    super(errorResponse.message());
    this.errorResponse = errorResponse;
    this.response = response;
  }


  public ErrorCode errorCode() {
    return this.errorResponse.errorCode();
  }


  @Override
  public String toString() {
    Request request = response.request();
    return "error occured\n"
        + errorResponse.getString() + "\n"
        + "request={"
        + "method=" + request.method() + ", "
        + "url=" + request.httpUrl() + ", "
        + "headers=" + request.headers()
        + "}\n"
        + "response={"
        + "code=" + response.code() + ", "
        + "headers=" + response.headers()
        + "}\n";
  }
}
