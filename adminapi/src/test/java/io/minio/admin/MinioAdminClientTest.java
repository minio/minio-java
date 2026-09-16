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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.errors.MinioException;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Assert;
import org.junit.Test;

public class MinioAdminClientTest {
  private interface QuotaCall {
    void run(MinioAdminClient client) throws MinioException;
  }

  private static MinioAdminClient client(MockWebServer server) {
    return MinioAdminClient.builder()
        .endpoint(server.url(""))
        .credentials("minio", "minio123")
        .build();
  }

  private static long getBucketQuota(String responseBody)
      throws IOException, InterruptedException, MinioException {
    MockWebServer server = new MockWebServer();
    try {
      server.enqueue(new MockResponse().setResponseCode(200).setBody(responseBody));
      server.start();
      long quota = client(server).getBucketQuota("my-bucket");
      Assert.assertEquals(
          "/minio/admin/v3/get-bucket-quota?bucket=my-bucket", server.takeRequest().getPath());
      return quota;
    } finally {
      server.shutdown();
    }
  }

  private static JsonNode sentQuota(QuotaCall call)
      throws IOException, InterruptedException, MinioException {
    MockWebServer server = new MockWebServer();
    try {
      server.enqueue(new MockResponse().setResponseCode(200));
      server.start();
      call.run(client(server));
      RecordedRequest request = server.takeRequest();
      Assert.assertEquals("PUT", request.getMethod());
      Assert.assertEquals("/minio/admin/v3/set-bucket-quota?bucket=my-bucket", request.getPath());
      return new ObjectMapper().readTree(request.getBody().readUtf8());
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
  public void testGetBucketQuotaFractional()
      throws IOException, InterruptedException, MinioException {
    getBucketQuota("{\"size\":1.5,\"quotatype\":\"hard\"}");
  }

  @Test(expected = MinioException.class)
  public void testGetBucketQuotaOutOfRange()
      throws IOException, InterruptedException, MinioException {
    getBucketQuota("{\"size\":18446744073709551616,\"quotatype\":\"hard\"}");
  }

  @Test
  public void testSetBucketQuotaSendsSize()
      throws IOException, InterruptedException, MinioException {
    JsonNode sent = sentQuota(client -> client.setBucketQuota("my-bucket", 1, QuotaUnit.MB));
    Assert.assertEquals(1048576L, sent.get("size").longValue());
    Assert.assertEquals("hard", sent.get("quotatype").textValue());
    Assert.assertFalse(sent.has("quota"));
  }

  @Test
  public void testClearBucketQuotaSendsZeroSize()
      throws IOException, InterruptedException, MinioException {
    JsonNode sent = sentQuota(client -> client.clearBucketQuota("my-bucket"));
    Assert.assertEquals(0L, sent.get("size").longValue());
    Assert.assertFalse(sent.has("quotatype"));
    Assert.assertFalse(sent.has("quota"));
  }
}
