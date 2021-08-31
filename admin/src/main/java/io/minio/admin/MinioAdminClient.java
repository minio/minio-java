package io.minio.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.minio.MinioClient;
import io.minio.S3Base;
import io.minio.S3Escaper;
import io.minio.Signer;
import io.minio.admin.security.EncryptionUtils;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.http.Method;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.*;
import org.bouncycastle.crypto.InvalidCipherTextException;

public class MinioAdminClient extends S3Base {

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  // default network I/O timeout is 5 minutes
  protected static final long DEFAULT_CONNECTION_TIMEOUT = 5;

  private final String region;
  private final HttpUrl baseUrl;
  private final Provider provider;
  private final OkHttpClient httpClient;

  private MinioAdminClient(
      HttpUrl baseUrl, String region, Provider provider, OkHttpClient httpClient) {
    super(baseUrl, region, false, false, false, false, provider, httpClient);
    this.baseUrl = baseUrl;
    this.region = region;
    this.provider = provider;
    this.httpClient = httpClient;
  }

  /**
   * Adds a user with the specified access and secret key.
   *
   * @param args {@link AddUserArgs} object.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws InvalidCipherTextException thrown to indicate data cannot be encrypted/decrypted.
   */
  public void addUser(AddUserArgs args)
      throws InternalException, InsufficientDataException, NoSuchAlgorithmException,
          InvalidKeyException, IOException, InvalidCipherTextException {
    Credentials creds = provider.fetch();
    byte[] encryptedUserInfo =
        EncryptionUtils.encrypt(
                creds.secretKey(), OBJECT_MAPPER.writeValueAsBytes(args.toUserInfo()))
            .array();
    executeAdmin(
        Method.PUT,
        "add-user",
        region,
        null,
        ImmutableMultimap.of("accessKey", args.accessKey()),
        encryptedUserInfo,
        encryptedUserInfo.length);
  }

  /**
   * Obtains a list of all MinIO users.
   *
   * @return List of all users.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws InvalidCipherTextException thrown to indicate data cannot be encrypted/decrypted.
   */
  public Map<String, UserInfo> listUsers()
      throws InternalException, InsufficientDataException, NoSuchAlgorithmException,
          InvalidKeyException, IOException, InvalidCipherTextException {
    Credentials creds = provider.fetch();
    byte[] jsonData =
        EncryptionUtils.decrypt(
                creds.secretKey(),
                executeAdmin(Method.GET, "list-users", region, null, null, null, 0).body().bytes())
            .array();
    MapType mapType =
        OBJECT_MAPPER
            .getTypeFactory()
            .constructMapType(HashMap.class, String.class, UserInfo.class);
    return OBJECT_MAPPER.readValue(jsonData, mapType);
  }

  /**
   * Deletes a user by it's access key
   *
   * @param accessKey Access Key of user to delete.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws InvalidCipherTextException thrown to indicate data cannot be encrypted/decrypted.
   */
  public void deleteUser(String accessKey)
      throws InternalException, InsufficientDataException, NoSuchAlgorithmException,
          InvalidKeyException, IOException {
    executeAdmin(
        Method.DELETE,
        "remove-user",
        region,
        null,
        ImmutableMultimap.of("accessKey", accessKey),
        null,
        0);
  }

  /**
   * Creates a policy.
   *
   * <pre>Example:{@code
   * client.addCannedPolicy(
   *         AddPolicyArgs.builder()
   *             .policyName("policy-test")
   *             .policyString(
   *                 "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Action\": [\"s3:GetObject\"],\"Effect\": \"Allow\",\"Resource\": [\"arn:aws:s3:::my-bucketname/*\"],\"Sid\": \"\"}]}")
   *             .build());
   * }
   * </pre>
   *
   * @param args {@Link AddPolicyArgs} object.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   */
  public void addCannedPolicy(AddPolicyArgs args)
      throws InternalException, InsufficientDataException, NoSuchAlgorithmException,
          InvalidKeyException, IOException {
    byte[] policy = args.policyString().getBytes(StandardCharsets.UTF_8);
    executeAdmin(
        Method.PUT,
        "add-canned-policy",
        region,
        null,
        ImmutableMultimap.of("name", args.policyName()),
        policy,
        policy.length);
  }

  /**
   * Sets a policy to a given user or group.
   *
   * @param args
   * @throws InternalException thrown to indicate internal library error.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   */
  public void setPolicy(SetPolicyArgs args)
      throws InternalException, InsufficientDataException, NoSuchAlgorithmException,
          InvalidKeyException, IOException {
    String groupStr = (args.isGroup()) ? "true" : "false";
    Multimap<String, String> queryValues =
        ImmutableMultimap.of(
            "policyName",
            args.policyName(),
            "userOrGroup",
            args.userOrGroup(),
            "isGroup",
            groupStr);
    executeAdmin(Method.PUT, "set-user-or-group-policy", region, null, queryValues, null, 0);
  }

  /**
   * Lists all configured canned policies.
   *
   * @return Map of policies, keyed by their name, with their actual policy as their value.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   */
  public Map<String, String> listCannedPolicies()
      throws InternalException, InsufficientDataException, NoSuchAlgorithmException,
          InvalidKeyException, IOException {
    byte[] body =
        executeAdmin(Method.GET, "list-canned-policies", region, null, null, null, 0)
            .body()
            .bytes();
    MapType mapType =
        OBJECT_MAPPER.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);
    return OBJECT_MAPPER.readValue(body, mapType);
  }

  /**
   * Removes canned policy by name.
   *
   * @param policyName Name of policy to remove.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   */
  public void removeCannedPolicy(String policyName)
      throws InternalException, InsufficientDataException, NoSuchAlgorithmException,
          InvalidKeyException, IOException {
    executeAdmin(
        Method.DELETE,
        "remove-canned-policy",
        region,
        null,
        ImmutableMultimap.of("name", policyName),
        null,
        0);
  }

  /** Build URL for given parameters. */
  protected HttpUrl buildAdminUrl(String action, Multimap<String, String> queryParamMap) {

    HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();
    String host = this.baseUrl.host();

    urlBuilder.host(host);
    urlBuilder.addEncodedPathSegment(S3Escaper.encode("minio"));
    urlBuilder.addEncodedPathSegment(S3Escaper.encode("admin"));
    urlBuilder.addEncodedPathSegment(S3Escaper.encode("v3"));
    urlBuilder.addEncodedPathSegment(S3Escaper.encode(action));

    if (queryParamMap != null) {
      for (Map.Entry<String, String> entry : queryParamMap.entries()) {
        urlBuilder.addEncodedQueryParameter(
            S3Escaper.encode(entry.getKey()), S3Escaper.encode(entry.getValue()));
      }
    }

    return urlBuilder.build();
  }

  /** Execute HTTP request for given parameters. */
  protected Response executeAdmin(
      Method method,
      String action,
      String region,
      Headers headers,
      Multimap<String, String> queryParamMap,
      Object body,
      int length)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException {

    if (body != null && !(body instanceof byte[])) {
      body = OBJECT_MAPPER.writeValueAsString(body);
    }

    if (body == null && (method == Method.PUT || method == Method.POST)) body = EMPTY_BODY;

    HttpUrl url = buildAdminUrl(action, queryParamMap);
    Credentials creds = (provider == null) ? null : provider.fetch();
    Request request = createRequest(url, method, headers, body, length, creds);
    if (creds != null) {
      request =
          Signer.signV4S3(
              request,
              region,
              creds.accessKey(),
              creds.secretKey(),
              request.header("x-amz-content-sha256"));
    }

    OkHttpClient httpClient = this.httpClient;

    Response response = httpClient.newCall(request).execute();

    if (response.code() != 200) {
      throw new RuntimeException("Request failed with response: " + response.body().string());
    }

    return response;
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
}
