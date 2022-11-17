/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2022 MinIO, Inc.
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

package io.minio.admin.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.Assert;
import org.junit.Test;

public class DataUsageInfoTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  @Test
  public void deserializeTest() throws IOException {

    DataUsageInfo info =
        OBJECT_MAPPER.readValue(
            new File(
                getClass().getClassLoader().getResource("messages/datausageinfo.json").getFile()),
            DataUsageInfo.class);
    Assert.assertNotNull(info.lastUpdate());
    Assert.assertEquals(
        15, LocalDateTime.ofInstant(info.lastUpdate().toInstant(), ZoneOffset.UTC).getHour());

    Assert.assertEquals(1, info.bucketsCount());
    Assert.assertTrue(info.bucketsUsageInfo().containsKey("tier-bucket"));
    Assert.assertTrue(info.tierStats().tiers().containsKey("STANDARD"));

    Assert.assertEquals(7155L, (long) info.bucketsSizes().get("tier-bucket"));
  }
}
