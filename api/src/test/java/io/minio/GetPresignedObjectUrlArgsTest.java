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

import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class GetPresignedObjectUrlArgsTest {
  private static final int MAX_EXPIRY = (int) TimeUnit.DAYS.toSeconds(7);

  /**
   * A duration whose seconds exceed Integer.MAX_VALUE must be rejected. Narrowing to int before
   * validating let 49711 days wrap to 63104 seconds and pass the range check, so the caller
   * silently got a ~17 hour URL instead of the ~136 years it asked for.
   */
  @Test
  public void testExpiryOverflowIsRejected() {
    // 49711 days is 4295030400 seconds, which truncates to 63104 when narrowed to int.
    Assert.assertTrue(
        "test premise: the narrowed value must land inside the valid range",
        (int) TimeUnit.DAYS.toSeconds(49711) >= 1
            && (int) TimeUnit.DAYS.toSeconds(49711) <= MAX_EXPIRY);

    for (int days : new int[] {49711, 100000, Integer.MAX_VALUE}) {
      try {
        GetPresignedObjectUrlArgs.builder()
            .bucket("my-bucketname")
            .object("my-objectname")
            .method(Http.Method.GET)
            .expiry(days, TimeUnit.DAYS);
        Assert.fail("expiry of " + days + " days must be rejected");
      } catch (IllegalArgumentException e) {
        // expected
      }
    }
  }
}
