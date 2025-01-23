/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Item;

public class ListObjects {
  /** MinioClient.listObjects() example. */
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
      // Lists objects information.
      Iterable<Result<Item>> results =
          minioClient.listObjects(ListObjectsArgs.builder().bucket("my-bucket").build());

      for (Result<Item> result : results) {
        Item item = result.get();
        System.out.println(item.lastModified() + "\t" + item.size() + "\t" + item.objectName());
      }
    }

    {
      // Lists objects information recursively.
      Iterable<Result<Item>> results =
          minioClient.listObjects(
              ListObjectsArgs.builder().bucket("my-bucket").recursive(true).build());

      for (Result<Item> result : results) {
        Item item = result.get();
        System.out.println(item.lastModified() + "\t" + item.size() + "\t" + item.objectName());
      }
    }

    {
      // Lists maximum 100 objects information those names starts with 'E' and after
      // 'ExampleGuide.pdf'.
      Iterable<Result<Item>> results =
          minioClient.listObjects(
              ListObjectsArgs.builder()
                  .bucket("my-bucket")
                  .startAfter("ExampleGuide.pdf")
                  .prefix("E")
                  .maxKeys(100)
                  .build());

      for (Result<Item> result : results) {
        Item item = result.get();
        System.out.println(item.lastModified() + "\t" + item.size() + "\t" + item.objectName());
      }
    }

    {
      // Lists maximum 100 objects information with version those names starts with 'E' and after
      // 'ExampleGuide.pdf'.
      Iterable<Result<Item>> results =
          minioClient.listObjects(
              ListObjectsArgs.builder()
                  .bucket("my-bucket")
                  .startAfter("ExampleGuide.pdf")
                  .prefix("E")
                  .maxKeys(100)
                  .includeVersions(true)
                  .build());

      for (Result<Item> result : results) {
        Item item = result.get();
        System.out.println(
            item.lastModified()
                + "\t"
                + item.size()
                + "\t"
                + item.objectName()
                + " ["
                + item.versionId()
                + "]");
      }
    }
  }
}
