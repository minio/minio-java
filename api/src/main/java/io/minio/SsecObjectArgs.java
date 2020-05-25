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

import okhttp3.HttpUrl;

public abstract class SsecObjectArgs extends ObjectArgs {
  protected ServerSideEncryptionCustomerKey ssec;
  public static final int TYPE_READ = 0;
  public static final int TYPE_WRITE = 1;

  public ServerSideEncryptionCustomerKey ssec() {
    return ssec;
  }

  protected void validateSsec(HttpUrl baseUrl) {
    if (ssec == null) {
      return;
    }

    if (ssec.type().requiresTls() && !baseUrl.isHttps()) {
      throw new IllegalArgumentException(
          ssec.type().name() + "operations must be performed over a secure connection.");
    }
  }

  public abstract static class Builder<B extends Builder<B, A>, A extends SsecObjectArgs>
      extends ObjectArgs.Builder<B, A> {
    @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
    public B ssec(ServerSideEncryptionCustomerKey ssec) {
      operations.add(args -> args.ssec = ssec);
      return (B) this;
    }
  }
}
