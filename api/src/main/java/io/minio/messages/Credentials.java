package io.minio.messages;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "Credentials", strict = false)
public class Credentials {

    public static final Credentials EMPTY = new Credentials();

    @Element(name = "AccessKeyId")
    private final String accessKey;

    @Element(name = "SecretAccessKey")
    private final String secretKey;

    @Element(name = "Expiration")
    private final ResponseDate expiredAt;

    @Element(name = "SessionToken")
    private final String sessionToken;

    private Credentials() {
        accessKey = null;
        secretKey = null;
        expiredAt = null;
        sessionToken = null;
    }

    public Credentials(@Nonnull String accessKey, @Nonnull String secretKey) {
        this(accessKey, secretKey, null);
    }

    public Credentials(@Nonnull String accessKey, @Nonnull String secretKey, @Nullable ZonedDateTime expiredAt) {
        this(accessKey, secretKey, expiredAt, null);
    }

    public Credentials(@Nonnull String accessKey,
                       @Nonnull String secretKey,
                       @Nullable ZonedDateTime expiredAt,
                       @Nullable String sessionToken) {
        this(accessKey, secretKey, new ResponseDate(expiredAt), sessionToken);
    }

    // deserialization constructor
    @SuppressWarnings("unused")
    public Credentials(@Nonnull @Element(name = "AccessKeyId") String accessKey,
                       @Nonnull @Element(name = "SecretAccessKey") String secretKey,
                       @Nullable @Element(name = "Expiration") ResponseDate expiredAt,
                       @Nullable @Element(name = "SessionToken") String sessionToken) {
        this.accessKey = Objects.requireNonNull(accessKey, "AccessKey must not be null");
        this.secretKey = Objects.requireNonNull(secretKey, "SecretKey must not be null");
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            throw new IllegalArgumentException("AccessKey and SecretKey must not be empty");
        }
        this.sessionToken = sessionToken;
        this.expiredAt = expiredAt;
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
        return accessKey == null || secretKey == null;
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
