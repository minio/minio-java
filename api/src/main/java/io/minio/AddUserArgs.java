package io.minio;

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
}
