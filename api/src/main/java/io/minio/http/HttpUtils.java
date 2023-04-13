/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
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

package io.minio.http;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.org.apache.commons.validator.routines.InetAddressValidator;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/** HTTP utilities. */
public class HttpUtils {
  public static final byte[] EMPTY_BODY = new byte[] {};

  public static void validateNotNull(Object arg, String argName) {
    if (arg == null) {
      throw new IllegalArgumentException(argName + " must not be null.");
    }
  }

  public static void validateNotEmptyString(String arg, String argName) {
    validateNotNull(arg, argName);
    if (arg.isEmpty()) {
      throw new IllegalArgumentException(argName + " must be a non-empty string.");
    }
  }

  public static void validateNullOrNotEmptyString(String arg, String argName) {
    if (arg != null && arg.isEmpty()) {
      throw new IllegalArgumentException(argName + " must be a non-empty string.");
    }
  }

  public static void validateHostnameOrIPAddress(String endpoint) {
    // Check endpoint is IPv4 or IPv6.
    if (InetAddressValidator.getInstance().isValid(endpoint)) {
      return;
    }

    // Check endpoint is a hostname.

    // Refer https://en.wikipedia.org/wiki/Hostname#Restrictions_on_valid_host_names
    // why checks are done like below
    if (endpoint.length() < 1 || endpoint.length() > 253) {
      throw new IllegalArgumentException("invalid hostname");
    }

    for (String label : endpoint.split("\\.")) {
      if (label.length() < 1 || label.length() > 63) {
        throw new IllegalArgumentException("invalid hostname");
      }

      if (!(label.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$"))) {
        throw new IllegalArgumentException("invalid hostname");
      }
    }
  }

  public static void validateUrl(HttpUrl url) {
    if (!url.encodedPath().equals("/")) {
      throw new IllegalArgumentException("no path allowed in endpoint " + url);
    }
  }

  public static HttpUrl getBaseUrl(String endpoint) {
    validateNotEmptyString(endpoint, "endpoint");
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url == null) {
      validateHostnameOrIPAddress(endpoint);
      url = new HttpUrl.Builder().scheme("https").host(endpoint).build();
    } else {
      validateUrl(url);
    }

    return url;
  }

  public static String getHostHeader(HttpUrl url) {
    String host = url.host();
    if (InetAddressValidator.getInstance().isValidInet6Address(host)) {
      host = "[" + host + "]";
    }

    // ignore port when port and service matches i.e HTTP -> 80, HTTPS -> 443
    if ((url.scheme().equals("http") && url.port() == 80)
        || (url.scheme().equals("https") && url.port() == 443)) {
      return host;
    }

    return host + ":" + url.port();
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

  public static OkHttpClient newDefaultHttpClient(
      long connectTimeout, long writeTimeout, long readTimeout) {
    OkHttpClient httpClient =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
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

  @SuppressFBWarnings(value = "SIC", justification = "Should not be used in production anyways.")
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
}
