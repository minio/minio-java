/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
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

import com.google.common.io.ByteStreams;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import me.tongfei.progressbar.ProgressBarStyle;

public class GetObjectProgressBar {
  /** MinioClient.getObjectProgressBar() example. */
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
      // "YOUR-SECRETACCESSKEY");

      // Check whether the object exists using statObject(). If the object is not found,
      // statObject() throws an exception. It means that the object exists when statObject()
      // execution is successful.

      // Get object stat information.
      ObjectStat objectStat = minioClient.statObject("testbucket", "resumes/4.original.pdf");

      // Get input stream to have content of 'my-objectname' from 'my-bucketname'
      InputStream is =
          new ProgressStream(
              "Downloading .. ",
              ProgressBarStyle.ASCII,
              objectStat.length(),
              minioClient.getObject("my-bucketname", "my-objectname"));

      Path path = Paths.get("my-filename");
      OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE);

      long bytesWritten = ByteStreams.copy(is, os);
      is.close();
      os.close();

      if (bytesWritten != objectStat.length()) {
        throw new IOException(
            path
                + ": unexpected data written.  expected = "
                + objectStat.length()
                + ", written = "
                + bytesWritten);
      }

    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
