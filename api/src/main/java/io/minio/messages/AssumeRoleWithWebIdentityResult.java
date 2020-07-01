package io.minio.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "AssumeRoleWithWebIdentityResult", strict = false)
public class AssumeRoleWithWebIdentityResult {

  @Element(name = "Credentials")
  private Credentials credentials;

  public Credentials credentials() {
    return credentials;
  }
}
