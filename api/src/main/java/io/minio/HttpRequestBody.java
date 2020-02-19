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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/** RequestBody that wraps a single data object. */
class HttpRequestBody extends RequestBody {
  private RandomAccessFile file = null;
  private BufferedInputStream stream = null;
  private byte[] bytes = null;
  private int length = -1;
  private String contentType = null;

  HttpRequestBody(final RandomAccessFile file, final int length, final String contentType) {
    this.file = file;
    this.length = length;
    this.contentType = contentType;
  }

  HttpRequestBody(final BufferedInputStream stream, final int length, final String contentType) {
    this.stream = stream;
    this.length = length;
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

    if (contentType != null) {
      mediaType = MediaType.parse(contentType);
    }
    if (mediaType == null) {
      mediaType = MediaType.parse("application/octet-stream");
    }

    return mediaType;
  }

  @Override
  public long contentLength() {
    return length;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    if (file != null) {
      sink.write(Okio.source(Channels.newInputStream(file.getChannel())), length);
    } else if (stream != null) {
      sink.write(Okio.source(stream), length);
    } else {
      sink.write(bytes, 0, length);
    }
  }
}
