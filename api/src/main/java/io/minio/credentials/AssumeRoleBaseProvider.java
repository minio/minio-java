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
import java.security.GeneralSecurityException;
import java.security.ProviderException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;

/** Base class to AssumeRole based providers. */
public abstract class AssumeRoleBaseProvider implements Provider {
  public static final int DEFAULT_DURATION_SECONDS = (int) TimeUnit.HOURS.toSeconds(1);
  private final OkHttpClient httpClient;
  private Credentials credentials;

  public AssumeRoleBaseProvider(OkHttpClient customHttpClient) {
    // HTTP/1.1 is only supported in default client because of HTTP/2 in OkHttpClient cause 5
    // minutes timeout on program exit.
    this.httpClient =
        (customHttpClient != null)
            ? customHttpClient
            : new OkHttpClient().newBuilder().protocols(Arrays.asList(Protocol.HTTP_1_1)).build();
  }

  public AssumeRoleBaseProvider(
      OkHttpClient customHttpClient,
      SSLSocketFactory sslSocketFactory,
      X509TrustManager trustManager)
      throws GeneralSecurityException, IOException {
    if (customHttpClient != null) {
      if (sslSocketFactory != null || trustManager != null) {
        throw new IllegalArgumentException(
            "Either sslSocketFactory/trustManager or custom HTTP client must be provided");
      }

      this.httpClient = customHttpClient;
    } else {
      if (sslSocketFactory == null || trustManager == null) {
        throw new IllegalArgumentException(
            "Both sslSocketFactory and trustManager must be provided");
      }

      // HTTP/1.1 is only supported in default client because of HTTP/2 in OkHttpClient cause 5
      // minutes timeout on program exit.
      this.httpClient =
          new OkHttpClient()
              .newBuilder()
              .protocols(Arrays.asList(Protocol.HTTP_1_1))
              .sslSocketFactory(sslSocketFactory, trustManager)
              .build();
    }
  }

  @Override
  public synchronized Credentials fetch() {
    if (credentials != null && !credentials.isExpired()) {
      return credentials;
    }

    try (okhttp3.Response response = httpClient.newCall(getRequest()).execute()) {
      if (!response.isSuccessful()) {
        throw new ProviderException("STS service failed with HTTP status code " + response.code());
      }

      credentials = parseResponse(response);
      return credentials;
    } catch (XmlParserException | IOException e) {
      throw new ProviderException("Unable to parse STS response", e);
    }
  }

  protected static int getValidDurationSeconds(Integer duration) {
    return (duration != null && duration > DEFAULT_DURATION_SECONDS)
        ? duration
        : DEFAULT_DURATION_SECONDS;
  }

  protected HttpUrl.Builder newUrlBuilder(
      HttpUrl url,
      String action,
      int durationSeconds,
      String policy,
      String roleArn,
      String roleSessionName) {
    HttpUrl.Builder urlBuilder =
        url.newBuilder()
            .addQueryParameter("Action", action)
            .addQueryParameter("Version", "2011-06-15");

    if (durationSeconds > 0) {
      urlBuilder.addQueryParameter("DurationSeconds", String.valueOf(durationSeconds));
    }

    if (policy != null) {
      urlBuilder.addQueryParameter("Policy", policy);
    }

    if (roleArn != null) {
      urlBuilder.addQueryParameter("RoleArn", roleArn);
    }

    if (roleSessionName != null) {
      urlBuilder.addQueryParameter("RoleSessionName", roleSessionName);
    }

    return urlBuilder;
  }

  protected Credentials parseResponse(okhttp3.Response response)
      throws XmlParserException, IOException {
    Response result = Xml.unmarshal(getResponseClass(), response.body().charStream());
    return result.getCredentials();
  }

  protected abstract Request getRequest();

  protected abstract Class<? extends AssumeRoleBaseProvider.Response> getResponseClass();

  public static interface Response {
    public Credentials getCredentials();
  }
}
