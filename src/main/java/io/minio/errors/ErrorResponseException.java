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

import io.minio.messages.ErrorResponse;
import io.minio.ErrorCode;


@SuppressWarnings({"WeakerAccess", "unused"})
public class ErrorResponseException extends MinioException {
  private ErrorResponse errorResponse;
  private Request request;


  public ErrorResponseException(ErrorResponse errorResponse) {
    super(ErrorCode.fromString(errorResponse.getCode()).message());
    this.errorResponse = errorResponse;
  }


  public ErrorResponseException(ErrorResponse errorResponse, Request request) {
    this(errorResponse);
    this.request = request;
  }


  public ErrorCode getErrorCode() {
    return ErrorCode.fromString(this.errorResponse.getCode());
  }
}
