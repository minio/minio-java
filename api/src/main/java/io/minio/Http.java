/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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

import com.google.common.collect.Multimap;
import io.minio.credentials.Credentials;
import io.minio.errors.XmlParserException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okio.BufferedSink;
import okio.Okio;
import java.net.URL;

/** HTTP utilities. */
public class Http {
  public static class BaseUrl {
    private HttpUrl url;
    private String awsS3Prefix;
    private String awsDomainSuffix;
    private boolean awsDualstack;
    private String region;
    private boolean useVirtualStyle;

    public BaseUrl(String endpoint) {
      setUrl(parse(endpoint));
    }

    public BaseUrl(String endpoint, int port, boolean secure) {
      HttpUrl url = parse(endpoint);
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("port must be in range of 1 to 65535");
      }
      url = url.newBuilder().port(port).scheme(secure ? "https" : "http").build();

      setUrl(url);
    }

    public BaseUrl(HttpUrl url) {
      Utils.validateNotNull(url, "url");
      Utils.validateUrl(url);
      setUrl(url);
    }

    public BaseUrl(URL url) {
      Utils.validateNotNull(url, "url");
      return this(HttpUrl.get(url));
    }

    private void setAwsInfo(String host, boolean https) {
      this.awsS3Prefix = null;
      this.awsDomainSuffix = null;
      this.awsDualstack = false;

      if (!Utils.HOSTNAME_REGEX.matcher(host).find()) return;

      if (Utils.AWS_ELB_ENDPOINT_REGEX.matcher(host).find()) {
        String[] tokens = host.split("\\.elb\\.amazonaws\\.com", 1)[0].split("\\.");
        this.region = tokens[tokens.length - 1];
        return;
      }

      if (!Utils.AWS_ENDPOINT_REGEX.matcher(host).find()) return;

      if (!Utils.AWS_S3_ENDPOINT_REGEX.matcher(host).find()) {
        throw new IllegalArgumentException("invalid Amazon AWS host " + host);
      }

      Matcher matcher = Utils.AWS_S3_PREFIX_REGEX.matcher(host);
      matcher.lookingAt();
      int end = matcher.end();

      this.awsS3Prefix = host.substring(0, end);
      if (this.awsS3Prefix.contains("s3-accesspoint") && !https) {
        throw new IllegalArgumentException("use HTTPS scheme for host " + host);
      }

      String[] tokens = host.substring(end).split("\\.");
      awsDualstack = "dualstack".equals(tokens[0]);
      if (awsDualstack) tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
      String regionInHost = null;
      if (!tokens[0].equals("vpce") && !tokens[0].equals("amazonaws")) {
        regionInHost = tokens[0];
        tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
      }
      this.awsDomainSuffix = String.join(".", tokens);

      if (host.equals("s3-external-1.amazonaws.com")) regionInHost = "us-east-1";
      if (host.equals("s3-us-gov-west-1.amazonaws.com")
          || host.equals("s3-fips-us-gov-west-1.amazonaws.com")) {
        regionInHost = "us-gov-west-1";
      }

      if (regionInHost != null) this.region = regionInHost;
    }

    private void setUrl(HttpUrl url) {
      this.url = url;
      this.setAwsInfo(url.host(), url.isHttps());
      this.useVirtualStyle = this.awsDomainSuffix != null || url.host().endsWith("aliyuncs.com");
    }

    private HttpUrl parse(String endpoint) {
      Utils.validateNotEmptyString(endpoint, "endpoint");
      HttpUrl url = HttpUrl.parse(endpoint);
      if (url == null) {
        Utils.validateHostnameOrIPAddress(endpoint);
        url = new HttpUrl.Builder().scheme("https").host(endpoint).build();
      } else {
        Utils.validateUrl(url);
      }
      return url;
    }

    public boolean isHttps() {
      return baseUrl.isHttps();
    }

    public String awsS3Prefix() {
      return awsS3Prefix;
    }

    public String awsDomainSuffix() {
      return awsDomainSuffix;
    }

    public String region() {
      return region;
    }

    public void setRegion(String region) {
      this.region = region;
    }

    /** Enables dual-stack endpoint for Amazon S3 endpoint. */
    public void enableDualStackEndpoint() {
      awsDualstack = true;
    }

    /** Disables dual-stack endpoint for Amazon S3 endpoint. */
    public void disableDualStackEndpoint() {
      awsDualstack = false;
    }

    /** Enables virtual-style endpoint. */
    public void enableVirtualStyleEndpoint() {
      useVirtualStyle = true;
    }

    /** Disables virtual-style endpoint. */
    public void disableVirtualStyleEndpoint() {
      useVirtualStyle = false;
    }

    /** Sets AWS S3 domain prefix. */
    public void setAwsS3Prefix(@Nonnull String awsS3Prefix) {
      if (awsS3Prefix == null)
        throw new IllegalArgumentException("null Amazon AWS S3 domain prefix");
      if (!Utils.AWS_S3_PREFIX_REGEX.matcher(awsS3Prefix).find()) {
        throw new IllegalArgumentException("invalid Amazon AWS S3 domain prefix " + awsS3Prefix);
      }
      this.awsS3Prefix = awsS3Prefix;
    }

    private String buildAwsUrl(
        HttpUrl.Builder builder, String bucketName, boolean enforcePathStyle, String region) {
      String host = this.awsS3Prefix + this.awsDomainSuffix;
      if (host.equals("s3-external-1.amazonaws.com")
          || host.equals("s3-us-gov-west-1.amazonaws.com")
          || host.equals("s3-fips-us-gov-west-1.amazonaws.com")) {
        builder.host(host);
        return host;
      }

      host = this.awsS3Prefix;
      if (this.awsS3Prefix.contains("s3-accelerate")) {
        if (bucketName.contains(".")) {
          throw new IllegalArgumentException(
              "bucket name '" + bucketName + "' with '.' is not allowed for accelerate endpoint");
        }
        if (enforcePathStyle) host = host.replaceFirst("-accelerate", "");
      }

      if (this.awsDualstack) host += "dualstack.";
      if (!this.awsS3Prefix.contains("s3-accelerate")) host += region + ".";
      host += this.awsDomainSuffix;

      builder.host(host);
      return host;
    }

    private String buildListBucketsUrl(HttpUrl.Builder builder, String region) {
      if (this.awsDomainSuffix == null) return null;

      String host = this.awsS3Prefix + this.awsDomainSuffix;
      if (host.equals("s3-external-1.amazonaws.com")
          || host.equals("s3-us-gov-west-1.amazonaws.com")
          || host.equals("s3-fips-us-gov-west-1.amazonaws.com")) {
        builder.host(host);
        return host;
      }

      String s3Prefix = this.awsS3Prefix;
      String domainSuffix = this.awsDomainSuffix;
      if (this.awsS3Prefix.startsWith("s3.") || this.awsS3Prefix.startsWith("s3-")) {
        s3Prefix = "s3.";
        domainSuffix = "amazonaws.com" + (domainSuffix.endsWith(".cn") ? ".cn" : "");
      }

      host = s3Prefix + region + "." + domainSuffix;
      builder.host(host);
      return host;
    }

    /** Build URL for given parameters. */
    public HttpUrl buildUrl(
        Method method,
        String bucketName,
        String objectName,
        String region,
        Multimap<String, String> queryParams)
        throws NoSuchAlgorithmException {
      if (bucketName == null && objectName != null) {
        throw new IllegalArgumentException("null bucket name for object '" + objectName + "'");
      }

      HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();

      if (queryParams != null) {
        for (Map.Entry<String, String> entry : queryParams.entries()) {
          urlBuilder.addEncodedQueryParameter(
              Utils.encode(entry.getKey()), Utils.encode(entry.getValue()));
        }
      }

      if (bucketName == null) {
        this.buildListBucketsUrl(urlBuilder, region);
        return urlBuilder.build();
      }

      boolean enforcePathStyle = (
          // use path style for make bucket to workaround "AuthorizationHeaderMalformed" error from
          // s3.amazonaws.com
          (method == Method.PUT && objectName == null && queryParams == null)

              // use path style for location query
              || (queryParams != null && queryParams.containsKey("location"))

              // use path style where '.' in bucketName causes SSL certificate validation error
              || (bucketName.contains(".") && this.baseUrl.isHttps()));

      String host = this.baseUrl.host();
      if (this.awsDomainSuffix != null) {
        host = this.buildAwsUrl(urlBuilder, bucketName, enforcePathStyle, region);
      }

      if (enforcePathStyle || !this.useVirtualStyle) {
        urlBuilder.addEncodedPathSegment(Utils.encode(bucketName));
      } else {
        urlBuilder.host(bucketName + "." + host);
      }

      if (objectName != null) {
        urlBuilder.addEncodedPathSegments(Utils.encodePath(objectName));
      }

      return urlBuilder.build();
    }

    @Override
    public String toString() {
      return url.toString();
    }
  }

  public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/octet-stream");
  public static final MediaType XML_MEDIA_TYPE = MediaType.parse("application/xml");
  public static final String US_EAST_1 = "us-east-1";
  public static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  public static MediaType mediaType(String value) {
    if (value == null) return DEFAULT_MEDIA_TYPE;
    MediaType mediaType = MediaType.parse(value);
    if (mediaType == null) {
      throw new IllegalArgumentException(
          "invalid media/content type '" + value + "' as per RFC 2045");
    }
    return mediaType;
  }

  private static OkHttpClient enableJKSPKCS12Certificates(
      OkHttpClient httpClient,
      String trustStorePath,
      String trustStorePassword,
      String keyStorePath,
      String keyStorePassword,
      String keyStoreType)
      throws GeneralSecurityException, IOException {
    if (trustStorePath == null || trustStorePath.isEmpty()) {
      throw new IllegalArgumentException("trust store path must be provided");
    }
    if (trustStorePassword == null) {
      throw new IllegalArgumentException("trust store password must be provided");
    }
    if (keyStorePath == null || keyStorePath.isEmpty()) {
      throw new IllegalArgumentException("key store path must be provided");
    }
    if (keyStorePassword == null) {
      throw new IllegalArgumentException("key store password must be provided");
    }

    SSLContext sslContext = SSLContext.getInstance("TLS");
    KeyStore trustStore = KeyStore.getInstance("JKS");
    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
    try (FileInputStream trustInput = new FileInputStream(trustStorePath);
        FileInputStream keyInput = new FileInputStream(keyStorePath); ) {
      trustStore.load(trustInput, trustStorePassword.toCharArray());
      keyStore.load(keyInput, keyStorePassword.toCharArray());
    }
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);

    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

    sslContext.init(
        keyManagerFactory.getKeyManagers(),
        trustManagerFactory.getTrustManagers(),
        new java.security.SecureRandom());

    return httpClient
        .newBuilder()
        .sslSocketFactory(
            sslContext.getSocketFactory(),
            (X509TrustManager) trustManagerFactory.getTrustManagers()[0])
        .build();
  }

  public static OkHttpClient enableJKSCertificates(
      OkHttpClient httpClient,
      String trustStorePath,
      String trustStorePassword,
      String keyStorePath,
      String keyStorePassword)
      throws GeneralSecurityException, IOException {
    return enableJKSPKCS12Certificates(
        httpClient, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword, "JKS");
  }

  public static OkHttpClient enablePKCS12Certificates(
      OkHttpClient httpClient,
      String trustStorePath,
      String trustStorePassword,
      String keyStorePath,
      String keyStorePassword)
      throws GeneralSecurityException, IOException {
    return enableJKSPKCS12Certificates(
        httpClient, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword, "PKCS12");
  }

  /**
   * copied logic from
   * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/CustomTrust.java
   */
  public static OkHttpClient enableExternalCertificates(OkHttpClient httpClient, String filename)
      throws GeneralSecurityException, IOException {
    Collection<? extends Certificate> certificates = null;
    try (FileInputStream fis = new FileInputStream(filename)) {
      certificates = CertificateFactory.getInstance("X.509").generateCertificates(fis);
    }

    if (certificates == null || certificates.isEmpty()) {
      throw new IllegalArgumentException("expected non-empty set of trusted certificates");
    }

    char[] password = "password".toCharArray(); // Any password will work.

    // Put the certificates a key store.
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    // By convention, 'null' creates an empty key store.
    keyStore.load(null, password);

    int index = 0;
    for (Certificate certificate : certificates) {
      String certificateAlias = Integer.toString(index++);
      keyStore.setCertificateEntry(certificateAlias, certificate);
    }

    // Use it to build an X509 trust manager.
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, password);
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
    final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagers, trustManagers, null);
    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

    return httpClient
        .newBuilder()
        .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0])
        .build();
  }

  public static OkHttpClient newDefaultClient() {
    OkHttpClient httpClient =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .protocols(Arrays.asList(Protocol.HTTP_1_1))
            .build();
    String filename = System.getenv("SSL_CERT_FILE");
    if (filename != null && !filename.isEmpty()) {
      try {
        httpClient = enableExternalCertificates(httpClient, filename);
      } catch (GeneralSecurityException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    return httpClient;
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "SIC",
      justification = "Should not be used in production anyways.")
  public static OkHttpClient disableCertCheck(OkHttpClient client)
      throws KeyManagementException, NoSuchAlgorithmException {
    final TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[] {};
            }
          }
        };

    final SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

    return client
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

  public static OkHttpClient setTimeout(
      OkHttpClient client, long connectTimeout, long writeTimeout, long readTimeout) {
    return client
        .newBuilder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .build();
  }

  public static okhttp3.Request newRequest(
      okhttp3.HttpUrl url,
      Method method,
      okhttp3.Headers headers,
      RequestBody body,
      String userAgent) {
    okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
    requestBuilder.url(url);

    if (headers != null) requestBuilder.headers(headers);
    requestBuilder.header("Accept-Encoding", "identity"); // Disable default okhttp gzip compression
    requestBuilder.header("User-Agent", userAgent);
    requestBuilder.header("Host", Utils.getHostHeader(url));

    if (body != null) {
      requestBuilder.header("Content-Type", body.contentType().toString());
      requestBuilder.header("Content-Length", String.valueOf(body.contentLength()));
    }

    return requestBuilder.method(method.toString(), body).build();
  }

  public static class S3Request {
    private String userAgent;
    private Method method;
    private String bucket;
    private String region;
    private String object;
    private Multimap<String, String> headers;
    private Multimap<String, String> queryParams;
    private MediaType contentType;

    private okhttp3.RequestBody requestBody;
    private RandomAccessFile file;
    private ByteBuffer buffer;
    private byte[] data;
    private Long length;
    private String sha256Hash;
    private String md5Hash;
    private boolean traceBody;
    private boolean retryFailure;

    private String traces = null;

    private S3Request(Builder builder) {
      this.userAgent = builder.userAgent;
      this.method = builder.method;
      this.bucket = builder.bucket;
      this.region = builder.region;
      this.object = builder.object;
      this.headers = builder.headers;
      this.queryParams = builder.queryParams;
      this.contentType = builder.contentType;
      this.requestBody = builder.requestBody;
      this.file = builder.file;
      this.buffer = builder.buffer;
      this.data = builder.data;
      this.length = builder.length;
      this.sha256Hash = builder.sha256Hash;
      this.md5Hash = builder.md5Hash;
      this.traceBody = builder.traceBody;
      this.retryFailure = builder.retryFailure;
    }

    public String userAgent() {
      return userAgent;
    }

    public Method method() {
      return method;
    }

    public String bucket() {
      return bucket;
    }

    public String region() {
      return region;
    }

    public String object() {
      return object;
    }

    public Multimap<String, String> headers() {
      return headers;
    }

    public Multimap<String, String> queryParams() {
      return queryParams;
    }

    public MediaType contentType() {
      return contentType;
    }

    public okhttp3.RequestBody requestBody() {
      return requestBody;
    }

    public RandomAccessFile file() {
      return file;
    }

    public ByteBuffer buffer() {
      return buffer;
    }

    public byte[] data() {
      return data;
    }

    public Long length() {
      return length;
    }

    public String sha256Hash() {
      return sha256Hash;
    }

    public String md5Hash() {
      return md5Hash;
    }

    public boolean traceBody() {
      return traceBody;
    }

    public boolean retryFailure() {
      return retryFailure;
    }

    public String traces() {
      return traces;
    }

    public okhttp3.Request httpRequest(HttpUrl url, Credentials credentials)
        throws InvalidKeyException, IOException, NoSuchAlgorithmException {
      Headers headers = Utils.httpHeaders(this.headers);

      if (requestBody != null) {
        return newRequest(url, method, headers, new RequestBody(requestBody), userAgent);
      }

      if (credentials == null) {
        if (md5Hash == null) throw new IllegalArgumentException("MD5 hash must be provided");
      } else if (!url.isHttps()) {
        if (sha256Hash == null) throw new IllegalArgumentException("SHA256 hash must be provided");
      } else if (sha256Hash == null) {
        sha256Hash = "UNSIGNED-PAYLOAD";
      }

      {
        Headers.Builder builder = new Headers.Builder();
        builder.addAll(headers);
        builder.add("Content-Type", contentType.toString());
        if (md5Hash != null) builder.add("Content-MD5", md5Hash);
        if (sha256Hash != null) builder.add("x-amz-content-sha256", sha256Hash);
        headers = builder.build();
      }

      RequestBody requestBody = null;
      if (file != null) {
        requestBody = new RequestBody(file, length, contentType);
      } else if (buffer != null) {
        requestBody = new RequestBody(buffer, contentType);
      } else {
        requestBody = new RequestBody(data, length.intValue(), contentType);
      }

      okhttp3.Request request = newRequest(url, method, headers, requestBody, userAgent);
      if (credentials != null) {
        // Sign the request
        okhttp3.Request.Builder builder = request.newBuilder();
        String sessionToken = credentials.sessionToken();
        if (sessionToken != null) builder.header("X-Amz-Security-Token", sessionToken);
        builder.header("x-amz-date", ZonedDateTime.now().format(Time.AMZ_DATE_FORMAT));
        request = builder.build();
        request =
            Signer.signV4S3(
                request, region, credentials.accessKey(), credentials.secretKey(), sha256Hash);
      }

      StringBuilder traceBuilder = new StringBuilder();
      traceBuilder.append("---------START-HTTP---------\n");
      String encodedPath = request.url().encodedPath();
      String encodedQuery = request.url().encodedQuery();
      if (encodedQuery != null) encodedPath += "?" + encodedQuery;
      traceBuilder.append(request.method()).append(" ").append(encodedPath).append(" HTTP/1.1\n");
      traceBuilder
          .append(
              request
                  .headers()
                  .toString()
                  .replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*")
                  .replaceAll("Credential=([^/]+)", "Credential=*REDACTED*"))
          .append("\n\n");
      if (data != null && traceBody) {
        String value = new String(data, StandardCharsets.UTF_8);
        traceBuilder.append(value);
        if (!value.endsWith("\n")) traceBuilder.append("\n");
      }
      traces = traceBuilder.toString();

      return request;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String userAgent;
      private Method method;
      private String bucket = null;
      private String region = US_EAST_1;
      private String object = null;
      private Multimap<String, String> headers = null;
      private Multimap<String, String> queryParams = null;
      private MediaType contentType = DEFAULT_MEDIA_TYPE;

      private okhttp3.RequestBody requestBody = null;
      private RandomAccessFile file = null;
      private ByteBuffer buffer = null;
      private byte[] data = null;
      private Long length = null;
      private String sha256Hash = null;
      private String md5Hash = null;
      private boolean traceBody = false;
      private boolean retryFailure = false;

      public Builder userAgent(String userAgent) {
        this.userAgent = Utils.validateNotNull(userAgent, "user agent");
        return this;
      }

      public Builder method(Method method) {
        this.method = Utils.validateNotNull(method, "HTTP method");
        return this;
      }

      public Builder bucket(String bucket) {
        this.bucket = bucket;
        return this;
      }

      public Builder region(String region) {
        this.region = region;
        return this;
      }

      public Builder object(String object) {
        this.object = object;
        return this;
      }

      public Builder headers(Multimap<String, String> headers) {
        this.headers = headers;
        return this;
      }

      public Builder queryParams(Multimap<String, String> queryParams) {
        this.queryParams = queryParams;
        return this;
      }

      public Builder baseArgs(BaseArgs baseArgs) {
        if (baseArgs != null) {
          headers = Utils.mergeMultimap(baseArgs.extraHeaders(), headers);
          queryParams = Utils.mergeMultimap(baseArgs.extraQueryParams(), queryParams);
          if (baseArgs instanceof BucketArgs) {
            bucket = ((BucketArgs) baseArgs).bucket();
            region = ((BucketArgs) baseArgs).region();
          }
          if (baseArgs instanceof ObjectArgs) object = ((ObjectArgs) baseArgs).object();
        }
        return this;
      }

      public Builder body(
          Object body, Long length, MediaType contentType, String sha256Hash, String md5Hash)
          throws XmlParserException {
        Utils.validateNotNull(body, "body");
        if (length != null && length < 0) {
          throw new IllegalArgumentException("valid length must be provided");
        }

        this.contentType = contentType;
        this.sha256Hash = sha256Hash;
        this.md5Hash = md5Hash;

        if (body instanceof okhttp3.RequestBody) {
          this.requestBody = (okhttp3.RequestBody) body;
        } else if (body instanceof ByteBuffer) {
          this.buffer = (ByteBuffer) body;
        } else if (body instanceof RandomAccessFile) {
          if (length == null) {
            throw new IllegalArgumentException(
                "valid length must be provided for random access file");
          }
          this.file = (RandomAccessFile) body;
          this.length = length;
        } else if (body instanceof byte[]) {
          if (length == null) {
            throw new IllegalArgumentException("valid length must be provided for byte array body");
          }
          this.data = (byte[]) body;
          this.length = length;
        } else {
          this.traceBody = true;
          if (body instanceof CharSequence) {
            this.data = ((CharSequence) body).toString().getBytes(StandardCharsets.UTF_8);
            this.length = (long) this.data.length;
          } else {
            // For any other object, do XML marshalling.
            this.data = Xml.marshal(body).getBytes(StandardCharsets.UTF_8);
            this.length = length;
            if (contentType == null) this.contentType = XML_MEDIA_TYPE;
          }
        }

        return this;
      }

      public S3Request build() {
        if (method == null) throw new IllegalArgumentException("method must be provided");
        if (userAgent == null) throw new IllegalArgumentException("user agent must be provided");

        if ((method == Method.PUT || method == Method.POST)
            && requestBody == null
            && file == null
            && buffer == null
            && data == null) {
          data = Utils.EMPTY_BODY;
          length = 0L;
          sha256Hash = Checksum.ZERO_SHA256_HASH;
          md5Hash = Checksum.ZERO_MD5_HASH;
        }

        return new S3Request(this);
      }
    }
  }

  /** RequestBody that wraps a single data object. */
  public static class RequestBody extends okhttp3.RequestBody {
    private okhttp3.RequestBody body;
    private ByteBuffer buffer;
    private RandomAccessFile file;
    private long position;
    private byte[] bytes;
    private long length;
    private MediaType contentType;

    public RequestBody(
        @Nonnull final byte[] bytes, final int length, @Nonnull final MediaType contentType) {
      this.bytes = Utils.validateNotNull(bytes, "data bytes");
      if (length < 0) throw new IllegalArgumentException("length must not be negative value");
      this.length = length;
      this.contentType = Utils.validateNotNull(contentType, "content type");
    }

    public RequestBody(
        @Nonnull final RandomAccessFile file,
        final long length,
        @Nonnull final MediaType contentType) {
      this.file = Utils.validateNotNull(file, "randome access file");
      if (length < 0) throw new IllegalArgumentException("length must not be negative value");
      this.length = length;
      this.contentType = Utils.validateNotNull(contentType, "content type");
      this.position = file.getFilePointer();
    }

    public RequestBody(@Nonnull final ByteBuffer buffer, @Nonnull final MediaType contentType) {
      this.buffer = Utils.validateNotNull(buffer, "buffer");
      this.length = buffer.length();
      this.contentType = Utils.validateNotNull(body.contentType(), "content type");
    }

    public RequestBody(@Nonnull final okhttp3.RequestBody body) throws IOException {
      this.body = Utils.validateNotNull(body, "body");
      if (body.contentLength() < 0) {
        throw new IllegalArgumentException("length must not be negative value");
      }
      this.length = body.contentLength();
      this.contentType = Utils.validateNotNull(body.contentType(), "content type");
    }

    @Override
    public MediaType contentType() {
      return contentType;
    }

    @Override
    public long contentLength() {
      return length;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
      if (body != null) {
        body.writeTo(sink);
      } else if (buffer != null) {
        sink.write(Okio.source(buffer.inputStream()), length);
      } else if (file != null) {
        file.seek(position);
        sink.write(Okio.source(Channels.newInputStream(file.getChannel())), length);
      } else {
        sink.write(bytes, 0, (int) length);
      }
    }
  }

  /** HTTP methods. */
  public static enum Method {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE;
  }
}
