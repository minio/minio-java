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

import com.google.common.base.Objects;
import java.util.Map;
import okhttp3.HttpUrl;

/** Base argument class for reading object. */
public abstract class ObjectReadArgs extends ObjectVersionArgs {
  protected ServerSideEncryptionCustomerKey ssec;
  protected Map<String, String> sseKmsContext;

  public ServerSideEncryptionCustomerKey ssec() {
    return ssec;
  }

  public Map<String, String> sseKmsContext() {
    return sseKmsContext;
  }

  protected void validateSsec(HttpUrl url) {
    checkSse(ssec, url);
  }

  /** Base argument builder class for {@link ObjectReadArgs}. */
  public abstract static class Builder<B extends Builder<B, A>, A extends ObjectReadArgs>
      extends ObjectVersionArgs.Builder<B, A> {
    protected void validate(A args) {
      super.validate(args);
      if (args.ssec != null && args.sseKmsContext != null) {
        throw new IllegalArgumentException("both SSE-C and SSE-KMS context cannot be set");
      }
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
    public B ssec(ServerSideEncryptionCustomerKey ssec) {
      operations.add(args -> args.ssec = ssec);
      return (B) this;
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
    public B sseKmsContext(Map<String, String> sseKmsContext) {
      operations.add(args -> args.sseKmsContext = sseKmsContext);
      return (B) this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ObjectReadArgs)) return false;
    if (!super.equals(o)) return false;
    ObjectReadArgs that = (ObjectReadArgs) o;
    return Objects.equal(ssec, that.ssec) && Objects.equal(sseKmsContext, that.sseKmsContext);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), ssec, sseKmsContext);
  }
}
