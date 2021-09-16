/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015-2021 MinIO, Inc.
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

package io.minio.admin;

import io.minio.BaseArgs;
import io.minio.ListBucketsArgs;

public class AddUserArgs extends BaseArgs {

  protected String accessKey;
  protected String secretKey;

  public String accessKey() {
    return accessKey;
  }

  public String secretKey() {
    return secretKey;
  }

  public static AddUserArgs.Builder builder() {
    return new AddUserArgs.Builder();
  }

  /** Argument builder of {@link ListBucketsArgs}. */
  public static final class Builder extends BaseArgs.Builder<AddUserArgs.Builder, AddUserArgs> {
    @Override
    protected void validate(AddUserArgs args) {}

    public AddUserArgs.Builder accessKey(String accessKey) {
      this.operations.add(args -> args.accessKey = accessKey);
      return this;
    }

    public AddUserArgs.Builder secretKey(String secretKey) {
      this.operations.add(args -> args.secretKey = secretKey);
      return this;
    }
  }

  public UserInfo toUserInfo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setSecretKey(secretKey());
    userInfo.setStatus(UserInfo.STATUS_ENABLED);
    return userInfo;
  }
}
