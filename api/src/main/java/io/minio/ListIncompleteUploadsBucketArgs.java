package io.minio;

/** Argument class of @see #listIncompleteUploads(ListIncompleteUploadsBucketArgs args). */
public class ListIncompleteUploadsBucketArgs extends BucketArgs {
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

  /** Argument builder of @see #listIncompleteUploads(ListIncompleteUploadsBucketArgs args). */
  public static final class Builder
      extends BucketArgs.Builder<Builder, ListIncompleteUploadsBucketArgs> {
    public Builder prefix(String prefix) {
      operations.add(args -> args.prefix = prefix);
      return this;
    }

    public Builder recursive(boolean recursive) {
      operations.add(args -> args.recursive = recursive);
      return this;
    }

    public ListIncompleteUploadsBucketArgs build() throws IllegalArgumentException {
      return build(ListIncompleteUploadsBucketArgs.class);
    }
  }
}
