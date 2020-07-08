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
import io.minio.messages.ResponseDate;
import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class MinioEnvironmentProvider extends EnvironmentProvider {

  private static final String ACCESS_KEY_ALIAS = "MINIO_ACCESS_KEY";
  private static final String SECRET_KEY_ALIAS = "MINIO_SECRET_KEY";

  private Credentials credentials;

  public MinioEnvironmentProvider() {
    credentials = readCredentials();
  }

  @Override
  public Credentials fetch() {
    if (!isExpired(credentials)) {
      return credentials;
    }
    // avoid race conditions with credentials rewriting
    synchronized (this) {
      if (isExpired(credentials)) {
        credentials = readCredentials();
      }
    }
    return credentials;
  }

  private Credentials readCredentials() {
    final String accessKey = readProperty(ACCESS_KEY_ALIAS);
    final String secretKey = readProperty(SECRET_KEY_ALIAS);
    final ZonedDateTime lifeTime = ZonedDateTime.now().plus(REFRESHED_AFTER);
    //noinspection ConstantConditions
    return new Credentials(accessKey, secretKey, new ResponseDate(lifeTime), null);
  }
}
