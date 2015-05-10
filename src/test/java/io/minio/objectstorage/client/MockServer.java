/*
 * Minimal Object Storage Library, (C) 2015 Minio, Inc.
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

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MockServer extends CloseableHttpClient {
    private byte[] content;
    private int statusCode;
    private String reason;
    private Map<String,String> headers = new HashMap<>();

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        return new CloseableHttpResponse() {

            @Override
            public void close() throws IOException {

            }

            @Override
            public StatusLine getStatusLine() {
                return new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, reason);
            }

            @Override
            public void setStatusLine(StatusLine statusline) {

            }

            @Override
            public void setStatusLine(ProtocolVersion ver, int code) {

            }

            @Override
            public void setStatusLine(ProtocolVersion ver, int code, String reason) {

            }

            @Override
            public void setStatusCode(int code) throws IllegalStateException {

            }

            @Override
            public void setReasonPhrase(String reason) throws IllegalStateException {

            }

            @Override
            public HttpEntity getEntity() {
                BasicHttpEntity entity =  new BasicHttpEntity();
                entity.setContent(new ByteArrayInputStream(content));
                entity.setContentLength(content.length);
                return entity;
            }

            @Override
            public void setEntity(HttpEntity entity) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public void setLocale(Locale loc) {

            }

            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public boolean containsHeader(String name) {
                return false;
            }

            @Override
            public Header[] getHeaders(String name) {
                return new Header[0];
            }

            @Override
            public Header getFirstHeader(String name) {
                name = name.toLowerCase();
                Header header = null;
                String value = headers.get(name);
                if(value != null) {
                    header = new BasicHeader(name, value);
                }
                return header;
            }

            @Override
            public Header getLastHeader(String name) {
                return null;
            }

            @Override
            public Header[] getAllHeaders() {
                return new Header[0];
            }

            @Override
            public void addHeader(Header header) {

            }

            @Override
            public void addHeader(String name, String value) {

            }

            @Override
            public void setHeader(Header header) {

            }

            @Override
            public void setHeader(String name, String value) {

            }

            @Override
            public void setHeaders(Header[] headers) {

            }

            @Override
            public void removeHeader(Header header) {

            }

            @Override
            public void removeHeaders(String name) {

            }

            @Override
            public HeaderIterator headerIterator() {
                return null;
            }

            @Override
            public HeaderIterator headerIterator(String name) {
                return null;
            }

            @Override
            public HttpParams getParams() {
                return null;
            }

            @Override
            public void setParams(HttpParams params) {

            }
        };
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public HttpParams getParams() {
        return null;
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return null;
    }

    public void setContent(byte[] response) {
        this.content = response.clone();
    }

    public void setContent(String response) {
        try {
            this.content = response.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int status) {
        this.statusCode = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void addHeader(String key, String value) {
        key = key.toLowerCase();
        this.headers.put(key, value);
    }
}
