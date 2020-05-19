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

/** Argument class of MinioClient.statObject(). */
public class StatObjectArgs extends ObjectArgs {
  private ServerSideEncryption ssec;

  public ServerSideEncryption ssec() {
    return ssec;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link StatObjectArgs}. */
  public static final class Builder extends ObjectArgs.Builder<Builder, StatObjectArgs> {
    public Builder ssec(ServerSideEncryption ssec) {
      if (ssec != null && ssec.type() != ServerSideEncryption.Type.SSE_C) {
        throw new IllegalArgumentException("only SSE-C type server-side encryption is allowed");
      }

      operations.add(args -> args.ssec = ssec);
      return this;
    }
  }
}
