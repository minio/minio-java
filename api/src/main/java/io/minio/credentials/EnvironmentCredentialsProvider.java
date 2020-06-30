package io.minio.credentials;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class EnvironmentCredentialsProvider implements CredentialsProvider {

    // it's ok to re-read values from env.variables every 5 min.
    protected static final Duration REFRESHED_AFTER = Duration.ofMinutes(5);

    /**
     * Method used to read system/env properties. If property not found through system properties it will search the
     * property in environment properties.
     *
     * @param propertyName name of the property to retrieve.
     * @return property value.
     * @throws NullPointerException if {@literal propertyName} is null.
     */
    @Nullable
    protected String readProperty(@Nonnull String propertyName) {
        final String systemProperty = System.getProperty(propertyName);
        if (systemProperty != null) {
            return systemProperty;
        }
        return System.getenv(propertyName);
    }

}
