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

import io.minio.messages.DeleteRequest;
import java.util.ArrayList;
import java.util.Objects;

/** Arguments of {@link MinioAsyncClient#removeObjects} and {@link MinioClient#removeObjects}. */
public class RemoveObjectsArgs extends BucketArgs {
  private boolean bypassGovernanceMode;
  private Iterable<DeleteRequest.Object> objects = new ArrayList<>();
  private long delayMs = 200L;
  private int maxRetries = 5;

  public boolean bypassGovernanceMode() {
    return bypassGovernanceMode;
  }

  public Iterable<DeleteRequest.Object> objects() {
    return objects;
  }

  public long delayMs() {
    return delayMs;
  }

  public int maxRetries() {
    return maxRetries;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder of {@link RemoveObjectsArgs}. */
  public static final class Builder extends BucketArgs.Builder<Builder, RemoveObjectsArgs> {
    public Builder bypassGovernanceMode(boolean flag) {
      operations.add(args -> args.bypassGovernanceMode = flag);
      return this;
    }

    public Builder objects(Iterable<DeleteRequest.Object> objects) {
      Utils.validateNotNull(objects, "objects");
      operations.add(args -> args.objects = objects);
      return this;
    }

    /** Set delay between retries. Value &lt;= 0 makes no delay (default 200ms). */
    public Builder delayMs(long delayMs) {
      operations.add(args -> args.delayMs = delayMs);
      return this;
    }

    /** Set maximum retry between failure. Value &lt;= 0 disables retry (default 5). */
    public Builder maxRetries(int maxRetries) {
      operations.add(args -> args.maxRetries = maxRetries);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RemoveObjectsArgs)) return false;
    if (!super.equals(o)) return false;
    RemoveObjectsArgs that = (RemoveObjectsArgs) o;
    return bypassGovernanceMode == that.bypassGovernanceMode
        && Objects.equals(objects, that.objects)
        && delayMs == that.delayMs
        && maxRetries == that.maxRetries;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), bypassGovernanceMode, objects, delayMs, maxRetries);
  }
}
