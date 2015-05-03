package io.minio.objectstorage.client;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class ClientTest {
    @Test()
    public void InstantiateNewClient() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Client.newClient("http://" + expectedHost);

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test()
    public void InstantiateNewClientWithTrailingSlash() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Client.newClient("http://" + expectedHost + "/");

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test(expected = MalformedURLException.class)
    public void NewClientWithPathFails() throws MalformedURLException {
        Client.newClient("http://example.com/path");
    }

    @Test(expected = NullPointerException.class)
    public void NewClientWithNullURLFails() throws MalformedURLException {
        Client.newClient((URL) null);
    }

    @Test(expected = NullPointerException.class)
    public void NewClientWithNullURLStringFails() throws MalformedURLException {
        Client.newClient((String) null);
    }
}