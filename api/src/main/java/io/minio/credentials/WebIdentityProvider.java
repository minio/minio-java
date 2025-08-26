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

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

/**
 * Credential provider using <a
 * href="https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html">AssumeRoleWithWebIdentity
 * API</a>.
 */
public class WebIdentityProvider extends WebIdentityClientGrantsProvider {
  public WebIdentityProvider(
      @Nonnull Supplier<Jwt> supplier,
      @Nonnull String stsEndpoint,
      @Nullable Integer durationSeconds,
      @Nullable String policy,
      @Nullable String roleArn,
      @Nullable String roleSessionName,
      @Nullable OkHttpClient customHttpClient) {
    super(
        supplier, stsEndpoint, durationSeconds, policy, roleArn, roleSessionName, customHttpClient);
  }

  @Override
  protected HttpUrl.Builder newUrlBuilder(Jwt jwt) {
    HttpUrl.Builder urlBuilder =
        newUrlBuilder(
            stsEndpoint,
            "AssumeRoleWithWebIdentity",
            getDurationSeconds(jwt.expiry()),
            policy,
            roleArn,
            (roleArn != null && roleSessionName == null)
                ? String.valueOf(System.currentTimeMillis())
                : roleSessionName);
    return urlBuilder.addQueryParameter("WebIdentityToken", jwt.token());
  }

  @Override
  protected Class<? extends BaseIdentityProvider.Response> getResponseClass() {
    return Response.class;
  }

  /**
   * Response XML <a
   * href="https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html">AssumeRoleWithWebIdentity
   * API</a>.
   */
  @Root(name = "AssumeRoleWithWebIdentityResponse", strict = false)
  @Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
  public static class Response implements BaseIdentityProvider.Response {
    @Path(value = "AssumeRoleWithWebIdentityResult")
    @Element(name = "Credentials")
    private Credentials credentials;

    public Credentials getCredentials() {
      return credentials;
    }
  }
}
