package io.minio.credentials;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

import io.minio.errors.InvalidResponseException;
import io.minio.http.Method;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static io.minio.S3Escaper.encode;

public abstract class StsCredentialsProvider implements CredentialsProvider {

    private static final int MINIMUM_TOKEN_DURATION = 900;
    private static final int MAXIMUM_TOKEN_DURATION = 43200;
    private static final RequestBody EMPTY_BODY = RequestBody.create(
            MediaType.parse("application/octet-stream"), new byte[] {});

    private final HttpUrl endpoint;
    private final OkHttpClient httpClient = new OkHttpClient();

    public StsCredentialsProvider(@Nonnull String stsEndpoint) {
        this.endpoint = HttpUrl.parse(Objects.requireNonNull(stsEndpoint, "STS endpoint cannot be empty"));
    }

    protected Response callSecurityTokenService() throws IOException, InvalidResponseException {
        final Map<String, String> queryParams = queryParams();
        final HttpUrl.Builder url = endpoint.newBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            url.addEncodedQueryParameter(encode(entry.getKey()), encode(entry.getValue()));
        }
        final Request request = new Request.Builder()
            .url(url.build())
            // Disable default gzip compression by okhttp library.
            .header("Accept-Encoding", "identity")
            .method(Method.POST.toString(), EMPTY_BODY).build();

        final Response response = httpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response;
        }
        throw new InvalidResponseException(request);
    }

    protected String tokenDuration(long requiredSeconds) {
        if (requiredSeconds < MINIMUM_TOKEN_DURATION) {
            return String.valueOf(MINIMUM_TOKEN_DURATION);
        } else if (requiredSeconds > MAXIMUM_TOKEN_DURATION) {
            return String.valueOf(MAXIMUM_TOKEN_DURATION);
        }
        return String.valueOf(requiredSeconds);
    }

    /**
     * @return specific for concrete method query parameters.
     */
    protected abstract Map<String, String> queryParams();
}
