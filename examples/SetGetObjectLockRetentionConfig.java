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
import io.minio.PutObjectOptions;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.RegionConflictException;
import io.minio.messages.Retention;
import io.minio.messages.RetentionMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;

public class SetGetObjectLockRetentionConfig {
  /** MinioClient.setObjectRetention() example. MinioClient.getObjectRetention() example. */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException,
          InsufficientDataException, InternalException, ErrorResponseException,
          InvalidBucketNameException, InvalidPortException, InvalidEndpointException,
          RegionConflictException, IllegalArgumentException {
    try {

      /* play.min.io for test and development. */
      MinioClient minioClient =
          new MinioClient(
              "https://play.min.io",
              "Q3AM3UQ867SPQQA43P2F",
              "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      // Create bucket if it doesn't exist.
      boolean found = minioClient.bucketExists("my-bucketname");
      if (found) {
        System.out.println("my-bucketname already exists");
      } else {
        // Create bucket 'my-bucketname' with object lock functionality enabled
        minioClient.makeBucket("my-bucketname", null, true);
        System.out.println(
            "my-bucketname is created successfully with object lock functionality enabled.");
      }

      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < 100; i++) {
        builder.append(
            "Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
        builder.append("(29 letters)\n");
        builder.append(
            "Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
        builder.append("---\n");
      }

      // Create a InputStream for object upload.
      ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));

      // Create object 'my-objectname' in 'my-bucketname' with content from the input stream.
      minioClient.putObject(
          "my-bucketname", "my-objectname", bais, new PutObjectOptions(bais.available(), -1));
      bais.close();
      System.out.println("my-objectname is uploaded successfully");

      // Declaring config with Retention mode as Compliance and
      // retain until one year to current date.
      ZonedDateTime retentionUntil = ZonedDateTime.now().plusYears(1);
      Retention config = new Retention(RetentionMode.COMPLIANCE, retentionUntil);

      // Set object lock configuration
      minioClient.setObjectRetention("my-bucketname", "my-objectname", "", config, true);

      // Get object lock retention
      Retention retention = minioClient.getObjectRetention("my-bucketname", "my-objectname", "");

      System.out.println("Mode: " + retention.mode());
      System.out.println("Retainuntil Date: " + retention.retainUntilDate());
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
