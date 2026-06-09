/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2026 MinIO, Inc.
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class HttpExternalCertificatesTest {
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

  @Parameters(name = "{0}")
  public static Collection<Object[]> bundles() {
    return Arrays.asList(
        new Object[][] {
          {"single trailing newline", CERT},
          {"no trailing newline", CERT.trim()},
          {"trailing blank line", CERT + "\n"},
          {"trailing whitespace", CERT + "   \n"},
        });
  }

  @Parameter()
  public String name;

  @Parameter(1)
  public String bundle;

  @Test
  public void loadsExternalCertificateBundle() throws Exception {
    String path = writeBundle(bundle);
    OkHttpClient client = Http.enableExternalCertificates(new OkHttpClient(), path, null);
    Assert.assertNotNull(client);
  }

  private static String writeBundle(String content) throws Exception {
    File file = File.createTempFile("minio-ca-bundle", ".pem");
    file.deleteOnExit();
    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    return file.getAbsolutePath();
  }
}
