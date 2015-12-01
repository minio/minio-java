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

import io.minio.errors.ClientException;

import java.io.IOException;

public class Result<T> {
  private final T type;
  private final Exception ex;

  public Result(T type, Exception ex) {
    this.type = type;
    this.ex = ex;
  }

  /**
   * get result.
   */
  public T getResult() throws IOException, ClientException {
    if (ex != null) {
      if (ex instanceof IOException) {
        throw (IOException) ex;
      }
      if (ex instanceof ClientException) {
        throw (ClientException) ex;
      }
    }
    return type;
  }
}
