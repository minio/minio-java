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

/**
 * Thrown to indicate that unexpected internal library error occurred while processing given request.
 */
public class InternalException extends MinioException {
  private static final long serialVersionUID = 138336287983212416L;

  /** Constructs a new InternalException with given error message. */
  public InternalException(String message, String httpTrace) {
    super(message, httpTrace);
  }
}
