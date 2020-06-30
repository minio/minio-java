package io.minio.messages;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    public Credentials() {
        // default constructor for deserialization stuff
    }

    public Credentials(@Nonnull String accessKey, @Nonnull String secretKey) {
        this.accessKey = Objects.requireNonNull(accessKey, "AccessKey must not be null");
        this.secretKey = Objects.requireNonNull(secretKey, "SecretKey must not be null");
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            throw new IllegalArgumentException("AccessKey and SecretKey must not be empty");
        }
    }

    public Credentials(@Nonnull String accessKey, @Nonnull String secretKey, @Nonnull ZonedDateTime expiredAt) {
        this(accessKey, secretKey);
        this.expiredAt = new ResponseDate(Objects.requireNonNull(expiredAt));
    }

    public Credentials(@Nonnull String accessKey, @Nonnull String secretKey, @Nonnull ZonedDateTime expiredAt,
                       @Nullable String sessionToken) {
        this(accessKey, secretKey, expiredAt);
        this.sessionToken = sessionToken;
    }

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

    public boolean isAnonymous() {
        return accessKey == null && secretKey == null;
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
