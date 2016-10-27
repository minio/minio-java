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


/**
 * Thrown to indicate that no response is received from Amazon AWS S3 service.
 */
public class NoResponseException extends MinioException {
  private Request request;


  public NoResponseException() {
    super("no response from server");
  }


/**
 * Constructs a new NoResponseException with HTTP request object causes the error.
 */
  public NoResponseException(Request request) {
    this();
    this.request = request;
  }

  public Request request() {
    return this.request;
  }
}
