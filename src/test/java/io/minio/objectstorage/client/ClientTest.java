/*
 * Minimalist Object Storage Java Client, (C) 2015 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio.objectstorage.client;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class ClientTest {
    @Test()
    public void InstantiateNewClient() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Clients.getClient("http://" + expectedHost);

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test()
    public void InstantiateNewClientWithTrailingSlash() throws MalformedURLException {
        String expectedHost = "example.com";
        Client client = Clients.getClient("http://" + expectedHost + "/");

        URL url = client.getUrl();
        // check schema
        assertEquals("http", url.getProtocol());
        // check host
        assertEquals(expectedHost, url.getHost());
    }

    @Test(expected = MalformedURLException.class)
    public void NewClientWithPathFails() throws MalformedURLException {
        Clients.getClient("http://example.com/path");
    }

    @Test(expected = NullPointerException.class)
    public void NewClientWithNullURLFails() throws MalformedURLException {
        Clients.getClient((URL) null);
    }

    @Test(expected = NullPointerException.class)
    public void NewClientWithNullURLStringFails() throws MalformedURLException {
        Clients.getClient((String) null);
    }
}