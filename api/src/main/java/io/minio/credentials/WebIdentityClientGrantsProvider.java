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

import io.minio.Xml;
import io.minio.errors.XmlParserException;
import java.io.IOException;
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
import okhttp3.Response;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

/** Base class of WebIdentity and ClientGrants providers. */
public abstract class WebIdentityClientGrantsProvider implements Provider {
  public static final int MIN_DURATION_SECONDS = (int) TimeUnit.MINUTES.toSeconds(15);
  public static final int MAX_DURATION_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);
  private static final RequestBody EMPTY_BODY =
      RequestBody.create(MediaType.parse("application/octet-stream"), new byte[] {});
  private final Supplier<Jwt> supplier;
  private final HttpUrl stsEndpoint;
  private final Integer durationSeconds;
  private final String policy;
  private final String roleArn;
  private final String roleSessionName;
  private final OkHttpClient httpClient;
  private Credentials credentials;

  public WebIdentityClientGrantsProvider(
      @Nonnull Supplier<Jwt> supplier,
      @Nonnull String stsEndpoint,
      @Nullable Integer durationSeconds,
      @Nullable String policy,
      @Nullable String roleArn,
      @Nullable String roleSessionName,
      @Nullable OkHttpClient customHttpClient) {
    this.supplier = Objects.requireNonNull(supplier, "JWT token supplier must not be null");
    stsEndpoint = Objects.requireNonNull(stsEndpoint, "STS endpoint cannot be empty");
    this.stsEndpoint = Objects.requireNonNull(HttpUrl.parse(stsEndpoint), "Invalid STS endpoint");
    this.durationSeconds = durationSeconds;
    this.policy = policy;
    this.roleArn = roleArn;
    this.roleSessionName = roleSessionName;
    this.httpClient = (customHttpClient != null) ? customHttpClient : new OkHttpClient();
  }

  private int getDurationSeconds(int expiry) {
    if (durationSeconds != null && durationSeconds > 0) {
      expiry = durationSeconds;
    }

    if (expiry > MAX_DURATION_SECONDS) {
      return MAX_DURATION_SECONDS;
    }

    if (expiry <= 0) {
      return expiry;
    }

    return (expiry < MIN_DURATION_SECONDS) ? MIN_DURATION_SECONDS : expiry;
  }

  @Override
  public synchronized Credentials fetch() {
    if (credentials != null && !credentials.isExpired()) {
      return credentials;
    }

    Jwt jwt = supplier.get();

    HttpUrl.Builder urlBuilder =
        stsEndpoint.newBuilder().addQueryParameter("Version", "2011-06-15");

    int durationSeconds = getDurationSeconds(jwt.expiry());
    if (durationSeconds > 0) {
      urlBuilder.addQueryParameter("DurationSeconds", String.valueOf(durationSeconds));
    }

    if (policy != null) {
      urlBuilder.addQueryParameter("Policy", policy);
    }

    if (isWebIdentity()) {
      urlBuilder
          .addQueryParameter("Action", "AssumeRoleWithWebIdentity")
          .addQueryParameter("WebIdentityToken", jwt.token());
      if (roleArn != null) {
        urlBuilder
            .addQueryParameter("RoleArn", roleArn)
            .addQueryParameter(
                "RoleSessionName",
                (roleSessionName != null)
                    ? roleSessionName
                    : String.valueOf(System.currentTimeMillis()));
      }
    } else {
      urlBuilder
          .addQueryParameter("Action", "AssumeRoleWithClientGrants")
          .addQueryParameter("Token", jwt.token());
    }

    Request request =
        new Request.Builder().url(urlBuilder.build()).method("POST", EMPTY_BODY).build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IllegalStateException(
            "STS service failed with HTTP status code " + response.code());
      }

      if (isWebIdentity()) {
        WebIdentityResponse result =
            Xml.unmarshal(WebIdentityResponse.class, response.body().charStream());
        credentials = result.credentials();
      } else {
        ClientGrantsResponse result =
            Xml.unmarshal(ClientGrantsResponse.class, response.body().charStream());
        credentials = result.credentials();
      }

      return credentials;
    } catch (XmlParserException | IOException e) {
      throw new IllegalStateException("Unable to parse STS response", e);
    }
  }

  protected abstract boolean isWebIdentity();

  /** Object representation of response XML of AssumeRoleWithWebIdentity API. */
  @Root(name = "AssumeRoleWithWebIdentityResponse", strict = false)
  @Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
  public static class WebIdentityResponse {
    @Path(value = "AssumeRoleWithWebIdentityResult")
    @Element(name = "Credentials")
    private Credentials credentials;

    public Credentials credentials() {
      return credentials;
    }
  }

  /** Object representation of response XML of AssumeRoleWithClientGrants API. */
  @Root(name = "AssumeRoleWithClientGrantsResponse", strict = false)
  @Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
  public static class ClientGrantsResponse {
    @Path(value = "AssumeRoleWithClientGrantsResult")
    @Element(name = "Credentials")
    private Credentials credentials;

    public Credentials credentials() {
      return credentials;
    }
  }
}
