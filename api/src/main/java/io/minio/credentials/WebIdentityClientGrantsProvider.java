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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/** Base provider of {@link WebIdentityProvider} and {@link ClientGrantsProvider}. */
public abstract class WebIdentityClientGrantsProvider extends BaseIdentityProvider {
  public static final int MIN_DURATION_SECONDS = (int) TimeUnit.MINUTES.toSeconds(15);
  public static final int MAX_DURATION_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);
  private static final RequestBody EMPTY_BODY =
      RequestBody.create(new byte[] {}, MediaType.parse("application/octet-stream"));
  private final Supplier<Jwt> supplier;
  protected final HttpUrl stsEndpoint;
  protected final Integer durationSeconds;
  protected final String policy;
  protected final String roleArn;
  protected final String roleSessionName;

  public WebIdentityClientGrantsProvider(
      @Nonnull Supplier<Jwt> supplier,
      @Nonnull String stsEndpoint,
      @Nullable Integer durationSeconds,
      @Nullable String policy,
      @Nullable String roleArn,
      @Nullable String roleSessionName,
      @Nullable OkHttpClient customHttpClient) {
    super(customHttpClient);
    this.supplier = Objects.requireNonNull(supplier, "JWT token supplier must not be null");
    stsEndpoint = Objects.requireNonNull(stsEndpoint, "STS endpoint cannot be empty");
    this.stsEndpoint = Objects.requireNonNull(HttpUrl.parse(stsEndpoint), "Invalid STS endpoint");
    this.durationSeconds = durationSeconds;
    this.policy = policy;
    this.roleArn = roleArn;
    this.roleSessionName = roleSessionName;
  }

  protected int getDurationSeconds(int expiry) {
    if (durationSeconds != null && durationSeconds > 0) expiry = durationSeconds;
    if (expiry > MAX_DURATION_SECONDS) return MAX_DURATION_SECONDS;
    if (expiry <= 0) return expiry;
    return (expiry < MIN_DURATION_SECONDS) ? MIN_DURATION_SECONDS : expiry;
  }

  @Override
  protected Request getRequest() {
    Jwt jwt = supplier.get();
    HttpUrl.Builder urlBuilder = newUrlBuilder(jwt);
    return new Request.Builder().url(urlBuilder.build()).method("POST", EMPTY_BODY).build();
  }

  protected abstract HttpUrl.Builder newUrlBuilder(Jwt jwt);
}
