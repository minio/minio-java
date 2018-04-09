
/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

import java.io.InputStream;
import java.io.IOException;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import org.xmlpull.v1.XmlPullParserException;

import io.minio.GetOptions;
import io.minio.MinioClient;
import io.minio.ServerSideEncryption;
import io.minio.errors.MinioException;

public class GetObjectEncrypted {
  /**
   * MinioClient.getObject() example.
   */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, XmlPullParserException {
    try {
      /* play.minio.io for test and development. */
      MinioClient minioClient = new MinioClient("http://localhost:9001", "YLH2I0HHUGPF22H2ZH2T",
          "mizuV0YjE68kMY3pigoPV14sjlK7PC+4e2QafY9c");

      /* Amazon S3: */
      // MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
      // "YOUR-SECRETACCESSKEY");

      
      // Specify a 256 bit AES key. 
      GetOptions options = new GetOptions();
      byte[] encodedKey = "My custom 32 byte secret AES key".getBytes("UTF-8"); // Use your own key here
      options.setEncryption(ServerSideEncryption.withCustomerKey(new SecretKeySpec(encodedKey, "AES")));

      // Get input stream to have content of 'my-objectname' from 'my-bucketname'
      InputStream stream = minioClient.getObject("testbucket", "my-objectname", options);

      // Read the input stream and print to the console till EOF.
      byte[] buf = new byte[16384];
      int bytesRead;
      while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
        System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
      }

      // Close the input stream.
      stream.close();
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
