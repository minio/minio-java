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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.Checksum;
import io.minio.Http;
import io.minio.Signer;
import io.minio.Time;
import io.minio.Utils;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Client to perform MinIO administration operations. */
public class MinioAdminClient {
  private enum Command {
    ADD_USER("add-user"),
    USER_INFO("user-info"),
    LIST_USERS("list-users"),
    REMOVE_USER("remove-user"),
    ADD_CANNED_POLICY("add-canned-policy"),
    SET_USER_OR_GROUP_POLICY("set-user-or-group-policy"),
    LIST_CANNED_POLICIES("list-canned-policies"),
    REMOVE_CANNED_POLICY("remove-canned-policy"),
    SET_BUCKET_QUOTA("set-bucket-quota"),
    GET_BUCKET_QUOTA("get-bucket-quota"),
    DATA_USAGE_INFO("datausageinfo"),
    ADD_UPDATE_REMOVE_GROUP("update-group-members"),
    GROUP_INFO("group"),
    LIST_GROUPS("groups"),
    INFO("info"),
    ADD_SERVICE_ACCOUNT("add-service-account"),
    UPDATE_SERVICE_ACCOUNT("update-service-account"),
    LIST_SERVICE_ACCOUNTS("list-service-accounts"),
    DELETE_SERVICE_ACCOUNT("delete-service-account"),
    INFO_SERVICE_ACCOUNT("info-service-account"),
    IDP_BUILTIN_POLICY_ATTACH("idp/builtin/policy/attach"),
    IDP_BUILTIN_POLICY_DETACH("idp/builtin/policy/detach");
    private final String value;

    private Command(String value) {
      this.value = value;
    }

    public String toString() {
      return this.value;
    }
  }

  private static final long DEFAULT_CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/octet-stream");
  private static final ObjectMapper OBJECT_MAPPER =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();

  private static final Pattern SERVICE_ACCOUNT_NAME_REGEX =
      Pattern.compile("^(?!-)(?!_)[a-z_\\d-]{1,31}(?<!-)(?<!_)$", Pattern.CASE_INSENSITIVE);

  static {
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  private String userAgent = Utils.getDefaultUserAgent();
  private PrintWriter traceStream;

  private HttpUrl baseUrl;
  private String region;
  private Provider provider;
  private OkHttpClient httpClient;

  private MinioAdminClient(
      HttpUrl baseUrl, String region, Provider provider, OkHttpClient httpClient) {
    this.baseUrl = baseUrl;
    this.region = region;
    this.provider = provider;
    this.httpClient = httpClient;
  }

  private Credentials getCredentials() {
    Credentials creds = provider.fetch();
    if (creds == null) throw new RuntimeException("Credential provider returns null credential");
    return creds;
  }

  private Response httpExecute(
      Http.Method method, Command command, Multimap<String, String> queryParamMap, byte[] body)
      throws IOException, MinioException {
    Credentials creds = getCredentials();

    HttpUrl.Builder urlBuilder =
        this.baseUrl
            .newBuilder()
            .host(this.baseUrl.host())
            .addEncodedPathSegments(Utils.encodePath("minio/admin/v3/" + command.toString()));
    if (queryParamMap != null) {
      for (Map.Entry<String, String> entry : queryParamMap.entries()) {
        urlBuilder.addEncodedQueryParameter(
            Utils.encode(entry.getKey()), Utils.encode(entry.getValue()));
      }
    }
    HttpUrl url = urlBuilder.build();

    Request.Builder requestBuilder = new Request.Builder();
    requestBuilder.url(url);
    requestBuilder.header("Host", Utils.getHostHeader(url));
    requestBuilder.header("Accept-Encoding", "identity"); // Disable default gzip compression.
    requestBuilder.header("User-Agent", this.userAgent);
    requestBuilder.header("x-amz-date", ZonedDateTime.now().format(Time.AMZ_DATE_FORMAT));
    if (creds.sessionToken() != null) {
      requestBuilder.header("X-Amz-Security-Token", creds.sessionToken());
    }
    if (body == null && (method != Http.Method.GET && method != Http.Method.HEAD)) {
      body = Utils.EMPTY_BYTE_ARRAY;
    }
    if (body != null) {
      requestBuilder.header(
          "x-amz-content-sha256", Checksum.hexString(Checksum.SHA256.sum(body, 0, body.length)));
      requestBuilder.method(method.toString(), RequestBody.create(body, DEFAULT_MEDIA_TYPE));
    } else {
      requestBuilder.header("x-amz-content-sha256", Checksum.ZERO_SHA256_HASH);
    }
    Request request = requestBuilder.build();

    request =
        Signer.signV4S3(
            request,
            region,
            creds.accessKey(),
            creds.secretKey(),
            request.header("x-amz-content-sha256"));

    PrintWriter traceStream = this.traceStream;
    if (traceStream != null) {
      StringBuilder traceBuilder = new StringBuilder();
      traceBuilder.append("---------START-HTTP---------\n");
      String encodedPath = request.url().encodedPath();
      String encodedQuery = request.url().encodedQuery();
      if (encodedQuery != null) encodedPath += "?" + encodedQuery;
      traceBuilder.append(request.method()).append(" ").append(encodedPath).append(" HTTP/1.1\n");
      traceBuilder.append(
          request
              .headers()
              .toString()
              .replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*")
              .replaceAll("Credential=([^/]+)", "Credential=*REDACTED*"));
      if (body != null) traceBuilder.append("\n").append(new String(body, StandardCharsets.UTF_8));
      traceStream.println(traceBuilder.toString());
    }

    OkHttpClient httpClient = this.httpClient;
    Response response = httpClient.newCall(request).execute();

    if (traceStream != null) {
      String trace =
          response.protocol().toString().toUpperCase(Locale.US)
              + " "
              + response.code()
              + "\n"
              + response.headers();
      traceStream.println(trace);
      ResponseBody responseBody = response.peekBody(1024 * 1024);
      traceStream.println(responseBody.string());
      traceStream.println("----------END-HTTP----------");
    }

    if (response.isSuccessful()) return response;

    throw new RuntimeException("Request failed with response: " + response.body().string());
  }

  private Response execute(
      Http.Method method, Command command, Multimap<String, String> queryParamMap, byte[] body)
      throws MinioException {
    try {
      return httpExecute(method, command, queryParamMap, body);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Adds a user with the specified access and secret key.
   *
   * @param accessKey Access key.
   * @param status Status.
   * @param secretKey Secret key.
   * @param policyName Policy name.
   * @param memberOf List of group.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void addUser(
      @Nonnull String accessKey,
      @Nonnull Status status,
      @Nullable String secretKey,
      @Nullable String policyName,
      @Nullable List<String> memberOf)
      throws MinioException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }
    UserInfo userInfo = new UserInfo(status, secretKey, policyName, memberOf);

    Credentials creds = getCredentials();
    try (Response response =
        execute(
            Http.Method.PUT,
            Command.ADD_USER,
            ImmutableMultimap.of("accessKey", accessKey),
            Crypto.encrypt(OBJECT_MAPPER.writeValueAsBytes(userInfo), creds.secretKey()))) {
    } catch (JsonProcessingException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Obtains user info for a specified MinIO user.
   *
   * @param accessKey Access Key.
   * @return {@link UserInfo} - user info for the specified accessKey.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public UserInfo getUserInfo(String accessKey) throws MinioException {
    try (Response response =
        execute(
            Http.Method.GET,
            Command.USER_INFO,
            ImmutableMultimap.of("accessKey", accessKey),
            null)) {
      byte[] jsonData = response.body().bytes();
      return OBJECT_MAPPER.readValue(jsonData, UserInfo.class);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Obtains a list of all MinIO users.
   *
   * @return {@link Map<String, UserInfo>} - List of all users.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public Map<String, UserInfo> listUsers() throws MinioException {
    try (Response response = execute(Http.Method.GET, Command.LIST_USERS, null, null)) {
      Credentials creds = getCredentials();
      byte[] jsonData = Crypto.decrypt(response.body().byteStream(), creds.secretKey());
      MapType mapType =
          OBJECT_MAPPER
              .getTypeFactory()
              .constructMapType(HashMap.class, String.class, UserInfo.class);
      try {
        return OBJECT_MAPPER.readValue(jsonData, mapType);
      } catch (IOException e) {
        throw new MinioException(e);
      }
    }
  }

  /**
   * Deletes a user by it's access key
   *
   * @param accessKey Access Key.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void deleteUser(@Nonnull String accessKey) throws MinioException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }

    try (Response response =
        execute(
            Http.Method.DELETE,
            Command.REMOVE_USER,
            ImmutableMultimap.of("accessKey", accessKey),
            null)) {}
  }

  /**
   * Adds or updates a group.
   *
   * @param group Group name.
   * @param groupStatus Status.
   * @param members Members of group.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void addUpdateGroup(
      @Nonnull String group, @Nullable Status groupStatus, @Nullable List<String> members)
      throws MinioException {
    if (group == null || group.isEmpty()) {
      throw new IllegalArgumentException("group must be provided");
    }
    AddUpdateRemoveGroupArgs args =
        new AddUpdateRemoveGroupArgs(group, groupStatus, members, false);

    try (Response response =
        execute(
            Http.Method.PUT,
            Command.ADD_UPDATE_REMOVE_GROUP,
            null,
            OBJECT_MAPPER.writeValueAsBytes(args))) {
    } catch (JsonProcessingException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Obtains group info for a specified MinIO group.
   *
   * @param group Group name.
   * @return {@link GetGroupInfoResponse} - group info for the specified group.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public GetGroupInfoResponse getGroupInfo(String group) throws MinioException {
    try (Response response =
        execute(Http.Method.GET, Command.GROUP_INFO, ImmutableMultimap.of("group", group), null)) {
      byte[] jsonData = response.body().bytes();
      return OBJECT_MAPPER.readValue(jsonData, GetGroupInfoResponse.class);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Obtains a list of all MinIO groups.
   *
   * @return {@link List<String>} - List of all groups.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public List<String> listGroups() throws MinioException {
    try (Response response = execute(Http.Method.GET, Command.LIST_GROUPS, null, null)) {
      byte[] jsonData = response.body().bytes();
      CollectionType mapType =
          OBJECT_MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, String.class);
      return OBJECT_MAPPER.readValue(jsonData, mapType);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Removes a group.
   *
   * @param group Group name.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void removeGroup(@Nonnull String group) throws MinioException {
    if (group == null || group.isEmpty()) {
      throw new IllegalArgumentException("group must be provided");
    }
    AddUpdateRemoveGroupArgs args = new AddUpdateRemoveGroupArgs(group, null, null, true);

    try (Response response =
        execute(
            Http.Method.PUT,
            Command.ADD_UPDATE_REMOVE_GROUP,
            null,
            OBJECT_MAPPER.writeValueAsBytes(args))) {
    } catch (JsonProcessingException e) {
      throw new MinioException(e);
    }
  }

  /**
   * set bucket quota size
   *
   * @param bucketName bucketName
   * @param size the capacity of the bucket
   * @param unit the quota unit of the size argument
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void setBucketQuota(@Nonnull String bucketName, long size, @Nonnull QuotaUnit unit)
      throws MinioException {
    Map<String, Object> quotaEntity = new HashMap<>();
    if (size > 0) quotaEntity.put("quotatype", "hard");
    quotaEntity.put("quota", unit.toBytes(size));
    try (Response response =
        execute(
            Http.Method.PUT,
            Command.SET_BUCKET_QUOTA,
            ImmutableMultimap.of("bucket", bucketName),
            OBJECT_MAPPER.writeValueAsBytes(quotaEntity))) {
    } catch (JsonProcessingException e) {
      throw new MinioException(e);
    }
  }

  /**
   * get bucket quota size
   *
   * @param bucketName bucketName
   * @return bytes of bucket
   * @throws MinioException thrown to indicate SDK exception.
   */
  public long getBucketQuota(String bucketName) throws MinioException {
    try (Response response =
        execute(
            Http.Method.GET,
            Command.GET_BUCKET_QUOTA,
            ImmutableMultimap.of("bucket", bucketName),
            null)) {
      MapType mapType =
          OBJECT_MAPPER
              .getTypeFactory()
              .constructMapType(HashMap.class, String.class, JsonNode.class);
      return OBJECT_MAPPER.<Map<String, JsonNode>>readValue(response.body().bytes(), mapType)
          .entrySet().stream()
          .filter(entry -> "quota".equals(entry.getKey()))
          .findFirst()
          .map(entry -> Long.valueOf(entry.getValue().toString()))
          .orElseThrow(() -> new IllegalArgumentException("found not quota"));
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Reset bucket quota
   *
   * @param bucketName bucketName
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void clearBucketQuota(@Nonnull String bucketName) throws MinioException {
    setBucketQuota(bucketName, 0, QuotaUnit.KB);
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
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void addCannedPolicy(@Nonnull String name, @Nonnull String policy) throws MinioException {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must be provided");
    }
    if (policy == null || policy.isEmpty()) {
      throw new IllegalArgumentException("policy must be provided");
    }

    try (Response response =
        execute(
            Http.Method.PUT,
            Command.ADD_CANNED_POLICY,
            ImmutableMultimap.of("name", name),
            policy.getBytes(StandardCharsets.UTF_8))) {}
  }

  /**
   * Sets a policy to a given user or group.
   *
   * @param userOrGroupName User/Group name.
   * @param isGroup Flag to denote userOrGroupName is a group name.
   * @param policyName Policy name or comma separated policy names.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void setPolicy(
      @Nonnull String userOrGroupName, boolean isGroup, @Nonnull String policyName)
      throws MinioException {
    if (userOrGroupName == null || userOrGroupName.isEmpty()) {
      throw new IllegalArgumentException("user/group name must be provided");
    }
    if (policyName == null || policyName.isEmpty()) {
      throw new IllegalArgumentException("policy name must be provided");
    }

    try (Response response =
        execute(
            Http.Method.PUT,
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
   * @return {@link Map<String, String>} - Map of policies, keyed by their name, with their actual
   *     policy as their value.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public Map<String, String> listCannedPolicies() throws MinioException {
    try (Response response = execute(Http.Method.GET, Command.LIST_CANNED_POLICIES, null, null)) {
      MapType mapType =
          OBJECT_MAPPER
              .getTypeFactory()
              .constructMapType(HashMap.class, String.class, JsonNode.class);
      HashMap<String, String> policies = new HashMap<>();
      OBJECT_MAPPER
          .<Map<String, JsonNode>>readValue(response.body().bytes(), mapType)
          .forEach((key, value) -> policies.put(key, value.toString()));
      return policies;
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Removes canned policy by name.
   *
   * @param name Policy name.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void removeCannedPolicy(@Nonnull String name) throws MinioException {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must be provided");
    }

    try (Response response =
        execute(
            Http.Method.DELETE,
            Command.REMOVE_CANNED_POLICY,
            ImmutableMultimap.of("name", name),
            null)) {}
  }

  /**
   * Get server/cluster data usage info
   *
   * @return {@link GetDataUsageInfoResponse}
   * @throws MinioException thrown to indicate SDK exception.
   */
  public GetDataUsageInfoResponse getDataUsageInfo() throws MinioException {
    try (Response response = execute(Http.Method.GET, Command.DATA_USAGE_INFO, null, null)) {
      return OBJECT_MAPPER.readValue(response.body().bytes(), GetDataUsageInfoResponse.class);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Obtains admin info for the Minio server.
   *
   * @return {@link GetServerInfoResponse}
   * @throws MinioException thrown to indicate SDK exception.
   */
  public GetServerInfoResponse getServerInfo() throws MinioException {
    try (Response response = execute(Http.Method.GET, Command.INFO, null, null)) {
      return OBJECT_MAPPER.readValue(response.body().charStream(), GetServerInfoResponse.class);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Creates a new service account belonging to the user sending.
   *
   * @param accessKey Access key.
   * @param secretKey Secret key.
   * @param targetUser Target user.
   * @param policy Policy as map .
   * @param name Service account name.
   * @param description Description for this access key.
   * @param expiration Expiry time.
   * @return {@link Credentials} - Service account info for the specified accessKey.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public Credentials addServiceAccount(
      @Nonnull String accessKey,
      @Nonnull String secretKey,
      @Nullable String targetUser,
      @Nullable Map<String, Object> policy,
      @Nullable String name,
      @Nullable String description,
      @Nullable ZonedDateTime expiration)
      throws MinioException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }
    if (secretKey == null || secretKey.isEmpty()) {
      throw new IllegalArgumentException("secret key must be provided");
    }
    if (name != null && !SERVICE_ACCOUNT_NAME_REGEX.matcher(name).find()) {
      throw new IllegalArgumentException(
          "name must contain non-empty alphanumeric,  underscore and hyphen characters not longer"
              + " than 32 characters");
    }
    if (description != null && description.length() > 256) {
      throw new IllegalArgumentException("description must be at most 256 characters long");
    }

    Map<String, Object> serviceAccount = new HashMap<>();
    serviceAccount.put("accessKey", accessKey);
    serviceAccount.put("secretKey", secretKey);
    if (targetUser != null && !targetUser.isEmpty()) {
      serviceAccount.put("targetUser", targetUser);
    }
    if (policy != null && !policy.isEmpty()) serviceAccount.put("policy", policy);
    if (name != null && !name.isEmpty()) serviceAccount.put("name", name);
    if (description != null && !description.isEmpty()) {
      serviceAccount.put("description", description);
    }
    if (expiration != null) {
      serviceAccount.put("expiration", expiration.format(Time.ISO8601UTC_FORMAT));
    }

    Credentials creds = getCredentials();
    try (Response response =
        execute(
            Http.Method.PUT,
            Command.ADD_SERVICE_ACCOUNT,
            null,
            Crypto.encrypt(OBJECT_MAPPER.writeValueAsBytes(serviceAccount), creds.secretKey()))) {
      byte[] jsonData = Crypto.decrypt(response.body().byteStream(), creds.secretKey());
      return OBJECT_MAPPER.readValue(jsonData, AddServiceAccountResponse.class).credentials();
    } catch (JsonProcessingException e) {
      throw new MinioException(e);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Edit an existing service account.
   *
   * @param accessKey Access key.
   * @param newSecretKey New secret key.
   * @param newPolicy New policy as JSON string .
   * @param newStatus New service account status.
   * @param newName New service account name.
   * @param newDescription New description.
   * @param newExpiration New expiry time.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void updateServiceAccount(
      @Nonnull String accessKey,
      @Nullable String newSecretKey,
      @Nullable Map<String, Object> newPolicy,
      @Nullable boolean newStatus,
      @Nullable String newName,
      @Nullable String newDescription,
      @Nullable ZonedDateTime newExpiration)
      throws MinioException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }
    if (newName != null && !SERVICE_ACCOUNT_NAME_REGEX.matcher(newName).find()) {
      throw new IllegalArgumentException(
          "new name must contain non-empty alphanumeric,  underscore and hyphen characters not"
              + " longer than 32 characters");
    }
    if (newDescription != null && newDescription.length() > 256) {
      throw new IllegalArgumentException("new description must be at most 256 characters long");
    }

    Map<String, Object> serviceAccount = new HashMap<>();
    if (newSecretKey != null && !newSecretKey.isEmpty()) {
      serviceAccount.put("newSecretKey", newSecretKey);
    }
    if (newPolicy != null && !newPolicy.isEmpty()) serviceAccount.put("newPolicy", newPolicy);
    serviceAccount.put("newStatus", newStatus ? "on" : "off");
    if (newName != null && !newName.isEmpty()) serviceAccount.put("newName", newName);
    if (newDescription != null && !newDescription.isEmpty()) {
      serviceAccount.put("newDescription", newDescription);
    }
    if (newExpiration != null) {
      serviceAccount.put("newExpiration", newExpiration.format(Time.ISO8601UTC_FORMAT));
    }

    Credentials creds = getCredentials();
    try (Response response =
        execute(
            Http.Method.POST,
            Command.UPDATE_SERVICE_ACCOUNT,
            ImmutableMultimap.of("accessKey", accessKey),
            Crypto.encrypt(OBJECT_MAPPER.writeValueAsBytes(serviceAccount), creds.secretKey()))) {
    } catch (JsonProcessingException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Deletes a service account by it's access key
   *
   * @param accessKey Access Key.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public void deleteServiceAccount(@Nonnull String accessKey) throws MinioException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }

    try (Response response =
        execute(
            Http.Method.DELETE,
            Command.DELETE_SERVICE_ACCOUNT,
            ImmutableMultimap.of("accessKey", accessKey),
            null)) {}
  }

  /**
   * Obtains a list of minio service account by user name.
   *
   * @param username user name.
   * @return {@link ListServiceAccountResponse} - List of minio service account.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public ListServiceAccountResponse listServiceAccount(@Nonnull String username)
      throws MinioException {
    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("user name must be provided");
    }

    try (Response response =
        execute(
            Http.Method.GET,
            Command.LIST_SERVICE_ACCOUNTS,
            ImmutableMultimap.of("user", username),
            null)) {
      Credentials creds = getCredentials();
      byte[] jsonData = Crypto.decrypt(response.body().byteStream(), creds.secretKey());
      return OBJECT_MAPPER.readValue(jsonData, ListServiceAccountResponse.class);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Obtains service account info for a specified MinIO user.
   *
   * @param accessKey Access Key.
   * @return {@link GetServiceAccountInfoResponse} - Service account info for the specified
   *     accessKey.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public GetServiceAccountInfoResponse getServiceAccountInfo(@Nonnull String accessKey)
      throws MinioException {
    if (accessKey == null || accessKey.isEmpty()) {
      throw new IllegalArgumentException("access key must be provided");
    }
    try (Response response =
        execute(
            Http.Method.GET,
            Command.INFO_SERVICE_ACCOUNT,
            ImmutableMultimap.of("accessKey", accessKey),
            null)) {
      Credentials creds = getCredentials();
      byte[] jsonData = Crypto.decrypt(response.body().byteStream(), creds.secretKey());
      return OBJECT_MAPPER.readValue(jsonData, GetServiceAccountInfoResponse.class);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  private PolicyAssociationResponse attachDetachPolicy(
      @Nonnull Command command,
      @Nonnull String[] polices,
      @Nullable String user,
      @Nullable String group)
      throws MinioException {
    if (!(user != null ^ group != null)) {
      throw new IllegalArgumentException("either user or group must be provided");
    }

    Map<String, Object> map = new HashMap<>();
    map.put("policies", polices);
    if (user != null) {
      map.put("user", user);
    } else {
      map.put("group", group);
    }

    Credentials creds = getCredentials();
    try (Response response =
        execute(
            Http.Method.POST,
            command,
            null,
            Crypto.encrypt(OBJECT_MAPPER.writeValueAsBytes(map), creds.secretKey()))) {
      return OBJECT_MAPPER.readValue(
          Crypto.decrypt(response.body().byteStream(), creds.secretKey()),
          PolicyAssociationResponse.class);
    } catch (JsonProcessingException e) {
      throw new MinioException(e);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  /** Attach policies to a user or group. */
  public PolicyAssociationResponse attachPolicy(
      @Nonnull String[] policies, @Nullable String user, @Nullable String group)
      throws MinioException {
    return attachDetachPolicy(Command.IDP_BUILTIN_POLICY_ATTACH, policies, user, group);
  }

  /** Detach policies from a user or group. */
  public PolicyAssociationResponse detachPolicy(
      @Nonnull String[] policies, @Nullable String user, @Nullable String group)
      throws MinioException {
    return attachDetachPolicy(Command.IDP_BUILTIN_POLICY_DETACH, policies, user, group);
  }

  /**
   * Sets HTTP connect, write and read timeouts. A value of 0 means no timeout, otherwise values
   * must be between 1 and Integer.MAX_VALUE when converted to milliseconds.
   *
   * <pre>Example:{@code
   * minioClient.setTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10),
   *     TimeUnit.SECONDS.toMillis(30));
   * }</pre>
   *
   * @param connectTimeout HTTP connect timeout in milliseconds.
   * @param writeTimeout HTTP write timeout in milliseconds.
   * @param readTimeout HTTP read timeout in milliseconds.
   */
  public void setTimeout(long connectTimeout, long writeTimeout, long readTimeout) {
    this.httpClient = Http.setTimeout(this.httpClient, connectTimeout, writeTimeout, readTimeout);
  }

  /**
   * Ignores check on server certificate for HTTPS connection.
   *
   * <pre>Example:{@code
   * client.ignoreCertCheck();
   * }</pre>
   *
   * @throws MinioException thrown to indicate SDK exception.
   */
  @SuppressFBWarnings(value = "SIC", justification = "Should not be used in production anyways.")
  public void ignoreCertCheck() throws MinioException {
    this.httpClient = Http.disableCertCheck(this.httpClient);
  }

  /**
   * Sets application's name/version to user agent. For more information about user agent refer <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">#rfc2616</a>.
   *
   * @param name Your application name.
   * @param version Your application version.
   */
  public void setAppInfo(String name, String version) {
    if (name == null || version == null) return;
    this.userAgent = Utils.getDefaultUserAgent() + " " + name.trim() + "/" + version.trim();
  }

  /**
   * Enables HTTP call tracing and written to traceStream.
   *
   * @param traceStream {@link OutputStream} for writing HTTP call tracing.
   * @see #traceOff
   */
  public void traceOn(OutputStream traceStream) {
    if (traceStream == null) throw new IllegalArgumentException("trace stream must be provided");
    this.traceStream =
        new PrintWriter(new OutputStreamWriter(traceStream, StandardCharsets.UTF_8), true);
  }

  /**
   * Disables HTTP call tracing previously enabled.
   *
   * @see #traceOn
   */
  public void traceOff() {
    this.traceStream = null;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link MinioAdminClient}. */
  public static final class Builder {
    private HttpUrl baseUrl;
    private String region = "";
    private Provider provider;
    private OkHttpClient httpClient;

    public Builder endpoint(String endpoint) {
      this.baseUrl = Utils.getBaseUrl(endpoint);
      return this;
    }

    public Builder endpoint(String endpoint, int port, boolean secure) {
      HttpUrl url = Utils.getBaseUrl(endpoint);
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("port must be in range of 1 to 65535");
      }

      this.baseUrl = url.newBuilder().port(port).scheme(secure ? "https" : "http").build();
      return this;
    }

    public Builder endpoint(HttpUrl url) {
      Utils.validateNotNull(url, "url");
      Utils.validateUrl(url);

      this.baseUrl = url;
      return this;
    }

    public Builder endpoint(URL url) {
      Utils.validateNotNull(url, "url");
      return endpoint(HttpUrl.get(url));
    }

    public Builder region(String region) {
      Utils.validateNotNull(region, "region");
      this.region = region;
      return this;
    }

    public Builder credentials(String accessKey, String secretKey) {
      this.provider = new StaticProvider(accessKey, secretKey, null);
      return this;
    }

    public Builder credentialsProvider(Provider provider) {
      Utils.validateNotNull(provider, "credential provider");
      this.provider = provider;
      return this;
    }

    public Builder httpClient(OkHttpClient httpClient) {
      Utils.validateNotNull(httpClient, "http client");
      this.httpClient = httpClient;
      return this;
    }

    public MinioAdminClient build() {
      Utils.validateNotNull(baseUrl, "base url");
      Utils.validateNotNull(provider, "credential provider");
      if (httpClient == null) httpClient = Http.newDefaultClient();
      return new MinioAdminClient(baseUrl, region, provider, httpClient);
    }
  }
}
