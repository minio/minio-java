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

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.SelectResponseStream;
import io.minio.errors.MinioException;
import io.minio.messages.FileHeaderInfo;
import io.minio.messages.InputSerialization;
import io.minio.messages.OutputSerialization;
import io.minio.messages.QuoteFields;
import io.minio.messages.Stats;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SelectObjectContent {
  /** MinioClient.getObject() example. */
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

      byte[] data =
          ("Year,Make,Model,Description,Price\n"
                  + "1997,Ford,E350,\"ac, abs, moon\",3000.00\n"
                  + "1999,Chevy,\"Venture \"\"Extended Edition\"\"\",\"\",4900.00\n"
                  + "1999,Chevy,\"Venture \"\"Extended Edition, Very Large\"\"\",,5000.00\n"
                  + "1996,Jeep,Grand Cherokee,\"MUST SELL!\n"
                  + "air, moon roof, loaded\",4799.00\n")
              .getBytes(StandardCharsets.UTF_8);
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      minioClient.putObject(
          "my-bucketname", "my-objectname", bais, new PutObjectOptions(data.length, -1));

      String sqlExpression = "select * from S3Object";
      InputSerialization is =
          new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null);
      OutputSerialization os =
          new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);

      SelectResponseStream stream =
          minioClient.selectObjectContent(
              "my-bucketname", "my-objectName", sqlExpression, is, os, true, null, null, null);

      byte[] buf = new byte[512];
      int bytesRead = stream.read(buf, 0, buf.length);
      System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
      Stats stats = stream.stats();
      System.out.println("bytes scanned: " + stats.bytesScanned());
      System.out.println("bytes processed: " + stats.bytesProcessed());
      System.out.println("bytes returned: " + stats.bytesReturned());
      stream.close();
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
