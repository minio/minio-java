/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015,2016 MinIO, Inc.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.*;
import java.nio.file.*;

@SuppressFBWarnings(
    value = "REC",
    justification = "Allow catching super class Exception since it's tests")
class PutObjectRunnable implements Runnable {
  MinioClient client;
  String bucketName;
  String filename;
  long size;

  public PutObjectRunnable(MinioClient client, String bucketName, String filename, long size) {
    this.client = client;
    this.bucketName = bucketName;
    this.filename = filename;
    this.size = size;
  }

  public void run() {
    StringBuffer traceBuffer = new StringBuffer();

    try {
      traceBuffer.append("[" + filename + "]: threaded put object\n");
      client.putObject(bucketName, filename, filename, new PutObjectOptions(size, -1));
      traceBuffer.append("[" + filename + "]: delete file\n");
      Files.delete(Paths.get(filename));
      traceBuffer.append("[" + filename + "]: delete object\n");
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename).build());
    } catch (Exception e) {
      System.err.print(traceBuffer.toString());
      e.printStackTrace();
    }
  }
}
