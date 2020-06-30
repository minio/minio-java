package io.minio.credentials;

import java.time.Duration;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;

import io.minio.messages.Credentials;

/**
 * This component allows {@link io.minio.MinioClient} to fetch valid (not expired) credentials. Note: any provider
 * implementation should cache valid credentials and control it's lifetime to prevent unnesessary computation logic of
 * repeatedly called {@link #fetch()}, while holding a valid {@link Credentials} instance.
 */
@SuppressWarnings("unused")
public interface CredentialsProvider {

    /**
     * @return a valid (not expired) {@link Credentials} instance for {@link io.minio.MinioClient}.
     */
    Credentials fetch();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isExpired(@Nullable Credentials credentials) {
        if (credentials == null || credentials.expiredAt() == null || credentials.isAnonymous()) {
            return false;
        }
        // fair enough amount of time to execute the call to avoid situations when the check returns ok and credentials
        // expire immediately after that.
        return ZonedDateTime.now().plus(Duration.ofSeconds(30)).isAfter(credentials.expiredAt());
    }
}
