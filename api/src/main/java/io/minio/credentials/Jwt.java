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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.beans.ConstructorProperties;
import java.util.Objects;
import javax.annotation.Nonnull;

/** JSON web token used in {@link WebIdentityProvider} and {@link ClientGrantsProvider}. */
public class Jwt {
  @JsonProperty("access_token")
  private final String token;

  @JsonProperty("expires_in")
  private final int expiry;

  @ConstructorProperties({"access_token", "expires_in"})
  public Jwt(@Nonnull String token, int expiry) {
    this.token = Objects.requireNonNull(token);
    this.expiry = expiry;
  }

  public String token() {
    return token;
  }

  public int expiry() {
    return expiry;
  }
}
