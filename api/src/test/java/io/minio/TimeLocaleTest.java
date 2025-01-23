/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2025 MinIO, Inc.
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

import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Test;

public class TimeLocaleTest {
  @Test
  public void testHttpHeaderDateFormat() {
    ZonedDateTime time = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]");
    Assert.assertEquals(time.format(Time.HTTP_HEADER_DATE_FORMAT), "Mon, 03 Dec 2007 09:15:30 GMT");
    ZonedDateTime parsedTime =
        ZonedDateTime.parse("Mon, 03 Dec 2007 09:15:30 GMT", Time.HTTP_HEADER_DATE_FORMAT);
    Assert.assertEquals(time.format(Time.AMZ_DATE_FORMAT), parsedTime.format(Time.AMZ_DATE_FORMAT));
  }
}
