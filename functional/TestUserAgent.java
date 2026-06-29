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

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TestUserAgent {
  public static void main(String[] args) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // try-with-resources closes the client so its OkHttp dispatcher/connection-pool threads shut
    // down and the JVM exits promptly instead of waiting out the idle keep-alive.
    try (MinioClient client = MinioClient.builder().endpoint("http://httpbin.org").build()) {
      client.setRetry(null, null, null);
      client.traceOn(baos);
      try {
        client.bucketExists(BucketExistsArgs.builder().bucket("any-bucket-name-works").build());
      } catch (MinioException e) {
        // ignore
      }
      client.traceOff();
    }

    String expectedVersion = System.getProperty("version");
    if (expectedVersion == null || expectedVersion.isEmpty()) {
      throw new Exception("system property 'version' must be set");
    }
    String version = null;
    try (Scanner scanner = new Scanner(new String(baos.toByteArray(), StandardCharsets.UTF_8))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.startsWith("User-Agent:")) {
          String[] tokens = line.split("/");
          if (tokens.length > 1) version = tokens[1];
          break;
        }
      }
    }

    if (!expectedVersion.equals(version)) {
      throw new Exception(
          "version does not match; expected=" + expectedVersion + ", got=" + version);
    }
  }
}
