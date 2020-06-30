package io.minio.credentials;

import io.minio.messages.Credentials;

@SuppressWarnings("unused")
public class AnonymousCredentialsProvider implements CredentialsProvider {

    private static final Credentials EMPTY = new Credentials();

    @Override
    public Credentials fetch() {
        return EMPTY;
    }
}
