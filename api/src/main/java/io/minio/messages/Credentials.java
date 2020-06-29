package io.minio.messages;

import java.time.ZonedDateTime;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "Credentials")
public class Credentials {

  @Element(name = "AccessKeyId")
  private String accessKey;

  @Element(name = "SecretAccessKey")
  private String secretKey;

  @Element(name = "Expiration")
  private ResponseDate expiredAt;

  @Element(name = "SessionToken")
  private String sessionToken;

  public String accessKey() {
    return accessKey;
  }

  public String secretKey() {
    return secretKey;
  }

  public ZonedDateTime expiredAt() {
    return expiredAt.zonedDateTime();
  }

  public String sessionToken() {
    return sessionToken;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + " [");
    sb.append("accessKey: ").append(accessKey);
    sb.append(", secretKey: ").append(secretKey);
    sb.append(", expiredAt: ").append(expiredAt);
    sb.append(", sessionToken: ").append(sessionToken);
    sb.append(']');
    return sb.toString();
  }
}
