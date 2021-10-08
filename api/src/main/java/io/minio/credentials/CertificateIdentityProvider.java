/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
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
import java.security.GeneralSecurityException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

/**
 * Credential provider using <a
 * href="https://github.com/minio/minio/blob/master/docs/sts/tls.md">AssumeRoleWithCertificate
 * API</a>.
 */
public class CertificateIdentityProvider extends AssumeRoleBaseProvider {
  private static final RequestBody EMPTY_BODY =
      RequestBody.create(new byte[] {}, MediaType.parse("application/octet-stream"));
  private final Request request;

  public CertificateIdentityProvider(
      @Nonnull String stsEndpoint,
      @Nullable SSLSocketFactory sslSocketFactory,
      @Nullable X509TrustManager trustManager,
      @Nullable Integer durationSeconds,
      @Nullable OkHttpClient customHttpClient)
      throws GeneralSecurityException, IOException {
    super(customHttpClient, sslSocketFactory, trustManager);
    stsEndpoint = Objects.requireNonNull(stsEndpoint, "STS endpoint cannot be empty");
    HttpUrl url = Objects.requireNonNull(HttpUrl.parse(stsEndpoint), "Invalid STS endpoint");
    if (!url.isHttps()) {
      throw new IllegalArgumentException("STS endpoint scheme must be HTTPS");
    }

    HttpUrl.Builder urlBuilder =
        newUrlBuilder(
            url,
            "AssumeRoleWithCertificate",
            getValidDurationSeconds(durationSeconds),
            null,
            null,
            null);
    url = urlBuilder.build();
    this.request = new Request.Builder().url(url).method("POST", EMPTY_BODY).build();
  }

  @Override
  protected Request getRequest() {
    return this.request;
  }

  @Override
  protected Class<? extends AssumeRoleBaseProvider.Response> getResponseClass() {
    return CertificateIdentityResponse.class;
  }

  /** Object representation of response XML of AssumeRoleWithCertificate API. */
  @Root(name = "AssumeRoleWithCertificateResponse", strict = false)
  @Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
  public static class CertificateIdentityResponse implements AssumeRoleBaseProvider.Response {
    @Path(value = "AssumeRoleWithCertificateResult")
    @Element(name = "Credentials")
    private Credentials credentials;

    public Credentials getCredentials() {
      return credentials;
    }
  }
}
