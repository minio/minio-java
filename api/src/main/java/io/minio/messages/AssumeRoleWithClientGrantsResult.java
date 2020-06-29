package io.minio.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "AssumeRoleWithClientGrantsResult", strict = false)
public class AssumeRoleWithClientGrantsResult {

  @Element(name = "Credentials")
  private Credentials credentials;

  public Credentials credentials() {
    return credentials;
  }
}
