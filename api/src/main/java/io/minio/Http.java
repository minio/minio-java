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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.minio.credentials.Credentials;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okio.BufferedSink;
import okio.Okio;

/** HTTP utilities. */
public class Http {
  public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/octet-stream");
  public static final MediaType XML_MEDIA_TYPE = MediaType.parse("application/xml");
  public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
  public static final String US_EAST_1 = "us-east-1";
  public static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
  public static final Body EMPTY_BODY =
      new Body(
          Utils.EMPTY_BYTE_ARRAY,
          0,
          DEFAULT_MEDIA_TYPE,
          Checksum.ZERO_SHA256_HASH,
          Checksum.ZERO_MD5_HASH);

  /** Base URL of S3 endpoint. */
  public static class BaseUrl {
    private okhttp3.HttpUrl url;
    private String awsS3Prefix;
    private String awsDomainSuffix;
    private boolean awsDualstack;
    private String region;
    private boolean useVirtualStyle;

    /** Creates BaseUrl to the specified endpoint. */
    public BaseUrl(String endpoint) {
      setUrl(parse(endpoint));
    }

    /** Creates BaseUrl to the specified endpoint, port and secure flag. */
    public BaseUrl(String endpoint, int port, boolean secure) {
      okhttp3.HttpUrl url = parse(endpoint);
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("port must be in range of 1 to 65535");
      }
      url = url.newBuilder().port(port).scheme(secure ? "https" : "http").build();

      setUrl(url);
    }

    /** Creates BaseUrl to the specified url. */
    public BaseUrl(okhttp3.HttpUrl url) {
      Utils.validateNotNull(url, "url");
      Utils.validateUrl(url);
      setUrl(url);
    }

    /** Creates BaseUrl to the specified url. */
    public BaseUrl(URL url) {
      Utils.validateNotNull(url, "url");
      setUrl(okhttp3.HttpUrl.get(url));
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

    private void setUrl(okhttp3.HttpUrl url) {
      this.url = url;
      this.setAwsInfo(url.host(), url.isHttps());
      this.useVirtualStyle = this.awsDomainSuffix != null || url.host().endsWith("aliyuncs.com");
    }

    private okhttp3.HttpUrl parse(String endpoint) {
      Utils.validateNotEmptyString(endpoint, "endpoint");
      okhttp3.HttpUrl url = okhttp3.HttpUrl.parse(endpoint);
      if (url == null) {
        Utils.validateHostnameOrIPAddress(endpoint);
        url = new okhttp3.HttpUrl.Builder().scheme("https").host(endpoint).build();
      } else {
        Utils.validateUrl(url);
      }
      return url;
    }

    /** Checks this base url is HTTPS scheme or not. */
    public boolean isHttps() {
      return url.isHttps();
    }

    /** Gets AWS S3 prefix. */
    public String awsS3Prefix() {
      return awsS3Prefix;
    }

    /** Gets AWS domain suffix. */
    public String awsDomainSuffix() {
      return awsDomainSuffix;
    }

    /** Gets region if present in this base url. */
    public String region() {
      return region;
    }

    /** Sets region to this base url. */
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
        okhttp3.HttpUrl.Builder builder,
        String bucketName,
        boolean enforcePathStyle,
        String region) {
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

    private String buildListBucketsUrl(okhttp3.HttpUrl.Builder builder, String region) {
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

    /** Builds URL for given parameters. */
    public okhttp3.HttpUrl buildUrl(
        Method method,
        String bucketName,
        String objectName,
        String region,
        QueryParameters queryParams)
        throws MinioException {
      if (bucketName == null && objectName != null) {
        throw new IllegalArgumentException("null bucket name for object '" + objectName + "'");
      }

      okhttp3.HttpUrl.Builder urlBuilder = this.url.newBuilder();

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
              || (bucketName.contains(".") && this.url.isHttps()));

      String host = this.url.host();
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

  /** Gets media type of the specified string value. */
  public static MediaType mediaType(String value) {
    if (value == null) return DEFAULT_MEDIA_TYPE;
    MediaType mediaType = MediaType.parse(value);
    if (mediaType == null) {
      throw new IllegalArgumentException(
          "invalid media/content type '" + value + "' as per RFC 2045");
    }
    return mediaType;
  }

  private static X509TrustManager createCompositeTrustManager(
      List<X509TrustManager> trustManagers) {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType)
          throws CertificateException {
        for (X509TrustManager tm : trustManagers) {
          try {
            tm.checkClientTrusted(chain, authType);
            return;
          } catch (CertificateException ignored) {
          }
        }
        throw new CertificateException(
            "None of the TrustManagers trust this client certificate chain");
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType)
          throws CertificateException {
        for (X509TrustManager tm : trustManagers) {
          try {
            tm.checkServerTrusted(chain, authType);
            return;
          } catch (CertificateException ignored) {
          }
        }
        throw new CertificateException(
            "None of the TrustManagers trust this server certificate chain");
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return trustManagers.stream()
            .flatMap(tm -> Arrays.stream(tm.getAcceptedIssuers()))
            .toArray(X509Certificate[]::new);
      }
    };
  }

  private static X509TrustManager buildTrustManagerFromKeyStore(KeyStore ks)
      throws KeyStoreException, NoSuchAlgorithmException {
    TrustManagerFactory factory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(ks);
    for (TrustManager tm : factory.getTrustManagers()) {
      if (tm instanceof X509TrustManager) {
        return (X509TrustManager) tm;
      }
    }
    return null;
  }

  private static int setCertificateEntry(
      CertificateFactory cf, KeyStore ks, Path file, String namePrefix)
      throws CertificateException, IOException, KeyStoreException {
    try (InputStream in = Files.newInputStream(file)) {
      int index = 0;
      while (in.available() > 0) {
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
        ks.setCertificateEntry(namePrefix + (index++), cert);
      }
      return index;
    }
  }

  private static X509TrustManager getTrustManagerFromFile(String filePath)
      throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null);
    if (setCertificateEntry(cf, ks, Paths.get(filePath), "cert-file-") == 0) return null;
    return buildTrustManagerFromKeyStore(ks);
  }

  private static X509TrustManager getTrustManagerFromDir(String dirPath)
      throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null);

    int index = 0;
    try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
      int number = 1;
      for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
        try {
          index += setCertificateEntry(cf, ks, file, "cert-dir-file-" + number + "-");
          number++;
        } catch (CertificateException | IOException | KeyStoreException e) {
          // Ignore these errors.
        }
      }
    }

    if (index == 0) return null;

    return buildTrustManagerFromKeyStore(ks);
  }

  private static X509TrustManager getDefaultTrustManager()
      throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
    TrustManagerFactory factory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init((KeyStore) null);
    for (TrustManager tm : factory.getTrustManagers()) {
      if (tm instanceof X509TrustManager) return (X509TrustManager) tm;
    }
    return null;
  }

  private static X509TrustManager getCompositeTrustManager(String filePath, String dirPath)
      throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
    List<X509TrustManager> trustManagers = new ArrayList<>();

    X509TrustManager defaultTm = getDefaultTrustManager();
    if (defaultTm != null) trustManagers.add(defaultTm);

    if (dirPath != null && !dirPath.isEmpty()) {
      X509TrustManager dirTm = getTrustManagerFromDir(dirPath);
      if (dirTm != null) trustManagers.add(dirTm);
    }

    if (filePath != null && !filePath.isEmpty()) {
      X509TrustManager fileTm = getTrustManagerFromFile(filePath);
      if (fileTm != null) trustManagers.add(fileTm);
    }

    if (trustManagers.isEmpty()) return null;

    return createCompositeTrustManager(trustManagers);
  }

  private static OkHttpClient enableJKSPKCS12Certificates(
      OkHttpClient httpClient,
      String trustStorePath,
      String trustStorePassword,
      String keyStorePath,
      String keyStorePassword,
      String keyStoreType)
      throws MinioException {
    try {
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
      try (InputStream trustInput = Files.newInputStream(Paths.get(trustStorePath));
          InputStream keyInput = Files.newInputStream(Paths.get(keyStorePath)); ) {
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
    } catch (GeneralSecurityException | IOException e) {
      throw new MinioException(e);
    }
  }

  /** Enables JKS formatted TLS certificates to the specified HTTP client. */
  public static OkHttpClient enableJKSCertificates(
      OkHttpClient httpClient,
      String trustStorePath,
      String trustStorePassword,
      String keyStorePath,
      String keyStorePassword)
      throws MinioException {
    return enableJKSPKCS12Certificates(
        httpClient, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword, "JKS");
  }

  /** Enables PKCS12 formatted TLS certificates to the specified HTTP client. */
  public static OkHttpClient enablePKCS12Certificates(
      OkHttpClient httpClient,
      String trustStorePath,
      String trustStorePassword,
      String keyStorePath,
      String keyStorePassword)
      throws MinioException {
    return enableJKSPKCS12Certificates(
        httpClient, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword, "PKCS12");
  }

  /** Enable external TLS certificates from given file path and all valid files from dir path. */
  public static OkHttpClient enableExternalCertificates(
      OkHttpClient client, String filePath, String dirPath) throws MinioException {
    try {
      X509TrustManager tm = getCompositeTrustManager(filePath, dirPath);
      if (tm == null) return client;

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] {tm}, new SecureRandom());
      return client.newBuilder().sslSocketFactory(sslContext.getSocketFactory(), tm).build();
    } catch (CertificateException
        | IOException
        | KeyManagementException
        | KeyStoreException
        | NoSuchAlgorithmException e) {
      throw new MinioException(e);
    }
  }

  /**
   * Enables external TLS certificates from SSL_CERT_FILE and SSL_CERT_DIR environment variables if
   * present.
   */
  public static OkHttpClient enableExternalCertificatesFromEnv(OkHttpClient client)
      throws MinioException {
    return enableExternalCertificates(
        client, System.getenv("SSL_CERT_FILE"), System.getenv("SSL_CERT_DIR"));
  }

  /**
   * Creates new HTTP client with default timeout with additional TLS certificates from
   * SSL_CERT_FILE and SSL_CERT_DIR environment variables if present.
   */
  public static OkHttpClient newDefaultClient() {
    OkHttpClient client =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .protocols(Arrays.asList(Protocol.HTTP_1_1))
            .build();
    try {
      return enableExternalCertificatesFromEnv(client);
    } catch (MinioException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Disables TLS certificate check as a special case for self-signed certificate and testing to the
   * specified HTTP client.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "SIC",
      justification = "Should not be used in production anyways.")
  public static OkHttpClient disableCertCheck(OkHttpClient client) throws MinioException {
    try {
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
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      throw new MinioException(e);
    }
  }

  /** Sets connect, write and read timeout in milliseconds to the specified HTTP client. */
  public static OkHttpClient setTimeout(
      OkHttpClient client, long connectTimeout, long writeTimeout, long readTimeout) {
    return client
        .newBuilder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .build();
  }

  /** HTTP body of {@link RandomAccessFile}, {@link ByteBuffer} or {@link byte} array. */
  public static class Body {
    private okhttp3.RequestBody requestBody;
    private RandomAccessFile file;
    private ByteBuffer buffer;
    private byte[] data;
    private Long length;
    private MediaType contentType;
    private String sha256Hash;
    private String md5Hash;
    private boolean bodyString;

    /** Creates Body for okhttp3 RequestBody. */
    public Body(okhttp3.RequestBody requestBody) {
      this.requestBody = requestBody;
      this.contentType = requestBody.contentType();
    }

    /** Creates Body for RandomAccessFile. */
    public Body(
        RandomAccessFile file,
        long length,
        MediaType contentType,
        String sha256Hash,
        String md5Hash) {
      if (length < 0) throw new IllegalArgumentException("valid length must be provided");
      this.file = file;
      set(length, contentType, sha256Hash, md5Hash);
    }

    /** Creates Body for byte array. */
    public Body(byte[] data, int length, MediaType contentType, String sha256Hash, String md5Hash) {
      if (length < 0) throw new IllegalArgumentException("valid length must be provided");
      this.data = data;
      set((long) length, contentType, sha256Hash, md5Hash);
    }

    /** Creates Body for ByteBuffer, string or XML encodable object. */
    public Body(Object body, MediaType contentType, String sha256Hash, String md5Hash)
        throws MinioException {
      if (body instanceof ByteBuffer) {
        this.buffer = (ByteBuffer) body;
        set(null, contentType, sha256Hash, md5Hash);
        return;
      }

      byte[] data = null;
      if (body instanceof CharSequence) {
        data = ((CharSequence) body).toString().getBytes(StandardCharsets.UTF_8);
      } else {
        // For any other object, do XML marshalling.
        data = Xml.marshal(body).getBytes(StandardCharsets.UTF_8);
        contentType = XML_MEDIA_TYPE;
      }
      sha256Hash = Checksum.hexString(Checksum.SHA256.sum(data));
      md5Hash = Checksum.base64String(Checksum.MD5.sum(data));

      this.bodyString = true;
      this.data = data;
      set((long) data.length, contentType, sha256Hash, md5Hash);
    }

    private void set(Long length, MediaType contentType, String sha256Hash, String md5Hash) {
      this.length = length;
      this.contentType = contentType == null ? DEFAULT_MEDIA_TYPE : contentType;
      this.sha256Hash = sha256Hash;
      this.md5Hash = md5Hash;
    }

    /** Gets content type of this body. */
    public MediaType contentType() {
      return contentType;
    }

    /** Gets SHA256 hash of this body. */
    public String sha256Hash() {
      return sha256Hash;
    }

    /** Gets SHA256 hash of this body. */
    public String md5Hash() {
      return md5Hash;
    }

    /** Checks whether this body is okhttp3 RequestBody. */
    public boolean isHttpRequestBody() {
      return requestBody != null;
    }

    /** Creates headers for this body. */
    public Headers headers() {
      Headers headers = new Headers(Headers.CONTENT_TYPE, contentType.toString());
      if (sha256Hash != null) headers.put(Headers.X_AMZ_CONTENT_SHA256, sha256Hash);
      if (md5Hash != null) headers.put(Headers.CONTENT_MD5, md5Hash);
      return headers;
    }

    /** Creates HTTP RequestBody for this body. */
    public RequestBody toRequestBody() throws MinioException {
      if (requestBody != null) return new RequestBody(requestBody);
      if (file != null) return new RequestBody(file, length, contentType);
      if (buffer != null) return new RequestBody(buffer, contentType);
      return new RequestBody(data, length.intValue(), contentType);
    }

    @Override
    public String toString() {
      return bodyString ? new String(data, StandardCharsets.UTF_8) : "<<<BYTE>>>";
    }
  }

  /** HTTP request body of {@link RandomAccessFile}, {@link ByteBuffer} or byte array. */
  public static class RequestBody extends okhttp3.RequestBody {
    private okhttp3.RequestBody body;
    private ByteBuffer buffer;
    private RandomAccessFile file;
    private long position;
    private byte[] bytes;
    private long length;
    private MediaType contentType;

    /** Creates RequestBody for byte array. */
    public RequestBody(
        @Nonnull final byte[] bytes, final int length, @Nonnull final MediaType contentType) {
      this.bytes = Utils.validateNotNull(bytes, "data bytes");
      if (length < 0) throw new IllegalArgumentException("length must not be negative value");
      this.length = length;
      this.contentType = Utils.validateNotNull(contentType, "content type");
    }

    /** Creates RequestBody for RandomAccessFile. */
    public RequestBody(
        @Nonnull final RandomAccessFile file,
        final long length,
        @Nonnull final MediaType contentType)
        throws MinioException {
      this.file = Utils.validateNotNull(file, "randome access file");
      if (length < 0) throw new IllegalArgumentException("length must not be negative value");
      this.length = length;
      this.contentType = Utils.validateNotNull(contentType, "content type");
      try {
        this.position = file.getFilePointer();
      } catch (IOException e) {
        throw new MinioException(e);
      }
    }

    /** Creates RequestBody for ByteBuffer. */
    public RequestBody(@Nonnull final ByteBuffer buffer, @Nonnull final MediaType contentType) {
      this.buffer = Utils.validateNotNull(buffer, "buffer");
      this.length = buffer.length();
      this.contentType = Utils.validateNotNull(contentType, "content type");
    }

    /** Creates RequestBody for okhttp3 RequestBody. */
    public RequestBody(@Nonnull final okhttp3.RequestBody body) throws MinioException {
      try {
        this.body = Utils.validateNotNull(body, "body");
        if (body.contentLength() < 0) {
          throw new IllegalArgumentException("length must not be negative value");
        }
        this.length = body.contentLength();
        this.contentType = Utils.validateNotNull(body.contentType(), "content type");
      } catch (IOException e) {
        throw new MinioException(e);
      }
    }

    /** Gets content type. */
    @Override
    public MediaType contentType() {
      return contentType;
    }

    /** Gets content length. */
    @Override
    public long contentLength() {
      return length;
    }

    /** Writes data to the specified sink. */
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

  /** HTTP headers. */
  public static class Headers implements Iterable<Map.Entry<String, String>> {
    private static final long serialVersionUID = -8099023918647559669L;

    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_MD5 = "Content-Md5";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String HOST = "Host";
    public static final String USER_AGENT = "User-Agent";
    public static final String X_AMZ_CHECKSUM_SHA256 = "X-Amz-Checksum-Sha256";
    public static final String X_AMZ_CONTENT_SHA256 = "X-Amz-Content-Sha256";
    public static final String X_AMZ_COPY_SOURCE_RANGE = "X-Amz-Copy-Source-Range";
    public static final String X_AMZ_DATE = "X-Amz-Date";
    public static final String X_AMZ_SDK_CHECKSUM_ALGORITHM = "X-Amz-Sdk-Checksum-Algorithm";
    public static final String X_AMZ_SECURITY_TOKEN = "X-Amz-Security-Token";

    private static final Set<String> NON_EMPTY_HEADERS =
        ImmutableSet.of(
            ACCEPT_ENCODING,
            AUTHORIZATION,
            CONTENT_ENCODING,
            CONTENT_LENGTH,
            CONTENT_MD5,
            CONTENT_TYPE,
            HOST,
            USER_AGENT,
            X_AMZ_CHECKSUM_SHA256,
            X_AMZ_CONTENT_SHA256,
            X_AMZ_DATE,
            X_AMZ_SDK_CHECKSUM_ALGORITHM,
            X_AMZ_SECURITY_TOKEN);
    private final Map<String, Set<String>> headers = new HashMap<>();

    // Normalize header names to Title-Case
    private String normalize(String name) {
      String[] parts = name.toLowerCase(Locale.US).split("-", -1);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < parts.length; i++) {
        String part = parts[i];
        if (!part.isEmpty()) {
          sb.append(Character.toUpperCase(part.charAt(0)));
          if (part.length() > 1) {
            sb.append(part.substring(1));
          }
        }
        if (i < parts.length - 1) {
          sb.append("-");
        }
      }
      return sb.toString();
    }

    /** Creates empty headers. */
    public Headers() {}

    /** Creates headers as copy of the specified source. */
    public Headers(Headers source) {
      if (source != null) putAll(source);
    }

    /** Creates headers as copy of the specified source. */
    public Headers(Map<String, String> source) {
      if (source != null) putAll(source);
    }

    /** Creates headers as copy of the specified source. */
    public Headers(Multimap<String, String> source) {
      if (source != null) putAll(source);
    }

    /** Creates headers by alternating names and values. */
    public Headers(String... keysAndValues) {
      if (keysAndValues.length % 2 != 0) {
        throw new IllegalArgumentException("Expected alternating keys and values");
      }
      for (int i = 0; i < keysAndValues.length; i += 2) {
        set(keysAndValues[i], keysAndValues[i + 1]);
      }
    }

    /** Creates new headers by merging names and values from the specified headers list. */
    public static Headers merge(Headers... headersList) {
      Headers headers = new Headers();
      for (Headers h : headersList) headers.putAll(h);
      return headers;
    }

    private String validateName(String name) {
      if (!Utils.validateNotNull(name, "name").trim().equals(name)) {
        throw new IllegalArgumentException("leading/trailing spaces are not allowed in name");
      }
      if (name.isEmpty()) throw new IllegalArgumentException("name must not be empty");
      return normalize(name);
    }

    private String validateValue(String name, String value) {
      Utils.validateNotNull(value, "value");
      if (NON_EMPTY_HEADERS.contains(name) && value.isEmpty()) {
        throw new IllegalArgumentException("value must not be empty for name " + name);
      }
      if (CONTENT_TYPE.equals(name) && MediaType.parse(value) == null) {
        throw new IllegalArgumentException("invalid content type '" + value + "' as per RFC 2045");
      }
      return value;
    }

    /**
     * Adds the specified value to the name in this headers. If the name already exists, the value
     * is appended uniquely.
     */
    public void add(@Nonnull String name, @Nonnull String value) {
      name = validateName(name);
      value = validateValue(name, value);
      headers.computeIfAbsent(name, k -> new HashSet<>()).add(value);
    }

    /** Sets the specified name and value in this headers. */
    public void set(@Nonnull String name, @Nonnull String value) {
      name = validateName(name);
      value = validateValue(name, value);
      headers.put(name, new HashSet<>(Collections.singletonList(value)));
    }

    /** Gets the first value of the specified name in this headers. */
    public String getFirst(String name) {
      if (name == null) return null;
      Set<String> values = get(name);
      return values.isEmpty() ? null : values.iterator().next();
    }

    /** Checks whether the specified name exists in this headers. */
    public boolean contains(String name) {
      if (name == null) return false;
      return headers.containsKey(normalize(name));
    }

    /** Checks whether any name in this headers starts with the specified prefix or not. */
    public boolean namePrefixAny(String prefix) {
      if (prefix == null) return false;
      final String finalPrefix = normalize(prefix);
      return names().stream().anyMatch(name -> name.startsWith(finalPrefix));
    }

    /** Gets all names in this headers. */
    public Set<String> names() {
      return Collections.unmodifiableSet(headers.keySet());
    }

    /** Gets iterator of this headers. */
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
      return entrySet().iterator();
    }

    @Override
    public String toString() {
      return headers.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Headers)) return false;
      Headers headers = (Headers) o;
      return Objects.equals(this.headers, headers.headers);
    }

    @Override
    public int hashCode() {
      return Objects.hash(headers);
    }

    /** Gets new okhttp3.Headers by populating this headers. */
    public okhttp3.Headers toHttpHeaders() {
      okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder();
      if (containsKey(CONTENT_ENCODING)) {
        builder.add(
            CONTENT_ENCODING,
            get(CONTENT_ENCODING).stream()
                .distinct()
                .filter(encoding -> !encoding.isEmpty())
                .collect(Collectors.joining(",")));
      }

      for (Map.Entry<String, String> entry : entries()) {
        if (!entry.getKey().equals(CONTENT_ENCODING)) {
          builder.addUnsafeNonAscii(entry.getKey(), entry.getValue());
        }
      }

      return builder.build();
    }

    /** Clears all names and values in this headers. */
    public void clear() {
      headers.clear();
    }

    /** Checks whether the specified name exists in this headers. */
    public boolean containsKey(String name) {
      return contains(name);
    }

    /** Gets Set of Map.Entry of this headers. */
    public Set<Map.Entry<String, String>> entrySet() {
      Set<Map.Entry<String, String>> result = new LinkedHashSet<>();
      for (Map.Entry<String, Set<String>> entry : headers.entrySet()) {
        for (String value : entry.getValue()) {
          result.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), value));
        }
      }
      return result;
    }

    /** Gets Set of Map.Entry of this headers. */
    public Set<Map.Entry<String, String>> entries() {
      return entrySet();
    }

    /** Gets set of values of the specified name in this headers. */
    public Set<String> get(String name) {
      if (name == null) name = "";
      return headers.getOrDefault(normalize(name), Collections.emptySet());
    }

    /** Gets set of values of the specified name in this headers. */
    public Set<String> keySet() {
      return names();
    }

    /**
     * Adds the specified value to the name in this headers. If the name already exists, the value
     * is appended uniquely.
     */
    public boolean put(@Nonnull String name, @Nonnull String value) {
      add(name, value);
      return true;
    }

    /** Adds set of values for the specified name to this headers. */
    public boolean put(@Nonnull String name, @Nonnull Set<String> values) {
      Utils.validateNotNull(values, "value");
      for (String value : values) put(name, value);
      return true;
    }

    /** Adds list of values for the specified name to this headers. */
    public boolean put(@Nonnull String name, @Nonnull List<String> values) {
      Utils.validateNotNull(values, "value");
      for (String value : values) put(name, value);
      return true;
    }

    /** Adds all names and values from the specified source to this headers. */
    public Headers putAll(Map<String, String> source) {
      if (source == null) return this;
      for (Map.Entry<String, String> entry : source.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /** Adds all names and values from the specified source to this headers. */
    public Headers putAll(Multimap<String, String> source) {
      if (source == null) return this;
      for (Map.Entry<String, String> entry : source.entries()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /** Adds all names and values from the specified source to this headers. */
    public Headers putAll(Headers source) {
      if (source == null) return this;
      for (Map.Entry<String, String> entry : source.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /** Removes the specified name from this headers. */
    public Set<String> remove(String name) {
      if (name == null) name = "";
      return headers.remove(normalize(name));
    }

    /** Removes the specified value of the specified name from this headers. */
    public boolean remove(String name, String value) {
      if (name == null) return false;
      name = normalize(name);
      boolean result = headers.containsKey(name) ? headers.get(name).remove(value) : false;
      if (result && headers.get(name).size() == 0) headers.remove(name);
      return result;
    }

    /** Removes the specified name from this headers. */
    public boolean removeAll(String name) {
      return remove(name) != null;
    }

    /** Gets size of this headers. */
    public int size() {
      return headers.size();
    }
  }

  /** HTTP query parameters. */
  public static class QueryParameters implements Iterable<Map.Entry<String, String>> {
    private static final long serialVersionUID = 5193347714796984439L;

    private final Map<String, List<String>> parameters = new HashMap<>();

    /** Creates empty query parameters. */
    public QueryParameters() {}

    /** Creates query parameters as copy of the specified source. */
    public QueryParameters(QueryParameters source) {
      if (source != null) putAll(source);
    }

    /** Creates query parameters as copy of the specified source. */
    public QueryParameters(Map<String, String> source) {
      if (source != null) putAll(source);
    }

    /** Creates query parameters as copy of the specified source. */
    public QueryParameters(Multimap<String, String> source) {
      if (source != null) putAll(source);
    }

    /** Creates query parameters by alternating keys and values. */
    public QueryParameters(String... keysAndValues) {
      if (keysAndValues.length % 2 != 0) {
        throw new IllegalArgumentException("Expected alternating keys and values");
      }
      for (int i = 0; i < keysAndValues.length; i += 2) {
        set(keysAndValues[i], keysAndValues[i + 1]);
      }
    }

    /**
     * Creates new query parameters by merging keys and values from the specified query parameters
     * list.
     */
    public static QueryParameters merge(QueryParameters... queryParamsList) {
      QueryParameters queryParams = new QueryParameters();
      for (QueryParameters q : queryParamsList) queryParams.putAll(q);
      return queryParams;
    }

    /** Adds the specified value to the key in this query parameters. */
    public void add(@Nonnull String key, @Nonnull String value) {
      Utils.validateNotEmptyString(key, "key");
      if (value == null) value = "";
      parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    /** Sets the specified key and value in this query parameters. */
    public void set(@Nonnull String key, @Nonnull String value) {
      Utils.validateNotEmptyString(key, "key");
      if (value == null) value = "";
      parameters.put(key, new ArrayList<>(Collections.singletonList(value)));
    }

    /** Gets the first value of the specified key in this query parameters. */
    public String getFirst(String key) {
      if (key == null) return null;
      List<String> values = get(key);
      return values.isEmpty() ? null : values.get(0);
    }

    /** Checks whether the specified key exists in this query parameters. */
    public boolean contains(String key) {
      if (key == null) return false;
      return parameters.containsKey(key);
    }

    /** Checks whether any key in this query parameters starts with the specified prefix or not. */
    public boolean keyPrefixAny(String prefix) {
      if (prefix == null) return false;
      return keys().stream().anyMatch(key -> key.startsWith(prefix));
    }

    /** Gets all keys in this query parameters. */
    public Set<String> keys() {
      return Collections.unmodifiableSet(parameters.keySet());
    }

    /** Gets iterator of this query parameters. */
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
      return entrySet().iterator();
    }

    @Override
    public String toString() {
      return parameters.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof QueryParameters)) return false;
      QueryParameters queryParams = (QueryParameters) o;
      return Objects.equals(this.parameters, queryParams.parameters);
    }

    @Override
    public int hashCode() {
      return Objects.hash(parameters);
    }

    /** Clears all keys and values in this query parameters. */
    public void clear() {
      parameters.clear();
    }

    /** Checks whether the specified key exists in this query parameters. */
    public boolean containsKey(String key) {
      if (key == null) return false;
      return parameters.containsKey(key);
    }

    /** Gets Set of Map.Entry of this query parameters. */
    public Set<Map.Entry<String, String>> entrySet() {
      Set<Map.Entry<String, String>> result = new LinkedHashSet<>();
      for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
        for (String value : entry.getValue()) {
          result.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), value));
        }
      }
      return result;
    }

    /** Gets Set of Map.Entry of this query parameters. */
    public Set<Map.Entry<String, String>> entries() {
      return entrySet();
    }

    /** Gets set of values of the specified key in this query parameters. */
    public List<String> get(String key) {
      if (key == null) key = "";
      return parameters.getOrDefault(key, Collections.emptyList());
    }

    /** Gets set of values of the specified key in this query parameters. */
    public Set<String> keySet() {
      return keys();
    }

    /** Adds the specified value to the key in this query parameters. */
    public boolean put(@Nonnull String key, @Nonnull String value) {
      add(key, value);
      return true;
    }

    /** Adds list of values for the specified key to this query parameters. */
    public QueryParameters put(@Nonnull String key, @Nonnull List<String> values) {
      Utils.validateNotNull(values, "value");
      for (String value : values) put(key, value);
      return this;
    }

    /** Adds all keys and values from the specified source to this query parameters. */
    public QueryParameters putAll(Map<String, String> source) {
      if (source == null) return this;
      for (Map.Entry<String, String> entry : source.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /** Adds all keys and values from the specified source to this query parameters. */
    public QueryParameters putAll(Multimap<String, String> source) {
      if (source == null) return this;
      for (Map.Entry<String, String> entry : source.entries()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /** Adds all keys and values from the specified source to this query parameters. */
    public QueryParameters putAll(QueryParameters source) {
      if (source == null) return this;
      for (Map.Entry<String, String> entry : source.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /** Removes the specified key from this query parameters. */
    public List<String> remove(String key) {
      if (key == null) key = "";
      return parameters.remove(key);
    }

    /** Removes the specified value of the specified key from this query parameters. */
    public boolean remove(String key, String value) {
      if (key == null) return false;
      return parameters.containsKey(key) ? parameters.get(key).remove(value) : false;
    }

    /** Removes the specified key from this query parameters. */
    public boolean removeAll(String key) {
      return remove(key) != null;
    }

    /** Gets size of this query parameters. */
    public int size() {
      return parameters.size();
    }
  }

  /** HTTP request. */
  public static class Request {
    private okhttp3.Request httpRequest;
    private String httpTraces;

    /** Creates request with specified HTTP request and HTTP trace. */
    public Request(okhttp3.Request httpRequest, String httpTraces) {
      this.httpRequest = httpRequest;
      this.httpTraces = httpTraces;
    }

    /** Gets HTTP request. */
    public okhttp3.Request httpRequest() {
      return httpRequest;
    }

    /** Gets HTTP trace. */
    public String httpTraces() {
      return httpTraces;
    }
  }

  /** S3 request. */
  public static class S3Request {
    private String userAgent;
    private Method method;
    private BaseArgs args;
    private Headers headers;
    private QueryParameters queryParams;
    private Body body;

    private String bucket;
    private String region;
    private String object;

    private S3Request(Builder builder) {
      this.userAgent = builder.userAgent;
      this.method = builder.method;
      this.args = builder.args;
      this.headers = builder.headers;
      this.queryParams = builder.queryParams;
      this.body = builder.body;

      if (args != null) {
        this.headers = Headers.merge(args.extraHeaders(), builder.headers);
        this.queryParams = QueryParameters.merge(args.extraQueryParams(), builder.queryParams);

        if (args instanceof BucketArgs) {
          this.bucket = ((BucketArgs) args).bucket();
          this.region = ((BucketArgs) args).region();
        }
        if (args instanceof ObjectArgs) this.object = ((ObjectArgs) args).object();
      }
    }

    public String userAgent() {
      return userAgent;
    }

    public Method method() {
      return method;
    }

    public BaseArgs args() {
      return args;
    }

    public Headers headers() {
      return headers;
    }

    public QueryParameters queryParams() {
      return queryParams;
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

    private Request toRequest(
        BaseUrl baseUrl, String region, Credentials credentials, Integer expiry)
        throws MinioException {
      if (region == null) region = this.region;
      if (region == null) region = US_EAST_1;

      okhttp3.HttpUrl url = baseUrl.buildUrl(method, bucket, object, region, queryParams);

      Body body = this.body;
      if (body == null) {
        body =
            headers.containsKey(Headers.CONTENT_TYPE)
                ? new Body(
                    Utils.EMPTY_BYTE_ARRAY,
                    0,
                    MediaType.parse(headers.getFirst(Headers.CONTENT_TYPE)),
                    Checksum.ZERO_SHA256_HASH,
                    Checksum.ZERO_MD5_HASH)
                : EMPTY_BODY;
      }

      String sha256Hash = null;
      if (!body.isHttpRequestBody()) {
        if (credentials == null) {
          if (body.md5Hash() == null) {
            throw new IllegalArgumentException("MD5 hash must be provided to request body");
          }
        } else if (!url.isHttps()) {
          if (body.sha256Hash() == null) {
            throw new IllegalArgumentException("SHA256 hash must be provided to request body");
          }
        } else if (body.sha256Hash() == null) {
          sha256Hash = Checksum.UNSIGNED_PAYLOAD;
        }
      }

      okhttp3.RequestBody requestBody = body.toRequestBody();

      Headers headers = Headers.merge(this.headers, body.headers());
      if (sha256Hash != null) headers.put(Headers.X_AMZ_CONTENT_SHA256, sha256Hash);
      if (credentials != null) {
        String sessionToken = credentials.sessionToken();
        if (sessionToken != null) headers.put(Headers.X_AMZ_SECURITY_TOKEN, sessionToken);
        headers.put(Headers.X_AMZ_DATE, ZonedDateTime.now().format(Time.AMZ_DATE_FORMAT));
      }
      // Disable default okhttp gzip compression
      headers.put(Headers.ACCEPT_ENCODING, "identity");
      headers.put(Headers.USER_AGENT, userAgent);
      headers.put(Headers.HOST, Utils.getHostHeader(url));
      if (method == Method.PUT || method == Method.POST) {
        headers.put(Headers.CONTENT_TYPE, body.contentType().toString());
        try {
          headers.put(Headers.CONTENT_LENGTH, String.valueOf(requestBody.contentLength()));
        } catch (IOException e) {
          throw new MinioException(e);
        }
      } else {
        headers.remove(Headers.CONTENT_TYPE);
      }

      if (expiry != null) {
        headers.remove(Headers.CONTENT_LENGTH);
        headers.remove(Headers.CONTENT_TYPE);
      }

      okhttp3.Request request =
          new okhttp3.Request.Builder()
              .url(url)
              .headers(headers.toHttpHeaders())
              .method(
                  method.toString(),
                  (method == Method.PUT || method == Method.POST) ? requestBody : null)
              .build();
      if (!body.isHttpRequestBody()) {
        if (credentials != null) {
          if (expiry == null) {
            request =
                Signer.signV4S3(
                    request,
                    region,
                    credentials.accessKey(),
                    credentials.secretKey(),
                    sha256Hash != null ? sha256Hash : body.sha256Hash());
          } else {
            okhttp3.HttpUrl signedUrl =
                Signer.presignV4(
                    request, region, credentials.accessKey(), credentials.secretKey(), expiry);
            request = request.newBuilder().url(signedUrl).build();
          }
        }
      }

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
      String lastTwoChars = traceBuilder.substring(traceBuilder.length() - 2);
      if (lastTwoChars.charAt(1) != '\n') {
        traceBuilder.append("\n\n");
      } else if (lastTwoChars.charAt(0) != '\n') {
        traceBuilder.append("\n");
      }
      String value = body.toString();
      if (method == Method.PUT || method == Method.POST) {
        traceBuilder.append(value);
        if (!value.endsWith("\n")) traceBuilder.append("\n");
      }

      return new Request(request, traceBuilder.toString());
    }

    public Request toRequest(BaseUrl baseUrl, String region, Credentials credentials)
        throws MinioException {
      return toRequest(baseUrl, region, credentials, null);
    }

    public Request toPresignedRequest(
        BaseUrl baseUrl, String region, Credentials credentials, int expiry) throws MinioException {
      return toRequest(baseUrl, region, credentials, expiry);
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder of {@link S3Request}. */
    public static class Builder {
      private String userAgent;
      private Method method;
      private BaseArgs args;
      private Headers headers;
      private QueryParameters queryParams;
      private Body body;

      public Builder userAgent(String userAgent) {
        this.userAgent = Utils.validateNotNull(userAgent, "user agent");
        return this;
      }

      public Builder method(Method method) {
        this.method = Utils.validateNotNull(method, "HTTP method");
        return this;
      }

      public Builder args(BaseArgs args) {
        this.args = args;
        return this;
      }

      public Builder headers(Headers headers) {
        this.headers = headers;
        return this;
      }

      public Builder queryParams(QueryParameters queryParams) {
        this.queryParams = queryParams;
        return this;
      }

      public Builder body(Body body) {
        this.body = body;
        return this;
      }

      public S3Request build() {
        if (userAgent == null) throw new IllegalArgumentException("user agent must be provided");
        if (method == null) throw new IllegalArgumentException("method must be provided");
        return new S3Request(this);
      }
    }
  }
}
