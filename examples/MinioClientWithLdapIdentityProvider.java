/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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
import io.minio.credentials.LdapIdentityProvider;
import io.minio.credentials.Provider;
import io.minio.errors.MinioException;

public class MinioClientWithLdapIdentityProvider {
  public static void main(String[] args) throws MinioException {
    // STS endpoint usually point to MinIO server.
    String stsEndpoint = "http://STS-HOST:STS-PORT/";

    // LDAP username.
    String ldapUsername = "USERNAME";

    // LDAP password.
    String ldapPassword = "PASSWORD";

    Provider provider = new LdapIdentityProvider(stsEndpoint, ldapUsername, ldapPassword, null);

    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://MINIO-HOST:MINIO-PORT")
            .credentialsProvider(provider)
            .build();

    // Get information of an object.
    StatObjectResponse stat =
        minioClient.statObject(
            StatObjectArgs.builder().bucket("my-bucket").object("my-object").build());
    System.out.println(stat);
  }
}
