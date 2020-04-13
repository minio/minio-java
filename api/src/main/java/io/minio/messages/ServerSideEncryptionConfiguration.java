/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

package io.minio.messages;

import java.util.List;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Helper class to construct create bucket encryption configuration request XML for Amazon AWS S3.
 */
@Root(name = "ServerSideEncryptionConfiguration", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class ServerSideEncryptionConfiguration {

  @Element(name = "Rule", required = false)
  private List<ServerSideEncryptionByDefault> rules;

  /** Constructs a new ServerSideEncryptionConfiguration object with given retention. */
  public ServerSideEncryptionConfiguration(
      ServerSideEncryptionByDefault serverSideEncryptionByDefault) {
    // this.rules.add(serverSideEncryptionByDefault);
    System.out.println(serverSideEncryptionByDefault);
  }
}
