/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PutObjectProgressBar {
  /** MinioClient.putObjectProgressBar() example. */
  public static void main(String[] args) throws IOException, MinioException {
    /* play.min.io for test and development. */
    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://play.min.io")
            .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
            .build();

    /* Amazon S3: */
    // MinioClient minioClient =
    //     MinioClient.builder()
    //         .endpoint("https://s3.amazonaws.com")
    //         .credentials("YOUR-ACCESSKEY", "YOUR-SECRETACCESSKEY")
    //         .build();

    String objectName = "my-object";
    String bucketName = "my-bucket";

    InputStream pis =
        new BufferedInputStream(
            new ProgressStream("Uploading... ", Files.newInputStream(Paths.get("my-filename"))));
    minioClient.putObject(
        PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                pis, (long) pis.available(), null)
            .build());
    pis.close();
    System.out.println("my-object is uploaded successfully");
  }
}
