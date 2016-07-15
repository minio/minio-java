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

package io.minio;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;


/**
 * Packs {@link ResponseHeader} and {@link Response} into one object to pass/return in various methods.
 */
class HttpResponse {
  ResponseHeader header;
  Response response;


  public HttpResponse(ResponseHeader header, Response response) {
    this.header = header;
    this.response = response;
  }


  /**
   * Gets header.
   */
  public ResponseHeader header() {
    return this.header;
  }


  /**
   * Gets body.
   */
  public ResponseBody body() {
    return this.response.body();
  }

  /**
   * Gets Response.
   */
  public Response response() {
    return this.response;
  }
}
