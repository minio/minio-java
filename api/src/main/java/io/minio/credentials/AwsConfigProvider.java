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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;

/** Credential provider using Amazon AWS credentials file. */
public class AwsConfigProvider extends EnvironmentProvider {
  private final String filename;
  private final String profile;

  public AwsConfigProvider(@Nullable String filename, @Nullable String profile) {
    if (filename != null && filename.isEmpty()) {
      throw new IllegalArgumentException("Filename must not be empty");
    }

    if (profile != null && profile.isEmpty()) {
      throw new IllegalArgumentException("Profile must not be empty");
    }

    this.filename = filename;
    this.profile = profile;
  }

  /**
   * Retrieve credentials in provided profile or AWS_PROFILE or "default" section in INI file from
   * provided filename or AWS_SHARED_CREDENTIALS_FILE environment variable or file .aws/credentials
   * in user's home directory.
   */
  @Override
  public Credentials fetch() {
    String filename =
        this.filename != null ? this.filename : getProperty("AWS_SHARED_CREDENTIALS_FILE");
    if (filename == null) {
      filename = Paths.get(System.getProperty("user.home"), ".aws", "credentials").toString();
    }

    String profile = this.profile;
    if (profile == null) profile = getProperty("AWS_PROFILE");
    if (profile == null) profile = "default";

    try (InputStream is = Files.newInputStream(Paths.get(filename))) {
      Map<String, Properties> result = unmarshal(new InputStreamReader(is, StandardCharsets.UTF_8));
      Properties values = result.get(profile);
      if (values == null) {
        throw new ProviderException(
            "Profile " + profile + " does not exist in AWS credential file");
      }

      String accessKey = values.getProperty("aws_access_key_id");
      String secretKey = values.getProperty("aws_secret_access_key");
      String sessionToken = values.getProperty("aws_session_token");

      if (accessKey == null) {
        throw new ProviderException(
            "Access key does not exist in profile " + profile + " in AWS credential file");
      }

      if (secretKey == null) {
        throw new ProviderException(
            "Secret key does not exist in profile " + profile + " in AWS credential file");
      }

      return new Credentials(accessKey, secretKey, sessionToken, null);
    } catch (IOException e) {
      throw new ProviderException("Unable to read AWS credential file", e);
    }
  }

  private Map<String, Properties> unmarshal(Reader reader) throws IOException {
    return new Ini().unmarshal(reader);
  }

  private static class Ini {
    private Map<String, Properties> result = new HashMap<>();

    public Map<String, Properties> unmarshal(Reader reader) throws IOException {
      new Properties() {
        private Properties section;

        @Override
        public Object put(Object key, Object value) {
          String header = (((String) key) + " " + value).trim();
          if (header.startsWith("[") && header.endsWith("]")) {
            section = new Properties();
            return result.put(header.substring(1, header.length() - 1), section);
          }
          return section.put(key, value);
        }

        @Override
        public boolean equals(Object o) {
          return super.equals(o);
        }

        @Override
        public int hashCode() {
          return super.hashCode();
        }
      }.load(reader);

      return result;
    }
  }
}
