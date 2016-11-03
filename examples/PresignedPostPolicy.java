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
import java.util.Map;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import org.xmlpull.v1.XmlPullParserException;
import org.joda.time.DateTime;

import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.errors.MinioException;

public class PresignedPostPolicy {
  /**
   * MinioClient.presignedPostPolicy() example.
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

      // Create new PostPolicy object for 'my-bucketname', 'my-objectname' and 7 days expire time from now.
      PostPolicy policy = new PostPolicy("my-bucketname", "my-objectname", DateTime.now().plusDays(7));
      // 'my-objectname' should be 'image/png' content type
      policy.setContentType("image/png");
      Map<String,String> formData = minioClient.presignedPostPolicy(policy);

      // Print a curl command that can be executable with the file /tmp/userpic.png and the file will be uploaded.
      System.out.print("curl -X POST ");
      for (Map.Entry<String,String> entry : formData.entrySet()) {
        System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
      }
      System.out.println(" -F file=@/tmp/userpic.png https://play.minio.io:9000/my-bucketname");
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
