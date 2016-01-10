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

import io.minio.MinioClient;
import io.minio.errors.MinioException;

import java.io.InputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import org.xmlpull.v1.XmlPullParserException;


public class GetObject {
  public static void main(String[] args)
    throws NoSuchAlgorithmException, IOException, InvalidKeyException, XmlPullParserException, MinioException {
    // Note: YOUR-ACCESSKEYID, YOUR-SECRETACCESSKEY and my-bucketname are
    // dummy values, please replace them with original values.
    // Set s3 endpoint, region is calculated automatically
    MinioClient s3Client = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");
    InputStream stream = s3Client.getObject("my-bucketname", "my-objectname");

    byte[] buf = new byte[16384];
    int bytesRead;
    while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
      System.out.println(new String(buf, 0, bytesRead));
    }

    stream.close();
  }
}
