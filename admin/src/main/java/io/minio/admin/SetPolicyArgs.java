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

public class SetPolicyArgs extends BaseArgs {

  protected String policyName;
  protected String userOrGroup;
  protected boolean isGroup;

  public String policyName() {
    return policyName;
  }

  public String userOrGroup() {
    return userOrGroup;
  }

  public boolean isGroup() {
    return isGroup;
  }

  public static SetPolicyArgs.Builder builder() {
    return new SetPolicyArgs.Builder();
  }

  /** Argument builder of {@link ListBucketsArgs}. */
  public static final class Builder extends BaseArgs.Builder<SetPolicyArgs.Builder, SetPolicyArgs> {
    @Override
    protected void validate(SetPolicyArgs args) {}

    public SetPolicyArgs.Builder policyName(String policyName) {
      this.operations.add(args -> args.policyName = policyName);
      return this;
    }

    public SetPolicyArgs.Builder isGroup(boolean isGroup) {
      this.operations.add(args -> args.isGroup = isGroup);
      return this;
    }

    public SetPolicyArgs.Builder userOrGroup(String userOrGroup) {
      this.operations.add(args -> args.userOrGroup = userOrGroup);
      return this;
    }
  }
}
