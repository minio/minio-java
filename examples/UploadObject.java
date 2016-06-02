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

import java.lang.StringBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import org.xmlpull.v1.XmlPullParserException;

import io.minio.MinioClient;
import io.minio.errors.MinioException;

public class UploadObject {
  public static void main(String[] args)
    throws IOException, NoSuchAlgorithmException, InvalidKeyException, XmlPullParserException {
    // Note: YOUR-ACCESSKEYID, YOUR-SECRETACCESSKEY and my-bucketname are
    // dummy values, please replace them with original values.
    // For Amazon S3 endpoint, region is calculated automatically
    try {
      MinioClient minioClient = new MinioClient("https://play.minio.io:9000", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

      // Upload 'my-filename' as object 'my-objectname' in 'my-bucketname'.
      minioClient.putObject("my-bucketname", "my-objectname", "my-filename");
      System.out.println("my-filename is uploaded to my-objectname successfully");
    } catch (MinioException e) {
      System.out.println("Error occured: " + e);
    }
  }
}
