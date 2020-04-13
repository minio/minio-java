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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Describes the default server-side encryption to apply to new objects in the bucket. If Put Object
 * request does not specify any server-side encryption, this default encryption will be applied.
 */
@SuppressWarnings("SameParameterValue")
@Root(name = "ServerSideEncryptionByDefault", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class ServerSideEncryptionByDefault {

  @Element(name = "SSEAlgorithm", required = false)
  private String sseAlgorithm;

  @Element(name = "KMSMasterKeyID", required = false)
  private String kmsMasterKeyId;

  /** Constructs a new ServerSideEncryptionByDefault object with given SSEAlgorithm. */
  public ServerSideEncryptionByDefault(String sseAlgorithm) {
    if (sseAlgorithm != null) {
      this.sseAlgorithm = sseAlgorithm;
    }
  }

  /**
   * Constructs a new ServerSideEncryptionByDefault object with given SSEAlgorithm and
   * KMSMasterKeyID.
   */
  public ServerSideEncryptionByDefault(String sseAlgorithm, String kmsMasterKeyId) {
    if (sseAlgorithm != null) {
      this.sseAlgorithm = sseAlgorithm;
    }
    if (kmsMasterKeyId != null) {
      this.kmsMasterKeyId = kmsMasterKeyId;
    }
  }

  /** Returns sseAlgorithm. */
  public String sseAlgorithm() {
    return this.sseAlgorithm;
  }

  /** Returns kmsMasterKeyID. */
  public String kmsMasterKeyId() {
    return this.kmsMasterKeyId;
  }
}
