package io.minio.credentials;

import io.minio.messages.Credentials;

public class AnonymousCredentialsProvider implements CredentialsProvider {

  @Override
  public Credentials fetch() {
    return Credentials.EMPTY;
  }
}
