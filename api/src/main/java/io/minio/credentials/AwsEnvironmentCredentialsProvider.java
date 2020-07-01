package io.minio.credentials;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.minio.messages.Credentials;

@SuppressWarnings("unused")
public class AwsEnvironmentCredentialsProvider extends EnvironmentCredentialsProvider {

    private static final List<String> ACCESS_KEY_ALIASES = Arrays.asList("AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY");
    private static final List<String> SECRET_KEY_ALIASES = Arrays.asList("AWS_SECRET_ACCESS_KEY", "AWS_SECRET_KEY");
    private static final String SESSION_TOKEN_ALIAS = "AWS_SESSION_TOKEN";

    private Credentials credentials;

    public AwsEnvironmentCredentialsProvider() {
        credentials = readCredentials();
    }

    @Override
    public Credentials fetch() {
        if (!isExpired(credentials)) {
            return credentials;
        }
        // avoid race conditions with credentials rewriting
        synchronized (this) {
            if (isExpired(credentials)) {
                credentials = readCredentials();
            }
        }
        return credentials;
    }

    private Credentials readCredentials() {
        final String accessKey = ACCESS_KEY_ALIASES.stream()
                .map(this::readProperty)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find env variables for " + ACCESS_KEY_ALIASES));
        final String secretKey = SECRET_KEY_ALIASES.stream()
                .map(this::readProperty)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find env variables for " + SECRET_KEY_ALIASES));
        final ZonedDateTime lifeTime = ZonedDateTime.now().plus(REFRESHED_AFTER);
        final String sessionToken = readProperty(SESSION_TOKEN_ALIAS);
        return new Credentials(accessKey, secretKey, lifeTime, sessionToken);
    }
}
