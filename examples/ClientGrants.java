package io.minio.example;

import java.beans.ConstructorProperties;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

import io.minio.MinioClient;
import io.minio.messages.Bucket;
import io.minio.messages.ClientGrantsToken;
import io.minio.messages.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import static java.util.Objects.requireNonNull;

public class ClientGrants {

    private static String clientName = "user";
    private static String clientSecret = "password";
    private static String idpEndpoint = "http://idp-host:idp-port/auth/realms/master/protocol/openid-connect/token";
    private static String stsEndpoint = "http://minio-host:minio-port/sts";

    // client id for minio on idp
    private static String clientID = "minio-client-id";

    private static String policy = new StringBuilder()
            .append("{\n")
            .append("    \"Statement\": [\n")
            .append("        {\n")
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
            .append("}\n").toString();

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

    private static ClientGrantsToken getTokenAndExpiry() {
        requireNonNull(clientID, "ClientID must not be null");
        requireNonNull(clientSecret, "ClientSecret must not be null");

        final RequestBody requestBody = new FormBody.Builder()
                .add("username", clientName)
                .add("password", clientSecret)
                .add("grant_type", "password")
                .add("client_id", clientID)
                .build();

        final Request request = new Request.Builder().url(idpEndpoint)
                .post(requestBody)
                .build();

        final OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(Visibility.ANY));

            final JwtToken jwtToken = mapper.readValue(requireNonNull(response.body()).charStream(), JwtToken.class);
            return new ClientGrantsToken(jwtToken.accessToken, jwtToken.expiredAfter);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    // todo: parse cmd to get user/password etc..
    public static void main(String[] args) throws Exception {

        final MinioClient minioClient = MinioClient.builder()
                .endpoint("http://minio-host:minio-port")
                .stsEndpoint("http://minio-host:minio-port/sts").build();

        final ClientGrantsToken clientGrantsToken = getTokenAndExpiry();
        final Credentials clientGrants = minioClient.newSTSClientGrants(clientGrantsToken, policy);
        System.out.println(clientGrants);
        minioClient.withCredentials(clientGrants);
        List<Bucket> buckets = minioClient.listBuckets();
        for (Bucket bucket : buckets) {
            System.out.print(bucket.name() + " ");
            System.out.println(bucket.creationDate());
        }

        final Credentials refreshedCredentials = minioClient.newSTSClientGrants(getTokenAndExpiry());
        System.out.println(refreshedCredentials);
        minioClient.withCredentials(refreshedCredentials);
        buckets = minioClient.listBuckets();
        for (Bucket bucket : buckets) {
            System.out.print(bucket.name() + " ");
            System.out.println(bucket.creationDate());
        }
    }

}
