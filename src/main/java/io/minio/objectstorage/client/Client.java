package io.minio.objectstorage.client;

import java.net.MalformedURLException;
import java.net.URL;

public class Client {
    private final URL url;

    private Client(URL url) {
        this.url = url;
    }

    public static Client newClient(String url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        return newClient(new URL(url));
    }

    public static Client newClient(URL url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        if (url.getPath().length() > 0 && !url.getPath().equals("/")) {
            throw new java.net.MalformedURLException("Path should be empty: '" + url.getPath() + "'");
        }
        return new Client(url);
    }

    public URL getUrl() {
        return url;
    }
}
