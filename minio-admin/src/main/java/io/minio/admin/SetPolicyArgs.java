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
