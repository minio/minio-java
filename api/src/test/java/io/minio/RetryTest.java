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

package io.minio;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.Assert;
import org.junit.Test;

/** Integration tests for {@link Http.RetryInterceptor}. */
public class RetryTest {
  private static final String LIST_BUCKETS_OK =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<ListAllMyBucketsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
          + "<Owner><ID>test</ID><DisplayName>test</DisplayName></Owner>"
          + "<Buckets/></ListAllMyBucketsResult>";

  private MockResponse successResponse() {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/xml")
        .setBody(new Buffer().writeUtf8(LIST_BUCKETS_OK));
  }

  private MockResponse retryableServerError() {
    return new MockResponse()
        .setResponseCode(503)
        .setHeader("Content-Type", "text/html")
        .setBody(new Buffer().writeUtf8("<html>Service Unavailable</html>"));
  }

  @Test
  public void testRetryOnRetryableStatusThenSuccess() throws IOException, MinioException {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(retryableServerError());
      server.enqueue(successResponse());
      server.start();

      MinioClient client = MinioClient.builder().endpoint(server.url("").toString()).build();
      client.listBuckets();

      Assert.assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  public void testNoRetryOn4xx() throws IOException, MinioException {
    String notFoundXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Error><Code>NoSuchBucket</Code><Message>not found</Message>"
            + "<Resource>/</Resource><RequestId>abc</RequestId></Error>";

    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(404)
              .setHeader("Content-Type", "application/xml")
              .setBody(new Buffer().writeUtf8(notFoundXml)));
      server.start();

      MinioClient client = MinioClient.builder().endpoint(server.url("").toString()).build();
      try {
        client.listBuckets();
        Assert.fail("expected ErrorResponseException");
      } catch (ErrorResponseException e) {
        Assert.assertEquals("NoSuchBucket", e.errorResponse().code());
      }
      Assert.assertEquals(1, server.getRequestCount());
    }
  }
}
