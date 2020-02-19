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

package io.minio;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.XmlParserException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/** A container class keeps any type and exception occured. */
public class Result<T> {
  private final T type;
  private final Exception ex;

  public Result(T type) {
    this.type = type;
    this.ex = null;
  }

  public Result(Exception ex) {
    this.type = null;
    this.ex = ex;
  }

  /** Returns given Type if exception is null, else respective exception is thrown. */
  public T get()
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, JsonParseException, JsonMappingException, IOException,
          InvalidKeyException, XmlParserException, ErrorResponseException, InternalException {
    if (ex == null) {
      return type;
    }

    if (ex instanceof InvalidBucketNameException) {
      throw (InvalidBucketNameException) ex;
    }

    if (ex instanceof IllegalArgumentException) {
      throw (IllegalArgumentException) ex;
    }

    if (ex instanceof NoSuchAlgorithmException) {
      throw (NoSuchAlgorithmException) ex;
    }

    if (ex instanceof InsufficientDataException) {
      throw (InsufficientDataException) ex;
    }

    if (ex instanceof InvalidKeyException) {
      throw (InvalidKeyException) ex;
    }

    if (ex instanceof XmlParserException) {
      throw (XmlParserException) ex;
    }

    if (ex instanceof ErrorResponseException) {
      throw (ErrorResponseException) ex;
    }

    if (ex instanceof JsonParseException) {
      throw (JsonParseException) ex;
    }

    if (ex instanceof JsonMappingException) {
      throw (JsonMappingException) ex;
    }

    if (ex instanceof IOException) {
      throw (IOException) ex;
    }

    throw (InternalException) ex;
  }
}
