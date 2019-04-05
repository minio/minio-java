/*
 *MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015MinIO, Inc.
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import org.xmlpull.v1.XmlPullParserException;

import io.minio.MinioClient;
import io.minio.BucketEventListener;
import io.minio.errors.MinioException;
import io.minio.notification.NotificationInfo;

public class ListenBucketNotification {
  /**
   * MinioClient.listenBucketNotification() example.
   */
  public static void main(String[] args)
    throws IOException, NoSuchAlgorithmException, InvalidKeyException, XmlPullParserException {
    try {
      /* play.minio.io for test and development. */
      MinioClient minioClient = new MinioClient("https://play.min.io:9000", "Q3AM3UQ867SPQQA43P2F",
                                                "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      /* Amazon S3: */
      // MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
      //                                           "YOUR-SECRETACCESSKEY");

      class TestBucketListener implements BucketEventListener {
        @Override
        public void updateEvent(NotificationInfo info) {
          System.out.println(info.records[0].s3.bucket.name + "/"
              + info.records[0].s3.object.key + " has been created");
        }
      }

      minioClient.listenBucketNotification("my-bucketname", "", "",
          new String[]{"s3:ObjectCreated:*"}, new TestBucketListener());

    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
