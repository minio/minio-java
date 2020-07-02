/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.minio.credentials;

import io.minio.messages.Credentials;
import java.time.Duration;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;

/**
 * This component allows {@link io.minio.MinioClient} to fetch valid (not expired) credentials.
 * Note: any provider implementation should cache valid credentials and control it's lifetime to
 * prevent unnesessary computation logic of repeatedly called {@link #fetch()}, while holding a
 * valid {@link Credentials} instance.
 */
@SuppressWarnings("unused")
public interface Provider {

  /**
   * @return a valid (not expired) {@link Credentials} instance for {@link io.minio.MinioClient}.
   */
  Credentials fetch();

  default boolean isExpired(@Nullable Credentials credentials) {
    if (credentials == null || credentials.expiredAt() == null || credentials.isAnonymous()) {
      return false;
    }
    // fair enough amount of time to execute the call to avoid situations when the check returns ok
    // and credentials
    // expire immediately after that.
    return ZonedDateTime.now().plus(Duration.ofSeconds(30)).isAfter(credentials.expiredAt());
  }
}
