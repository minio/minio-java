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

import io.minio.CloseableIterator;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Event;
import io.minio.messages.NotificationRecords;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ListenBucketNotification {
  /** MinioClient.listenBucketNotification() example. */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException {
    try {
      /* play.min.io for test and development. */
      MinioClient minioClient =
          new MinioClient(
              "https://play.min.io",
              "Q3AM3UQ867SPQQA43P2F",
              "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
      try (CloseableIterator<Result<NotificationRecords>> ci =
          minioClient.listenBucketNotification("bcketName", "", "", events)) {
        while (ci.hasNext()) {
          NotificationRecords records = ci.next().get();
          Event event = records.events().get(0);
          System.out.println(event.bucketName() + "/" + event.objectName() + " has been created");
        }
      } catch (IOException e) {
        System.out.println("Error occurred: " + e);
      }
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
