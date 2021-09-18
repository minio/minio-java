/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2021 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.minio.Digest;
import io.minio.MinioClient;
import io.minio.S3Base;
import io.minio.S3Escaper;
import io.minio.Signer;
import io.minio.Time;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.http.Method;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bouncycastle.crypto.InvalidCipherTextException;

/** Client to perform MinIO administration operations. */
public class MinioAdminClient extends S3Base {
  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/octet-stream");
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private MinioAdminClient(
      HttpUrl baseUrl, String region, Provider provider, OkHttpClient httpClient) {
    super(baseUrl, region, false, false, false, false, provider, httpClient);
  }

  private Credentials getCredentials() {
    Credentials creds = provider.fetch();
    if (creds == null) throw new RuntimeException("Credential provider returns null credential");
    return creds;
  }

  private Response execute(
      Method method, Command command, Multimap<String, String> queryParamMap, byte[] body)
      throws InvalidKeyException, IOException, NoSuchAlgorithmException {
    Credentials creds = getCredentials();

    HttpUrl.Builder urlBuilder =
        this.baseUrl
            .newBuilder()
            .host(this.baseUrl.host())
            .addEncodedPathSegments(S3Escaper.encodePath("minio/admin/v3/" + command.toString()));
    if (queryParamMap != null) {
      for (Map.Entry<String, String> entry : queryParamMap.entries()) {
        urlBuilder.addEncodedQueryParameter(
            S3Escaper.encode(entry.getKey()), S3Escaper.encode(entry.getValue()));
      }
    }
    HttpUrl url = urlBuilder.build();

    Request.Builder requestBuilder = new Request.Builder();
    requestBuilder.url(url);
    requestBuilder.header("Host", getHostHeader(url));
    requestBuilder.header("Accept-Encoding", "identity"); // Disable default gzip compression.
    requestBuilder.header("User-Agent", this.userAgent);
    requestBuilder.header("x-amz-date", ZonedDateTime.now().format(Time.AMZ_DATE_FORMAT));
    if (creds.sessionToken() != null) {
      requestBuilder.header("X-Amz-Security-Token", creds.sessionToken());
    }
    if (body == null && (method != Method.GET && method != Method.HEAD)) body = EMPTY_BODY;
    if (body != null) {
      requestBuilder.header("x-amz-content-sha256", Digest.sha256Hash(body, body.length));
      requestBuilder.method(method.toString(), RequestBody.create(body, DEFAULT_MEDIA_TYPE));
    } else {
      requestBuilder.header("x-amz-content-sha256", Digest.ZERO_SHA256_HASH);
    }
    Request request = requestBuilder.build();

    request =
        Signer.signV4S3(
            request,
            region,
            creds.accessKey(),
            creds.secretKey(),
            request.header("x-amz-content-sha256"));

    StringBuilder traceBuilder =
        newTraceBuilder(request, (body == null) ? null : new String(body, StandardCharsets.UTF_8));
    PrintWriter traceStream = this.traceStream;
    if (traceStream != null) traceStream.println(traceBuilder.toString());
    traceBuilder.append("\n");

    OkHttpClient httpClient = this.httpClient;
    Response response = httpClient.newCall(request).execute();

    String trace =
        response.protocol().toString().toUpperCase(Locale.US)
            + " "
            + response.code()
            + "\n"
            + response.headers();
    traceBuilder.append(trace).append("\n");
    if (traceStream != null) {
      traceStream.println(trace);
      ResponseBody responseBody = response.peekBody(1024 * 1024);
      traceStream.println(responseBody.string());
      traceStream.println(END_HTTP);
    }

    if (response.isSuccessful()) return response;

    throw new RuntimeException("Request failed with response: " + response.body().string());
  }

  /**
   * Adds a user with the specified access and secret key.
   *
   * @param accessKey Access key.
   * @param status Status.
   * @param secretKey Secret key.
   * @param policyName Policy name.
   * @param memberOf List of group.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on MinIO REST operation.
   * @throws InvalidCipherTextException thrown to indicate data cannot be encrypted/decrypted.
   */
  public void addUser(
      @Nonnull String accessKey,
      @Nonnull UserInfo.Status status,
      @Nullable String secretKey,
      @Nullable String policyName,
      @Nullable List<String> memberOf)
      throws NoSuchAlgorithmException, InvalidKeyException, IOException,
          InvalidCipherTextException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }
    UserInfo userInfo = new UserInfo(status, secretKey, policyName, memberOf);

    Credentials creds = getCredentials();
    try (Response response =
        execute(
            Method.PUT,
            Command.ADD_USER,
            ImmutableMultimap.of("accessKey", accessKey),
            Crypto.encrypt(creds.secretKey(), OBJECT_MAPPER.writeValueAsBytes(userInfo)))) {}
  }

  /**
   * Obtains a list of all MinIO users.
   *
   * @return List of all users.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on MinIO REST operation.
   * @throws InvalidCipherTextException thrown to indicate data cannot be encrypted/decrypted.
   */
  public Map<String, UserInfo> listUsers()
      throws NoSuchAlgorithmException, InvalidKeyException, IOException,
          InvalidCipherTextException {
    try (Response response = execute(Method.GET, Command.LIST_USERS, null, null)) {
      Credentials creds = getCredentials();
      byte[] jsonData = Crypto.decrypt(creds.secretKey(), response.body().bytes());
      MapType mapType =
          OBJECT_MAPPER
              .getTypeFactory()
              .constructMapType(HashMap.class, String.class, UserInfo.class);
      return OBJECT_MAPPER.readValue(jsonData, mapType);
    }
  }

  /**
   * Deletes a user by it's access key
   *
   * @param accessKey Access Key.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on MinIO REST operation.
   */
  public void deleteUser(@Nonnull String accessKey)
      throws NoSuchAlgorithmException, InvalidKeyException, IOException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }

    try (Response response =
        execute(
            Method.DELETE,
            Command.REMOVE_USER,
            ImmutableMultimap.of("accessKey", accessKey),
            null)) {}
  }

  /**
   * Creates a policy.
   *
   * <pre>Example:{@code
   * // Assume policyJson contains below JSON string;
   * // {
   * //     "Statement": [
   * //         {
   * //             "Action": "s3:GetObject",
   * //             "Effect": "Allow",
   * //             "Principal": "*",
   * //             "Resource": "arn:aws:s3:::my-bucketname/myobject*"
   * //         }
   * //     ],
   * //     "Version": "2012-10-17"
   * // }
   * //
   * client.addCannedPolicy("my-policy-name", policyJson);
   * }</pre>
   *
   * @param name Policy name.
   * @param policy Policy as JSON string.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on MinIO REST operation.
   */
  public void addCannedPolicy(@Nonnull String name, @Nonnull String policy)
      throws NoSuchAlgorithmException, InvalidKeyException, IOException {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must be provided");
    }
    if (policy == null || policy.isEmpty()) {
      throw new IllegalArgumentException("policy must be provided");
    }

    try (Response response =
        execute(
            Method.PUT,
            Command.ADD_CANNED_POLICY,
            ImmutableMultimap.of("name", name),
            policy.getBytes(StandardCharsets.UTF_8))) {}
  }

  /**
   * Sets a policy to a given user or group.
   *
   * @param userOrGroupName User/Group name.
   * @param isGroup Flag to denote userOrGroupName is a group name.
   * @param policyName Policy name.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on MinIO REST operation.
   */
  public void setPolicy(
      @Nonnull String userOrGroupName, boolean isGroup, @Nonnull String policyName)
      throws NoSuchAlgorithmException, InvalidKeyException, IOException {
    if (userOrGroupName == null || userOrGroupName.isEmpty()) {
      throw new IllegalArgumentException("user/group name must be provided");
    }
    if (policyName == null || policyName.isEmpty()) {
      throw new IllegalArgumentException("policy name must be provided");
    }

    try (Response response =
        execute(
            Method.PUT,
            Command.SET_USER_OR_GROUP_POLICY,
            ImmutableMultimap.of(
                "userOrGroup",
                userOrGroupName,
                "isGroup",
                String.valueOf(isGroup),
                "policyName",
                policyName),
            null)) {}
  }

  /**
   * Lists all configured canned policies.
   *
   * @return Map of policies, keyed by their name, with their actual policy as their value.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on MinIO REST operation.
   */
  public Map<String, String> listCannedPolicies()
      throws NoSuchAlgorithmException, InvalidKeyException, IOException {
    try (Response response = execute(Method.GET, Command.LIST_CANNED_POLICIES, null, null)) {
      MapType mapType =
          OBJECT_MAPPER
              .getTypeFactory()
              .constructMapType(HashMap.class, String.class, Object.class);
      return OBJECT_MAPPER.readValue(response.body().bytes(), mapType);
    }
  }

  /**
   * Removes canned policy by name.
   *
   * @param name Policy name.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on MinIO REST operation.
   */
  public void removeCannedPolicy(@Nonnull String name)
      throws NoSuchAlgorithmException, InvalidKeyException, IOException {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must be provided");
    }

    try (Response response =
        execute(
            Method.DELETE,
            Command.REMOVE_CANNED_POLICY,
            ImmutableMultimap.of("name", name),
            null)) {}
  }

  public static MinioAdminClient.Builder builder() {
    return new MinioAdminClient.Builder();
  }

  public static final class Builder extends MinioClient.BaseBuilder<MinioAdminClient> {
    public MinioAdminClient build() {
      validateNotNull(baseUrl, "endpoint");
      maybeSetHttpClient();

      return new MinioAdminClient(
          baseUrl, (region != null) ? region : US_EAST_1, provider, httpClient);
    }
  }

  private enum Command {
    ADD_USER("add-user"),
    LIST_USERS("list-users"),
    REMOVE_USER("remove-user"),
    ADD_CANNED_POLICY("add-canned-policy"),
    SET_USER_OR_GROUP_POLICY("set-user-or-group-policy"),
    LIST_CANNED_POLICIES("list-canned-policies"),
    REMOVE_CANNED_POLICY("remove-canned-policy");
    private final String value;

    private Command(String value) {
      this.value = value;
    }

    public String toString() {
      return this.value;
    }
  }
}
