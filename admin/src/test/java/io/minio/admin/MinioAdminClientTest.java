/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015-2021 MinIO, Inc.
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

import com.google.common.collect.ImmutableMultimap;
import java.security.NoSuchAlgorithmException;
import okhttp3.HttpUrl;
import org.junit.Assert;
import org.junit.Test;

public class MinioAdminClientTest {

  @Test
  public void testAdminUrlsAreCorrect() throws NoSuchAlgorithmException {
    MinioAdminClient client =
        MinioAdminClient.builder()
            .endpoint("http://play.min.io:9000")
            .credentials("foo", "bar")
            .region("us-east-1")
            .build();
    HttpUrl url = client.buildAdminUrl("list-canned-policies", null);
    Assert.assertEquals(
        url.toString(), "http://play.min.io:9000/minio/admin/v3/list-canned-policies");
    url = client.buildAdminUrl("add-canned-policy", ImmutableMultimap.of("name", "foo"));
    Assert.assertEquals(
        url.toString(), "http://play.min.io:9000/minio/admin/v3/add-canned-policy?name=foo");
    url = client.buildAdminUrl("remove-canned-policy", ImmutableMultimap.of("name", "foo"));
    Assert.assertEquals(
        url.toString(), "http://play.min.io:9000/minio/admin/v3/remove-canned-policy?name=foo");
    url = client.buildAdminUrl("add-user", ImmutableMultimap.of("accessKey", "foo"));
    Assert.assertEquals(
        url.toString(), "http://play.min.io:9000/minio/admin/v3/add-user?accessKey=foo");
  }
}
