package io.minio.credentials;

import io.minio.messages.Credentials;
import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class StaticCredentialsProvider implements CredentialsProvider {

  private final Credentials credentials;

  public StaticCredentialsProvider(@Nonnull String accessKey, @Nonnull String secretKey) {
    this.credentials = new Credentials(accessKey, secretKey);
  }

  @Override
  public Credentials fetch() {
    return credentials;
  }
}
