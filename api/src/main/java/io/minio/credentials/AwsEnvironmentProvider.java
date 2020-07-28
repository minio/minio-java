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

/** Credential provider using Amazon AWS specific environment variables. */
public class AwsEnvironmentProvider extends EnvironmentProvider {
  public AwsEnvironmentProvider() {}

  private final String getAccessKey() {
    String value = getProperty("AWS_ACCESS_KEY_ID");
    return (value != null) ? value : getProperty("AWS_ACCESS_KEY");
  }

  private final String getSecretKey() {
    String value = getProperty("AWS_SECRET_ACCESS_KEY");
    return (value != null) ? value : getProperty("AWS_SECRET_KEY");
  }

  @Override
  public Credentials fetch() {
    return new Credentials(getAccessKey(), getSecretKey(), getProperty("AWS_SESSION_TOKEN"), null);
  }
}
