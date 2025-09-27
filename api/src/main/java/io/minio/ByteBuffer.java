/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2025 MinIO, Inc.
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

import io.minio.errors.MinioException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** {@link OutputStream} compatible byte buffer to store bytes to maximum 5GiB in size. */
public class ByteBuffer extends OutputStream {
  private static class Buffer extends ByteArrayOutputStream {
    public InputStream inputStream() {
      return (count > 0) ? new ByteArrayInputStream(buf, 0, count) : null;
    }
  }

  private static final int CHUNK_SIZE = Integer.MAX_VALUE;
  private final long size;
  private final List<Buffer> buffers = new ArrayList<>();
  private int index = 0;
  private long writtenBytes = 0;
  private boolean isClosed = false;

  /** Creates ByteBuffer for given size. */
  public ByteBuffer(long size) {
    if (size > ObjectWriteArgs.MAX_PART_SIZE) {
      throw new IllegalArgumentException("Size cannot exceed 5GiB");
    }
    this.size = size;
  }

  private void updateIndex() {
    if (buffers.isEmpty()) {
      index = 0;
      buffers.add(new Buffer());
    } else if (writtenBytes >= (long) (index + 1) * CHUNK_SIZE) {
      index++;
      if (index > buffers.size() - 1) buffers.add(new Buffer());
    }
  }

  /** Writes the specified byte to this buffer. */
  @Override
  public void write(int b) throws IOException {
    if (isClosed) throw new IOException("Stream is closed");
    if (writtenBytes >= size) throw new IOException("Exceeded total size limit");
    updateIndex();
    buffers.get(index).write(b);
    writtenBytes++;
  }

  /** Writes len bytes from the specified byte array starting at offset off to this buffer. */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (isClosed) throw new IOException("Stream is closed");
    if (len > (size - writtenBytes)) throw new IOException("Exceeded total size limit");
    int remaining = len;
    while (remaining > 0) {
      updateIndex();
      Buffer currentBuffer = buffers.get(index);
      int bytesToWrite = Math.min(remaining, CHUNK_SIZE - currentBuffer.size());
      currentBuffer.write(b, off + (len - remaining), bytesToWrite);
      writtenBytes += bytesToWrite;
      remaining -= bytesToWrite;
    }
  }

  /** Writes b.length bytes from the specified byte array to this buffer. */
  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  /** Returns the current length of bytes written to this buffer. */
  public long length() {
    return writtenBytes;
  }

  /** Returns the size of this buffer. */
  public long size() {
    return size;
  }

  /** Resets this buffer to freshen up for reuse. */
  public void reset() throws MinioException {
    if (isClosed) throw new MinioException("Cannot reset a closed stream");
    writtenBytes = 0;
    index = 0;
    for (Buffer buffer : buffers) buffer.reset();
  }

  /** Closes this buffer and releases any system resources associated with the buffer. */
  public void close() throws IOException {
    if (!isClosed) {
      isClosed = true;
      buffers.clear();
    }
  }

  /** Returns this buffer as {@link InputStream}. */
  public InputStream inputStream() {
    List<InputStream> streams = new ArrayList<>();
    for (Buffer buffer : buffers) {
      InputStream stream = buffer.inputStream();
      if (stream != null) streams.add(stream);
    }
    switch (streams.size()) {
      case 0:
        return new ByteArrayInputStream(Utils.EMPTY_BYTE_ARRAY);
      case 1:
        return streams.get(0);
      default:
        return new SequenceInputStream(Collections.enumeration(streams));
    }
  }
}
