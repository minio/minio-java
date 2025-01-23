/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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
import io.minio.SetBucketCorsArgs;
import io.minio.errors.MinioException;
import io.minio.messages.CORSConfiguration;
import java.util.Arrays;

public class SetBucketCors {
  /** MinioClient.setBucketCors() example. */
  public static void main(String[] args) throws MinioException {
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

    CORSConfiguration config =
        new CORSConfiguration(
            Arrays.asList(
                new CORSConfiguration.CORSRule[] {
                  // Rule 1
                  new CORSConfiguration.CORSRule(
                      Arrays.asList(new String[] {"*"}), // Allowed headers
                      Arrays.asList(new String[] {"PUT", "POST", "DELETE"}), // Allowed methods
                      Arrays.asList(new String[] {"http://www.example.com"}), // Allowed origins
                      Arrays.asList(
                          new String[] {"x-amz-server-side-encryption"}), // Expose headers
                      null, // ID
                      3000), // Maximum age seconds
                  // Rule 2
                  new CORSConfiguration.CORSRule(
                      null, // Allowed headers
                      Arrays.asList(new String[] {"GET"}), // Allowed methods
                      Arrays.asList(new String[] {"*"}), // Allowed origins
                      null, // Expose headers
                      null, // ID
                      null // Maximum age seconds
                      )
                }));

    minioClient.setBucketCors(
        SetBucketCorsArgs.builder().bucket("my-bucket").config(config).build());
  }
}
