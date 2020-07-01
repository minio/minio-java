package io.minio.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name = "AssumeRoleWithWebIdentityResponse", strict = false)
@Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
public class AssumeRoleWithWebIdentityResponse {

    @Element(name = "AssumeRoleWithWebIdentityResult")
    private AssumeRoleWithWebIdentityResult webIdentityResult;

    public Credentials credentials() {
        return webIdentityResult == null ? null : webIdentityResult.credentials();
    }

}
