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
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.minio.Http;
import io.minio.Time;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProviderException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Credential provider using <a
 * href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html">IAM roles
 * for Amazon EC2</a>, <a
 * href="https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html">IAM
 * roles for service accounts</a>, ECS task roles and <a
 * href="https://docs.aws.amazon.com/eks/latest/userguide/pod-identities.html">EKS Pod Identity</a>.
 */
public class IamAwsProvider extends EnvironmentProvider {
  // Hosts of ECS task metadata and EKS Pod Identity Agent credential endpoints. AWS allows these
  // link-local addresses in AWS_CONTAINER_CREDENTIALS_FULL_URI in addition to loopback addresses.
  private static final Set<InetAddress> ALLOWED_CONTAINER_ADDRESSES =
      allowedContainerAddresses("169.254.170.2", "169.254.170.23", "fd00:ec2::23");

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
    this.mapper =
        JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .build();
  }

  private static Set<InetAddress> allowedContainerAddresses(String... hosts) {
    Set<InetAddress> addresses = new HashSet<>();
    for (String host : hosts) {
      try {
        // Literal IP addresses are parsed without doing a DNS lookup.
        addresses.add(InetAddress.getByName(host));
      } catch (UnknownHostException e) {
        throw new ProviderException("Unable to parse address " + host, e);
      }
    }
    return Collections.unmodifiableSet(addresses);
  }

  /**
   * Check the host of container credential endpoint is allowed as done by AWS; it must either
   * resolve to loopback addresses only, resolve to the ECS or EKS Pod Identity link-local addresses
   * only, or be accessed over HTTPS.
   */
  private void checkContainerHost(HttpUrl url) {
    if (url.isHttps()) return;

    InetAddress[] addrs;
    try {
      addrs = InetAddress.getAllByName(url.host());
    } catch (UnknownHostException e) {
      throw new ProviderException("Host in " + url + " is not loopback address", e);
    }

    boolean loopback = addrs.length > 0;
    boolean allowed = addrs.length > 0;
    for (InetAddress addr : addrs) {
      loopback = loopback && addr.isLoopbackAddress();
      allowed = allowed && ALLOWED_CONTAINER_ADDRESSES.contains(addr);
    }

    if (!loopback && !allowed) {
      throw new ProviderException(
          url.host()
              + " is neither loopback only host nor ECS/EKS credential endpoint; use HTTPS scheme"
              + " to fetch credentials from other hosts");
    }
  }

  /**
   * Get authorization token of container credential endpoint.
   * AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE takes precedence over AWS_CONTAINER_AUTHORIZATION_TOKEN
   * as done by AWS; EKS Pod Identity sets the former only.
   */
  private String containerAuthorizationToken() {
    String filename = getProperty("AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE");
    if (filename == null) return getProperty("AWS_CONTAINER_AUTHORIZATION_TOKEN");

    try {
      byte[] data = Files.readAllBytes(Paths.get(filename));
      // The token is passed as HTTP header value; trailing newline in the file must go away.
      return new String(data, StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      throw new ProviderException("Error in reading file " + filename, e);
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

  private Credentials fetchCredentials(HttpUrl url, String tokenHeader, String token) {
    Request.Builder builder = new Request.Builder().url(url).method("GET", null);
    if (token != null && !token.isEmpty()) builder.header(tokenHeader, token);
    try (Response response = httpClient.newCall(builder.build()).execute()) {
      if (!response.isSuccessful()) {
        throw new ProviderException(url + " failed with HTTP status code " + response.code());
      }

      EcsCredentials creds = mapper.readValue(response.body().charStream(), EcsCredentials.class);
      if (creds.code() != null && !creds.code().equals("Success")) {
        throw new ProviderException(
            url + " failed with code " + creds.code() + " and message " + creds.message());
      }
      return creds.toCredentials();
    } catch (IOException e) {
      throw new ProviderException("Unable to parse response", e);
    }
  }

  private String fetchImdsToken() {
    HttpUrl url = this.customEndpoint;
    if (url == null) {
      url = HttpUrl.parse("http://169.254.169.254/latest/api/token");
    } else {
      url =
          new HttpUrl.Builder()
              .scheme(url.scheme())
              .host(url.host())
              .port(url.port())
              .addPathSegments("latest/api/token")
              .build();
    }
    String token = "";
    Request request =
        new Request.Builder()
            .url(url)
            .method("PUT", RequestBody.create(new byte[] {}, null))
            .header("X-aws-ec2-metadata-token-ttl-seconds", "21600")
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (response.isSuccessful()) token = response.body().string();
    } catch (IOException e) {
      token = "";
    }
    return token;
  }

  private String getIamRoleName(HttpUrl url, String token) {
    String[] roleNames = null;
    Request.Builder builder = new Request.Builder().url(url).method("GET", null);
    if (token != null && !token.isEmpty()) builder.header("X-aws-ec2-metadata-token", token);
    try (Response response = httpClient.newCall(builder.build()).execute()) {
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

  private HttpUrl getIamRoleNamedUrl(String token) {
    HttpUrl url = this.customEndpoint;
    if (url == null) {
      url = HttpUrl.parse("http://169.254.169.254/latest/meta-data/iam/security-credentials/");
    } else {
      url =
          new HttpUrl.Builder()
              .scheme(url.scheme())
              .host(url.host())
              .port(url.port())
              .addPathSegments("latest/meta-data/iam/security-credentials/")
              .build();
    }

    String roleName = getIamRoleName(url, token);
    return url.newBuilder().addPathSegment(roleName).build();
  }

  @Override
  public synchronized Credentials fetch() {
    if (credentials != null && !credentials.isExpired()) return credentials;

    HttpUrl url = this.customEndpoint;
    String tokenFile = getProperty("AWS_WEB_IDENTITY_TOKEN_FILE");
    if (tokenFile != null) {
      credentials = fetchCredentials(tokenFile);
      return credentials;
    }

    String tokenHeader = Http.Headers.AUTHORIZATION;
    String token;
    String relativeUri = getProperty("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI");
    String fullUri = getProperty("AWS_CONTAINER_CREDENTIALS_FULL_URI");
    if (relativeUri != null) {
      token = containerAuthorizationToken();
      if (url == null) {
        // AWS sets the value with leading slash and it may have query parameters; parsing the
        // whole URI avoids the empty path segment added by HttpUrl.Builder.addPathSegments().
        url =
            HttpUrl.parse(
                "http://169.254.170.2"
                    + (relativeUri.startsWith("/") ? relativeUri : "/" + relativeUri));
        if (url == null) {
          throw new ProviderException(
              "Invalid AWS_CONTAINER_CREDENTIALS_RELATIVE_URI value " + relativeUri);
        }
      }
    } else if (fullUri != null) {
      token = containerAuthorizationToken();
      if (url == null) {
        url = HttpUrl.parse(fullUri);
        if (url == null) {
          throw new ProviderException(
              "Invalid AWS_CONTAINER_CREDENTIALS_FULL_URI value " + fullUri);
        }
      }
      checkContainerHost(url);
    } else {
      token = fetchImdsToken();
      tokenHeader = "X-aws-ec2-metadata-token";
      url = getIamRoleNamedUrl(token);
    }

    credentials = fetchCredentials(url, tokenHeader, token);
    return credentials;
  }

  /** ECS Credentials of {@link IamAwsProvider}. */
  public static class EcsCredentials {
    @JsonProperty("AccessKeyID")
    private String accessKey;

    @JsonProperty("SecretAccessKey")
    private String secretKey;

    @JsonProperty("Token")
    private String sessionToken;

    @JsonProperty("Expiration")
    private Time.S3Time expiration;

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
