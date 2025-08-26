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

import java.security.ProviderException;

/** Credential provider using Amazon AWS specific environment variables. */
public class AwsEnvironmentProvider extends EnvironmentProvider {
  public AwsEnvironmentProvider() {}

  private final String getValue(String key, String name) {
    String value = getProperty(key);
    if (value != null && value.isEmpty()) {
      throw new ProviderException("Empty " + name + " in " + key + " environment variable");
    }
    return value;
  }

  private final String getValue(String primaryKey, String secondaryKey, String name) {
    String value = getValue(primaryKey, name);
    return value != null ? value : getValue(secondaryKey, name);
  }

  private final String getAccessKey() {
    String value = getValue("AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY", "access key");
    if (value == null) {
      throw new ProviderException(
          "Access key does not exist in AWS_ACCESS_KEY_ID or AWS_ACCESS_KEY environment variable");
    }
    return value;
  }

  private final String getSecretKey() {
    String value = getValue("AWS_SECRET_ACCESS_KEY", "AWS_SECRET_KEY", "secret key");
    if (value == null) {
      throw new ProviderException(
          "Secret key does not exist in AWS_SECRET_ACCESS_KEY or AWS_SECRET_KEY environment variable");
    }
    return value;
  }

  @Override
  public Credentials fetch() {
    return new Credentials(getAccessKey(), getSecretKey(), getProperty("AWS_SESSION_TOKEN"), null);
  }
}
