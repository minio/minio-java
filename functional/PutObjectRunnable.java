/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015,2016 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.file.*;

import io.minio.*;


class PutObjectRunnable implements Runnable {
  MinioClient client;
  String bucketName;
  String fileName;

  public PutObjectRunnable(MinioClient client, String bucketName, String fileName) {
    this.client = client;
    this.bucketName = bucketName;
    this.fileName = fileName;
  }

  public void run() {
    StringBuffer traceBuffer = new StringBuffer();

    try {
      traceBuffer.append("[" + fileName + "]: threaded put object\n");
      client.putObject(bucketName, fileName, fileName);
      traceBuffer.append("[" + fileName + "]: delete file\n");
      Files.delete(Paths.get(fileName));
      traceBuffer.append("[" + fileName + "]: delete object\n");
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      System.err.print(traceBuffer.toString());
      e.printStackTrace();
    }
  }
}
