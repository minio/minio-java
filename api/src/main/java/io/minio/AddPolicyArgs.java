package io.minio;

import io.minio.messages.UserInfo;

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
