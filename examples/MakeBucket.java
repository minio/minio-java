/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;

public class MakeBucket {
  /** MinioClient.makeBucket() example. */
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

    // Create bucket 'my-bucket' if it does not exist.
    if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket("my-bucket").build())) {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket("my-bucket").build());
      System.out.println("my-bucket is created successfully");
    }

    // Create bucket 'my-bucket-in-eu' in 'eu-west-1' region if it does not exist.
    if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket("my-bucket-in-eu").build())) {
      minioClient.makeBucket(
          MakeBucketArgs.builder().bucket("my-bucket-in-eu").region("eu-west-1").build());
      System.out.println("my-bucket-in-eu is created successfully");
    }

    // Create bucket 'my-bucket-in-eu-with-object-lock' in 'eu-west-1' with object lock
    // functionality enabled.
    if (!minioClient.bucketExists(
        BucketExistsArgs.builder().bucket("my-bucket-in-eu-with-object-lock").build())) {
      minioClient.makeBucket(
          MakeBucketArgs.builder()
              .bucket("my-bucket-in-eu-with-object-lock")
              .region("eu-west-1")
              .objectLock(true)
              .build());
      System.out.println("my-bucket-in-eu-with-object-lock is created successfully");
    }
  }
}
