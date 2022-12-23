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

/** Helper class to denote source selection criteria information for {@link ReplicationRule}. */
@Root(name = "SourceSelectionCriteria")
public class SourceSelectionCriteria {
  @Element(name = "ReplicaModifications", required = false)
  private ReplicaModifications replicaModifications;

  @Element(name = "SseKmsEncryptedObjects", required = false)
  private SseKmsEncryptedObjects sseKmsEncryptedObjects;

  public SourceSelectionCriteria(
      @Nullable @Element(name = "SseKmsEncryptedObjects", required = false)
          SseKmsEncryptedObjects sseKmsEncryptedObjects,
      @Nullable @Element(name = "ReplicaModifications", required = false)
          ReplicaModifications replicaModifications) {
    this.sseKmsEncryptedObjects = sseKmsEncryptedObjects;
    this.replicaModifications = replicaModifications;
  }

  public SourceSelectionCriteria(@Nullable SseKmsEncryptedObjects sseKmsEncryptedObjects) {
    this(sseKmsEncryptedObjects, null);
  }

  public ReplicaModifications replicaModifications() {
    return this.replicaModifications;
  }

  public SseKmsEncryptedObjects sseKmsEncryptedObjects() {
    return this.sseKmsEncryptedObjects;
  }
}
