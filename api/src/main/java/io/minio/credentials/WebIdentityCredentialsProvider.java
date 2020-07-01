package io.minio.credentials;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

import io.minio.Xml;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.XmlParserException;
import io.minio.messages.AssumeRoleWithWebIdentityResponse;
import io.minio.messages.Credentials;
import io.minio.messages.WebIdentityToken;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("unused")
public class WebIdentityCredentialsProvider extends StsCredentialsProvider {

    private Credentials credentials;
    private final Supplier<WebIdentityToken> tokenProducer;

    public WebIdentityCredentialsProvider(@Nonnull String stsEndpoint,
                                          @Nonnull Supplier<WebIdentityToken> tokenProducer) {
        super(stsEndpoint);
        this.tokenProducer = Objects.requireNonNull(tokenProducer, "Token producer must not be null");
    }

    @Override
    public Credentials fetch() {
        if (credentials != null && !isExpired(credentials)) {
            return credentials;
        }
        synchronized (this) {
            if (credentials == null || isExpired(credentials)) {
                try (Response response = callSecurityTokenService()) {
                    final ResponseBody body = response.body();
                    if (body == null) {
                        // should not happen
                        throw new IllegalStateException("Received empty response");
                    }
                    credentials = Xml.unmarshal(AssumeRoleWithWebIdentityResponse.class,
                            body.charStream()).credentials();
                } catch (XmlParserException | IOException | InvalidResponseException e) {
                    throw new IllegalStateException("Failed to process STS call", e);
                }
            }
        }
        return credentials;
    }

    @Override
    protected Map<String, String> queryParams() {
        final WebIdentityToken grantsToken = tokenProducer.get();
        final Map<String, String> queryParamenters = new HashMap<>();
        queryParamenters.put("Action", "AssumeRoleWithWebIdentity");
        queryParamenters.put("DurationSeconds", tokenDuration(grantsToken.expiredAfter()));
        queryParamenters.put("WebIdentityToken", grantsToken.token());
        queryParamenters.put("Version", "2011-06-15");
        if (grantsToken.policy() != null) {
            queryParamenters.put("Policy", grantsToken.policy());
        }
        return queryParamenters;
    }
}
