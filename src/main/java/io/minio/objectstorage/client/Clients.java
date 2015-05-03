package io.minio.objectstorage.client;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Clients
 */
public class Clients {
    private Clients() {
    }

    public static Client getClient(String url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        return getClient(new URL(url));
    }

    public static Client getClient(URL url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        if (url.getPath().length() > 0 && !url.getPath().equals("/")) {
            throw new java.net.MalformedURLException("Path should be empty: '" + url.getPath() + "'");
        }
        return new HttpClient(url);
    }
}
