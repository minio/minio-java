/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

package io.minio;

import java.io.IOException;
import java.io.InputStream;
import okhttp3.Headers;

public class GetObjectResponse extends InputStream {
  private BaseResponse response;
  private InputStream body;

  public GetObjectResponse(
      Headers headers, String bucket, String region, String object, InputStream body) {
    this.response = new BaseResponse(headers, bucket, region, object);
    this.body = body;
  }

  public Headers headers() {
    return response.headers();
  }

  public String bucket() {
    return response.bucket();
  }

  public String region() {
    return response.region();
  }

  public String object() {
    return response.object();
  }

  public InputStream body() {
    return body;
  }

  @Override
  public int available() throws IOException {
    return body.available();
  }

  @Override
  public void close() throws IOException {
    body.close();
  }

  @Override
  public void mark(int readlimit) {
    body.mark(readlimit);
  }

  @Override
  public boolean markSupported() {
    return body.markSupported();
  }

  @Override
  public int read() throws IOException {
    return body.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return body.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return body.read(b, off, len);
  }

  @Override
  public void reset() throws IOException {
    body.reset();
  }

  @Override
  public long skip(long n) throws IOException {
    return body.skip(n);
  }

  private static class BaseResponse extends GenericResponse {
    public BaseResponse(Headers headers, String bucket, String region, String object) {
      super(headers, bucket, region, object);
    }
  }
}
