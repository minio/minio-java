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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/** Base Exception class for all minio-java exceptions. */
public class MinioException extends Exception {
  private static final long serialVersionUID = -7241010318779326306L;

  String httpTrace = null;

  /** Constructs a new MinioException with given error message. */
  public MinioException(String message) {
    super(message);
  }

  /** Constructs a new MinioException with given error message. */
  public MinioException(String message, String httpTrace) {
    super(message);
    this.httpTrace = httpTrace;
  }

  /** Constructs a new MinioException with the specified detail message and cause. */
  public MinioException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Constructs a new MinioException with the specified cause. */
  public MinioException(Throwable cause) {
    super(cause);
  }

  public String httpTrace() {
    return this.httpTrace;
  }

  /** Throws encapsulated exception. */
  public void throwEncapsulatedException()
      throws BucketPolicyTooLargeException, CertificateException, EOFException,
          ErrorResponseException, FileNotFoundException, GeneralSecurityException,
          InsufficientDataException, InternalException, InvalidKeyException,
          InvalidResponseException, IOException, JsonMappingException, JsonParseException,
          JsonProcessingException, KeyManagementException, KeyStoreException, MinioException,
          NoSuchAlgorithmException, ServerException, XmlParserException {
    Throwable e = getCause();

    // Inherited by MinioException
    if (e instanceof BucketPolicyTooLargeException) throw (BucketPolicyTooLargeException) e;
    if (e instanceof ErrorResponseException) throw (ErrorResponseException) e;
    if (e instanceof InsufficientDataException) throw (InsufficientDataException) e;
    if (e instanceof InternalException) throw (InternalException) e;
    if (e instanceof InvalidResponseException) throw (InvalidResponseException) e;
    if (e instanceof ServerException) throw (ServerException) e;
    if (e instanceof XmlParserException) throw (XmlParserException) e;

    // Inherited by IOException
    if (e instanceof JsonMappingException) throw (JsonMappingException) e;
    if (e instanceof JsonParseException) throw (JsonParseException) e;
    if (e instanceof JsonProcessingException) throw (JsonProcessingException) e;
    if (e instanceof EOFException) throw (EOFException) e;
    if (e instanceof FileNotFoundException) throw (FileNotFoundException) e;
    if (e instanceof IOException) throw (IOException) e;

    // Inherited by GeneralSecurityException
    if (e instanceof CertificateException) throw (CertificateException) e;
    if (e instanceof InvalidKeyException) throw (InvalidKeyException) e;
    if (e instanceof KeyManagementException) throw (KeyManagementException) e;
    if (e instanceof KeyStoreException) throw (KeyStoreException) e;
    if (e instanceof NoSuchAlgorithmException) throw (NoSuchAlgorithmException) e;
    if (e instanceof GeneralSecurityException) throw (GeneralSecurityException) e;

    throw this;
  }
}
