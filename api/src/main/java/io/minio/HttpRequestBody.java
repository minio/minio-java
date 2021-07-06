/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/** RequestBody that wraps a single data object. */
class HttpRequestBody extends RequestBody {
  private PartSource partSource;
  private byte[] bytes;
  private int length;
  private String contentType;

  HttpRequestBody(final PartSource partSource, final String contentType) {
    this.partSource = partSource;
    this.contentType = contentType;
  }

  HttpRequestBody(final byte[] bytes, final int length, final String contentType) {
    this.bytes = bytes;
    this.length = length;
    this.contentType = contentType;
  }

  @Override
  public MediaType contentType() {
    MediaType mediaType = null;
    if (contentType != null) mediaType = MediaType.parse(contentType);
    return (mediaType == null) ? MediaType.parse("application/octet-stream") : mediaType;
  }

  @Override
  public long contentLength() {
    return (partSource != null) ? partSource.size() : length;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    if (partSource != null) {
      sink.write(partSource.source(), partSource.size());
    } else {
      sink.write(bytes, 0, length);
    }
  }
}
