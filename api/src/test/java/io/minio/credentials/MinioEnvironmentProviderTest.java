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

import java.security.ProviderException;
import org.junit.Assert;
import org.junit.Test;

public class MinioEnvironmentProviderTest {
  private static final String ACCESS_KEY = "MINIO_ACCESS_KEY";
  private static final String SECRET_KEY = "MINIO_SECRET_KEY";

  /**
   * {@link EnvironmentProvider#getProperty} reads the system property before the environment
   * variable, so setting the property here models an exported-but-empty variable without touching
   * the real environment.
   */
  private void withProperties(String accessKey, String secretKey, Runnable body) {
    String savedAccessKey = System.getProperty(ACCESS_KEY);
    String savedSecretKey = System.getProperty(SECRET_KEY);
    try {
      set(ACCESS_KEY, accessKey);
      set(SECRET_KEY, secretKey);
      body.run();
    } finally {
      set(ACCESS_KEY, savedAccessKey);
      set(SECRET_KEY, savedSecretKey);
    }
  }

  private void set(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }

  /**
   * An exported but empty MINIO_ACCESS_KEY must be reported as a ProviderException. Letting the
   * empty value through to Credentials raises IllegalArgumentException instead, which {@link
   * ChainedProvider} does not catch.
   */
  @Test
  public void testEmptyAccessKeyThrowsProviderException() {
    withProperties(
        "",
        "my-secretkey",
        () -> {
          try {
            new MinioEnvironmentProvider().fetch();
            Assert.fail("empty MINIO_ACCESS_KEY must be rejected");
          } catch (ProviderException e) {
            // expected
          }
        });
  }

  @Test
  public void testEmptySecretKeyThrowsProviderException() {
    withProperties(
        "my-accesskey",
        "",
        () -> {
          try {
            new MinioEnvironmentProvider().fetch();
            Assert.fail("empty MINIO_SECRET_KEY must be rejected");
          } catch (ProviderException e) {
            // expected
          }
        });
  }

  /** The failure must be recoverable: a chain has to fall through to the next provider. */
  @Test
  public void testEmptyAccessKeyFallsThroughChain() {
    withProperties(
        "",
        "my-secretkey",
        () -> {
          Credentials credentials =
              new ChainedProvider(
                      new MinioEnvironmentProvider(),
                      new StaticProvider("fallback-accesskey", "fallback-secretkey", null))
                  .fetch();
          Assert.assertEquals("fallback-accesskey", credentials.accessKey());
          Assert.assertEquals("fallback-secretkey", credentials.secretKey());
        });
  }

  /** Both keys present must still work. */
  @Test
  public void testValidKeys() {
    withProperties(
        "my-accesskey",
        "my-secretkey",
        () -> {
          Credentials credentials = new MinioEnvironmentProvider().fetch();
          Assert.assertEquals("my-accesskey", credentials.accessKey());
          Assert.assertEquals("my-secretkey", credentials.secretKey());
        });
  }
}
