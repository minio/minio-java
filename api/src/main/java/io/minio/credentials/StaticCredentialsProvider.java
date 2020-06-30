package io.minio.credentials;

import javax.annotation.Nonnull;

import io.minio.messages.Credentials;

@SuppressWarnings("unused")
public class StaticCredentialsProvider implements CredentialsProvider {

    private final Credentials credentials;

    public StaticCredentialsProvider(@Nonnull String accessKey, @Nonnull String secretKey) {
        this.credentials = new Credentials(accessKey, secretKey);
    }

    @Override
    public Credentials fetch() {
        return credentials;
    }
}
