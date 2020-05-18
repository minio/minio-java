package io.minio;

/** Argument class of @see #listIncompleteUploads(ListIncompleteUploadsArgs args). */
public class ListIncompleteUploadsArgs extends BucketArgs {
  private String prefix;
  private boolean recursive;

  /** Returns prefix. */
  public String prefix() {
    return prefix;
  }

  /** Returns recursive flag. */
  public boolean recursive() {
    return recursive;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of @see #listIncompleteUploads(ListIncompleteUploadsArgs args). */
  public static final class Builder extends BucketArgs.Builder<Builder, ListIncompleteUploadsArgs> {
    public Builder prefix(String prefix) {
      operations.add(args -> args.prefix = prefix);
      return this;
    }

    public Builder recursive(boolean recursive) {
      operations.add(args -> args.recursive = recursive);
      return this;
    }
  }
}
