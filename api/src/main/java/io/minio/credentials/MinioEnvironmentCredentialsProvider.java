package io.minio.credentials;

import java.time.ZonedDateTime;

import io.minio.messages.Credentials;

@SuppressWarnings("unused")
public class MinioEnvironmentCredentialsProvider extends EnvironmentCredentialsProvider {

    private static final String ACCESS_KEY_ALIAS = "MINIO_ACCESS_KEY";
    private static final String SECRET_KEY_ALIAS = "MINIO_SECRET_KEY";

    private Credentials credentials;

    public MinioEnvironmentCredentialsProvider() {
        credentials = readCredentials();
    }

    @Override
    public Credentials fetch() {
        if (!isExpired(credentials)) {
            return credentials;
        }
        // avoid race conditions with credentials rewriting
        synchronized (this) {
            credentials = readCredentials();
        }
        return credentials;
    }

    private Credentials readCredentials() {
        final String accessKey = readProperty(ACCESS_KEY_ALIAS);
        final String secretKey = readProperty(SECRET_KEY_ALIAS);
        final ZonedDateTime lifeTime = ZonedDateTime.now().plus(REFRESHED_AFTER);
        //noinspection ConstantConditions
        return new Credentials(accessKey, secretKey, lifeTime);
    }
}
