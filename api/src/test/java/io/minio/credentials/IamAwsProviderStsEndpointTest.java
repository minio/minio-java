/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2026 MinIO, Inc.
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

package io.minio.credentials;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.ProviderException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests STS endpoint and AWS_CA_BUNDLE handling of {@link IamAwsProvider}. {@link
 * EnvironmentProvider#getProperty} reads system properties before environment variables, so tests
 * set system properties to model environment variables.
 */
public class IamAwsProviderStsEndpointTest {
  private static final List<String> NAMES =
      Arrays.asList(
          "AWS_WEB_IDENTITY_TOKEN_FILE",
          "AWS_ROLE_ARN",
          "AWS_REGION",
          "AWS_ENDPOINT_URL_STS",
          "AWS_ENDPOINT_URL",
          "AWS_IGNORE_CONFIGURED_ENDPOINT_URLS",
          "AWS_CA_BUNDLE",
          "SSL_CERT_FILE",
          "SSL_CERT_DIR",
          "javax.net.ssl.trustStore",
          "javax.net.ssl.trustStoreType",
          "javax.net.ssl.trustStorePassword",
          "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI",
          "AWS_CONTAINER_CREDENTIALS_FULL_URI");

  private static final List<String> MASKED =
      Arrays.asList(
          "AWS_REGION",
          "AWS_ENDPOINT_URL_STS",
          "AWS_ENDPOINT_URL",
          "AWS_IGNORE_CONFIGURED_ENDPOINT_URLS",
          "AWS_CA_BUNDLE",
          "SSL_CERT_FILE",
          "SSL_CERT_DIR");

  private static final String CERT =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIIC8TCCAdmgAwIBAgIIVQI5/aydlf4wDQYJKoZIhvcNAQEMBQAwJzElMCMGA1UE\n"
          + "AxMcbWluaW8tamF2YS10ZXN0LWUwZWVmYWQwYjRiZTAeFw0yNjA2MDkwOTI4MDNa\n"
          + "Fw0zNjA2MDYwOTI4MDNaMCcxJTAjBgNVBAMTHG1pbmlvLWphdmEtdGVzdC1lMGVl\n"
          + "ZmFkMGI0YmUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC2wswKet+8\n"
          + "P0KEPycjP0cUeWtVuzwReMG4iMJxU80xg6rHzHW33tx89HEyhqBM0tAhnOlv8uyN\n"
          + "dIlLQRKMNj2U82PW1DNfDqvahCqI1P5HEcqmHXYMXUIIuHQ42Vaq5Jw6LfUT5Xp3\n"
          + "OskJuXsrqhJ/lI2tjO2IA6Ihq2qWH8HMK13usrRU8ercMi3v3l+NmE2v9cAYNjDn\n"
          + "y+wE4TGIjxBnOcR7fSF6zcMydiu371FD53o3any47BGcjQrf11KuToMWCI6xRyox\n"
          + "oRFif2heDNtPlm+sN7fLoz8RozLLN0GCT1+g3RfLDnbMOD/Zpl4JSSW+ZW43wrhH\n"
          + "Kt+M32Wg1mvjAgMBAAGjITAfMB0GA1UdDgQWBBQc31QOSV+G44gzEaP0Nzki7+3j\n"
          + "zDANBgkqhkiG9w0BAQwFAAOCAQEAbS1xk1KS7yflxFHcD0kdwaUi3y+zsD7JEqPo\n"
          + "YtZsJB3YZF+7mCLcvpQpeOj/YjjS4Nfm+BTiBEm4iQ10XYJqq7Ld8+b37Lu0lUwq\n"
          + "BEM05XdGqIy2ZElYLB4uwai/foAPqpASbtqfuF3k/r7Iv+vuLAcNDIZ95gpIbgyS\n"
          + "1VezowSP4jSTlIISFhUlTJwD4sSA4FpdBs2JytjdQ+5bRbQPKC2lTRNUjDzIHWN0\n"
          + "FcA+xu6MMlXe1EtVYSPPRoHnc/qBE0yEiyBglgqETxd1XUGuCZfCNSAICMafHtua\n"
          + "DppeWJHfHv2CXNFva0iicwzYJ5kqoeJF8GAU3+QD0TMx59IfwA==\n"
          + "-----END CERTIFICATE-----\n";

  private static final String STS_RESPONSE =
      "<AssumeRoleWithWebIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">"
          + "<AssumeRoleWithWebIdentityResult><Credentials>"
          + "<AccessKeyId>ACCESS</AccessKeyId><SecretAccessKey>SECRET</SecretAccessKey>"
          + "<SessionToken>TOKEN</SessionToken><Expiration>2099-01-01T00:00:00Z</Expiration>"
          + "</Credentials></AssumeRoleWithWebIdentityResult>"
          + "</AssumeRoleWithWebIdentityResponse>";

  private static final String IMDS_RESPONSE =
      "{\"Code\":\"Success\",\"AccessKeyId\":\"ACCESS\",\"SecretAccessKey\":\"SECRET\","
          + "\"Token\":\"TOKEN\",\"Expiration\":\"2099-01-01T00:00:00Z\"}";

  private static final List<String> IMDS_HIDDEN =
      Arrays.asList(
          "AWS_WEB_IDENTITY_TOKEN_FILE",
          "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI",
          "AWS_CONTAINER_CREDENTIALS_FULL_URI");

  /** Provider hiding web identity and container settings of the real environment. */
  private static class IsolatedProvider extends IamAwsProvider {
    IsolatedProvider(String endpoint, OkHttpClient client) {
      super(endpoint, client);
    }

    @Override
    protected String getProperty(String name) {
      return IMDS_HIDDEN.contains(name) ? null : super.getProperty(name);
    }
  }

  private final List<String> saved = new ArrayList<>();
  private final List<MockWebServer> servers = new ArrayList<>();

  @Before
  public void setUp() throws IOException {
    for (String name : NAMES) {
      saved.add(System.getProperty(name));
      System.clearProperty(name);
    }
    // An empty property masks a variable of the real environment and counts as unset.
    for (String name : MASKED) System.setProperty(name, "");
    File token = File.createTempFile("minio-web-identity", ".token");
    token.deleteOnExit();
    Files.write(token.toPath(), "jwt".getBytes(StandardCharsets.UTF_8));
    System.setProperty("AWS_WEB_IDENTITY_TOKEN_FILE", token.getAbsolutePath());
  }

  @After
  public void tearDown() throws IOException {
    for (int i = 0; i < NAMES.size(); i++) {
      if (saved.get(i) == null) {
        System.clearProperty(NAMES.get(i));
      } else {
        System.setProperty(NAMES.get(i), saved.get(i));
      }
    }
    for (MockWebServer server : servers) server.shutdown();
  }

  private MockWebServer newServer(String body) throws IOException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(body));
    server.start();
    servers.add(server);
    return server;
  }

  private static String writeFile(String content) throws IOException {
    File file = File.createTempFile("minio-ca-bundle", ".pem");
    file.deleteOnExit();
    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    return file.getAbsolutePath();
  }

  private static void assertFetched(MockWebServer server, String path)
      throws IOException, InterruptedException {
    Assert.assertEquals(1, server.getRequestCount());
    RecordedRequest request = server.takeRequest();
    Assert.assertEquals(path, request.getRequestUrl().encodedPath());
    Assert.assertEquals(
        "AssumeRoleWithWebIdentity", request.getRequestUrl().queryParameter("Action"));
  }

  @Test
  public void testExplicitEndpointWinsOverEnvironment() throws IOException, InterruptedException {
    MockWebServer explicit = newServer(STS_RESPONSE);
    MockWebServer other = newServer(STS_RESPONSE);
    System.setProperty("AWS_ENDPOINT_URL_STS", other.url("/").toString());
    System.setProperty("AWS_ENDPOINT_URL", other.url("/").toString());

    Credentials creds = new IamAwsProvider(explicit.url("/").toString(), null).fetch();

    Assert.assertEquals("ACCESS", creds.accessKey());
    assertFetched(explicit, "/");
    Assert.assertEquals(0, other.getRequestCount());
  }

  @Test
  public void testStsEnvironmentIgnoresGlobalEndpoint() throws IOException, InterruptedException {
    MockWebServer sts = newServer(STS_RESPONSE);
    MockWebServer generic = newServer(STS_RESPONSE);
    System.setProperty("AWS_ENDPOINT_URL_STS", sts.url("/").toString());
    System.setProperty("AWS_ENDPOINT_URL", generic.url("/").toString());

    new IamAwsProvider(null, null).fetch();

    assertFetched(sts, "/");
    Assert.assertEquals(0, generic.getRequestCount());
  }

  /** AWS_ENDPOINT_URL is not read: the default endpoint is used. */
  @Test
  public void testGlobalEndpointUrlIsIgnored() {
    System.setProperty("AWS_ENDPOINT_URL", "https://sts.example.com/sts");
    Assert.assertEquals("sts.amazonaws.com", fetchRecorded().host());
    Assert.assertEquals("/", fetchRecorded().encodedPath());
  }

  @Test
  public void testEndpointPathIsPreserved() throws IOException, InterruptedException {
    MockWebServer sts = newServer(STS_RESPONSE);
    System.setProperty("AWS_ENDPOINT_URL_STS", sts.url("/sts/ns/name").toString());

    new IamAwsProvider(null, null).fetch();

    assertFetched(sts, "/sts/ns/name");
  }

  /** Client answering every request with an STS response and recording the URL, without network. */
  private static OkHttpClient recordingClient(List<HttpUrl> urls) {
    return new OkHttpClient.Builder()
        .addInterceptor(
            chain -> {
              urls.add(chain.request().url());
              return new Response.Builder()
                  .request(chain.request())
                  .protocol(Protocol.HTTP_1_1)
                  .code(200)
                  .message("OK")
                  .body(ResponseBody.create(STS_RESPONSE, MediaType.parse("application/xml")))
                  .build();
            })
        .build();
  }

  private static HttpUrl fetchRecorded() {
    List<HttpUrl> urls = new ArrayList<>();
    new IamAwsProvider(null, recordingClient(urls)).fetch();
    Assert.assertEquals(1, urls.size());
    return urls.get(0);
  }

  @Test
  public void testStsEnvironmentWinsOverRegionDefault() throws IOException, InterruptedException {
    MockWebServer sts = newServer(STS_RESPONSE);
    System.setProperty("AWS_REGION", "eu-west-1");
    System.setProperty("AWS_ENDPOINT_URL_STS", sts.url("/").toString());

    new IamAwsProvider(null, null).fetch();

    assertFetched(sts, "/");
  }

  @Test
  public void testIgnoreFlagChoosesDefaultHost() {
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://sts.example.com/sts");
    System.setProperty("AWS_IGNORE_CONFIGURED_ENDPOINT_URLS", "true");
    Assert.assertEquals("sts.amazonaws.com", fetchRecorded().host());

    System.setProperty("AWS_REGION", "eu-west-1");
    Assert.assertEquals("sts.eu-west-1.amazonaws.com", fetchRecorded().host());
  }

  @Test
  public void testIgnoreFlagCaseInsensitiveAndOtherValues() {
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://sts.example.com/sts");
    System.setProperty("AWS_IGNORE_CONFIGURED_ENDPOINT_URLS", "TRUE");
    Assert.assertEquals("sts.amazonaws.com", fetchRecorded().host());

    System.setProperty("AWS_IGNORE_CONFIGURED_ENDPOINT_URLS", "false");
    Assert.assertEquals("sts.example.com", fetchRecorded().host());
  }

  @Test
  public void testIgnoreFlagDoesNotOverrideExplicitEndpoint()
      throws IOException, InterruptedException {
    MockWebServer explicit = newServer(STS_RESPONSE);
    System.setProperty("AWS_IGNORE_CONFIGURED_ENDPOINT_URLS", "true");

    new IamAwsProvider(explicit.url("/").toString(), null).fetch();

    assertFetched(explicit, "/");
  }

  @Test
  public void testEmptyEnvironmentVariablesAreUnset() {
    System.setProperty("AWS_ENDPOINT_URL_STS", "");
    Assert.assertEquals("sts.amazonaws.com", fetchRecorded().host());
  }

  @Test
  public void testRegionDefaults() {
    System.setProperty("AWS_REGION", "us-east-1");
    Assert.assertEquals("sts.us-east-1.amazonaws.com", fetchRecorded().host());
  }

  @Test
  public void testChinaRegionDefault() {
    System.setProperty("AWS_REGION", "cn-north-1");
    Assert.assertEquals("sts.cn-north-1.amazonaws.com.cn", fetchRecorded().host());
  }

  @Test
  public void testEnvironmentEndpointPathPreserved() {
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://sts.example.com/sts/other/name");
    Assert.assertEquals("/sts/other/name", fetchRecorded().encodedPath());
  }

  @Test
  public void testInvalidEnvironmentEndpoint() {
    System.setProperty("AWS_ENDPOINT_URL_STS", "not a url");
    // Any request, e.g. to a fallback endpoint, fails the test.
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  throw new AssertionError("no request must be sent: " + chain.request().url());
                })
            .build();
    try {
      new IamAwsProvider(null, client).fetch();
      Assert.fail("invalid endpoint must be rejected");
    } catch (ProviderException e) {
      Assert.assertTrue(e.getMessage(), e.getMessage().contains("AWS_ENDPOINT_URL_STS"));
      Assert.assertFalse("value must not leak", e.getMessage().contains("not a url"));
    }
  }

  @Test
  public void testImdsPathIgnoresEnvironmentEndpoints() throws IOException, InterruptedException {
    MockWebServer env = newServer(STS_RESPONSE);
    System.setProperty("AWS_ENDPOINT_URL_STS", env.url("/").toString());
    // IMDS: token, role name, credentials.
    MockWebServer imds = new MockWebServer();
    imds.enqueue(new MockResponse().setBody("imds-token"));
    imds.enqueue(new MockResponse().setBody("role"));
    imds.enqueue(new MockResponse().setBody(IMDS_RESPONSE));
    imds.start();
    servers.add(imds);

    // Hide web identity and container settings of the real environment from the provider.
    IamAwsProvider provider = new IsolatedProvider(imds.url("/").toString(), null);
    Credentials creds = provider.fetch();

    Assert.assertEquals("ACCESS", creds.accessKey());
    Assert.assertEquals(3, imds.getRequestCount());
    Assert.assertEquals(0, env.getRequestCount());
  }

  @Test
  public void testCaBundleExtendsDefaultTrust() throws IOException, GeneralSecurityException {
    // Controlled JVM trust store with one known anchor.
    HeldCertificate anchor = new HeldCertificate.Builder().build();
    KeyStore store = KeyStore.getInstance("PKCS12");
    store.load(null, null);
    store.setCertificateEntry("anchor", anchor.certificate());
    File trustStore = File.createTempFile("minio-truststore", ".p12");
    trustStore.deleteOnExit();
    try (OutputStream out = Files.newOutputStream(trustStore.toPath())) {
      store.store(out, "changeit".toCharArray());
    }
    System.setProperty("javax.net.ssl.trustStore", trustStore.getAbsolutePath());
    System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

    HeldCertificate bundleCert = new HeldCertificate.Builder().build();
    HeldCertificate sslCertFileCert = new HeldCertificate.Builder().build();
    System.setProperty("AWS_CA_BUNDLE", writeFile(bundleCert.certificatePem()));
    System.setProperty("SSL_CERT_FILE", writeFile(sslCertFileCert.certificatePem()));

    OkHttpClient client = new IamAwsProvider(null, null).stsHttpClient();

    Set<X509Certificate> issuers =
        new HashSet<>(Arrays.asList(client.x509TrustManager().getAcceptedIssuers()));
    Assert.assertTrue("JVM trust anchor must be kept", issuers.contains(anchor.certificate()));
    Assert.assertTrue(
        "bundle certificate must be trusted", issuers.contains(bundleCert.certificate()));
    Assert.assertTrue(
        "SSL_CERT_FILE certificate must be kept", issuers.contains(sslCertFileCert.certificate()));
  }

  /** Provider counting how often the client trusting AWS_CA_BUNDLE is requested. */
  private static class CountingProvider extends IamAwsProvider {
    int calls;

    CountingProvider(String endpoint, OkHttpClient client) {
      super(endpoint, client);
    }

    @Override
    OkHttpClient stsHttpClient() {
      calls++;
      return super.stsHttpClient();
    }
  }

  @Test
  public void testMissingCaBundleIsIgnoredWithExplicitEndpoint()
      throws IOException, InterruptedException {
    MockWebServer explicit = newServer(STS_RESPONSE);
    System.setProperty("AWS_CA_BUNDLE", "/nonexistent/bundle.pem");
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://sts.example.com/sts");

    new IamAwsProvider(explicit.url("/").toString(), null).fetch();

    assertFetched(explicit, "/");
  }

  @Test
  public void testMissingCaBundleIsIgnoredWithDefaultEndpoint() {
    System.setProperty("AWS_CA_BUNDLE", "/nonexistent/bundle.pem");
    List<HttpUrl> urls = new ArrayList<>();
    CountingProvider provider = new CountingProvider(null, recordingClient(urls));

    provider.fetch();

    Assert.assertEquals("sts.amazonaws.com", urls.get(0).host());
    Assert.assertEquals(0, provider.calls);
  }

  @Test
  public void testCaBundleIsReadOnlyForEnvironmentEndpoint() {
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://sts.example.com/sts");
    List<HttpUrl> urls = new ArrayList<>();
    CountingProvider provider = new CountingProvider(null, recordingClient(urls));

    provider.fetch();

    Assert.assertEquals("sts.example.com", urls.get(0).host());
    Assert.assertEquals(1, provider.calls);
  }

  /** The IMDS path without an explicit endpoint must not be redirected by AWS_ENDPOINT_URL_STS. */
  @Test
  public void testDefaultImdsIgnoresEnvironmentEndpoint() {
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://sts.example.com/sts");
    List<HttpUrl> urls = new ArrayList<>();
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  HttpUrl url = chain.request().url();
                  urls.add(url);
                  String body = IMDS_RESPONSE;
                  if (url.encodedPath().equals("/latest/api/token")) body = "imds-token";
                  if (url.encodedPath().equals("/latest/meta-data/iam/security-credentials/")) {
                    body = "role";
                  }
                  return new Response.Builder()
                      .request(chain.request())
                      .protocol(Protocol.HTTP_1_1)
                      .code(200)
                      .message("OK")
                      .body(ResponseBody.create(body, MediaType.parse("text/plain")))
                      .build();
                })
            .build();

    Credentials creds = new IsolatedProvider(null, client).fetch();

    Assert.assertEquals("ACCESS", creds.accessKey());
    Assert.assertEquals(3, urls.size());
    for (HttpUrl url : urls) Assert.assertEquals("169.254.169.254", url.host());
  }

  private HeldCertificate startTlsServer(MockWebServer server) throws IOException {
    HeldCertificate cert =
        new HeldCertificate.Builder().addSubjectAlternativeName("127.0.0.1").build();
    HandshakeCertificates serverCerts =
        new HandshakeCertificates.Builder().heldCertificate(cert).build();
    server.useHttps(serverCerts.sslSocketFactory(), false);
    server.enqueue(new MockResponse().setResponseCode(200).setBody(STS_RESPONSE));
    server.start(InetAddress.getByName("127.0.0.1"), 0);
    servers.add(server);
    return cert;
  }

  private static void assertTlsFailure(Throwable e) {
    StringBuilder chain = new StringBuilder();
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof SSLException) return;
      chain.append(t).append(" <- ");
    }
    Assert.fail("expected a TLS failure, got " + chain);
  }

  @Test
  public void testCaBundleTrustedInTlsHandshake() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    HeldCertificate cert = startTlsServer(server);
    System.setProperty("AWS_CA_BUNDLE", writeFile(cert.certificatePem()));
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://127.0.0.1:" + server.getPort() + "/sts");

    Credentials creds = new IamAwsProvider(null, null).fetch();

    Assert.assertEquals("ACCESS", creds.accessKey());
    assertFetched(server, "/sts");
  }

  @Test
  public void testTlsFailsWithoutCaBundle() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    startTlsServer(server);
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://127.0.0.1:" + server.getPort());

    try {
      new IamAwsProvider(null, null).fetch();
      Assert.fail("untrusted certificate must be rejected");
    } catch (ProviderException e) {
      assertTlsFailure(e);
    }
  }

  @Test
  public void testCustomClientIgnoresCaBundle() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    HeldCertificate cert = startTlsServer(server);
    System.setProperty("AWS_CA_BUNDLE", writeFile(cert.certificatePem()));
    System.setProperty("AWS_ENDPOINT_URL_STS", "https://127.0.0.1:" + server.getPort());
    OkHttpClient custom = new OkHttpClient();
    SSLSocketFactory factory = custom.sslSocketFactory();
    X509TrustManager trustManager = custom.x509TrustManager();

    IamAwsProvider provider = new IamAwsProvider(null, custom);
    Assert.assertSame(custom, provider.stsHttpClient());
    try {
      provider.fetch();
      Assert.fail("bundle must not be applied to a caller supplied client");
    } catch (ProviderException e) {
      assertTlsFailure(e);
    }
    Assert.assertSame(factory, custom.sslSocketFactory());
    Assert.assertSame(trustManager, custom.x509TrustManager());
  }

  @Test
  public void testCaBundleIgnoredWithCustomClient() {
    System.setProperty("AWS_CA_BUNDLE", "/nonexistent/bundle.pem");
    OkHttpClient custom = new OkHttpClient();

    Assert.assertSame(custom, new IamAwsProvider(null, custom).stsHttpClient());
  }

  @Test
  public void testUnreadableCaBundle() {
    System.setProperty("AWS_CA_BUNDLE", "/nonexistent/bundle.pem");
    try {
      new IamAwsProvider(null, null).stsHttpClient();
      Assert.fail("unreadable bundle must be rejected");
    } catch (ProviderException e) {
      Assert.assertTrue(e.getMessage(), e.getMessage().contains("AWS_CA_BUNDLE"));
    }
  }

  @Test
  public void testCaBundleWithoutCertificates() throws IOException, InterruptedException {
    System.setProperty("AWS_CA_BUNDLE", writeFile("not a certificate\n"));
    try {
      new IamAwsProvider(null, null).stsHttpClient();
      Assert.fail("bundle without certificates must be rejected");
    } catch (ProviderException e) {
      Assert.assertTrue(e.getMessage(), e.getMessage().contains("AWS_CA_BUNDLE"));
    }
  }

  @Test
  public void testEmptyCaBundleFile() throws IOException {
    System.setProperty("AWS_CA_BUNDLE", writeFile(""));
    try {
      new IamAwsProvider(null, null).stsHttpClient();
      Assert.fail("zero-byte bundle must be rejected");
    } catch (ProviderException e) {
      Assert.assertTrue(e.getMessage(), e.getMessage().contains("AWS_CA_BUNDLE"));
    }
  }

  @Test
  public void testInvalidCaBundlePath() {
    System.setProperty("AWS_CA_BUNDLE", "bad\0name.pem");
    try {
      new IamAwsProvider(null, null).stsHttpClient();
      Assert.fail("invalid path must be rejected");
    } catch (ProviderException e) {
      Assert.assertTrue(e.getMessage(), e.getMessage().contains("AWS_CA_BUNDLE"));
      Assert.assertNotNull(e.getCause());
    }
  }

  @Test
  public void testNoCaBundleKeepsClient() {
    IamAwsProvider provider = new IamAwsProvider(null, null);
    Assert.assertSame(provider.stsHttpClient(), provider.stsHttpClient());
  }
}
