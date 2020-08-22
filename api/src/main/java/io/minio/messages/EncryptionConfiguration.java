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

import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Helper class to denote encryption configuration information for {@link ReplicationDestination}.
 */
@Root(name = "EncryptionConfiguration")
public class EncryptionConfiguration {
  @Element(name = "ReplicaKmsKeyID", required = false)
  private String replicaKmsKeyID;

  public EncryptionConfiguration(
      @Nullable @Element(name = "ReplicaKmsKeyID", required = false) String replicaKmsKeyID) {
    this.replicaKmsKeyID = replicaKmsKeyID;
  }

  public String replicaKmsKeyID() {
    return this.replicaKmsKeyID;
  }
}
