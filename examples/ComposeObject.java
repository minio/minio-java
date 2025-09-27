/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2019 Minio, Inc.
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

import io.minio.ComposeObjectArgs;
import io.minio.MinioClient;
import io.minio.ServerSideEncryption;
import io.minio.SourceObject;
import io.minio.errors.MinioException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;

public class ComposeObject {
  /** MinioClient.composeObject() example. */
  public static void main(String[] args) throws MinioException {
    /* play.min.io for test and development. */
    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://play.min.io")
            .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
            .build();

    /* Amazon S3: */
    // MinioClient minioClient =
    //     MinioClient.builder()
    //         .endpoint("https://s3.amazonaws.com")
    //         .credentials("YOUR-ACCESSKEY", "YOUR-SECRETACCESSKEY")
    //         .build();

    {
      // Create a SourceObject to compose Object.
      List<SourceObject> sources = new ArrayList<SourceObject>();
      sources.add(SourceObject.builder().bucket("my-bucket-one").object("my-object-one").build());
      sources.add(SourceObject.builder().bucket("my-bucket-two").object("my-object-two").build());

      minioClient.composeObject(
          ComposeObjectArgs.builder()
              .bucket("my-destination-bucket")
              .object("my-destination-object")
              .sources(sources)
              .build());
      System.out.println("Object Composed successfully");
    }

    {
      ServerSideEncryption.CustomerKey srcSsec =
          new ServerSideEncryption.CustomerKey(
              new SecretKeySpec(
                  "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8), "AES"));

      ServerSideEncryption sse =
          new ServerSideEncryption.CustomerKey(
              new SecretKeySpec(
                  "12345678912345678912345678912345".getBytes(StandardCharsets.UTF_8), "AES"));

      List<SourceObject> sources = new ArrayList<SourceObject>();
      sources.add(
          SourceObject.builder().bucket("my-bucket").object("my-object-one").ssec(srcSsec).build());
      sources.add(
          SourceObject.builder().bucket("my-bucket").object("my-object-two").ssec(srcSsec).build());

      minioClient.composeObject(
          ComposeObjectArgs.builder()
              .bucket("my-destination-bucket")
              .object("my-destination-object")
              .sources(sources)
              .sse(sse)
              .build());
      System.out.println("Object Composed successfully");
    }
  }
}
