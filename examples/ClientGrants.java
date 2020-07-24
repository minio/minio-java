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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import io.minio.MinioClient;
import io.minio.credentials.ClientGrantsProvider;
import io.minio.credentials.Provider;
import io.minio.messages.Bucket;
import io.minio.messages.ClientGrantsToken;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ClientGrants {

  private static final String POLICY =
      new StringBuilder()
          .append("{\n")
          .append("    \"Statement\": [\n")
          .append("       " + " {\n")
          .append("            \"Action\": [\n")
          .append("                \"s3:GetBucketLocation\",\n")
          .append("                \"s3:ListBucket\"\n")
          .append("            ],\n")
          .append("            \"Effect\": \"Allow\",\n")
          .append("            \"Principal\": \"*\",\n")
          .append("            \"Resource\": \"arn:aws:s3:::test\"\n")
          .append("        }\n")
          .append("    ],\n")
          .append("    \"Version\": \"2012-10-17\"\n")
          .append("}\n")
          .toString();

  static class JwtToken {

    @JsonProperty("access_token")
    private final String accessToken;

    @JsonProperty("expires_in")
    private final long expiredAfter;

    @ConstructorProperties({"access_token", "expires_in"})
    public JwtToken(String accessToken, long expiredAfter) {
      this.accessToken = accessToken;
      this.expiredAfter = expiredAfter;
    }
  }

  static ClientGrantsToken getTokenAndExpiry(
      @Nonnull String clientId,
      @Nonnull String clientSecret,
      @Nonnull String idpClientId,
      @Nonnull String idpEndpoint) {
    Objects.requireNonNull(clientId, "Client id must not be null");
    Objects.requireNonNull(clientSecret, "ClientSecret must not be null");

    final RequestBody requestBody =
        new FormBody.Builder()
            .add("username", clientId)
            .add("password", clientSecret)
            .add("grant_type", "password")
            .add("client_id", idpClientId)
            .build();

    final Request request = new Request.Builder().url(idpEndpoint).post(requestBody).build();

    final OkHttpClient client = new OkHttpClient();
    try (Response response = client.newCall(request).execute()) {
      final ObjectMapper mapper = new ObjectMapper();
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      mapper.setVisibility(
          VisibilityChecker.Std.defaultInstance()
              .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

      final JwtToken jwtToken =
          mapper.readValue(Objects.requireNonNull(response.body()).charStream(), JwtToken.class);
      return new ClientGrantsToken(jwtToken.accessToken, jwtToken.expiredAfter, POLICY);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void main(String[] args) throws Exception {
    final String clientId = "user";
    final String clientSecret = "password";
    final String idpEndpoint =
        "http://idp-host:idp-port/auth/realms/master/protocol/openid-connect/token";
    // STS endpoint usually points to MinIO endpoint in case of MinIO
    final String stsEndpoint = "http://sts-host:sts-port/";
    // client id for minio on idp
    final String idpClientId = "minio-client-id";

    final Provider credentialsProvider =
        new ClientGrantsProvider(
            stsEndpoint, () -> getTokenAndExpiry(clientId, clientSecret, idpClientId, idpEndpoint));

    final MinioClient minioClient =
        MinioClient.builder()
            .endpoint("http://minio-host:minio-port")
            .credentialsProvider(credentialsProvider)
            .build();

    final List<Bucket> buckets = minioClient.listBuckets();
    for (Bucket bucket : buckets) {
      System.out.print(bucket.name() + " created at ");
      System.out.println(bucket.creationDate());
    }
  }
}
