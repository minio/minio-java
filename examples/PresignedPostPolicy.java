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

import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Map;

public class PresignedPostPolicy {
  /** MinioClient.presignedPostPolicy() example. */
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

      // Create new PostPolicy object for 'my-bucketname', 'my-objectname' and 7 days expire time
      // from now.
      PostPolicy policy =
          new PostPolicy("my-bucketname", "my-objectname", ZonedDateTime.now().plusDays(7));
      // 'my-objectname' should be 'image/png' content type
      policy.setContentType("image/png");
      // set success action status to 201 because we want the client to notify us with the S3 key
      // where the file was uploaded to.
      policy.setSuccessActionStatus(201);

      Map<String, String> formData = minioClient.presignedPostPolicy(policy);

      // Print a curl command that can be executable with the file /tmp/userpic.png and the file
      // will be uploaded.
      System.out.print("curl -X POST ");
      for (Map.Entry<String, String> entry : formData.entrySet()) {
        System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
      }
      System.out.println(" -F file=@/tmp/userpic.png https://play.min.io/my-bucketname");
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
