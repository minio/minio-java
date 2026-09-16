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

package io.minio.admin;

import io.minio.errors.MinioException;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Assert;
import org.junit.Test;

public class MinioAdminClientTest {
  private static long getBucketQuota(String responseBody)
      throws IOException, InterruptedException, MinioException {
    MockWebServer server = new MockWebServer();
    try {
      server.enqueue(new MockResponse().setResponseCode(200).setBody(responseBody));
      server.start();
      MinioAdminClient client =
          MinioAdminClient.builder()
              .endpoint(server.url(""))
              .credentials("minio", "minio123")
              .build();
      long quota = client.getBucketQuota("my-bucket");
      Assert.assertEquals(
          "/minio/admin/v3/get-bucket-quota?bucket=my-bucket", server.takeRequest().getPath());
      return quota;
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void testGetBucketQuotaSize() throws IOException, InterruptedException, MinioException {
    Assert.assertEquals(
        1048576L,
        getBucketQuota("{\"size\":1048576,\"rate\":0,\"requests\":0,\"quotatype\":\"hard\"}"));
  }

  @Test
  public void testGetBucketQuotaLegacyQuotaWithZeroSize()
      throws IOException, InterruptedException, MinioException {
    Assert.assertEquals(
        2048L,
        getBucketQuota(
            "{\"quota\":2048,\"size\":0,\"rate\":0,\"requests\":0,\"quotatype\":\"hard\"}"));
  }

  @Test
  public void testGetBucketQuotaLegacyQuotaOnly()
      throws IOException, InterruptedException, MinioException {
    Assert.assertEquals(2048L, getBucketQuota("{\"quota\":2048,\"quotatype\":\"hard\"}"));
  }

  @Test
  public void testGetBucketQuotaPrefersNonZeroSize()
      throws IOException, InterruptedException, MinioException {
    Assert.assertEquals(
        4096L, getBucketQuota("{\"quota\":2048,\"size\":4096,\"quotatype\":\"hard\"}"));
  }

  @Test
  public void testGetBucketQuotaZeroSize()
      throws IOException, InterruptedException, MinioException {
    Assert.assertEquals(0L, getBucketQuota("{\"size\":0,\"rate\":0,\"requests\":0}"));
  }

  @Test(expected = MinioException.class)
  public void testGetBucketQuotaMissing() throws IOException, InterruptedException, MinioException {
    getBucketQuota("{\"quotatype\":\"hard\"}");
  }

  @Test(expected = MinioException.class)
  public void testGetBucketQuotaNonIntegral()
      throws IOException, InterruptedException, MinioException {
    getBucketQuota("{\"size\":\"1048576\",\"quotatype\":\"hard\"}");
  }

  @Test(expected = MinioException.class)
  public void testGetBucketQuotaNonIntegralSizeWithLegacyQuota()
      throws IOException, InterruptedException, MinioException {
    getBucketQuota("{\"size\":\"1048576\",\"quota\":2048,\"quotatype\":\"hard\"}");
  }
}
