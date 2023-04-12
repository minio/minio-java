/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
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
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.HttpUtils;
import okhttp3.OkHttpClient;

public class MinioClientWithTLS {
    public static void main(String[] args) throws Exception {
        // build OkHttpClient by jks cert
        OkHttpClient client = HttpUtils.newHttpClientByJKSCert("truststore.jks","keystore.jks","1234","1234");
        // build OkHttpClient by p12 cert
        // OkHttpClient client = HttpUtils.newHttpClientByJKSCert("truststore.jks","keystore.p12","1234","1234");
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint("https://MINIO-HOST:MINIO-PORT")
                        .credentials("YOUR-ACCESSKEY", "YOUR-SECRETACCESSKEY").httpClient(client)
                        .build();
        // minioClient.ignoreCertCheck();
        // Get information of an object.
        StatObjectResponse stat =
                minioClient.statObject(
                        StatObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
        System.out.println(stat);
    }

}