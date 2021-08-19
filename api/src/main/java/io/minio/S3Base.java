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

package io.minio;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import io.minio.messages.CompleteMultipartUpload;
import io.minio.messages.CompleteMultipartUploadOutput;
import io.minio.messages.CopyPartResult;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteMarker;
import io.minio.messages.DeleteObject;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.ErrorResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.Item;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListBucketResultV2;
import io.minio.messages.ListMultipartUploadsResult;
import io.minio.messages.ListObjectsResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.ListVersionsResult;
import io.minio.messages.LocationConstraint;
import io.minio.messages.NotificationRecords;
import io.minio.messages.Part;
import io.minio.messages.Prefix;

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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Core S3 API client.
 */
public abstract class S3Base {
    static {
        try {
            RequestBody.create(new byte[]{}, null);
        } catch (NoSuchMethodError ex) {
            throw new RuntimeException("Unsupported OkHttp library found. Must use okhttp >= 4.8.1", ex);
        }
    }

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static final String NO_SUCH_BUCKET_MESSAGE = "Bucket does not exist";
    protected static final String NO_SUCH_BUCKET = "NoSuchBucket";
    protected static final String NO_SUCH_BUCKET_POLICY = "NoSuchBucketPolicy";
    protected static final String NO_SUCH_OBJECT_LOCK_CONFIGURATION = "NoSuchObjectLockConfiguration";
    protected static final String SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR =
            "ServerSideEncryptionConfigurationNotFoundError";
    protected static final byte[] EMPTY_BODY = new byte[]{};
    // default network I/O timeout is 5 minutes
    protected static final long DEFAULT_CONNECTION_TIMEOUT = 5;
    // maximum allowed bucket policy size is 20KiB
    protected static final int MAX_BUCKET_POLICY_SIZE = 20 * 1024;
    protected static final String US_EAST_1 = "us-east-1";
    protected final Map<String, String> regionCache = new ConcurrentHashMap<>();

    private static final String RETRY_HEAD = "RetryHead";
    private static final String DEFAULT_USER_AGENT =
            "MinIO ("
                    + System.getProperty("os.name")
                    + "; "
                    + System.getProperty("os.arch")
                    + ") minio-java/"
                    + MinioProperties.INSTANCE.getVersion();
    private static final String END_HTTP = "----------END-HTTP----------";
    private static final String UPLOAD_ID = "uploadId";
    private static final Set<String> TRACE_QUERY_PARAMS =
            ImmutableSet.of("retention", "legal-hold", "tagging", UPLOAD_ID);
    private String userAgent = DEFAULT_USER_AGENT;
    private PrintWriter traceStream;

    protected HttpUrl baseUrl;
    protected String region;
    protected Provider provider;

    private boolean isAwsHost;
    private boolean isAcceleratedHost;
    private boolean isDualStackHost;
    private boolean useVirtualStyle;
    private OkHttpClient httpClient;

    protected S3Base(
            HttpUrl baseUrl,
            String region,
            boolean isAwsHost,
            boolean isAcceleratedHost,
            boolean isDualStackHost,
            boolean useVirtualStyle,
            Provider provider,
            OkHttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.region = region;
        this.isAwsHost = isAwsHost;
        this.isAcceleratedHost = isAcceleratedHost;
        this.isDualStackHost = isDualStackHost;
        this.useVirtualStyle = useVirtualStyle;
        this.provider = provider;
        this.httpClient = httpClient;
    }

    protected S3Base(S3Base client) {
        this.baseUrl = client.baseUrl;
        this.region = client.region;
        this.isAwsHost = client.isAwsHost;
        this.isAcceleratedHost = client.isAcceleratedHost;
        this.isDualStackHost = client.isDualStackHost;
        this.useVirtualStyle = client.useVirtualStyle;
        this.provider = client.provider;
        this.httpClient = client.httpClient;
    }

    /**
     * Check whether argument is valid or not.
     */
    protected void checkArgs(BaseArgs args) {
        if (args == null) throw new IllegalArgumentException("null arguments");
    }

    /**
     * Merge two Multimaps.
     */
    protected Multimap<String, String> merge(
            Multimap<String, String> m1, Multimap<String, String> m2) {
        Multimap<String, String> map = HashMultimap.create();
        if (m1 != null) map.putAll(m1);
        if (m2 != null) map.putAll(m2);
        return map;
    }

    /**
     * Create new HashMultimap by alternating keys and values.
     */
    protected Multimap<String, String> newMultimap(String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected alternating keys and values");
        }

        Multimap<String, String> map = HashMultimap.create();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }

        return map;
    }

    /**
     * Create new HashMultimap with copy of Map.
     */
    protected Multimap<String, String> newMultimap(Map<String, String> map) {
        return (map != null) ? Multimaps.forMap(map) : HashMultimap.create();
    }

    /**
     * Create new HashMultimap with copy of Multimap.
     */
    protected Multimap<String, String> newMultimap(Multimap<String, String> map) {
        return (map != null) ? HashMultimap.create(map) : HashMultimap.create();
    }

    private String[] handleRedirectResponse(
            Method method, String bucketName, Response response, boolean retry) {
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
                && method.equals(Method.HEAD)
                && bucketName != null
                && regionCache.get(bucketName) != null) {
            code = RETRY_HEAD;
            message = null;
        }

        return new String[]{code, message};
    }

    /**
     * Build URL for given parameters.
     */
    protected HttpUrl buildAdminUrl(
            String action,
            Multimap<String, String> queryParamMap) {

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

    /**
     * Build URL for given parameters.
     */
    protected HttpUrl buildUrl(
            Method method,
            String bucketName,
            String objectName,
            String region,
            Multimap<String, String> queryParamMap)
            throws NoSuchAlgorithmException {
        if (bucketName == null && objectName != null) {
            throw new IllegalArgumentException("null bucket name for object '" + objectName + "'");
        }

        HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();
        String host = this.baseUrl.host();
        if (bucketName != null) {
            boolean enforcePathStyle = false;
            if (method == Method.PUT && objectName == null && queryParamMap == null) {
                // use path style for make bucket to workaround "AuthorizationHeaderMalformed" error from
                // s3.amazonaws.com
                enforcePathStyle = true;
            } else if (queryParamMap != null && queryParamMap.containsKey("location")) {
                // use path style for location query
                enforcePathStyle = true;
            } else if (bucketName.contains(".") && this.baseUrl.isHttps()) {
                // use path style where '.' in bucketName causes SSL certificate validation error
                enforcePathStyle = true;
            }

            if (isAwsHost) {
                String s3Domain = "s3.";
                if (isAcceleratedHost) {
                    if (bucketName.contains(".")) {
                        throw new IllegalArgumentException(
                                "bucket name '"
                                        + bucketName
                                        + "' with '.' is not allowed for accelerated endpoint");
                    }

                    if (!enforcePathStyle) s3Domain = "s3-accelerate.";
                }

                String dualStack = "";
                if (isDualStackHost) dualStack = "dualstack.";

                String endpoint = s3Domain + dualStack;
                if (enforcePathStyle || !isAcceleratedHost) endpoint += region + ".";

                host = endpoint + host;
            }

            if (enforcePathStyle || !useVirtualStyle) {
                urlBuilder.host(host);
                urlBuilder.addEncodedPathSegment(S3Escaper.encode(bucketName));
            } else {
                urlBuilder.host(bucketName + "." + host);
            }

            if (objectName != null) {
                // Limitation: OkHttp does not allow to add '.' and '..' as path segment.
                for (String token : objectName.split("/")) {
                    if (token.equals(".") || token.equals("..")) {
                        throw new IllegalArgumentException(
                                "object name with '.' or '..' path segment is not supported");
                    }
                }

                urlBuilder.addEncodedPathSegments(S3Escaper.encodePath(objectName));
            }
        } else {
            if (isAwsHost) urlBuilder.host("s3." + region + "." + host);
        }

        if (queryParamMap != null) {
            for (Map.Entry<String, String> entry : queryParamMap.entries()) {
                urlBuilder.addEncodedQueryParameter(
                        S3Escaper.encode(entry.getKey()), S3Escaper.encode(entry.getValue()));
            }
        }

        return urlBuilder.build();
    }

    private String getHostHeader(HttpUrl url) {
        // ignore port when port and service matches i.e HTTP -> 80, HTTPS -> 443
        if ((url.scheme().equals("http") && url.port() == 80)
                || (url.scheme().equals("https") && url.port() == 443)) {
            return url.host();
        }

        return url.host() + ":" + url.port();
    }

    /**
     * Convert Multimap to Headers.
     */
    protected Headers httpHeaders(Multimap<String, String> headerMap) {
        Headers.Builder builder = new Headers.Builder();
        if (headerMap == null) return builder.build();

        if (headerMap.containsKey("Content-Encoding")) {
            builder.add(
                    "Content-Encoding",
                    headerMap.get("Content-Encoding").stream()
                            .distinct()
                            .filter(encoding -> !encoding.isEmpty())
                            .collect(Collectors.joining(",")));
        }

        for (Map.Entry<String, String> entry : headerMap.entries()) {
            if (!entry.getKey().equals("Content-Encoding")) {
                builder.addUnsafeNonAscii(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    /**
     * Create HTTP request for given paramaters.
     */
    protected Request createRequest(
            HttpUrl url, Method method, Headers headers, Object body, int length, Credentials creds)
            throws InsufficientDataException, InternalException, IOException, NoSuchAlgorithmException {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);

        if (headers != null) requestBuilder.headers(headers);
        requestBuilder.header("Host", getHostHeader(url));
        // Disable default gzip compression by okhttp library.
        requestBuilder.header("Accept-Encoding", "identity");
        requestBuilder.header("User-Agent", this.userAgent);

        String md5Hash = Digest.ZERO_MD5_HASH;
        if (body != null) {
            if (body instanceof PartSource) {
                md5Hash = ((PartSource) body).md5Hash();
            } else if (body instanceof byte[]) {
                md5Hash = Digest.md5Hash((byte[]) body, length);
            }
        }

        String sha256Hash = null;
        if (creds != null) {
            sha256Hash = Digest.ZERO_SHA256_HASH;
            if (!url.isHttps()) {
                if (body != null) {
                    if (body instanceof PartSource) {
                        sha256Hash = ((PartSource) body).sha256Hash();
                    } else if (body instanceof byte[]) {
                        sha256Hash = Digest.sha256Hash((byte[]) body, length);
                    }
                }
            } else {
                // Fix issue #415: No need to compute sha256 if endpoint scheme is HTTPS.
                sha256Hash = "UNSIGNED-PAYLOAD";
            }
        }

        if (md5Hash != null) requestBuilder.header("Content-MD5", md5Hash);
        if (sha256Hash != null) requestBuilder.header("x-amz-content-sha256", sha256Hash);

        if (creds != null && creds.sessionToken() != null) {
            requestBuilder.header("X-Amz-Security-Token", creds.sessionToken());
        }

        ZonedDateTime date = ZonedDateTime.now();
        requestBuilder.header("x-amz-date", date.format(Time.AMZ_DATE_FORMAT));

        RequestBody requestBody = null;
        if (body != null) {
            String contentType = (headers != null) ? headers.get("Content-Type") : null;
            if (body instanceof PartSource) {
                requestBody = new HttpRequestBody((PartSource) body, contentType);
            } else {
                requestBody = new HttpRequestBody((byte[]) body, length, contentType);
            }
        }

        requestBuilder.method(method.toString(), requestBody);
        return requestBuilder.build();
    }

    private StringBuilder newTraceBuilder(Request request, String body) {
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
        if (body != null) traceBuilder.append("\n").append(body);
        return traceBuilder;
    }

    /**
     * Execute HTTP request for given args and parameters.
     */
    protected Response execute(
            Method method,
            BaseArgs args,
            Multimap<String, String> headers,
            Multimap<String, String> queryParams,
            Object body,
            int length)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        String bucketName = null;
        String region = null;
        String objectName = null;

        if (args instanceof BucketArgs) {
            bucketName = ((BucketArgs) args).bucket();
            region = ((BucketArgs) args).region();
        }

        if (args instanceof ObjectArgs) objectName = ((ObjectArgs) args).object();

        return execute(
                method,
                bucketName,
                objectName,
                getRegion(bucketName, region),
                httpHeaders(merge(args.extraHeaders(), headers)),
                merge(args.extraQueryParams(), queryParams),
                body,
                length);
    }

    /**
     * Execute HTTP request for given parameters.
     */
    protected Response executeAdmin(
            Method method,
            String action,
            String region,
            Headers headers,
            Multimap<String, String> queryParamMap,
            Object body,
            int length)
            throws InsufficientDataException, InternalException,
            InvalidKeyException, IOException, NoSuchAlgorithmException {

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

    /**
     * Execute HTTP request for given parameters.
     */
    protected Response execute(
            Method method,
            String bucketName,
            String objectName,
            String region,
            Headers headers,
            Multimap<String, String> queryParamMap,
            Object body,
            int length)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        boolean traceRequestBody = false;
        if (body != null && !(body instanceof PartSource || body instanceof byte[])) {
            byte[] bytes;
            if (body instanceof CharSequence) {
                bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            } else {
                bytes = Xml.marshal(body).getBytes(StandardCharsets.UTF_8);
            }

            body = bytes;
            length = bytes.length;
            traceRequestBody = true;
        }

        if (body == null && (method == Method.PUT || method == Method.POST)) body = EMPTY_BODY;

        HttpUrl url = buildUrl(method, bucketName, objectName, region, queryParamMap);
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

        StringBuilder traceBuilder =
                newTraceBuilder(
                        request, traceRequestBody ? new String((byte[]) body, StandardCharsets.UTF_8) : null);
        PrintWriter traceStream = this.traceStream;
        if (traceStream != null) traceStream.println(traceBuilder.toString());
        traceBuilder.append("\n");

        OkHttpClient httpClient = this.httpClient;
        if (!(body instanceof byte[]) && (method == Method.PUT || method == Method.POST)) {
            // Issue #924: disable connection retry for PUT and POST methods for other than byte array.
            httpClient = this.httpClient.newBuilder().retryOnConnectionFailure(false).build();
        }

        Response response = httpClient.newCall(request).execute();
        String trace =
                response.protocol().toString().toUpperCase(Locale.US)
                        + " "
                        + response.code()
                        + "\n"
                        + response.headers();
        traceBuilder.append(trace).append("\n");
        if (traceStream != null) traceStream.println(trace);

        if (response.isSuccessful()) {
            if (traceStream != null) {
                // Trace response body only if the request is not GetObject/ListenBucketNotification S3 API.
                Set<String> keys = queryParamMap.keySet();
                if ((method != Method.GET
                        || objectName == null
                        || !Collections.disjoint(keys, TRACE_QUERY_PARAMS))
                        && !(keys.contains("events") && (keys.contains("prefix") || keys.contains("suffix")))) {
                    ResponseBody responseBody = response.peekBody(1024 * 1024);
                    traceStream.println(responseBody.string());
                }
                traceStream.println(END_HTTP);
            }
            return response;
        }

        String errorXml = null;
        try (ResponseBody responseBody = response.body()) {
            errorXml = responseBody.string();
        }

        if (!("".equals(errorXml) && method.equals(Method.HEAD))) {
            traceBuilder.append(errorXml).append("\n");
            if (traceStream != null) traceStream.println(errorXml);
        }

        traceBuilder.append(END_HTTP).append("\n");
        if (traceStream != null) traceStream.println(END_HTTP);

        // Error in case of Non-XML response from server for non-HEAD requests.
        String contentType = response.headers().get("content-type");
        if (!method.equals(Method.HEAD)
                && (contentType == null
                || !Arrays.asList(contentType.split(";")).contains("application/xml"))) {
            throw new InvalidResponseException(
                    response.code(),
                    contentType,
                    errorXml.substring(0, errorXml.length() > 1024 ? 1024 : errorXml.length()),
                    traceBuilder.toString());
        }

        ErrorResponse errorResponse = null;
        if (!"".equals(errorXml)) {
            errorResponse = Xml.unmarshal(ErrorResponse.class, errorXml);
        } else if (!method.equals(Method.HEAD)) {
            throw new InvalidResponseException(
                    response.code(), contentType, errorXml, traceBuilder.toString());
        }

        if (errorResponse == null) {
            String code = null;
            String message = null;
            switch (response.code()) {
                case 301:
                case 307:
                case 400:
                    String[] result = handleRedirectResponse(method, bucketName, response, true);
                    code = result[0];
                    message = result[1];
                    break;
                case 404:
                    if (objectName != null) {
                        code = "NoSuchKey";
                        message = "Object does not exist";
                    } else if (bucketName != null) {
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
                    if (bucketName != null) {
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
                    if (response.code() >= 500) {
                        throw new ServerException(
                                "server failed with HTTP status code " + response.code(), traceBuilder.toString());
                    }

                    throw new InternalException(
                            "unhandled HTTP code "
                                    + response.code()
                                    + ".  Please report this issue at "
                                    + "https://github.com/minio/minio-java/issues",
                            traceBuilder.toString());
            }

            errorResponse =
                    new ErrorResponse(
                            code,
                            message,
                            bucketName,
                            objectName,
                            request.url().encodedPath(),
                            response.header("x-amz-request-id"),
                            response.header("x-amz-id-2"));
        }

        // invalidate region cache if needed
        if (errorResponse.code().equals(NO_SUCH_BUCKET) || errorResponse.code().equals(RETRY_HEAD)) {
            regionCache.remove(bucketName);
        }

        throw new ErrorResponseException(errorResponse, response, traceBuilder.toString());
    }

    /**
     * Returns region of given bucket either from region cache or set in constructor.
     */
    protected String getRegion(String bucketName, String region)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        if (region != null) {
            // Error out if region does not match with region passed via constructor.
            if (this.region != null && !this.region.equals(region)) {
                throw new IllegalArgumentException(
                        "region must be " + this.region + ", but passed " + region);
            }
            return region;
        }

        if (this.region != null && !this.region.equals("")) return this.region;
        if (bucketName == null || this.provider == null) return US_EAST_1;
        region = regionCache.get(bucketName);
        if (region != null) return region;

        // Execute GetBucketLocation REST API to get region of the bucket.
        Response response =
                execute(
                        Method.GET, bucketName, null, US_EAST_1, null, newMultimap("location", null), null, 0);

        try (ResponseBody body = response.body()) {
            LocationConstraint lc = Xml.unmarshal(LocationConstraint.class, body.charStream());
            if (lc.location() == null || lc.location().equals("")) {
                region = US_EAST_1;
            } else if (lc.location().equals("EU")) {
                region = "eu-west-1"; // eu-west-1 is also referred as 'EU'.
            } else {
                region = lc.location();
            }
        }

        regionCache.put(bucketName, region);
        return region;
    }

    /**
     * Execute GET HTTP request for given parameters.
     */
    protected Response executeGet(
            BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return execute(Method.GET, args, headers, queryParams, null, 0);
    }

    /**
     * Execute HEAD HTTP request for given parameters.
     */
    protected Response executeHead(
            BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        try {
            Response response = execute(Method.HEAD, args, headers, queryParams, null, 0);
            response.body().close();
            return response;
        } catch (ErrorResponseException e) {
            if (!e.errorResponse().code().equals(RETRY_HEAD)) {
                throw e;
            }
        }

        try {
            // Retry once for RETRY_HEAD error.
            Response response = execute(Method.HEAD, args, headers, queryParams, null, 0);
            response.body().close();
            return response;
        } catch (ErrorResponseException e) {
            ErrorResponse errorResponse = e.errorResponse();
            if (!errorResponse.code().equals(RETRY_HEAD)) {
                throw e;
            }

            String[] result =
                    handleRedirectResponse(Method.HEAD, errorResponse.bucketName(), e.response(), false);
            throw new ErrorResponseException(
                    new ErrorResponse(
                            result[0],
                            result[1],
                            errorResponse.bucketName(),
                            errorResponse.objectName(),
                            errorResponse.resource(),
                            errorResponse.requestId(),
                            errorResponse.hostId()),
                    e.response(),
                    e.httpTrace());
        }
    }

    /**
     * Execute DELETE HTTP request for given parameters.
     */
    protected Response executeDelete(
            BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        Response response = execute(Method.DELETE, args, headers, queryParams, null, 0);
        response.body().close();
        return response;
    }

    /**
     * Execute POST HTTP request for given parameters.
     */
    protected Response executePost(
            BaseArgs args,
            Multimap<String, String> headers,
            Multimap<String, String> queryParams,
            Object data)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return execute(Method.POST, args, headers, queryParams, data, 0);
    }

    /**
     * Execute PUT HTTP request for given parameters.
     */
    protected Response executePut(
            BaseArgs args,
            Multimap<String, String> headers,
            Multimap<String, String> queryParams,
            Object data,
            int length)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return execute(Method.PUT, args, headers, queryParams, data, length);
    }

    /**
     * Calculate part count of given compose sources.
     */
    protected int calculatePartCount(List<ComposeSource> sources)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        long objectSize = 0;
        int partCount = 0;
        int i = 0;
        for (ComposeSource src : sources) {
            i++;
            StatObjectResponse stat = statObject(new StatObjectArgs((ObjectReadArgs) src));

            src.buildHeaders(stat.size(), stat.etag());

            long size = stat.size();
            if (src.length() != null) {
                size = src.length();
            } else if (src.offset() != null) {
                size -= src.offset();
            }

            if (size < ObjectWriteArgs.MIN_MULTIPART_SIZE && sources.size() != 1 && i != sources.size()) {
                throw new IllegalArgumentException(
                        "source "
                                + src.bucket()
                                + "/"
                                + src.object()
                                + ": size "
                                + size
                                + " must be greater than "
                                + ObjectWriteArgs.MIN_MULTIPART_SIZE);
            }

            objectSize += size;
            if (objectSize > ObjectWriteArgs.MAX_OBJECT_SIZE) {
                throw new IllegalArgumentException(
                        "destination object size must be less than " + ObjectWriteArgs.MAX_OBJECT_SIZE);
            }

            if (size > ObjectWriteArgs.MAX_PART_SIZE) {
                long count = size / ObjectWriteArgs.MAX_PART_SIZE;
                long lastPartSize = size - (count * ObjectWriteArgs.MAX_PART_SIZE);
                if (lastPartSize > 0) {
                    count++;
                } else {
                    lastPartSize = ObjectWriteArgs.MAX_PART_SIZE;
                }

                if (lastPartSize < ObjectWriteArgs.MIN_MULTIPART_SIZE
                        && sources.size() != 1
                        && i != sources.size()) {
                    throw new IllegalArgumentException(
                            "source "
                                    + src.bucket()
                                    + "/"
                                    + src.object()
                                    + ": "
                                    + "for multipart split upload of "
                                    + size
                                    + ", last part size is less than "
                                    + ObjectWriteArgs.MIN_MULTIPART_SIZE);
                }
                partCount += (int) count;
            } else {
                partCount++;
            }

            if (partCount > ObjectWriteArgs.MAX_MULTIPART_COUNT) {
                throw new IllegalArgumentException(
                        "Compose sources create more than allowed multipart count "
                                + ObjectWriteArgs.MAX_MULTIPART_COUNT);
            }
        }

        return partCount;
    }

    private abstract class ObjectIterator implements Iterator<Result<Item>> {
        protected Result<Item> error;
        protected Iterator<? extends Item> itemIterator;
        protected Iterator<DeleteMarker> deleteMarkerIterator;
        protected Iterator<Prefix> prefixIterator;
        protected boolean completed = false;
        protected ListObjectsResult listObjectsResult;
        protected String lastObjectName;

        protected abstract void populateResult()
                throws ErrorResponseException, InsufficientDataException, InternalException,
                InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
                ServerException, XmlParserException;

        protected synchronized void populate() {
            try {
                populateResult();
            } catch (ErrorResponseException
                    | InsufficientDataException
                    | InternalException
                    | InvalidKeyException
                    | InvalidResponseException
                    | IOException
                    | NoSuchAlgorithmException
                    | ServerException
                    | XmlParserException e) {
                this.error = new Result<>(e);
            }

            if (this.listObjectsResult != null) {
                this.itemIterator = this.listObjectsResult.contents().iterator();
                this.deleteMarkerIterator = this.listObjectsResult.deleteMarkers().iterator();
                this.prefixIterator = this.listObjectsResult.commonPrefixes().iterator();
            } else {
                this.itemIterator = new LinkedList<Item>().iterator();
                this.deleteMarkerIterator = new LinkedList<DeleteMarker>().iterator();
                this.prefixIterator = new LinkedList<Prefix>().iterator();
            }
        }

        @Override
        public boolean hasNext() {
            if (this.completed) return false;

            if (this.error == null
                    && this.itemIterator == null
                    && this.deleteMarkerIterator == null
                    && this.prefixIterator == null) {
                populate();
            }

            if (this.error == null
                    && !this.itemIterator.hasNext()
                    && !this.deleteMarkerIterator.hasNext()
                    && !this.prefixIterator.hasNext()
                    && this.listObjectsResult.isTruncated()) {
                populate();
            }

            if (this.error != null) return true;
            if (this.itemIterator.hasNext()) return true;
            if (this.deleteMarkerIterator.hasNext()) return true;
            if (this.prefixIterator.hasNext()) return true;

            this.completed = true;
            return false;
        }

        @Override
        public Result<Item> next() {
            if (this.completed) throw new NoSuchElementException();
            if (this.error == null
                    && this.itemIterator == null
                    && this.deleteMarkerIterator == null
                    && this.prefixIterator == null) {
                populate();
            }

            if (this.error == null
                    && !this.itemIterator.hasNext()
                    && !this.deleteMarkerIterator.hasNext()
                    && !this.prefixIterator.hasNext()
                    && this.listObjectsResult.isTruncated()) {
                populate();
            }

            if (this.error != null) {
                this.completed = true;
                return this.error;
            }

            Item item = null;
            if (this.itemIterator.hasNext()) {
                item = this.itemIterator.next();
                item.setEncodingType(this.listObjectsResult.encodingType());
                this.lastObjectName = item.objectName();
            } else if (this.deleteMarkerIterator.hasNext()) {
                item = this.deleteMarkerIterator.next();
            } else if (this.prefixIterator.hasNext()) {
                item = this.prefixIterator.next().toItem();
            }

            if (item != null) {
                item.setEncodingType(this.listObjectsResult.encodingType());
                return new Result<>(item);
            }

            this.completed = true;
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Execute list objects v2.
     */
    protected Iterable<Result<Item>> listObjectsV2(ListObjectsArgs args) {
        return new Iterable<Result<Item>>() {
            @Override
            public Iterator<Result<Item>> iterator() {
                return new ObjectIterator() {
                    private ListBucketResultV2 result = null;

                    @Override
                    protected void populateResult()
                            throws ErrorResponseException, InsufficientDataException, InternalException,
                            InvalidKeyException, InvalidResponseException, IOException,
                            NoSuchAlgorithmException, ServerException, XmlParserException {
                        this.listObjectsResult = null;
                        this.itemIterator = null;
                        this.prefixIterator = null;

                        ListObjectsV2Response response =
                                listObjectsV2(
                                        args.bucket(),
                                        args.region(),
                                        args.delimiter(),
                                        args.useUrlEncodingType() ? "url" : null,
                                        args.startAfter(),
                                        args.maxKeys(),
                                        args.prefix(),
                                        (result == null) ? args.continuationToken() : result.nextContinuationToken(),
                                        args.fetchOwner(),
                                        args.includeUserMetadata(),
                                        args.extraHeaders(),
                                        args.extraQueryParams());
                        result = response.result();
                        this.listObjectsResult = response.result();
                    }
                };
            }
        };
    }

    /**
     * Execute list objects v1.
     */
    protected Iterable<Result<Item>> listObjectsV1(ListObjectsArgs args) {
        return new Iterable<Result<Item>>() {
            @Override
            public Iterator<Result<Item>> iterator() {
                return new ObjectIterator() {
                    private ListBucketResultV1 result = null;

                    @Override
                    protected void populateResult()
                            throws ErrorResponseException, InsufficientDataException, InternalException,
                            InvalidKeyException, InvalidResponseException, IOException,
                            NoSuchAlgorithmException, ServerException, XmlParserException {
                        this.listObjectsResult = null;
                        this.itemIterator = null;
                        this.prefixIterator = null;

                        String nextMarker = (result == null) ? args.marker() : result.nextMarker();
                        if (nextMarker == null) nextMarker = this.lastObjectName;

                        ListObjectsV1Response response =
                                listObjectsV1(
                                        args.bucket(),
                                        args.region(),
                                        args.delimiter(),
                                        args.useUrlEncodingType() ? "url" : null,
                                        nextMarker,
                                        args.maxKeys(),
                                        args.prefix(),
                                        args.extraHeaders(),
                                        args.extraQueryParams());
                        result = response.result();
                        this.listObjectsResult = response.result();
                    }
                };
            }
        };
    }

    /**
     * Execute list object versions.
     */
    protected Iterable<Result<Item>> listObjectVersions(ListObjectsArgs args) {
        return new Iterable<Result<Item>>() {
            @Override
            public Iterator<Result<Item>> iterator() {
                return new ObjectIterator() {
                    private ListVersionsResult result = null;

                    @Override
                    protected void populateResult()
                            throws ErrorResponseException, InsufficientDataException, InternalException,
                            InvalidKeyException, InvalidResponseException, IOException,
                            NoSuchAlgorithmException, ServerException, XmlParserException {
                        this.listObjectsResult = null;
                        this.itemIterator = null;
                        this.prefixIterator = null;

                        ListObjectVersionsResponse response =
                                listObjectVersions(
                                        args.bucket(),
                                        args.region(),
                                        args.delimiter(),
                                        args.useUrlEncodingType() ? "url" : null,
                                        (result == null) ? args.keyMarker() : result.nextKeyMarker(),
                                        args.maxKeys(),
                                        args.prefix(),
                                        (result == null) ? args.versionIdMarker() : result.nextVersionIdMarker(),
                                        args.extraHeaders(),
                                        args.extraQueryParams());
                        result = response.result();
                        this.listObjectsResult = response.result();
                    }
                };
            }
        };
    }

    private PartReader newPartReader(Object data, long objectSize, long partSize, int partCount) {
        if (data instanceof RandomAccessFile) {
            return new PartReader((RandomAccessFile) data, objectSize, partSize, partCount);
        }

        if (data instanceof InputStream) {
            return new PartReader((InputStream) data, objectSize, partSize, partCount);
        }

        return null;
    }

    /**
     * Execute put object.
     */
    protected ObjectWriteResponse putObject(
            PutObjectBaseArgs args,
            Object data,
            long objectSize,
            long partSize,
            int partCount,
            String contentType)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        Multimap<String, String> headers = newMultimap(args.extraHeaders());
        headers.putAll(args.genHeaders());
        if (!headers.containsKey("Content-Type")) headers.put("Content-Type", contentType);

        String uploadId = null;
        Part[] parts = null;

        PartReader partReader = newPartReader(data, objectSize, partSize, partCount);
        if (partReader == null) {
            throw new IllegalArgumentException("data must be RandomAccessFile or InputStream");
        }

        try {
            while (true) {
                PartSource partSource = partReader.getPart(!this.baseUrl.isHttps());
                if (partSource == null) break;

                if (partReader.partCount() == 1) {
                    return putObject(
                            args.bucket(),
                            args.region(),
                            args.object(),
                            partSource,
                            headers,
                            args.extraQueryParams());
                }

                if (uploadId == null) {
                    CreateMultipartUploadResponse response =
                            createMultipartUpload(
                                    args.bucket(), args.region(), args.object(), headers, args.extraQueryParams());
                    uploadId = response.result().uploadId();
                    parts = new Part[ObjectWriteArgs.MAX_MULTIPART_COUNT];
                }

                Map<String, String> ssecHeaders = null;
                // set encryption headers in the case of SSE-C.
                if (args.sse() != null && args.sse() instanceof ServerSideEncryptionCustomerKey) {
                    ssecHeaders = args.sse().headers();
                }

                int partNumber = partSource.partNumber();
                UploadPartResponse response =
                        uploadPart(
                                args.bucket(),
                                args.region(),
                                args.object(),
                                partSource,
                                partNumber,
                                uploadId,
                                (ssecHeaders != null) ? Multimaps.forMap(ssecHeaders) : null,
                                null);
                String etag = response.etag();
                parts[partNumber - 1] = new Part(partNumber, etag);
            }

            return completeMultipartUpload(
                    args.bucket(), args.region(), args.object(), uploadId, parts, null, null);
        } catch (RuntimeException e) {
            if (uploadId != null) {
                abortMultipartUpload(args.bucket(), args.region(), args.object(), uploadId, null, null);
            }
            throw e;
        } catch (Exception e) {
            if (uploadId != null) {
                abortMultipartUpload(args.bucket(), args.region(), args.object(), uploadId, null, null);
            }
            throw e;
        }
    }

    /**
     * Notification result records representation.
     */
    protected static class NotificationResultRecords {
        Response response = null;
        Scanner scanner = null;
        ObjectMapper mapper = null;

        public NotificationResultRecords(Response response) {
            this.response = response;
            this.scanner = new Scanner(response.body().charStream()).useDelimiter("\n");
            this.mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        }

        /**
         * returns closeable iterator of result of notification records.
         */
        public CloseableIterator<Result<NotificationRecords>> closeableIterator() {
            return new CloseableIterator<Result<NotificationRecords>>() {
                String recordsString = null;
                NotificationRecords records = null;
                boolean isClosed = false;

                @Override
                public void close() throws IOException {
                    if (!isClosed) {
                        try {
                            response.body().close();
                            scanner.close();
                        } finally {
                            isClosed = true;
                        }
                    }
                }

                public boolean populate() {
                    if (isClosed) return false;
                    if (recordsString != null) return true;

                    while (scanner.hasNext()) {
                        recordsString = scanner.next().trim();
                        if (!recordsString.equals("")) break;
                    }

                    if (recordsString == null || recordsString.equals("")) {
                        try {
                            close();
                        } catch (IOException e) {
                            isClosed = true;
                        }
                        return false;
                    }
                    return true;
                }

                @Override
                public boolean hasNext() {
                    return populate();
                }

                @Override
                public Result<NotificationRecords> next() {
                    if (isClosed) throw new NoSuchElementException();
                    if ((recordsString == null || recordsString.equals("")) && !populate()) {
                        throw new NoSuchElementException();
                    }

                    try {
                        records = mapper.readValue(recordsString, NotificationRecords.class);
                        return new Result<>(records);
                    } catch (JsonMappingException e) {
                        return new Result<>(e);
                    } catch (JsonParseException e) {
                        return new Result<>(e);
                    } catch (IOException e) {
                        return new Result<>(e);
                    } finally {
                        recordsString = null;
                        records = null;
                    }
                }
            };
        }
    }

    private Multimap<String, String> getCommonListObjectsQueryParams(
            String delimiter, String encodingType, Integer maxKeys, String prefix) {
        Multimap<String, String> queryParams =
                newMultimap(
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
     * Sets HTTP connect, write and read timeouts. A value of 0 means no timeout, otherwise values
     * must be between 1 and Integer.MAX_VALUE when converted to milliseconds.
     *
     * <pre>Example:{@code
     * minioClient.setTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10),
     *     TimeUnit.SECONDS.toMillis(30));
     * }</pre>
     *
     * @param connectTimeout HTTP connect timeout in milliseconds.
     * @param writeTimeout   HTTP write timeout in milliseconds.
     * @param readTimeout    HTTP read timeout in milliseconds.
     */
    public void setTimeout(long connectTimeout, long writeTimeout, long readTimeout) {
        this.httpClient =
                this.httpClient
                        .newBuilder()
                        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                        .build();
    }

    /**
     * Ignores check on server certificate for HTTPS connection.
     *
     * <pre>Example:{@code
     * minioClient.ignoreCertCheck();
     * }</pre>
     *
     * @throws KeyManagementException   thrown to indicate key management error.
     * @throws NoSuchAlgorithmException thrown to indicate missing of SSL library.
     */
    @SuppressFBWarnings(value = "SIC", justification = "Should not be used in production anyways.")
    public void ignoreCertCheck() throws KeyManagementException, NoSuchAlgorithmException {
        final TrustManager[] trustAllCerts =
                new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[]{};
                            }
                        }
                };

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        this.httpClient =
                this.httpClient
                        .newBuilder()
                        .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier(
                                new HostnameVerifier() {
                                    @Override
                                    public boolean verify(String hostname, SSLSession session) {
                                        return true;
                                    }
                                })
                        .build();
    }

    /**
     * Sets application's name/version to user agent. For more information about user agent refer <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">#rfc2616</a>.
     *
     * @param name    Your application name.
     * @param version Your application version.
     */
    @SuppressWarnings("unused")
    public void setAppInfo(String name, String version) {
        if (name == null || version == null) return; // nothing to do
        this.userAgent = DEFAULT_USER_AGENT + " " + name.trim() + "/" + version.trim();
    }

    /**
     * Enables HTTP call tracing and written to traceStream.
     *
     * @param traceStream {@link OutputStream} for writing HTTP call tracing.
     * @see #traceOff
     */
    public void traceOn(OutputStream traceStream) {
        if (traceStream == null) {
            throw new NullPointerException();
        } else {
            this.traceStream =
                    new PrintWriter(new OutputStreamWriter(traceStream, StandardCharsets.UTF_8), true);
        }
    }

    /**
     * Disables HTTP call tracing previously enabled.
     *
     * @throws IOException upon connection error
     * @see #traceOn
     */
    public void traceOff() throws IOException {
        this.traceStream = null;
    }

    /**
     * Enables accelerate endpoint for Amazon S3 endpoint.
     */
    public void enableAccelerateEndpoint() {
        this.isAcceleratedHost = true;
    }

    /**
     * Disables accelerate endpoint for Amazon S3 endpoint.
     */
    public void disableAccelerateEndpoint() {
        this.isAcceleratedHost = false;
    }

    /**
     * Enables dual-stack endpoint for Amazon S3 endpoint.
     */
    public void enableDualStackEndpoint() {
        this.isDualStackHost = true;
    }

    /**
     * Disables dual-stack endpoint for Amazon S3 endpoint.
     */
    public void disableDualStackEndpoint() {
        this.isDualStackHost = false;
    }

    /**
     * Enables virtual-style endpoint.
     */
    public void enableVirtualStyleEndpoint() {
        this.useVirtualStyle = true;
    }

    /**
     * Disables virtual-style endpoint.
     */
    public void disableVirtualStyleEndpoint() {
        this.useVirtualStyle = false;
    }

    /**
     * Execute stat object.
     */
    protected StatObjectResponse statObject(StatObjectArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        checkArgs(args);
        args.validateSsec(baseUrl);
        Response response =
                executeHead(
                        args,
                        args.getHeaders(),
                        (args.versionId() != null) ? newMultimap("versionId", args.versionId()) : null);
        return new StatObjectResponse(response.headers(), args.bucket(), args.region(), args.object());
    }

    /**
     * Do <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html">AbortMultipartUpload
     * S3 API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region of the bucket.
     * @param objectName       Object name in the bucket.
     * @param uploadId         Upload ID.
     * @param extraHeaders     Extra headers (Optional).
     * @param extraQueryParams Extra query parameters (Optional).
     * @return {@link AbortMultipartUploadResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected AbortMultipartUploadResponse abortMultipartUpload(
            String bucketName,
            String region,
            String objectName,
            String uploadId,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        try (Response response =
                     execute(
                             Method.DELETE,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             merge(extraQueryParams, newMultimap(UPLOAD_ID, uploadId)),
                             null,
                             0)) {
            return new AbortMultipartUploadResponse(
                    response.headers(), bucketName, region, objectName, uploadId);
        }
    }

    /**
     * Do <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload
     * S3 API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region of the bucket.
     * @param objectName       Object name in the bucket.
     * @param uploadId         Upload ID.
     * @param parts            List of parts.
     * @param extraHeaders     Extra headers (Optional).
     * @param extraQueryParams Extra query parameters (Optional).
     * @return {@link ObjectWriteResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected ObjectWriteResponse completeMultipartUpload(
            String bucketName,
            String region,
            String objectName,
            String uploadId,
            Part[] parts,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        Multimap<String, String> queryParams = newMultimap(extraQueryParams);
        queryParams.put(UPLOAD_ID, uploadId);

        try (Response response =
                     execute(
                             Method.POST,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             queryParams,
                             new CompleteMultipartUpload(parts),
                             0)) {
            String bodyContent = response.body().string();
            bodyContent = bodyContent.trim();
            if (!bodyContent.isEmpty()) {
                try {
                    if (Xml.validate(ErrorResponse.class, bodyContent)) {
                        ErrorResponse errorResponse = Xml.unmarshal(ErrorResponse.class, bodyContent);
                        throw new ErrorResponseException(errorResponse, response, null);
                    }
                } catch (XmlParserException e) {
                    // As it is not <Error> message, fall-back to parse CompleteMultipartUploadOutput XML.
                }

                try {
                    CompleteMultipartUploadOutput result =
                            Xml.unmarshal(CompleteMultipartUploadOutput.class, bodyContent);
                    return new ObjectWriteResponse(
                            response.headers(),
                            result.bucket(),
                            result.location(),
                            result.object(),
                            result.etag(),
                            response.header("x-amz-version-id"));
                } catch (XmlParserException e) {
                    // As this CompleteMultipartUpload REST call succeeded, just log it.
                    Logger.getLogger(MinioClient.class.getName())
                            .warning(
                                    "S3 service returned unknown XML for CompleteMultipartUpload REST API. "
                                            + bodyContent);
                }
            }

            return new ObjectWriteResponse(
                    response.headers(),
                    bucketName,
                    region,
                    objectName,
                    null,
                    response.header("x-amz-version-id"));
        }
    }

    /**
     * Do <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload
     * S3 API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region name of buckets in S3 service.
     * @param objectName       Object name in the bucket.
     * @param headers          Request headers.
     * @param extraQueryParams Extra query parameters for request (Optional).
     * @return {@link CreateMultipartUploadResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected CreateMultipartUploadResponse createMultipartUpload(
            String bucketName,
            String region,
            String objectName,
            Multimap<String, String> headers,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        Multimap<String, String> queryParams = newMultimap(extraQueryParams);
        queryParams.put("uploads", "");

        Multimap<String, String> headersCopy = newMultimap(headers);
        // set content type if not set already
        if (!headersCopy.containsKey("Content-Type")) {
            headersCopy.put("Content-Type", "application/octet-stream");
        }

        try (Response response =
                     execute(
                             Method.POST,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(headersCopy),
                             queryParams,
                             null,
                             0)) {
            InitiateMultipartUploadResult result =
                    Xml.unmarshal(InitiateMultipartUploadResult.class, response.body().charStream());
            return new CreateMultipartUploadResponse(
                    response.headers(), bucketName, region, objectName, result);
        }
    }

    /**
     * Do <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html">DeleteObjects S3
     * API</a>.
     *
     * @param bucketName           Name of the bucket.
     * @param region               Region of the bucket (Optional).
     * @param objectList           List of object names.
     * @param quiet                Quiet flag.
     * @param bypassGovernanceMode Bypass Governance retention mode.
     * @param extraHeaders         Extra headers for request (Optional).
     * @param extraQueryParams     Extra query parameters for request (Optional).
     * @return {@link DeleteObjectsResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected DeleteObjectsResponse deleteObjects(
            String bucketName,
            String region,
            List<DeleteObject> objectList,
            boolean quiet,
            boolean bypassGovernanceMode,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        if (objectList == null) objectList = new LinkedList<>();

        if (objectList.size() > 1000) {
            throw new IllegalArgumentException("list of objects must not be more than 1000");
        }

        Multimap<String, String> headers =
                merge(
                        extraHeaders,
                        bypassGovernanceMode ? newMultimap("x-amz-bypass-governance-retention", "true") : null);
        try (Response response =
                     execute(
                             Method.POST,
                             bucketName,
                             null,
                             getRegion(bucketName, region),
                             httpHeaders(headers),
                             merge(extraQueryParams, newMultimap("delete", "")),
                             new DeleteRequest(objectList, quiet),
                             0)) {
            String bodyContent = response.body().string();
            try {
                if (Xml.validate(DeleteError.class, bodyContent)) {
                    DeleteError error = Xml.unmarshal(DeleteError.class, bodyContent);
                    DeleteResult result = new DeleteResult(error);
                    return new DeleteObjectsResponse(response.headers(), bucketName, region, result);
                }
            } catch (XmlParserException e) {
                // Ignore this exception as it is not <Error> message,
                // but parse it as <DeleteResult> message below.
            }

            DeleteResult result = Xml.unmarshal(DeleteResult.class, bodyContent);
            return new DeleteObjectsResponse(response.headers(), bucketName, region, result);
        }
    }

    /**
     * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects
     * version 1 S3 API</a>.
     *
     * @param bucketName          Name of the bucket.
     * @param region              Region of the bucket (Optional).
     * @param delimiter           Delimiter (Optional).
     * @param encodingType        Encoding type (Optional).
     * @param startAfter          Fetch listing after this key (Optional).
     * @param maxKeys             Maximum object information to fetch (Optional).
     * @param prefix              Prefix (Optional).
     * @param continuationToken   Continuation token (Optional).
     * @param fetchOwner          Flag to fetch owner information (Optional).
     * @param includeUserMetadata MinIO extension flag to include user metadata (Optional).
     * @param extraHeaders        Extra headers for request (Optional).
     * @param extraQueryParams    Extra query parameters for request (Optional).
     * @return {@link ListObjectsV2Response} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected ListObjectsV2Response listObjectsV2(
            String bucketName,
            String region,
            String delimiter,
            String encodingType,
            String startAfter,
            Integer maxKeys,
            String prefix,
            String continuationToken,
            boolean fetchOwner,
            boolean includeUserMetadata,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException, IOException {
        Multimap<String, String> queryParams =
                merge(
                        extraQueryParams,
                        getCommonListObjectsQueryParams(delimiter, encodingType, maxKeys, prefix));
        queryParams.put("list-type", "2");
        if (continuationToken != null) queryParams.put("continuation-token", continuationToken);
        if (fetchOwner) queryParams.put("fetch-owner", "true");
        if (startAfter != null) queryParams.put("start-after", startAfter);
        if (includeUserMetadata) queryParams.put("metadata", "true");

        try (Response response =
                     execute(
                             Method.GET,
                             bucketName,
                             null,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             queryParams,
                             null,
                             0)) {
            ListBucketResultV2 result =
                    Xml.unmarshal(ListBucketResultV2.class, response.body().charStream());
            return new ListObjectsV2Response(response.headers(), bucketName, region, result);
        }
    }

    /**
     * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects
     * version 1 S3 API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region of the bucket (Optional).
     * @param delimiter        Delimiter (Optional).
     * @param encodingType     Encoding type (Optional).
     * @param marker           Marker (Optional).
     * @param maxKeys          Maximum object information to fetch (Optional).
     * @param prefix           Prefix (Optional).
     * @param extraHeaders     Extra headers for request (Optional).
     * @param extraQueryParams Extra query parameters for request (Optional).
     * @return {@link ListObjectsV1Response} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected ListObjectsV1Response listObjectsV1(
            String bucketName,
            String region,
            String delimiter,
            String encodingType,
            String marker,
            Integer maxKeys,
            String prefix,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        Multimap<String, String> queryParams =
                merge(
                        extraQueryParams,
                        getCommonListObjectsQueryParams(delimiter, encodingType, maxKeys, prefix));
        if (marker != null) queryParams.put("marker", marker);

        try (Response response =
                     execute(
                             Method.GET,
                             bucketName,
                             null,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             queryParams,
                             null,
                             0)) {
            ListBucketResultV1 result =
                    Xml.unmarshal(ListBucketResultV1.class, response.body().charStream());
            return new ListObjectsV1Response(response.headers(), bucketName, region, result);
        }
    }

    /**
     * Do <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html">ListObjectVersions
     * API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region of the bucket (Optional).
     * @param delimiter        Delimiter (Optional).
     * @param encodingType     Encoding type (Optional).
     * @param keyMarker        Key marker (Optional).
     * @param maxKeys          Maximum object information to fetch (Optional).
     * @param prefix           Prefix (Optional).
     * @param versionIdMarker  Version ID marker (Optional).
     * @param extraHeaders     Extra headers for request (Optional).
     * @param extraQueryParams Extra query parameters for request (Optional).
     * @return {@link ListObjectVersionsResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected ListObjectVersionsResponse listObjectVersions(
            String bucketName,
            String region,
            String delimiter,
            String encodingType,
            String keyMarker,
            Integer maxKeys,
            String prefix,
            String versionIdMarker,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        Multimap<String, String> queryParams =
                merge(
                        extraQueryParams,
                        getCommonListObjectsQueryParams(delimiter, encodingType, maxKeys, prefix));
        if (keyMarker != null) queryParams.put("key-marker", keyMarker);
        if (versionIdMarker != null) queryParams.put("version-id-marker", versionIdMarker);
        queryParams.put("versions", "");

        try (Response response =
                     execute(
                             Method.GET,
                             bucketName,
                             null,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             queryParams,
                             null,
                             0)) {
            ListVersionsResult result =
                    Xml.unmarshal(ListVersionsResult.class, response.body().charStream());
            return new ListObjectVersionsResponse(response.headers(), bucketName, region, result);
        }
    }

    private ObjectWriteResponse putObject(
            String bucketName,
            String region,
            String objectName,
            PartSource partSource,
            Multimap<String, String> headers,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        try (Response response =
                     execute(
                             Method.PUT,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(headers),
                             extraQueryParams,
                             partSource,
                             0)) {
            return new ObjectWriteResponse(
                    response.headers(),
                    bucketName,
                    region,
                    objectName,
                    response.header("ETag").replaceAll("\"", ""),
                    response.header("x-amz-version-id"));
        }
    }

    /**
     * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject S3
     * API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param objectName       Object name in the bucket.
     * @param data             Object data must be InputStream, RandomAccessFile, byte[] or String.
     * @param length           Length of object data.
     * @param headers          Additional headers.
     * @param extraQueryParams Additional query parameters if any.
     * @return {@link ObjectWriteResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected ObjectWriteResponse putObject(
            String bucketName,
            String region,
            String objectName,
            Object data,
            long length,
            Multimap<String, String> headers,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        if (!(data instanceof InputStream
                || data instanceof RandomAccessFile
                || data instanceof byte[]
                || data instanceof CharSequence)) {
            throw new IllegalArgumentException(
                    "data must be InputStream, RandomAccessFile, byte[] or String");
        }

        PartReader partReader = newPartReader(data, length, length, 1);

        if (partReader != null) {
            return putObject(
                    bucketName,
                    region,
                    objectName,
                    partReader.getPart(!this.baseUrl.isHttps()),
                    headers,
                    extraQueryParams);
        }

        try (Response response =
                     execute(
                             Method.PUT,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(headers),
                             extraQueryParams,
                             data,
                             (int) length)) {
            return new ObjectWriteResponse(
                    response.headers(),
                    bucketName,
                    region,
                    objectName,
                    response.header("ETag").replaceAll("\"", ""),
                    response.header("x-amz-version-id"));
        }
    }

    /**
     * Do <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListMultipartUploads.html">ListMultipartUploads
     * S3 API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region of the bucket (Optional).
     * @param delimiter        Delimiter (Optional).
     * @param encodingType     Encoding type (Optional).
     * @param keyMarker        Key marker (Optional).
     * @param maxUploads       Maximum upload information to fetch (Optional).
     * @param prefix           Prefix (Optional).
     * @param uploadIdMarker   Upload ID marker (Optional).
     * @param extraHeaders     Extra headers for request (Optional).
     * @param extraQueryParams Extra query parameters for request (Optional).
     * @return {@link ListMultipartUploadsResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected ListMultipartUploadsResponse listMultipartUploads(
            String bucketName,
            String region,
            String delimiter,
            String encodingType,
            String keyMarker,
            Integer maxUploads,
            String prefix,
            String uploadIdMarker,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        Multimap<String, String> queryParams =
                merge(
                        extraQueryParams,
                        newMultimap(
                                "uploads",
                                "",
                                "delimiter",
                                (delimiter != null) ? delimiter : "",
                                "max-uploads",
                                (maxUploads != null) ? maxUploads.toString() : "1000",
                                "prefix",
                                (prefix != null) ? prefix : "",
                                "encoding-type",
                                "url"));
        if (encodingType != null) queryParams.put("encoding-type", encodingType);
        if (keyMarker != null) queryParams.put("key-marker", keyMarker);
        if (uploadIdMarker != null) queryParams.put("upload-id-marker", uploadIdMarker);

        try (Response response =
                     execute(
                             Method.GET,
                             bucketName,
                             null,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             queryParams,
                             null,
                             0)) {
            ListMultipartUploadsResult result =
                    Xml.unmarshal(ListMultipartUploadsResult.class, response.body().charStream());
            return new ListMultipartUploadsResponse(response.headers(), bucketName, region, result);
        }
    }

    /**
     * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts S3
     * API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Name of the bucket (Optional).
     * @param objectName       Object name in the bucket.
     * @param maxParts         Maximum parts information to fetch (Optional).
     * @param partNumberMarker Part number marker (Optional).
     * @param uploadId         Upload ID.
     * @param extraHeaders     Extra headers for request (Optional).
     * @param extraQueryParams Extra query parameters for request (Optional).
     * @return {@link ListPartsResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected ListPartsResponse listParts(
            String bucketName,
            String region,
            String objectName,
            Integer maxParts,
            Integer partNumberMarker,
            String uploadId,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        Multimap<String, String> queryParams =
                merge(
                        extraQueryParams,
                        newMultimap(
                                UPLOAD_ID,
                                uploadId,
                                "max-parts",
                                (maxParts != null) ? maxParts.toString() : "1000"));
        if (partNumberMarker != null) {
            queryParams.put("part-number-marker", partNumberMarker.toString());
        }

        try (Response response =
                     execute(
                             Method.GET,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             queryParams,
                             null,
                             0)) {
            ListPartsResult result = Xml.unmarshal(ListPartsResult.class, response.body().charStream());
            return new ListPartsResponse(response.headers(), bucketName, region, objectName, result);
        }
    }

    private UploadPartResponse uploadPart(
            String bucketName,
            String region,
            String objectName,
            PartSource partSource,
            int partNumber,
            String uploadId,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        try (Response response =
                     execute(
                             Method.PUT,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             merge(
                                     extraQueryParams,
                                     newMultimap("partNumber", Integer.toString(partNumber), UPLOAD_ID, uploadId)),
                             partSource,
                             0)) {
            return new UploadPartResponse(
                    response.headers(),
                    bucketName,
                    region,
                    objectName,
                    uploadId,
                    partNumber,
                    response.header("ETag").replaceAll("\"", ""));
        }
    }

    /**
     * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart S3
     * API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region of the bucket (Optional).
     * @param objectName       Object name in the bucket.
     * @param data             Object data must be InputStream, RandomAccessFile, byte[] or String.
     * @param length           Length of object data.
     * @param uploadId         Upload ID.
     * @param partNumber       Part number.
     * @param extraHeaders     Extra headers for request (Optional).
     * @param extraQueryParams Extra query parameters for request (Optional).
     * @return String - Contains ETag.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected UploadPartResponse uploadPart(
            String bucketName,
            String region,
            String objectName,
            Object data,
            long length,
            String uploadId,
            int partNumber,
            Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        if (!(data instanceof InputStream
                || data instanceof RandomAccessFile
                || data instanceof byte[]
                || data instanceof CharSequence)) {
            throw new IllegalArgumentException(
                    "data must be InputStream, RandomAccessFile, byte[] or String");
        }

        PartReader partReader = newPartReader(data, length, length, 1);

        if (partReader != null) {
            return uploadPart(
                    bucketName,
                    region,
                    objectName,
                    partReader.getPart(!this.baseUrl.isHttps()),
                    partNumber,
                    uploadId,
                    extraHeaders,
                    extraQueryParams);
        }

        try (Response response =
                     execute(
                             Method.PUT,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(extraHeaders),
                             merge(
                                     extraQueryParams,
                                     newMultimap("partNumber", Integer.toString(partNumber), UPLOAD_ID, uploadId)),
                             data,
                             (int) length)) {
            return new UploadPartResponse(
                    response.headers(),
                    bucketName,
                    region,
                    objectName,
                    uploadId,
                    partNumber,
                    response.header("ETag").replaceAll("\"", ""));
        }
    }

    /**
     * Do <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy
     * S3 API</a>.
     *
     * @param bucketName       Name of the bucket.
     * @param region           Region of the bucket (Optional).
     * @param objectName       Object name in the bucket.
     * @param uploadId         Upload ID.
     * @param partNumber       Part number.
     * @param headers          Request headers with source object definitions.
     * @param extraQueryParams Extra query parameters for request (Optional).
     * @return {@link UploadPartCopyResponse} object.
     * @throws ErrorResponseException    thrown to indicate S3 service returned an error response.
     * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
     * @throws InternalException         thrown to indicate internal library error.
     * @throws InvalidKeyException       thrown to indicate missing of HMAC SHA-256 library.
     * @throws InvalidResponseException  thrown to indicate S3 service returned invalid or no error
     *                                   response.
     * @throws IOException               thrown to indicate I/O error on S3 operation.
     * @throws NoSuchAlgorithmException  thrown to indicate missing of MD5 or SHA-256 digest library.
     * @throws XmlParserException        thrown to indicate XML parsing error.
     */
    protected UploadPartCopyResponse uploadPartCopy(
            String bucketName,
            String region,
            String objectName,
            String uploadId,
            int partNumber,
            Multimap<String, String> headers,
            Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException,
            InvalidResponseException {
        try (Response response =
                     execute(
                             Method.PUT,
                             bucketName,
                             objectName,
                             getRegion(bucketName, region),
                             httpHeaders(headers),
                             merge(
                                     extraQueryParams,
                                     newMultimap("partNumber", Integer.toString(partNumber), "uploadId", uploadId)),
                             null,
                             0)) {
            CopyPartResult result = Xml.unmarshal(CopyPartResult.class, response.body().charStream());
            return new UploadPartCopyResponse(
                    response.headers(), bucketName, region, objectName, uploadId, partNumber, result);
        }
    }
}
