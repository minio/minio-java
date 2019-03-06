/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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
import java.io.RandomAccessFile;
import java.nio.channels.Channels;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * RequestBody that wraps a single data object.
 */
class HttpRequestBody extends RequestBody {
  private final String contentType;
  private final Object data;
  private final int len;

  HttpRequestBody(final String contentType, final Object data, final int len) {
    this.contentType = contentType;
    this.data = data;
    this.len = len;
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
    if (data instanceof InputStream || data instanceof RandomAccessFile || data instanceof byte[]) {
      return len;
    }

    if (len == 0) {
      return -1;
    } else {
      return len;
    }
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    if (data instanceof InputStream) {
      InputStream stream = (InputStream) data;
      sink.writeAll(Okio.source(stream));
    } else if (data instanceof RandomAccessFile) {
      RandomAccessFile file = (RandomAccessFile) data;
      sink.write(Okio.source(Channels.newInputStream(file.getChannel())), len);
    } else if (data instanceof byte[]) {
      byte[] bytes = (byte[]) data;
      sink.write(bytes, 0, len);
    } else {
      sink.writeUtf8(data.toString());
    }
  }
}
