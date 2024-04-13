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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TestUserAgent {
  public static void main(String[] args) throws Exception {
    MinioClient client = MinioClient.builder().endpoint("http://httpbin.org").build();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    client.traceOn(baos);
    client.bucketExists(BucketExistsArgs.builder().bucket("any-bucket-name-works").build());
    client.traceOff();

    String expectedVersion = System.getProperty("version");
    String version = null;
    try (Scanner scanner = new Scanner(new String(baos.toByteArray(), StandardCharsets.UTF_8))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.startsWith("User-Agent:")) {
          version = line.split("/")[1];
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
