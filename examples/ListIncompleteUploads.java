/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Upload;

public class ListIncompleteUploads {
  /**
   * MinioClient.listIncompleteUploads() example.
   */
  public static void main(String[] args)
    throws IOException, NoSuchAlgorithmException, InvalidKeyException, XmlPullParserException {
    try {
      /* play.minio.io for test and development. */
      MinioClient minioClient = new MinioClient("http://play.minio.io:9000", "Q3AM3UQ867SPQQA43P2F",
                                                "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      /* Amazon S3: */
      // MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
      //                                           "YOUR-SECRETACCESSKEY");

      // Check whether 'my-bucketname' exist or not.
      boolean found = minioClient.bucketExists("my-bucketname");
      if (found) {
        // List all incomplete multipart upload of objects in 'my-bucketname'
        Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("my-bucketname");
        for (Result<Upload> result : myObjects) {
          Upload upload = result.get();
          System.out.println(upload.uploadId() + ", " + upload.objectName());
        }
      } else {
        System.out.println("my-bucketname does not exist");
      }
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
