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

import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.MinioException;

public class SetBucketPolicy {
  /** MinioClient.setBucketPolicy() example. */
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

    StringBuilder builder = new StringBuilder();
    builder.append("{\n");
    builder.append("    \"Statement\": [\n");
    builder.append("        {\n");
    builder.append("            \"Action\": [\n");
    builder.append("                \"s3:GetBucketLocation\",\n");
    builder.append("                \"s3:ListBucket\"\n");
    builder.append("            ],\n");
    builder.append("            \"Effect\": \"Allow\",\n");
    builder.append("            \"Principal\": \"*\",\n");
    builder.append("            \"Resource\": \"arn:aws:s3:::my-bucket\"\n");
    builder.append("        },\n");
    builder.append("        {\n");
    builder.append("            \"Action\": \"s3:GetObject\",\n");
    builder.append("            \"Effect\": \"Allow\",\n");
    builder.append("            \"Principal\": \"*\",\n");
    builder.append("            \"Resource\": \"arn:aws:s3:::my-bucket/myobject*\"\n");
    builder.append("        }\n");
    builder.append("    ],\n");
    builder.append("    \"Version\": \"2012-10-17\"\n");
    builder.append("}\n");
    minioClient.setBucketPolicy(
        SetBucketPolicyArgs.builder().bucket("my-bucket").config(builder.toString()).build());
  }
}
