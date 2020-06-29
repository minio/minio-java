package io.minio.messages;

import java.util.Objects;
import javax.annotation.Nonnull;

public class ClientGrantsToken {

  private final String token;
  private final long expiredAfter;

  public ClientGrantsToken(@Nonnull String token, long expiredAfter) {
    this.token = Objects.requireNonNull(token);
    this.expiredAfter = expiredAfter;
  }

  public String token() {
    return token;
  }

  public long expiredAfter() {
    return expiredAfter;
  }
}
