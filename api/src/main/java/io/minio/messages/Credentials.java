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
    private ZonedDateTime expiredAt;

    @Element(name = "SessionToken")
    private String sessionToken;

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public ZonedDateTime getExpiredAt() {
        return expiredAt;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
