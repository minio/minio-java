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
package io.minio.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name = "AssumeRoleWithClientGrantsResponse", strict = false)
@Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
public class AssumeRoleWithClientGrantsResponse {

  @Element(name = "AssumeRoleWithClientGrantsResult")
  private AssumeRoleWithClientGrantsResult clientGrantsResult;

  public Credentials credentials() {
    return clientGrantsResult == null ? null : clientGrantsResult.credentials();
  }
}
