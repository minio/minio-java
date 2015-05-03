package io.minio.objectstorage.client;

import java.net.URL;

public class HttpClient implements Client {
    private final URL url;

    public HttpClient(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }
}
