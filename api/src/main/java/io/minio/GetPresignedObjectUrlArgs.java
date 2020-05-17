/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

import io.minio.errors.InvalidExpiresRangeException;
import io.minio.http.Method;
import java.util.Map;

/** Argument class of MinioClient.getPresignedObjectUrl(). */
public class GetPresignedObjectUrlArgs extends ObjectArgs {
  private Method method;
  private Integer expires;
  private Map<String, String> reqParams;
  // default expiration for a presigned URL is 7 days in seconds
  private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 3600;

  public Method method() {
    return method;
  }

  public Integer expires() {
    return expires;
  }

  public Map<String, String> params() {
    return reqParams;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link GetPresignedObjectUrlArgs}. */
  public static final class Builder extends ObjectArgs.Builder<Builder, GetPresignedObjectUrlArgs> {
    /* method HTTP {@link Method} to generate presigned URL. */
    public Builder method(Method method) {
      operations.add(args -> args.method = method);
      return this;
    }

    /*expires Expiry in seconds; defaults to 7 days. */
    public Builder expires(Integer expires) throws InvalidExpiresRangeException {
      if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
        throw new InvalidExpiresRangeException(
            expires, "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
      }
      operations.add(args -> args.expires = expires);
      return this;
    }

    /* Supported params are response-expires, response-content-type,
     * response-cache-control and response-content-disposition. */
    public Builder params(Map<String, String> reqParams) {
      operations.add(args -> args.reqParams = reqParams);
      return this;
    }
  }
}
