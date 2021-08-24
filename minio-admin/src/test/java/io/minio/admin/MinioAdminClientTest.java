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
