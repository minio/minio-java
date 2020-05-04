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

package io.minio;

public class RemoveObjectArgs {
  private final Bucket bucket;
  private final ObjectWithVersion object;
  private final boolean bypassGovernanceRetention;

  private RemoveObjectArgs(Builder builder) {
    this.bucket = builder.bucket;
    this.object = builder.object;
    this.bypassGovernanceRetention = builder.bypassGovernanceRetention;
  }

  public Bucket bucket() {
    return bucket;
  }

  public ObjectWithVersion object() {
    return object;
  }

  public boolean bypassGovernanceRetention() {
    return bypassGovernanceRetention;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private Bucket bucket;
    private ObjectWithVersion object;
    private boolean bypassGovernanceRetention;

    public Builder bucket(String bucketName) throws IllegalArgumentException {
      this.bucket = new Bucket(bucketName);
      return this;
    }

    public Builder bucket(Bucket bucket) throws IllegalArgumentException {
      if (bucket == null) {
        throw new IllegalArgumentException("null value is not allowed for Bucket");
      }
      this.bucket = bucket;
      return this;
    }

    public Builder object(String objectName) {
      this.object = new ObjectWithVersion(objectName);
      return this;
    }

    public Builder object(String objectName, String versionId) {
      this.object = new ObjectWithVersion(objectName, versionId);
      return this;
    }

    public Builder object(ObjectWithVersion object) {
      if (object == null) {
        throw new IllegalArgumentException("null Object");
      }
      this.object = object;
      return this;
    }

    public Builder bypassGvernanceRetention(boolean bypassGovernanceRetention) {
      this.bypassGovernanceRetention = bypassGovernanceRetention;
      return this;
    }

    public RemoveObjectArgs build() {
      if (bucket == null || object == null) {
        throw new IllegalArgumentException("null Bucket or Object");
      }
      return new RemoveObjectArgs(this);
    }
  }
}
