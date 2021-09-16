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

public class AddPolicyArgs extends BaseArgs {

  protected String policyName;
  protected String policyString;

  public String policyName() {
    return policyName;
  }

  public String policyString() {
    return policyString;
  }

  public static AddPolicyArgs.Builder builder() {
    return new AddPolicyArgs.Builder();
  }

  /** Argument builder of {@link ListBucketsArgs}. */
  public static final class Builder extends BaseArgs.Builder<AddPolicyArgs.Builder, AddPolicyArgs> {
    @Override
    protected void validate(AddPolicyArgs args) {}

    public AddPolicyArgs.Builder policyName(String policyName) {
      this.operations.add(args -> args.policyName = policyName);
      return this;
    }

    public AddPolicyArgs.Builder policyString(String policyString) {
      this.operations.add(args -> args.policyString = policyString);
      return this;
    }
  }
}
