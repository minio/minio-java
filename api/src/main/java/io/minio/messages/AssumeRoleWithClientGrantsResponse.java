package io.minio.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name = "AssumeRoleWithClientGrantsResponse", strict = false)
@Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
public class AssumeRoleWithClientGrantsResponse {

  @Element(name = "AssumeRoleWithClientGrantsResult")
  private AssumeRoleWithClientGrantsResult clientGrantsResult;

  public AssumeRoleWithClientGrantsResult getClientGrantsResult() {
    return clientGrantsResult;
  }
}
