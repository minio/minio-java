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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.messages.ResponseDate;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProviderException;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Credential provider using <a
 * href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html">IAM roles
 * for Amazon EC2</a>.
 */
public class IamAwsProvider extends EnvironmentProvider {
  // Custom endpoint to fetch IAM role credentials.
  private final HttpUrl customEndpoint;
  private final OkHttpClient httpClient;
  private final ObjectMapper mapper;
  private Credentials credentials;

  public IamAwsProvider(@Nullable String customEndpoint, @Nullable OkHttpClient customHttpClient) {
    this.customEndpoint =
        (customEndpoint != null)
            ? Objects.requireNonNull(HttpUrl.parse(customEndpoint), "Invalid custom endpoint")
            : null;
    // HTTP/1.1 is only supported in default client because of HTTP/2 in OkHttpClient cause 5
    // minutes timeout on program exit.
    this.httpClient =
        (customHttpClient != null)
            ? customHttpClient
            : new OkHttpClient().newBuilder().protocols(Arrays.asList(Protocol.HTTP_1_1)).build();
    this.mapper = new ObjectMapper();
    this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
  }

  private void checkLoopbackHost(HttpUrl url) {
    try {
      for (InetAddress addr : InetAddress.getAllByName(url.host())) {
        if (!addr.isLoopbackAddress()) {
          throw new ProviderException(url.host() + " is not loopback only host");
        }
      }
    } catch (UnknownHostException e) {
      throw new ProviderException("Host in " + url + " is not loopback address");
    }
  }

  private Credentials fetchCredentials(String tokenFile) {
    HttpUrl url = this.customEndpoint;
    if (url == null) {
      String region = getProperty("AWS_REGION");
      url =
          HttpUrl.parse(
              (region == null)
                  ? "https://sts.amazonaws.com"
                  : "https://sts." + region + ".amazonaws.com");
    }

    Provider provider =
        new WebIdentityProvider(
            () -> {
              try {
                byte[] data = Files.readAllBytes(Paths.get(tokenFile));
                return new Jwt(new String(data, StandardCharsets.UTF_8), 0);
              } catch (IOException e) {
                throw new ProviderException("Error in reading file " + tokenFile, e);
              }
            },
            url.toString(),
            null,
            null,
            getProperty("AWS_ROLE_ARN"),
            getProperty("AWS_ROLE_SESSION_NAME"),
            httpClient);
    credentials = provider.fetch();
    return credentials;
  }

  private Credentials fetchCredentials(HttpUrl url) {
    try (Response response =
        httpClient.newCall(new Request.Builder().url(url).method("GET", null).build()).execute()) {
      if (!response.isSuccessful()) {
        throw new ProviderException(url + " failed with HTTP status code " + response.code());
      }

      EcsCredentials creds = mapper.readValue(response.body().charStream(), EcsCredentials.class);
      if (!"Success".equals(creds.code())) {
        throw new ProviderException(url + " failed with message " + creds.message());
      }
      return creds.toCredentials();
    } catch (IOException e) {
      throw new ProviderException("Unable to parse response", e);
    }
  }

  private String getIamRoleName(HttpUrl url) {
    String[] roleNames = null;
    try (Response response =
        httpClient.newCall(new Request.Builder().url(url).method("GET", null).build()).execute()) {
      if (!response.isSuccessful()) {
        throw new ProviderException(url + " failed with HTTP status code " + response.code());
      }

      roleNames = response.body().string().split("\\R");
    } catch (IOException e) {
      throw new ProviderException("Unable to parse response", e);
    }

    if (roleNames.length == 0) {
      throw new ProviderException("No IAM roles attached to EC2 service " + url);
    }

    return roleNames[0];
  }

  private HttpUrl getIamRoleNamedUrl() {
    HttpUrl url = this.customEndpoint;
    if (url == null) {
      url = HttpUrl.parse("http://169.254.169.254/latest/meta-data/iam/security-credentials/");
    } else {
      url =
          new HttpUrl.Builder()
              .scheme(url.scheme())
              .host(url.host())
              .addPathSegments("latest/meta-data/iam/security-credentials/")
              .build();
    }

    String roleName = getIamRoleName(url);
    return url.newBuilder().addPathSegment(roleName).build();
  }

  @Override
  public synchronized Credentials fetch() {
    if (credentials != null && !credentials.isExpired()) {
      return credentials;
    }

    HttpUrl url = this.customEndpoint;
    String tokenFile = getProperty("AWS_WEB_IDENTITY_TOKEN_FILE");
    if (tokenFile != null) {
      credentials = fetchCredentials(tokenFile);
      return credentials;
    }

    if (getProperty("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI") != null) {
      if (url == null) {
        url =
            new HttpUrl.Builder()
                .scheme("http")
                .host("169.254.170.2")
                .addPathSegments(getProperty("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI"))
                .build();
      }
    } else if (getProperty("AWS_CONTAINER_CREDENTIALS_FULL_URI") != null) {
      if (url == null) {
        url = HttpUrl.parse(getProperty("AWS_CONTAINER_CREDENTIALS_FULL_URI"));
      }
      checkLoopbackHost(url);
    } else {
      url = getIamRoleNamedUrl();
    }

    credentials = fetchCredentials(url);
    return credentials;
  }

  public static class EcsCredentials {
    @JsonProperty("AccessKeyID")
    private String accessKey;

    @JsonProperty("SecretAccessKey")
    private String secretKey;

    @JsonProperty("Token")
    private String sessionToken;

    @JsonProperty("Expiration")
    private ResponseDate expiration;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Message")
    private String message;

    public String code() {
      return this.code;
    }

    public String message() {
      return this.message;
    }

    public Credentials toCredentials() {
      return new Credentials(accessKey, secretKey, sessionToken, expiration);
    }
  }
}
