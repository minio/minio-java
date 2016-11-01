
/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2016 Minio, Inc.
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
import java.util.Date;
import java.security.InvalidKeyException;

import org.xmlpull.v1.XmlPullParserException;

import io.minio.CopyConditions;
import io.minio.MinioClient;
import io.minio.errors.MinioException;

public class CopyObject {
  /**
   * main().
   */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, XmlPullParserException {
    // Note: YOUR-ACCESSKEYID, YOUR-SECRETACCESSKEY and my-bucketname are
    // dummy values, please replace them with original values.
    // For Amazon S3 endpoint, region is calculated automatically
    try {
      MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

      // Create the CopyConditions Object. Then Setup the parameters for copyConditions
      CopyConditions copyConditions = new CopyConditions();

      // Set the ETag to match condition. If set, object will be copied only if its ETag matches the value set here.
      copyConditions.setMatchETag("ETag");

      // Set the ETag to avoid condition. If set, object will be copied only if its ETag does not match the value set
      // here.
      copyConditions.setMatchETagExcept("ETagToNotMatch");

      // Set the Date modified after condition. If set, object will be copied only if it is strictly modified after the
      // date set here.
      copyConditions.setModified(new Date());

      // Set the Date unmodified since condition. If set, object will be copied only if it is strictly unmodified since
      // the date set here.
      copyConditions.setUnmodified(new Date());

      // Copy Object. sourcePath is in the form - /sourceBucketName/sourceObjectName
      minioClient.copyObject("destinationBucketName", "destinationObjectName", "sourcePath",
          copyConditions);

    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
