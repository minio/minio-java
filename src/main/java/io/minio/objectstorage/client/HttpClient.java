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

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class HttpClient implements Client {
    private final URL url;
    private HttpTransport transport = new NetHttpTransport();

    HttpClient(URL url) {
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException {
        GenericUrl url = new GenericUrl(this.url);

        List<String> pathParts = new LinkedList<>();
        pathParts.add("");
        pathParts.add(bucket);
        pathParts.add(key);

        url.setPathParts(pathParts);


        HttpRequestFactory requestFactory = this.transport.createRequestFactory();
        HttpRequest httpRequest = requestFactory.buildGetRequest(url);
        httpRequest = httpRequest.setRequestMethod("GET");
        try {
            httpRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ObjectMetadata("bucket", "key", Calendar.getInstance().getTime(), 0);
    }

    void setTransport(HttpTransport transport) {
        this.transport = transport;
    }
}
