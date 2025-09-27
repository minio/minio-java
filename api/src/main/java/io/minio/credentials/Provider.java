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

package io.minio.credentials;

/**
 * Definition of credential provider. Provider always gives a valid, non-expired credentials to be
 * used to perform S3 operations. Credential consumer like {@link io.minio.MinioClient} calls {@link
 * #fetch()} to get credentials. It is provider's responsibility to retrieve credentials upon expiry
 * when {@link #fetch()} is called.
 */
public interface Provider {
  /**
   * Returns a valid {@link Credentials} instance by retrieving from credential provider service if
   * necessary.
   */
  Credentials fetch();
}
