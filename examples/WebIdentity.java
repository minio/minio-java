package io.minio.http;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import io.minio.MinioClient;
import io.minio.credentials.CredentialsProvider;
import io.minio.credentials.WebIdentityCredentialsProvider;
import io.minio.messages.Bucket;
import io.minio.messages.WebIdentityToken;
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

public class WebIdentity {

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

  @SuppressWarnings({"SameParameterValue", "squid:S1192"})
  static WebIdentityToken getTokenAndExpiry(
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
      return new WebIdentityToken(jwtToken.accessToken, jwtToken.expiredAfter);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("squid:S106")
  public static void main(String[] args) throws Exception {
    final String clientId = "user";
    final String clientSecret = "password";
    final String idpEndpoint =
        "http://npz-01.vm.cmx.ru:8081/auth/realms/master/protocol/openid-connect/token";
    final String stsEndpoint = "http://npz-01.vm.cmx.ru:9000/sts";

    // client id for minio on idp
    final String idpClientId = "minio-client-id";

    final CredentialsProvider credentialsProvider =
        new WebIdentityCredentialsProvider(
            stsEndpoint, () -> getTokenAndExpiry(clientId, clientSecret, idpClientId, idpEndpoint));

    final MinioClient minioClient =
        MinioClient.builder()
            .endpoint("http://npz-01.vm.cmx.ru:9000")
            .credentialsProvider(credentialsProvider)
            .build();

    final List<Bucket> buckets = minioClient.listBuckets();
    for (Bucket bucket : buckets) {
      System.out.print(bucket.name() + " created at ");
      System.out.println(bucket.creationDate());
    }
  }
}
