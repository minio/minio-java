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
import io.minio.ComposeSourceArgs;
import io.minio.MinioClient;
import io.minio.ServerSideEncryption;
import io.minio.ServerSideEncryptionCustomerKey;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;

public class ComposeObjectEncrypted {
  /** MinioClient.composeObject() example. */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException {
    try {
      /* play.minio.io for test and development. */
      MinioClient minioClient =
          new MinioClient(
              "https://play.min.io:9000",
              "Q3AM3UQ867SPQQA43P2F",
              "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      byte[] key = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

      ServerSideEncryptionCustomerKey ssePut = ServerSideEncryption.withCustomerKey(secretKeySpec);

      byte[] keyTarget = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpecTarget = new SecretKeySpec(keyTarget, "AES");

      ServerSideEncryption sseTarget = ServerSideEncryption.withCustomerKey(secretKeySpecTarget);

      String sourceObject1 = "my-objectname1";
      String sourceObject2 = "my-objectname2";
      String destObject = "my-destination-object";
      String bucketName = "my-bucketname";
      String inputfile1 = "my-inputfile";
      String inputfile2 = "my-inputfile";
      long inputfile1Size = 100000L;
      long inputfile2Size = 200000L;

      minioClient.uploadObject(
          UploadObjectArgs.builder()
              .bucket(bucketName)
              .object(sourceObject1)
              .filename(inputfile1)
              .sse(ssePut)
              .build());

      minioClient.uploadObject(
          UploadObjectArgs.builder()
              .bucket(bucketName)
              .object(sourceObject2)
              .filename(inputfile2)
              .sse(ssePut)
              .build());

      ComposeSourceArgs s1 =
          ComposeSourceArgs.builder()
              .srcBucket(bucketName)
              .srcObject(sourceObject1)
              .srcSsec(ssePut)
              .build();
      ComposeSourceArgs s2 =
          ComposeSourceArgs.builder()
              .srcBucket(bucketName)
              .srcObject(sourceObject2)
              .srcSsec(ssePut)
              .build();

      List<ComposeSourceArgs> listSourceObjects = new ArrayList<ComposeSourceArgs>();
      listSourceObjects.add(s1);
      listSourceObjects.add(s2);

      minioClient.composeObject(
          ComposeObjectArgs.builder()
              .bucket(destObject)
              .sources(listSourceObjects)
              .sse(sseTarget)
              .build());
      System.out.println("Object Composed successfully");
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
