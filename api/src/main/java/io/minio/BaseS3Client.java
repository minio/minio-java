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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableSet;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.CompleteMultipartUpload;
import io.minio.messages.CompleteMultipartUploadResult;
import io.minio.messages.CopyObjectResult;
import io.minio.messages.CopyPartResult;
import io.minio.messages.CreateBucketConfiguration;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.ErrorResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListBucketResultV2;
import io.minio.messages.ListMultipartUploadsResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.ListVersionsResult;
import io.minio.messages.LocationConstraint;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
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
  protected static final Random RANDOM = new Random(new SecureRandom().nextLong());
  protected static final ObjectMapper OBJECT_MAPPER =
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
  protected final Map<String, String> regionCache = new ConcurrentHashMap<>();
  protected String userAgent = Utils.getDefaultUserAgent();

  protected Http.BaseUrl baseUrl;
  protected Provider provider;
  protected OkHttpClient httpClient;
  protected boolean closeHttpClient;

  protected BaseS3Client(
      Http.BaseUrl baseUrl, Provider provider, OkHttpClient httpClient, boolean closeHttpClient) {
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

  /** Closes underneath HTTP client. */
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
   * @throws MinioException thrown to indicate SDK exception.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "SIC",
      justification = "Should not be used in production anyways.")
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
  protected CompletableFuture<Response> executeAsync(Http.S3Request s3request, String region) {
    Credentials credentials = (provider == null) ? null : provider.fetch();
    Http.Request request = null;
    try {
      request = s3request.toRequest(baseUrl, region, credentials);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }

    StringBuilder traceBuilder = new StringBuilder(request.httpTraces());
    PrintWriter traceStream = this.traceStream;
    if (traceStream != null) traceStream.print(request.httpTraces());

    OkHttpClient httpClient = this.httpClient;
    // FIXME: enable retry for all request.
    // if (!s3request.retryFailure()) {
    //   httpClient = httpClient.newBuilder().retryOnConnectionFailure(false).build();
    // }

    okhttp3.Request httpRequest = request.httpRequest();
    CompletableFuture<Response> completableFuture = new CompletableFuture<>();
    httpClient
        .newCall(httpRequest)
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
                        "%s %d %s%n%s",
                        response.protocol().toString().toUpperCase(Locale.US),
                        response.code(),
                        response.message(),
                        response.headers().toString());
                if (!trace.endsWith("\n\n")) {
                  trace += trace.endsWith("\n") ? "\n" : "\n\n";
                }
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
                      String responseBody = response.peekBody(1024 * 1024).string();
                      traceStream.print(responseBody);
                      if (!responseBody.endsWith("\n")) traceStream.println();
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

                // Error out for Non-XML response from server for non-HEAD requests.
                String contentType = response.headers().get(Http.Headers.CONTENT_TYPE);
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
                          httpRequest.url().encodedPath(),
                          response.header("x-amz-request-id"),
                          response.header("x-amz-id-2"));
                }

                // invalidate region cache if needed
                if (errorResponse.code().equals(NO_SUCH_BUCKET)
                    || errorResponse.code().equals(RETRY_HEAD)) {
                  regionCache.remove(s3request.bucket());
                }

                completableFuture.completeExceptionally(
                    new ErrorResponseException(errorResponse, response, traceBuilder.toString()));
              }
            });
    return completableFuture;
  }

  /** Execute HTTP request asynchronously for given args and parameters. */
  protected CompletableFuture<Response> executeAsync(Http.S3Request s3request) {
    return getRegion(s3request.bucket(), s3request.region())
        .thenCompose(
            location -> {
              s3request.args().setLocation(location);
              return executeAsync(s3request, location);
            });
  }

  /** Execute asynchronously GET HTTP request for given parameters. */
  protected CompletableFuture<Response> executeGetAsync(
      BaseArgs args, Http.Headers headers, Http.QueryParameters queryParams) {
    return executeAsync(
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.GET)
            .headers(headers)
            .queryParams(queryParams)
            .args(args)
            .build());
  }

  /** Execute asynchronously HEAD HTTP request for given parameters. */
  protected CompletableFuture<Response> executeHeadAsync(
      BaseArgs args, Http.Headers headers, Http.QueryParameters queryParams) {
    Http.S3Request s3request =
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.HEAD)
            .headers(headers)
            .queryParams(queryParams)
            .args(args)
            .build();
    return executeAsync(s3request)
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException
                  && ((ErrorResponseException) e).errorResponse().code().equals(RETRY_HEAD)) {
                return null;
              }
              throw new CompletionException(e);
            })
        .thenCompose(
            response -> {
              if (response != null) return CompletableFuture.completedFuture(response);
              return executeAsync(s3request);
            });
  }

  /** Execute asynchronously DELETE HTTP request for given parameters. */
  protected CompletableFuture<Response> executeDeleteAsync(
      BaseArgs args, Http.Headers headers, Http.QueryParameters queryParams) {
    return executeAsync(
            Http.S3Request.builder()
                .userAgent(userAgent)
                .method(Http.Method.DELETE)
                .headers(headers)
                .queryParams(queryParams)
                .args(args)
                .build())
        .thenApply(
            response -> {
              if (response != null) response.body().close();
              return response;
            });
  }

  /** Execute asynchronously POST HTTP request for given parameters. */
  protected CompletableFuture<Response> executePostAsync(
      BaseArgs args, Http.Headers headers, Http.QueryParameters queryParams, Http.Body body) {
    return executeAsync(
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.POST)
            .headers(headers)
            .queryParams(queryParams)
            .body(body)
            .args(args)
            .build());
  }

  /** Execute asynchronously PUT HTTP request for given parameters. */
  protected CompletableFuture<Response> executePutAsync(
      BaseArgs args, Http.Headers headers, Http.QueryParameters queryParams, Http.Body body) {
    return executeAsync(
        Http.S3Request.builder()
            .userAgent(userAgent)
            .method(Http.Method.PUT)
            .headers(headers)
            .queryParams(queryParams)
            .body(body)
            .args(args)
            .build());
  }

  /** Returns region of given bucket either from region cache or set in constructor. */
  protected CompletableFuture<String> getRegion(String bucket, String region) {
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
   */
  public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUpload(
      AbortMultipartUploadArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, new Http.QueryParameters(UPLOAD_ID, args.uploadId()))
        .thenApply(
            response -> {
              try {
                return new AbortMultipartUploadResponse(
                    response.headers(),
                    args.bucket(),
                    args.location(),
                    args.object(),
                    args.uploadId());
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
   */
  public CompletableFuture<ObjectWriteResponse> completeMultipartUpload(
      CompleteMultipartUploadArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(new CompleteMultipartUpload(args.parts()), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePostAsync(args, null, new Http.QueryParameters(UPLOAD_ID, args.uploadId()), body)
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
                throw new CompletionException(new MinioException(e));
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject S3
   * API</a> asynchronously.
   *
   * @param args {@link CopyObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> copyObject(CopyObjectArgs args) {
    checkArgs(args);
    args.validateSse(this.baseUrl.isHttps());
    if (args.source().offset() != null || args.source().length() != null) {
      throw new IllegalArgumentException("copy object with offset/length is unsupported");
    }

    Http.Headers headers = Http.Headers.merge(args.makeHeaders(), args.source().makeCopyHeaders());
    if (args.metadataDirective() != null) {
      headers.put("x-amz-metadata-directive", args.metadataDirective().toString());
    }
    if (args.taggingDirective() != null) {
      headers.put("x-amz-tagging-directive", args.taggingDirective().toString());
      if (args.taggingDirective() == Directive.REPLACE && !headers.containsKey("x-amz-tagging")) {
        headers.put("x-amz-tagging", "");
      }
    }

    return executePutAsync(args, headers, null, null)
        .thenApply(
            response -> {
              try {
                CopyObjectResult result =
                    Xml.unmarshal(CopyObjectResult.class, response.body().charStream());
                return new ObjectWriteResponse(
                    response.headers(),
                    args.bucket(),
                    args.region(),
                    args.object(),
                    result.etag(),
                    response.header("x-amz-version-id"),
                    result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket
   * S3 API</a> asynchronously.
   *
   * @param args {@link CreateBucketArgs} object.
   * @return {@link CompletableFuture}&lt;{@link GenericResponse}&gt; object.
   */
  public CompletableFuture<GenericResponse> createBucket(CreateBucketArgs args) {
    checkArgs(args);

    String region = args.region();
    String baseUrlRegion = baseUrl.region();
    if (baseUrlRegion != null && !baseUrlRegion.isEmpty()) {
      // Error out if region does not match with region passed via constructor.
      if (region != null && !region.equals(baseUrlRegion)) {
        throw new IllegalArgumentException(
            "region must be " + baseUrlRegion + ", but passed " + region);
      }
      region = baseUrlRegion;
    }
    if (region == null) {
      region = Http.US_EAST_1;
    }

    Http.Headers headers =
        args.objectLock() ? new Http.Headers("x-amz-bucket-object-lock-enabled", "true") : null;
    final String locationConstraint = region;

    CreateBucketConfiguration config = null;
    if (locationConstraint.equals(Http.US_EAST_1)) {
      config =
          new CreateBucketConfiguration(
              locationConstraint, args.locationConfig(), args.bucketConfig());
    }

    Http.Body body = null;
    try {
      body = new Http.Body(config, null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }

    return executeAsync(
            Http.S3Request.builder()
                .userAgent(userAgent)
                .method(Http.Method.PUT)
                .headers(headers)
                .body(body)
                .args(args)
                .build(),
            locationConstraint)
        .thenApply(
            response -> {
              try {
                return new GenericResponse(
                    response.headers(), args.bucket(), args.location(), null);
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
   */
  public CompletableFuture<CreateMultipartUploadResponse> createMultipartUpload(
      CreateMultipartUploadArgs args) {
    checkArgs(args);
    return executePostAsync(args, args.headers(), new Http.QueryParameters("uploads", ""), null)
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
   */
  public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(new DeleteRequest(args.objects(), args.quiet()), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePostAsync(
            args,
            args.bypassGovernanceMode()
                ? new Http.Headers("x-amz-bypass-governance-retention", "true")
                : null,
            new Http.QueryParameters("delete", ""),
            body)
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
              } catch (IOException e) {
                throw new CompletionException(new MinioException(e));
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLocation.html">GetBucketLocation
   * S3 API</a> asynchronously.
   *
   * @param args {@link GetBucketLocationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link String}&gt; object.
   */
  public CompletableFuture<String> getBucketLocation(GetBucketLocationArgs args) {
    checkArgs(args);
    return executeAsync(
            Http.S3Request.builder()
                .userAgent(userAgent)
                .method(Http.Method.GET)
                .args(args)
                .queryParams(new Http.QueryParameters("location", null))
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
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html">HeadObject S3
   * API</a> asynchronously.
   *
   * @param args {@link HeadObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link HeadObjectResponse}&gt; object.
   */
  public CompletableFuture<HeadObjectResponse> headObject(HeadObjectArgs args) {
    checkArgs(args);
    args.validateSsec(baseUrl.isHttps());
    return executeHeadAsync(
            args,
            args.makeHeaders(),
            (args.versionId() != null)
                ? new Http.QueryParameters("versionId", args.versionId())
                : null)
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
   */
  public CompletableFuture<ListBucketsResponse> listBucketsAPI(ListBucketsArgs args) {
    checkArgs(args);

    Http.QueryParameters queryParams = new Http.QueryParameters();
    if (args.bucketRegion() != null) queryParams.put("bucket-region", args.bucketRegion());
    queryParams.put(
        "max-buckets", Integer.toString(args.maxBuckets() > 0 ? args.maxBuckets() : 10000));
    if (args.prefix() != null) queryParams.put("prefix", args.prefix());
    if (args.continuationToken() != null) {
      queryParams.put("continuation-token", args.continuationToken());
    }

    return executeGetAsync(args, null, queryParams)
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
   */
  public CompletableFuture<ListMultipartUploadsResponse> listMultipartUploads(
      ListMultipartUploadsArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams =
        new Http.QueryParameters(
            "uploads",
            "",
            "delimiter",
            (args.delimiter() != null) ? args.delimiter() : "",
            "max-uploads",
            (args.maxUploads() != null) ? args.maxUploads().toString() : "1000",
            "prefix",
            (args.prefix() != null) ? args.prefix() : "");
    if (args.encodingType() != null) queryParams.put("encoding-type", args.encodingType());
    if (args.keyMarker() != null) queryParams.put("key-marker", args.keyMarker());
    if (args.uploadIdMarker() != null) queryParams.put("upload-id-marker", args.uploadIdMarker());

    return executeGetAsync(args, null, queryParams)
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

  private Http.QueryParameters getCommonListObjectsQueryParams(
      String delimiter, String encodingType, Integer maxKeys, String prefix) {
    Http.QueryParameters queryParams =
        new Http.QueryParameters(
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
   */
  public CompletableFuture<ListObjectsV1Response> listObjectsV1(ListObjectsV1Args args) {
    checkArgs(args);

    Http.QueryParameters queryParams =
        getCommonListObjectsQueryParams(
            args.delimiter(), args.encodingType(), args.maxKeys(), args.prefix());
    if (args.marker() != null) queryParams.put("marker", args.marker());

    return executeGetAsync(args, null, queryParams)
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
   */
  public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Args args) {
    checkArgs(args);

    Http.QueryParameters queryParams =
        getCommonListObjectsQueryParams(
            args.delimiter(), args.encodingType(), args.maxKeys(), args.prefix());
    if (args.startAfter() != null) queryParams.put("start-after", args.startAfter());
    if (args.continuationToken() != null)
      queryParams.put("continuation-token", args.continuationToken());
    if (args.fetchOwner()) queryParams.put("fetch-owner", "true");
    if (args.includeUserMetadata()) queryParams.put("metadata", "true");
    queryParams.put("list-type", "2");

    return executeGetAsync(args, null, queryParams)
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
   */
  public CompletableFuture<ListObjectVersionsResponse> listObjectVersions(
      ListObjectVersionsArgs args) {
    checkArgs(args);

    Http.QueryParameters queryParams =
        getCommonListObjectsQueryParams(
            args.delimiter(), args.encodingType(), args.maxKeys(), args.prefix());
    if (args.keyMarker() != null) queryParams.put("key-marker", args.keyMarker());
    if (args.versionIdMarker() != null) {
      queryParams.put("version-id-marker", args.versionIdMarker());
    }
    queryParams.put("versions", "");

    return executeGetAsync(args, null, queryParams)
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
   */
  public CompletableFuture<ListPartsResponse> listParts(ListPartsArgs args) {
    Http.QueryParameters queryParams =
        new Http.QueryParameters(
            UPLOAD_ID,
            args.uploadId(),
            "max-parts",
            (args.maxParts() != null) ? args.maxParts().toString() : "1000");
    if (args.partNumberMarker() != null) {
      queryParams.put("part-number-marker", args.partNumberMarker().toString());
    }

    return executeGetAsync(args, null, queryParams)
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

  private Object[] createBody(PutObjectAPIBaseArgs args, MediaType contentType)
      throws MinioException {
    Http.Headers headers = new Http.Headers(args.headers());
    String sha256HexString = headers.getFirst(Http.Headers.X_AMZ_CONTENT_SHA256);
    String sha256Base64String = headers.getFirst(Http.Headers.X_AMZ_CHECKSUM_SHA256);
    boolean checksumHeader = headers.namePrefixAny("x-amz-checksum-");
    String md5Hash = headers.getFirst(Http.Headers.CONTENT_MD5);

    if (sha256HexString == null && sha256Base64String == null) {
      if (!baseUrl.isHttps()) {
        Checksum.Hasher hasher = Checksum.Algorithm.SHA256.hasher();
        Map<Checksum.Algorithm, Checksum.Hasher> hashers = new HashMap<>();
        hashers.put(Checksum.Algorithm.SHA256, hasher);
        if (args.file() != null) {
          Checksum.update(hashers, args.file(), args.length());
        } else if (args.buffer() != null) {
          Checksum.update(hashers, args.buffer());
        } else if (args.data() != null) {
          Checksum.update(hashers, args.data(), args.length().intValue());
        }
        byte[] sum = hasher.sum();
        sha256HexString = Checksum.hexString(sum);
      } else {
        sha256HexString = Checksum.UNSIGNED_PAYLOAD;
      }
      headers.put(Http.Headers.X_AMZ_CONTENT_SHA256, sha256HexString);
    }

    if (sha256HexString == null && sha256Base64String != null) {
      sha256HexString = Checksum.UNSIGNED_PAYLOAD;
      if (!baseUrl.isHttps()) {
        sha256HexString = Checksum.hexString(Checksum.base64StringToSum(sha256Base64String));
      }
      headers.put(Http.Headers.X_AMZ_CONTENT_SHA256, sha256HexString);
    }

    if (sha256HexString != null
        && sha256Base64String == null
        && !checksumHeader
        && md5Hash == null) {
      if (Checksum.UNSIGNED_PAYLOAD.equals(sha256HexString)) {
        Checksum.Hasher hasher = Checksum.Algorithm.CRC32C.hasher();
        Map<Checksum.Algorithm, Checksum.Hasher> hashers = new HashMap<>();
        hashers.put(Checksum.Algorithm.CRC32C, hasher);
        if (args.file() != null) {
          Checksum.update(hashers, args.file(), args.length());
        } else if (args.buffer() != null) {
          Checksum.update(hashers, args.buffer());
        } else if (args.data() != null) {
          Checksum.update(hashers, args.data(), args.length().intValue());
        }
        byte[] sum = hasher.sum();
        headers.put(Checksum.Algorithm.CRC32C.header(), Checksum.base64String(sum));
        headers.put(
            Http.Headers.X_AMZ_SDK_CHECKSUM_ALGORITHM, Checksum.Algorithm.CRC32C.toString());
      } else {
        sha256Base64String = Checksum.base64String(Checksum.hexStringToSum(sha256HexString));
        headers.put(Http.Headers.X_AMZ_CHECKSUM_SHA256, sha256Base64String);
        headers.put(
            Http.Headers.X_AMZ_SDK_CHECKSUM_ALGORITHM, Checksum.Algorithm.SHA256.toString());
      }
    }

    Http.Body body = null;
    if (args.file() != null) {
      body = new Http.Body(args.file(), args.length(), contentType, sha256HexString, md5Hash);
    } else if (args.buffer() != null) {
      body = new Http.Body(args.buffer(), contentType, sha256HexString, md5Hash);
    } else if (args.data() != null) {
      body =
          new Http.Body(
              args.data(), args.length().intValue(), contentType, sha256HexString, md5Hash);
    } else {
      throw new InternalException("unknown body found; this should not happen");
    }

    return new Object[] {body, headers};
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject S3
   * API</a> asynchronously.
   *
   * @param args {@link PutObjectAPIArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> putObject(PutObjectAPIArgs args) {
    checkArgs(args);

    Object[] result = null;
    try {
      result = createBody(args, args.contentType());
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    Http.Body body = (Http.Body) result[0];
    Http.Headers headers = (Http.Headers) result[1];
    return executePutAsync(args, headers, null, body)
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
   */
  public CompletableFuture<UploadPartResponse> uploadPart(UploadPartArgs args) {
    checkArgs(args);

    Object[] result = null;
    try {
      result = createBody(args, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    Http.Body body = (Http.Body) result[0];
    Http.Headers headers = (Http.Headers) result[1];
    return executePutAsync(
            args,
            headers,
            new Http.QueryParameters(
                "partNumber", Integer.toString(args.partNumber()), "uploadId", args.uploadId()),
            body)
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
   */
  public CompletableFuture<UploadPartCopyResponse> uploadPartCopy(UploadPartCopyArgs args) {
    checkArgs(args);
    return executePutAsync(
            args,
            args.headers(),
            new Http.QueryParameters(
                "partNumber", Integer.toString(args.partNumber()), "uploadId", args.uploadId()),
            null)
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
