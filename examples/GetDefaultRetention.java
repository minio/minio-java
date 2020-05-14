/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2019 MinIO, Inc.
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

import io.minio.GetDefaultRetentionArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import io.minio.messages.ObjectLockConfiguration;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class GetDefaultRetention {
  /** MinioClient.getDefaultRetention() example. */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException {
    try {
      /* Amazon S3: */
      MinioClient s3Client =
          new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

      ObjectLockConfiguration config =
          s3Client.getDefaultRetention(
              GetDefaultRetentionArgs.builder().bucket("my-lock-enabled-bucketname").build());

      System.out.println("Default retention configuration of bucket");
      System.out.println("Mode: " + config.mode());
      System.out.println("Duration: " + config.duration());
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
