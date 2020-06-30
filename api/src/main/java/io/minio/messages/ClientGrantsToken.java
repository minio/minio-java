package io.minio.messages;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClientGrantsToken {

  private final String jwtAccessToken;
  private final long expiredAfter;
  private final String policy;

  @SuppressWarnings("unused")
  public ClientGrantsToken(@Nonnull String jwtAccessToken, long expiredAfter) {
    this(jwtAccessToken, expiredAfter, null);
  }

  public ClientGrantsToken(@Nonnull String jwtAccessToken, long expiredAfter, @Nullable String policy) {
    this.jwtAccessToken = Objects.requireNonNull(jwtAccessToken);
    this.expiredAfter = expiredAfter;
    this.policy = policy;
  }

  public String token() {
    return jwtAccessToken;
  }

  public long expiredAfter() {
    return expiredAfter;
  }

  public String policy() {
    return policy;
  }
}
