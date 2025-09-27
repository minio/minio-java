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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.credentials.Jwt;
import io.minio.credentials.Provider;
import io.minio.credentials.WebIdentityProvider;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.ProviderException;
import java.util.Objects;
import javax.annotation.Nonnull;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MinioClientWithWebIdentityProvider {
  static Jwt getJwt(
      @Nonnull String clientId,
      @Nonnull String clientSecret,
      @Nonnull String idpClientId,
      @Nonnull String idpEndpoint) {
    Objects.requireNonNull(clientId, "Client id must not be null");
    Objects.requireNonNull(clientSecret, "ClientSecret must not be null");

    RequestBody requestBody =
        new FormBody.Builder()
            .add("username", clientId)
            .add("password", clientSecret)
            .add("grant_type", "password")
            .add("client_id", idpClientId)
            .build();

    Request request = new Request.Builder().url(idpEndpoint).post(requestBody).build();

    OkHttpClient client = new OkHttpClient();
    try (Response response = client.newCall(request).execute()) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      mapper.setVisibility(
          VisibilityChecker.Std.defaultInstance()
              .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
      return mapper.readValue(response.body().charStream(), Jwt.class);
    } catch (IOException e) {
      throw new ProviderException(e);
    }
  }

  public static void main(String[] args) throws MinioException {
    // IDP endpoint.
    String idpEndpoint =
        "https://IDP-HOST:IDP-PORT/auth/realms/master/protocol/openid-connect/token";

    // Client-ID to fetch JWT.
    String clientId = "USER-ID";

    // Client secret to fetch JWT.
    String clientSecret = "PASSWORD";

    // Client-ID of MinIO service on IDP.
    String idpClientId = "MINIO-CLIENT-ID";

    // STS endpoint usually point to MinIO server.
    String stsEndpoint = "http://STS-HOST:STS-PORT/";

    // Role ARN if available.
    String roleArn = "ROLE-ARN";

    // Role session name if available.
    String roleSessionName = "ROLE-SESSION-NAME";

    Provider provider =
        new WebIdentityProvider(
            () -> getJwt(clientId, clientSecret, idpClientId, idpEndpoint),
            stsEndpoint,
            null,
            null,
            roleArn,
            roleSessionName,
            null);

    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://MINIO-HOST:MINIO-PORT")
            .credentialsProvider(provider)
            .build();

    // Get information of an object.
    StatObjectResponse stat =
        minioClient.statObject(
            StatObjectArgs.builder().bucket("my-bucket").object("my-object").build());
    System.out.println(stat);
  }
}
