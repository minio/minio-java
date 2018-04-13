package io.minio;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

class MinioRequestBody extends RequestBody {
  private final String contentType;
  private final Object data;
  private final int len;

  MinioRequestBody(final String contentType, final Object data, final int len) {
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
      sink.write(Okio.source(stream), len);
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
