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

import java.util.Objects;

/** Base argument class holds object name and version ID along with bucket information. */
public abstract class ObjectVersionArgs extends ObjectArgs {
  protected String versionId;

  public String versionId() {
    return versionId;
  }

  /** Base argument builder class for {@link ObjectVersionArgs}. */
  public abstract static class Builder<B extends Builder<B, A>, A extends ObjectVersionArgs>
      extends ObjectArgs.Builder<B, A> {
    @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
    public B versionId(String versionId) {
      operations.add(args -> args.versionId = versionId);
      return (B) this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ObjectVersionArgs)) return false;
    if (!super.equals(o)) return false;
    ObjectVersionArgs that = (ObjectVersionArgs) o;
    return Objects.equals(versionId, that.versionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), versionId);
  }
}
