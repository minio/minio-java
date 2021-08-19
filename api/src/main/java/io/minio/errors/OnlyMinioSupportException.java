package io.minio.errors;

/** Thrown when attempting to use MinIO-specific functionality with AWS. */
public class OnlyMinioSupportException extends MinioException {
  public OnlyMinioSupportException(String action) {
    super(action + " action(s) are only supported for MinIO, not AWS.");
  }
}
