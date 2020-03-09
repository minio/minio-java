/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2020 MinIO, Inc.
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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Time formatters for S3 APIs. */
public class Time {
  public static final ZoneId UTC = ZoneId.of("Z");

  public static final DateTimeFormatter AMZ_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US).withZone(UTC);

  public static final DateTimeFormatter RESPONSE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':'mm':'ss'.'SSS'Z'", Locale.US).withZone(UTC);

  // Formatted string is convertible to LocalDate only, not to LocalDateTime or ZonedDateTime.
  // Below example shows how to use this to get ZonedDateTime.
  // LocalDate.parse("20200225", SIGNER_DATE_FORMAT).atStartOfDay(UTC);
  public static final DateTimeFormatter SIGNER_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US).withZone(UTC);

  public static final DateTimeFormatter HTTP_HEADER_DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEE',' dd MMM yyyy HH':'mm':'ss 'GMT'", Locale.US).withZone(UTC);

  public static final DateTimeFormatter EXPIRATION_DATE_FORMAT = RESPONSE_DATE_FORMAT;

  private Time() {}
}
