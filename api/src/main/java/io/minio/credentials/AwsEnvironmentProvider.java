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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class AwsEnvironmentProvider extends EnvironmentProvider {

  private static final List<String> ACCESS_KEY_ALIASES =
      Arrays.asList("AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY");
  private static final List<String> SECRET_KEY_ALIASES =
      Arrays.asList("AWS_SECRET_ACCESS_KEY", "AWS_SECRET_KEY");
  private static final String SESSION_TOKEN_ALIAS = "AWS_SESSION_TOKEN";

  private Credentials credentials;

  public AwsEnvironmentProvider() {
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
    final String accessKey = readFirst(ACCESS_KEY_ALIASES);
    final String secretKey = readFirst(SECRET_KEY_ALIASES);
    final ZonedDateTime lifeTime = ZonedDateTime.now().plus(REFRESHED_AFTER);
    final String sessionToken = readProperty(SESSION_TOKEN_ALIAS);
    return new Credentials(accessKey, secretKey, new ResponseDate(lifeTime), sessionToken);
  }

  private String readFirst(@Nonnull Collection<String> propertyKeys) {
    for (String propertyKey : propertyKeys) {
      final String value = readProperty(propertyKey);
      if (value != null) {
        return value;
      }
    }
    throw new IllegalStateException("Can't find env variables for " + propertyKeys);
  }
}
