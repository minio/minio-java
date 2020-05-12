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

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MakeBucket {
  /** MinioClient.makeBucket() example. */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException {
    try {
      /* play.min.io for test and development. */
      MinioClient minioClient =
          new MinioClient(
              "https://play.min.io",
              "Q3AM3UQ867SPQQA43P2F",
              "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      /* Amazon S3: */
      // MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
      //                                           "YOUR-SECRETACCESSKEY");

      // Create bucket if it doesn't exist.
      if (minioClient.bucketExists("my-bucketname")) {
        System.out.println("my-bucketname already exists");
      } else {
        // Create bucket 'my-bucketname'.
        minioClient.makeBucket(MakeBucketArgs.builder().bucket("my-bucketname").build());
        System.out.println("my-bucketname is created successfully");
      }
      // Create bucket if it doesn't exist.
      if (minioClient.bucketExists("my-bucketname2")) {
        System.out.println("my-bucketname2 already exists");
      } else {
        // Create bucket 'my-bucketname2' and region.
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket("my-bucketname2").region("us-west-1").build());
        System.out.println("my-bucketname2 is created successfully");
      }
      // Create bucket if it doesn't exist.
      if (minioClient.bucketExists("my-bucketname3")) {
        System.out.println("my-bucketname3 already exists");
      } else {

        // Create bucket 'my-bucketname3' , egion and object lock functionality enabled.
        minioClient.makeBucket(
            MakeBucketArgs.builder()
                .bucket("my-bucketname3")
                .region("us-west-1")
                .objectLock(true)
                .build());
        System.out.println("my-bucketname3 is created successfully");
      }
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
