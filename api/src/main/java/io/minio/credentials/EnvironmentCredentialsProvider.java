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

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class EnvironmentCredentialsProvider implements CredentialsProvider {

  // it's ok to re-read values from env.variables every 5 min.
  protected static final Duration REFRESHED_AFTER = Duration.ofMinutes(5);

  /**
   * Method used to read system/env properties. If property not found through system properties it
   * will search the property in environment properties.
   *
   * @param propertyName name of the property to retrieve.
   * @return property value.
   * @throws NullPointerException if {@literal propertyName} is null.
   */
  @Nullable
  protected String readProperty(@Nonnull String propertyName) {
    final String systemProperty = System.getProperty(propertyName);
    if (systemProperty != null) {
      return systemProperty;
    }
    return System.getenv(propertyName);
  }
}
