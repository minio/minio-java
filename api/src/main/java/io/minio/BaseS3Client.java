/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2025 MinIO, Inc.
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

package io.minio;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.CompleteMultipartUpload;
import io.minio.messages.CompleteMultipartUploadResult;
import io.minio.messages.CopyPartResult;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.ErrorResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.Item;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListBucketResultV2;
import io.minio.messages.ListMultipartUploadsResult;
import io.minio.messages.ListObjectsResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.ListVersionsResult;
import io.minio.messages.LocationConstraint;
import io.minio.messages.NotificationRecords;
import io.minio.messages.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Core S3 API client. */
public abstract class BaseS3Client implements AutoCloseable {
  static {
    try {
      RequestBody.create(new byte[] {}, null);
    } catch (NoSuchMethodError ex) {
      throw new RuntimeException("Unsupported OkHttp library found. Must use okhttp >= 4.11.0", ex);
    }
  }

  protected static final String NO_SUCH_BUCKET_MESSAGE = "Bucket does not exist";
  protected static final String NO_SUCH_BUCKET = "NoSuchBucket";
  protected static final String NO_SUCH_BUCKET_POLICY = "NoSuchBucketPolicy";
  protected static final String NO_SUCH_OBJECT_LOCK_CONFIGURATION = "NoSuchObjectLockConfiguration";
  protected static final String SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR =
      "ServerSideEncryptionConfigurationNotFoundError";
  // maximum allowed bucket policy size is 20KiB
  protected static final int MAX_BUCKET_POLICY_SIZE = 20 * 1024;
  protected final Map<String, String> regionCache = new ConcurrentHashMap<>();
  protected static final Random random = new Random(new SecureRandom().nextLong());
  protected static final ObjectMapper objectMapper =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
          .build();

  private static final String RETRY_HEAD = "RetryHead";
  private static final String END_HTTP = "----------END-HTTP----------";
  private static final String UPLOAD_ID = "uploadId";
  private static final Set<String> TRACE_QUERY_PARAMS =
      ImmutableSet.of("retention", "legal-hold", "tagging", UPLOAD_ID, "acl", "attributes");
  private PrintWriter traceStream;
  private String userAgent = Utils.getDefaultUserAgent();

  protected Http.BaseUrl baseUrl;
  protected Provider provider;
  protected OkHttpClient httpClient;
  protected boolean closeHttpClient;

  protected BaseS3Client(
      Http.BaseUrl baseUrl,
      Provider provider,
      OkHttpClient httpClient,
      boolean closeHttpClient) {
    this.baseUrl = baseUrl;
    this.provider = provider;
    this.httpClient = httpClient;
    this.closeHttpClient = closeHttpClient;
  }

  protected BaseS3Client(BaseS3Client client) {
    this.baseUrl = client.baseUrl;
    this.provider = client.provider;
    this.httpClient = client.httpClient;
    this.closeHttpClient = client.closeHttpClient;
  }

  @Override
  public void close() throws Exception {
    if (closeHttpClient) {
      httpClient.dispatcher().executorService().shutdown();
      httpClient.connectionPool().evictAll();
    }
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
   * minioClient.ignoreCertCheck();
   * }</pre>
   *
   * @throws KeyManagementException thrown to indicate key management error.
   * @throws NoSuchAlgorithmException thrown to indicate missing of SSL library.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "SIC",
      justification = "Should not be used in production anyways.")
  public void ignoreCertCheck() throws KeyManagementException, NoSuchAlgorithmException {
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
   * @throws IOException upon connection error
   */
  public void traceOff() throws IOException {
    this.traceStream = null;
  }

  /** Enables dual-stack endpoint for Amazon S3 endpoint. */
  public void enableDualStackEndpoint() {
    baseUrl.enableDualStackEndpoint();
  }

  /** Disables dual-stack endpoint for Amazon S3 endpoint. */
  public void disableDualStackEndpoint() {
    baseUrl.disableDualStackEndpoint();
  }

  /** Enables virtual-style endpoint. */
  public void enableVirtualStyleEndpoint() {
    baseUrl.enableVirtualStyleEndpoint();
  }

  /** Disables virtual-style endpoint. */
  public void disableVirtualStyleEndpoint() {
    baseUrl.disableVirtualStyleEndpoint();
  }

  /** Sets AWS S3 domain prefix. */
  public void setAwsS3Prefix(@Nonnull String awsS3Prefix) {
    baseUrl.setAwsS3Prefix(awsS3Prefix);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////// HTTP execution methods ////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////

  private String[] handleRedirectResponse(
      Http.Method method, String bucketName, Response response, boolean retry) {
    String code = null;
    String message = null;

    if (response.code() == 301) {
      code = "PermanentRedirect";
      message = "Moved Permanently";
    } else if (response.code() == 307) {
      code = "Redirect";
      message = "Temporary redirect";
    } else if (response.code() == 400) {
      code = "BadRequest";
      message = "Bad request";
    }

    String region = response.headers().get("x-amz-bucket-region");
    if (message != null && region != null) message += ". Use region " + region;

    if (retry
        && region != null
        && method.equals(Http.Method.HEAD)
        && bucketName != null
        && regionCache.get(bucketName) != null) {
      code = RETRY_HEAD;
      message = null;
    }

    return new String[] {code, message};
  }

  /** Execute HTTP request asynchronously for given parameters. */
  protected CompletableFuture<Response> executeAsync(Http.S3Request s3request, String region)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    HttpUrl url =
        baseUrl.buildUrl(
            s3request.method(),
            s3request.bucket(),
            s3request.object(),
            region,
            s3request.queryParams());
    Credentials credentials = (provider == null) ? null : provider.fetch();
    okhttp3.Request request = s3request.httpRequest(url, credentials);

    StringBuilder traceBuilder = new StringBuilder(s3request.traces());
    PrintWriter traceStream = this.traceStream;
    if (traceStream != null) traceStream.print(s3request.traces());

    OkHttpClient httpClient = this.httpClient;
    if (!s3request.retryFailure()) {
      httpClient = httpClient.newBuilder().retryOnConnectionFailure(false).build();
    }

    CompletableFuture<Response> completableFuture = new CompletableFuture<>();
    httpClient
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(final Call call, IOException e) {
                completableFuture.completeExceptionally(e);
              }

              @Override
              public void onResponse(Call call, final Response response) throws IOException {
                try {
                  onResponse(response);
                } catch (Exception e) {
                  completableFuture.completeExceptionally(e);
                }
              }

              private void onResponse(final Response response) throws IOException {
                String trace =
                    String.format(
                        "%s %d %s\n%s\n\n",
                        response.protocol().toString().toUpperCase(Locale.US),
                        response.code(),
                        response.message(),
                        response.headers().toString());
                traceBuilder.append(trace);
                if (traceStream != null) traceStream.print(trace);

                if (response.isSuccessful()) {
                  if (traceStream != null) {
                    // Trace response body only if the request is not
                    // GetObject/ListenBucketNotification
                    // S3 API.
                    Set<String> keys = s3request.queryParams().keySet();
                    if ((s3request.method() != Http.Method.GET
                            || s3request.object() == null
                            || !Collections.disjoint(keys, TRACE_QUERY_PARAMS))
                        && !(keys.contains("events")
                            && (keys.contains("prefix") || keys.contains("suffix")))) {
                      ResponseBody responseBody = response.peekBody(1024 * 1024);
                      traceStream.println(responseBody.string());
                    }
                    traceStream.println(END_HTTP);
                  }

                  completableFuture.complete(response);
                  return;
                }

                String errorXml = null;
                try (ResponseBody responseBody = response.body()) {
                  errorXml = responseBody.string();
                }

                if (!("".equals(errorXml) && s3request.method().equals(Http.Method.HEAD))) {
                  traceBuilder.append(errorXml);
                  if (traceStream != null) traceStream.print(errorXml);
                  if (!errorXml.endsWith("\n")) {
                    traceBuilder.append("\n");
                    if (traceStream != null) traceStream.println();
                  }
                }
                traceBuilder.append(END_HTTP).append("\n");
                if (traceStream != null) traceStream.println(END_HTTP);

                // Error in case of Non-XML response from server for non-HEAD requests.
                String contentType = response.headers().get("content-type");
                if (!s3request.method().equals(Http.Method.HEAD)
                    && (contentType == null
                        || !Arrays.asList(contentType.split(";")).contains("application/xml"))) {
                  if (response.code() == 304 && response.body().contentLength() == 0) {
                    completableFuture.completeExceptionally(
                        new ServerException(
                            "server failed with HTTP status code " + response.code(),
                            response.code(),
                            traceBuilder.toString()));
                  }

                  completableFuture.completeExceptionally(
                      new InvalidResponseException(
                          response.code(),
                          contentType,
                          errorXml.substring(
                              0, errorXml.length() > 1024 ? 1024 : errorXml.length()),
                          traceBuilder.toString()));
                  return;
                }

                ErrorResponse errorResponse = null;
                if (!"".equals(errorXml)) {
                  try {
                    errorResponse = Xml.unmarshal(ErrorResponse.class, errorXml);
                  } catch (XmlParserException e) {
                    completableFuture.completeExceptionally(e);
                    return;
                  }
                } else if (!s3request.method().equals(Http.Method.HEAD)) {
                  completableFuture.completeExceptionally(
                      new InvalidResponseException(
                          response.code(), contentType, errorXml, traceBuilder.toString()));
                  return;
                }

                if (errorResponse == null) {
                  String code = null;
                  String message = null;
                  switch (response.code()) {
                    case 301:
                    case 307:
                    case 400:
                      String[] result =
                          handleRedirectResponse(
                              s3request.method(), s3request.bucket(), response, true);
                      code = result[0];
                      message = result[1];
                      break;
                    case 404:
                      if (s3request.object() != null) {
                        code = "NoSuchKey";
                        message = "Object does not exist";
                      } else if (s3request.bucket() != null) {
                        code = NO_SUCH_BUCKET;
                        message = NO_SUCH_BUCKET_MESSAGE;
                      } else {
                        code = "ResourceNotFound";
                        message = "Request resource not found";
                      }
                      break;
                    case 501:
                    case 405:
                      code = "MethodNotAllowed";
                      message = "The specified method is not allowed against this resource";
                      break;
                    case 409:
                      if (s3request.bucket() != null) {
                        code = NO_SUCH_BUCKET;
                        message = NO_SUCH_BUCKET_MESSAGE;
                      } else {
                        code = "ResourceConflict";
                        message = "Request resource conflicts";
                      }
                      break;
                    case 403:
                      code = "AccessDenied";
                      message = "Access denied";
                      break;
                    case 412:
                      code = "PreconditionFailed";
                      message = "At least one of the preconditions you specified did not hold";
                      break;
                    case 416:
                      code = "InvalidRange";
                      message = "The requested range cannot be satisfied";
                      break;
                    default:
                      completableFuture.completeExceptionally(
                          new ServerException(
                              "server failed with HTTP status code " + response.code(),
                              response.code(),
                              traceBuilder.toString()));
                      return;
                  }

                  errorResponse =
                      new ErrorResponse(
                          code,
                          message,
                          s3request.bucket(),
                          s3request.object(),
                          request.url().encodedPath(),
                          response.header("x-amz-request-id"),
                          response.header("x-amz-id-2"));
                }

                // invalidate region cache if needed
                if (errorResponse.code().equals(NO_SUCH_BUCKET)
                    || errorResponse.code().equals(RETRY_HEAD)) {
                  regionCache.remove(s3request.bucket());
                }

                ErrorResponseException e =
                    new ErrorResponseException(errorResponse, response, traceBuilder.toString());
                completableFuture.completeExceptionally(e);
              }
            });
    return completableFuture;
  }

  /** Execute HTTP request asynchronously for given args and parameters. */
  protected CompletableFuture<Response> executeAsync(Http.S3Request s3request)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    return getRegion(s3request.bucket(), s3request.region())
        .thenCompose(
            location -> {
              try {
                return executeAsync(s3request, location);
              } catch (InsufficientDataException
                  | InternalException
                  | InvalidKeyException
                  | IOException
                  | NoSuchAlgorithmException
                  | XmlParserException e) {
                throw new CompletionException(e);
              }
            });
  }

  /** Execute HTTP request asynchronously for given args and parameters. */
  protected CompletableFuture<Response> executeAsync(BaseArgs args, Http.S3Request s3request)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    return getRegion(s3request.bucket(), s3request.region())
        .thenCompose(
            location -> {
              args.setLocation(location);
              try {
                return executeAsync(s3request, location);
              } catch (InsufficientDataException
                  | InternalException
                  | InvalidKeyException
                  | IOException
                  | NoSuchAlgorithmException
                  | XmlParserException e) {
                throw new CompletionException(e);
              }
            });
  }

  /** Execute asynchronously GET HTTP request for given parameters. */
  protected CompletableFuture<Response> executeGetAsync(
      BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    return executeAsync(
        args,
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.GET)
            .headers(headers)
            .queryParams(queryParams)
            .baseArgs(args)
            .build());
  }

  /** Execute asynchronously HEAD HTTP request for given parameters. */
  protected CompletableFuture<Response> executeHeadAsync(
      BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    Http.S3Request s3request =
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.HEAD)
            .headers(headers)
            .queryParams(queryParams)
            .baseArgs(args)
            .build();
    return executeAsync(args, s3request)
        .exceptionally(
            e -> {
              if (e instanceof ErrorResponseException) {
                ErrorResponseException ex = (ErrorResponseException) e;
                if (ex.errorResponse().code().equals(RETRY_HEAD)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenCompose(
            response -> {
              if (response != null) {
                return CompletableFuture.completedFuture(response);
              }

              try {
                return executeAsync(s3request);
              } catch (InsufficientDataException
                  | InternalException
                  | InvalidKeyException
                  | IOException
                  | NoSuchAlgorithmException
                  | XmlParserException e) {
                throw new CompletionException(e);
              }
            });
  }

  /** Execute asynchronously DELETE HTTP request for given parameters. */
  protected CompletableFuture<Response> executeDeleteAsync(
      BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    return executeAsync(
            args,
            Http.S3Request.builder()
                .userAgent(userAgent)
                .method(Http.Method.DELETE)
                .headers(headers)
                .queryParams(queryParams)
                .baseArgs(args)
                .build())
        .thenApply(
            response -> {
              if (response != null) response.body().close();
              return response;
            });
  }

  /** Execute asynchronously POST HTTP request for given parameters. */
  protected CompletableFuture<Response> executePostAsync(
      BaseArgs args,
      Multimap<String, String> headers,
      Multimap<String, String> queryParams,
      Object data)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    return executeAsync(
        args,
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.POST)
            .headers(headers)
            .queryParams(queryParams)
            .body(data, null, null, null, null)
            .baseArgs(args)
            .build());
  }

  /** Execute asynchronously PUT HTTP request for given parameters. */
  protected CompletableFuture<Response> executePutAsync(
      BaseArgs args,
      Multimap<String, String> headers,
      Multimap<String, String> queryParams,
      Object data,
      long length)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    return executeAsync(
        args,
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.PUT)
            .headers(headers)
            .queryParams(queryParams)
            .body(data, (long) length, null, null, null)
            .baseArgs(args)
            .build());
  }

  /** Returns region of given bucket either from region cache or set in constructor. */
  protected CompletableFuture<String> getRegion(String bucket, String region)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    String thisRegion = this.baseUrl.region();
    if (region != null) {
      // Error out if region does not match with region passed via constructor.
      if (thisRegion != null && !thisRegion.equals(region)) {
        throw new IllegalArgumentException(
            "region must be " + thisRegion + ", but passed " + region);
      }
      return CompletableFuture.completedFuture(region);
    }

    if (thisRegion != null && !thisRegion.equals("")) {
      return CompletableFuture.completedFuture(thisRegion);
    }
    if (bucket == null || this.provider == null) {
      return CompletableFuture.completedFuture(Http.US_EAST_1);
    }
    region = regionCache.get(bucket);
    if (region != null) return CompletableFuture.completedFuture(region);

    return getBucketLocation(GetBucketLocationArgs.builder().bucket(bucket).build());
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////// S3 APIs and their helpers are added here ///////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////

  /** Check whether argument is valid or not. */
  protected void checkArgs(BaseArgs args) {
    if (args == null) throw new IllegalArgumentException("null arguments");

    if ((baseUrl.awsDomainSuffix() != null) && (args instanceof BucketArgs)) {
      String bucketName = ((BucketArgs) args).bucket();
      if (bucketName.startsWith("xn--")
          || bucketName.endsWith("--s3alias")
          || bucketName.endsWith("--ol-s3")) {
        throw new IllegalArgumentException(
            "bucket name '"
                + bucketName
                + "' must not start with 'xn--' and must not end with '--s3alias' or '--ol-s3'");
      }
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html">AbortMultipartUpload
   * S3 API</a> asynchronously.
   *
   * @param args {@link AbortMultipartUploadArgs} object.
   * @return {@link CompletableFuture}&lt;{@link AbortMultipartUploadResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUpload(
      AbortMultipartUploadArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    return executeDeleteAsync(
            args,
            null,
            Utils.mergeMultimap(
                args.extraQueryParams(), Utils.newMultimap(UPLOAD_ID, args.uploadId())))
        .thenApply(
            response -> {
              try {
                return new AbortMultipartUploadResponse(
                    response.headers(), args.bucket(), args.location(), args.object(), args.uploadId());
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload
   * S3 API</a> asynchronously.
   *
   * @param args {@link CompleteMultipartUploadArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ObjectWriteResponse> completeMultipartUpload(
      CompleteMultipartUploadArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    return executePostAsync(
            args,
            null,
            Utils.mergeMultimap(
                args.extraQueryParams(), Utils.newMultimap(UPLOAD_ID, args.uploadId())),
            new CompleteMultipartUpload(args.parts()))
        .thenApply(
            response -> {
              try {
                String bodyContent = response.body().string();
                bodyContent = bodyContent.trim();
                if (!bodyContent.isEmpty()) {
                  try {
                    if (Xml.validate(ErrorResponse.class, bodyContent)) {
                      ErrorResponse errorResponse = Xml.unmarshal(ErrorResponse.class, bodyContent);
                      throw new CompletionException(
                          new ErrorResponseException(errorResponse, response, null));
                    }
                  } catch (XmlParserException e) {
                    // As it is not <Error> message, fallback to parse CompleteMultipartUploadOutput
                    // XML.
                  }

                  try {
                    CompleteMultipartUploadResult result =
                        Xml.unmarshal(CompleteMultipartUploadResult.class, bodyContent);
                    return new ObjectWriteResponse(
                        response.headers(),
                        result.bucket(),
                        result.location(),
                        result.object(),
                        result.etag(),
                        response.header("x-amz-version-id"),
                        result);
                  } catch (XmlParserException e) {
                    // As this CompleteMultipartUpload REST call succeeded, just log it.
                    Logger.getLogger(BaseS3Client.class.getName())
                        .warning(
                            "S3 service returned unknown XML for CompleteMultipartUpload REST API. "
                                + bodyContent);
                  }
                }

                return new ObjectWriteResponse(
                    response.headers(),
                    args.bucket(),
                    args.location(),
                    args.object(),
                    null,
                    response.header("x-amz-version-id"));
              } catch (IOException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload
   * S3 API</a> asynchronously.
   *
   * @param args {@link CreateMultipartUploadArgs} object.
   * @return {@link CompletableFuture}&lt;{@link CreateMultipartUploadResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<CreateMultipartUploadResponse> createMultipartUpload(
      CreateMultipartUploadArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    return executePostAsync(
            args,
            null,
            Utils.mergeMultimap(args.extraQueryParams(), Utils.newMultimap("uploads", "")),
            null)
        .thenApply(
            response -> {
              try {
                InitiateMultipartUploadResult result =
                    Xml.unmarshal(
                        InitiateMultipartUploadResult.class, response.body().charStream());
                return new CreateMultipartUploadResponse(
                    response.headers(), args.bucket(), args.location(), args.object(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html">DeleteObjects S3
   * API</a> asynchronously.
   *
   * @param args {@link DeleteObjectsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link DeleteObjectsResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    return executePostAsync(
            args,
            Utils.mergeMultimap(
                args.extraHeaders(),
                args.bypassGovernanceMode()
                    ? Utils.newMultimap("x-amz-bypass-governance-retention", "true")
                    : null),
            Utils.mergeMultimap(args.extraQueryParams(), Utils.newMultimap("delete", "")),
            new DeleteRequest(args.objects(), args.quiet()))
        .thenApply(
            response -> {
              try {
                String bodyContent = response.body().string();
                try {
                  if (Xml.validate(DeleteResult.Error.class, bodyContent)) {
                    DeleteResult.Error error = Xml.unmarshal(DeleteResult.Error.class, bodyContent);
                    DeleteResult result = new DeleteResult(error);
                    return new DeleteObjectsResponse(
                        response.headers(), args.bucket(), args.region(), result);
                  }
                } catch (XmlParserException e) {
                  // Ignore this exception as it is not <Error> message,
                  // but parse it as <DeleteResult> message below.
                }

                DeleteResult result = Xml.unmarshal(DeleteResult.class, bodyContent);
                return new DeleteObjectsResponse(
                    response.headers(), args.bucket(), args.region(), result);
              } catch (IOException | XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLocation.html">GetBucketLocation S3
   * API</a> asynchronously.
   *
   * @param args {@link GetBucketLocationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link String}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<String> getBucketLocation(GetBucketLocationArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    return executeAsync(
            Http.S3Request.builder()
                .userAgent(userAgent)
                .method(Http.Method.GET)
                .baseArgs(args)
                .queryParams(Utils.newMultimap("location", null))
                .build(),
            Http.US_EAST_1)
        .thenApply(
            response -> {
              String location;
              try (ResponseBody body = response.body()) {
                LocationConstraint lc = Xml.unmarshal(LocationConstraint.class, body.charStream());
                if (lc.location() == null || lc.location().equals("")) {
                  location = Http.US_EAST_1;
                } else if (lc.location().equals("EU") && this.baseUrl.awsDomainSuffix() != null) {
                  location = "eu-west-1"; // eu-west-1 is also referred as 'EU'.
                } else {
                  location = lc.location();
                }
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              }

              regionCache.put(args.bucket(), location);
              return location;
            });
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html">HeadObject
   * S3 API</a> asynchronously.
   *
   * @param args {@link HeadObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link HeadObjectResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<HeadObjectResponse> headObject(HeadObjectArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    args.validateSsec(baseUrl.isHttps());
    return executeHeadAsync(
            args,
            args.getHeaders(),
            (args.versionId() != null) ? Utils.newMultimap("versionId", args.versionId()) : null)
        .thenApply(
            response ->
                new HeadObjectResponse(
                    response.headers(), args.bucket(), args.region(), args.object()));
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html">ListBuckets
   * S3 API</a> asynchronously.
   *
   * @param args {@link ListBucketsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ListBucketsResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ListBucketsResponse> listBuckets(ListBucketsArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);

    Multimap<String, String> queryParams = Utils.newMultimap(args.extraQueryParams());
    if (args.bucketRegion() != null) queryParams.put("bucket-region", args.bucketRegion());
    queryParams.put(
        "max-buckets", Integer.toString(args.maxBuckets() > 0 ? args.maxBuckets() : 10000));
    if (args.prefix() != null) queryParams.put("prefix", args.prefix());
    if (args.continuationToken() != null) {
      queryParams.put("continuation-token", args.continuationToken());
    }

    return executeGetAsync(args, args.extraHeaders(), queryParams)
        .thenApply(
            response -> {
              try {
                ListAllMyBucketsResult result =
                    Xml.unmarshal(ListAllMyBucketsResult.class, response.body().charStream());
                return new ListBucketsResponse(response.headers(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListMultipartUploads.html">ListMultipartUploads
   * S3 API</a> asynchronously.
   *
   * @param args {@link ListMultipartUploadsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ListMultipartUploadsResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ListMultipartUploadsResponse> listMultipartUploads(
      ListMultipartUploadsArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    Multimap<String, String> queryParams =
        Utils.mergeMultimap(
            args.extraQueryParams(),
            Utils.newMultimap(
                "uploads",
                "",
                "delimiter",
                (args.delimiter() != null) ? args.delimiter() : "",
                "max-uploads",
                (args.maxUploads() != null) ? args.maxUploads().toString() : "1000",
                "prefix",
                (args.prefix() != null) ? args.prefix() : ""));
    if (args.encodingType() != null) queryParams.put("encoding-type", args.encodingType());
    if (args.keyMarker() != null) queryParams.put("key-marker", args.keyMarker());
    if (args.uploadIdMarker() != null) queryParams.put("upload-id-marker", args.uploadIdMarker());

    return executeGetAsync(args, args.extraHeaders(), queryParams)
        .thenApply(
            response -> {
              try {
                ListMultipartUploadsResult result =
                    Xml.unmarshal(ListMultipartUploadsResult.class, response.body().charStream());
                return new ListMultipartUploadsResponse(
                    response.headers(), args.bucket(), args.region(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  private Multimap<String, String> getCommonListObjectsQueryParams(
      String delimiter, String encodingType, Integer maxKeys, String prefix) {
    Multimap<String, String> queryParams =
        Utils.newMultimap(
            "delimiter",
            (delimiter == null) ? "" : delimiter,
            "max-keys",
            Integer.toString(maxKeys > 0 ? maxKeys : 1000),
            "prefix",
            (prefix == null) ? "" : prefix);
    if (encodingType != null) queryParams.put("encoding-type", encodingType);
    return queryParams;
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects
   * version 1 S3 API</a> asynchronously.
   *
   * @param args {@link ListObjectsV1Args} object.
   * @return {@link CompletableFuture}&lt;{@link ListObjectsV1Response}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ListObjectsV1Response> listObjectsV1(ListObjectsV1Args args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);

    Multimap<String, String> queryParams =
        Utils.mergeMultimap(
            args.extraQueryParams(),
            getCommonListObjectsQueryParams(
                args.delimiter(), args.encodingType(), args.maxKeys(), args.prefix()));
    if (args.marker() != null) queryParams.put("marker", args.marker());

    return executeGetAsync(args, args.extraHeaders(), queryParams)
        .thenApply(
            response -> {
              try {
                ListBucketResultV1 result =
                    Xml.unmarshal(ListBucketResultV1.class, response.body().charStream());
                return new ListObjectsV1Response(
                    response.headers(), args.bucket(), args.region(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html">ListObjects
   * version 2 S3 API</a> asynchronously.
   *
   * @param args {@link ListObjectsV2Args} object.
   * @return {@link CompletableFuture}&lt;{@link ListObjectsV2Response}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Args args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);

    Multimap<String, String> queryParams =
        Utils.mergeMultimap(
            args.extraQueryParams(),
            getCommonListObjectsQueryParams(
                args.delimiter(), args.encodingType(), args.maxKeys(), args.prefix()));
    if (args.startAfter() != null) queryParams.put("start-after", args.startAfter());
    if (args.continuationToken() != null)
      queryParams.put("continuation-token", args.continuationToken());
    if (args.fetchOwner()) queryParams.put("fetch-owner", "true");
    if (args.includeUserMetadata()) queryParams.put("metadata", "true");
    queryParams.put("list-type", "2");

    return executeGetAsync(args, args.extraHeaders(), queryParams)
        .thenApply(
            response -> {
              try {
                ListBucketResultV2 result =
                    Xml.unmarshal(ListBucketResultV2.class, response.body().charStream());
                return new ListObjectsV2Response(
                    response.headers(), args.bucket(), args.region(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html">ListObjectVersions
   * API</a> asynchronously.
   *
   * @param args {@link ListObjectVersionsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ListObjectVersionsResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ListObjectVersionsResponse> listObjectVersions(
      ListObjectVersionsArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);

    Multimap<String, String> queryParams =
        Utils.mergeMultimap(
            args.extraQueryParams(),
            getCommonListObjectsQueryParams(
                args.delimiter(), args.encodingType(), args.maxKeys(), args.prefix()));
    if (args.keyMarker() != null) queryParams.put("key-marker", args.keyMarker());
    if (args.versionIdMarker() != null) {
      queryParams.put("version-id-marker", args.versionIdMarker());
    }
    queryParams.put("versions", "");

    return executeGetAsync(args, args.extraHeaders(), queryParams)
        .thenApply(
            response -> {
              try {
                ListVersionsResult result =
                    Xml.unmarshal(ListVersionsResult.class, response.body().charStream());
                return new ListObjectVersionsResponse(
                    response.headers(), args.bucket(), args.region(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts S3
   * API</a> asynchronously.
   *
   * @param args {@link ListPartsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ListPartsResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ListPartsResponse> listParts(ListPartsArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    Multimap<String, String> queryParams =
        Utils.mergeMultimap(
            args.extraQueryParams(),
            Utils.newMultimap(
                UPLOAD_ID,
                args.uploadId(),
                "max-parts",
                (args.maxParts() != null) ? args.maxParts().toString() : "1000"));
    if (args.partNumberMarker() != null) {
      queryParams.put("part-number-marker", args.partNumberMarker().toString());
    }

    return executeGetAsync(args, args.extraHeaders(), queryParams)
        .thenApply(
            response -> {
              try {
                ListPartsResult result =
                    Xml.unmarshal(ListPartsResult.class, response.body().charStream());
                return new ListPartsResponse(
                    response.headers(), args.bucket(), args.region(), args.object(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  // /**
  //  * Execute put object asynchronously from object data from {@link RandomAccessFile} or {@link
  //  * InputStream}.
  //  *
  //  * @param args {@link PutObjectBaseArgs}.
  //  * @param data {@link RandomAccessFile} or {@link InputStream}.
  //  * @param objectSize object size.
  //  * @param partSize part size for multipart upload.
  //  * @param partCount Number of parts for multipart upload.
  //  * @param contentType content-type of object.
  //  * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
  //  * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
  //  * @throws InternalException thrown to indicate internal library error.
  //  * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
  //  * @throws IOException thrown to indicate I/O error on S3 operation.
  //  * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
  //  * @throws XmlParserException thrown to indicate XML parsing error.
  //  */
  // protected CompletableFuture<ObjectWriteResponse> putObjectAsync(
  //     PutObjectBaseArgs args,
  //     Object data,
  //     long objectSize,
  //     long partSize,
  //     int partCount,
  //     String contentType)
  //     throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
  //         NoSuchAlgorithmException, XmlParserException {
  //   PartReader partReader = newPartReader(data, objectSize, partSize, partCount);
  //   if (partReader == null) {
  //     throw new IllegalArgumentException("data must be RandomAccessFile or InputStream");
  //   }
  // 
  //   Multimap<String, String> headers = Utils.newMultimap(args.extraHeaders());
  //   headers.putAll(args.genHeaders());
  //   if (!headers.containsKey("Content-Type")) headers.put("Content-Type", contentType);
  // 
  //   return CompletableFuture.supplyAsync(
  //           () -> {
  //             try {
  //               return partReader.getPart();
  //             } catch (NoSuchAlgorithmException | IOException e) {
  //               throw new CompletionException(e);
  //             }
  //           })
  //       .thenCompose(
  //           partSource -> {
  //             try {
  //               if (partReader.partCount() == 1) {
  //                 return putObjectAsync(
  //                     args.bucket(),
  //                     args.region(),
  //                     args.object(),
  //                     partSource,
  //                     headers,
  //                     args.extraQueryParams());
  //               } else {
  //                 return putMultipartObjectAsync(args, headers, partReader, partSource);
  //               }
  //             } catch (InsufficientDataException
  //                 | InternalException
  //                 | InvalidKeyException
  //                 | IOException
  //                 | NoSuchAlgorithmException
  //                 | XmlParserException e) {
  //               throw new CompletionException(e);
  //             }
  //           });
  // }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject S3
   * API</a> asynchronously.
   *
   * @param args {@link PutObjectAPIArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<ObjectWriteResponse> putObject(PutObjectAPIArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    Long length = args.length();
    Object data = args.file();
    if (args.file() == null) data = args.buffer();
    if (args.buffer() == null) data = args.data();
    return executePutAsync(
            args,
            Utils.mergeMultimap(args.extraHeaders(), args.headers()),
            args.extraQueryParams(),
            data,
            length)
        .thenApply(
            response -> {
              try {
                return new ObjectWriteResponse(
                    response.headers(),
                    args.bucket(),
                    args.region(),
                    args.object(),
                    response.header("ETag").replaceAll("\"", ""),
                    response.header("x-amz-version-id"));
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart S3
   * API</a> asynchronously.
   *
   * @param args {@link UploadPartArgs} object.
   * @return {@link CompletableFuture}&lt;{@link UploadPartResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<UploadPartResponse> uploadPart(UploadPartArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    Long length = args.length();
    Object data = args.file();
    if (args.file() == null) data = args.buffer();
    if (args.buffer() == null) data = args.data();
    return executePutAsync(
            args,
            args.extraHeaders(),
            Utils.mergeMultimap(
                args.extraQueryParams(),
                Utils.newMultimap(
                    "partNumber",
                    Integer.toString(args.partNumber()),
                    "uploadId",
                    args.uploadId())),
            data,
            length)
        .thenApply(
            response -> {
              try {
                return new UploadPartResponse(
                    response.headers(),
                    args.bucket(),
                    args.region(),
                    args.object(),
                    args.uploadId(),
                    args.partNumber(),
                    response.header("ETag").replaceAll("\"", ""));
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy
   * S3 API</a>.
   *
   * @param args {@link UploadPartCopyArgs} object.
   * @return {@link CompletableFuture}&lt;{@link UploadPartCopyResponse}&gt; object.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CompletableFuture<UploadPartCopyResponse> uploadPartCopy(UploadPartCopyArgs args)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    checkArgs(args);
    return executePutAsync(
            args,
            Utils.mergeMultimap(args.extraHeaders(), args.headers()),
            Utils.mergeMultimap(
                args.extraQueryParams(),
                Utils.newMultimap(
                    "partNumber",
                    Integer.toString(args.partNumber()),
                    "uploadId",
                    args.uploadId())),
            null,
            0)
        .thenApply(
            response -> {
              try {
                CopyPartResult result =
                    Xml.unmarshal(CopyPartResult.class, response.body().charStream());
                return new UploadPartCopyResponse(
                    response.headers(),
                    args.bucket(),
                    args.region(),
                    args.object(),
                    args.uploadId(),
                    args.partNumber(),
                    result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }
}
