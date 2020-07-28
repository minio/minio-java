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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

/**
 * Object representation of response XML of <a
 * href="https://github.com/minio/minio/blob/master/docs/sts/client-grants.md">AssumeRoleWithClientGrants
 * API</a>.
 */
@Root(name = "AssumeRoleWithClientGrantsResponse", strict = false)
@Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
public class AssumeRoleWithClientGrantsResponse {
  @Path(value = "AssumeRoleWithClientGrantsResult")
  @Element(name = "Credentials")
  private Credentials credentials;

  public Credentials credentials() {
    return credentials;
  }
}
