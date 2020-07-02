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
package io.minio.messages;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WebIdentityToken {

  private final String jwtAccessToken;
  private final long expiredAfter;
  private final String policy;

  @SuppressWarnings("unused")
  public WebIdentityToken(@Nonnull String jwtAccessToken, long expiredAfter) {
    this(jwtAccessToken, expiredAfter, null);
  }

  public WebIdentityToken(
      @Nonnull String jwtAccessToken, long expiredAfter, @Nullable String policy) {
    this.jwtAccessToken = Objects.requireNonNull(jwtAccessToken);
    this.expiredAfter = expiredAfter;
    this.policy = policy;
  }

  public String token() {
    return jwtAccessToken;
  }

  public long expiredAfter() {
    return expiredAfter;
  }

  public String policy() {
    return policy;
  }
}
