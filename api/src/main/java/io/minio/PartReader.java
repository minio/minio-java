/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2021 MinIO, Inc.
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

import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;

/** PartReader reads part data from file or input stream sequentially and returns PartSource. */
class PartReader {
  private static final long CHUNK_SIZE = Integer.MAX_VALUE;

  private byte[] buf16k = new byte[16384]; // 16KiB buffer for optimization.

  private RandomAccessFile file;
  private InputStream stream;

  private long objectSize;
  private long partSize;
  private int partCount;

  private int partNumber;
  private long totalDataRead;

  private ByteBufferStream[] buffers;
  private byte[] oneByte = null;
  boolean eof;

  private PartReader(long objectSize, long partSize, int partCount) {
    this.objectSize = objectSize;
    this.partSize = partSize;
    this.partCount = partCount;

    long bufferCount = partSize / CHUNK_SIZE;
    if ((partSize - (bufferCount * CHUNK_SIZE)) > 0) bufferCount++;
    if (bufferCount == 0) bufferCount++;

    this.buffers = new ByteBufferStream[(int) bufferCount];
  }

  public PartReader(@Nonnull RandomAccessFile file, long objectSize, long partSize, int partCount) {
    this(objectSize, partSize, partCount);
    this.file = Objects.requireNonNull(file, "file must not be null");
    if (this.objectSize < 0) throw new IllegalArgumentException("object size must be provided");
  }

  public PartReader(@Nonnull InputStream stream, long objectSize, long partSize, int partCount) {
    this(objectSize, partSize, partCount);
    this.stream = Objects.requireNonNull(stream, "stream must not be null");
    for (int i = 0; i < this.buffers.length; i++) this.buffers[i] = new ByteBufferStream();
  }

  private long readStreamChunk(ByteBufferStream buffer, long size, MessageDigest sha256)
      throws IOException {
    long totalBytesRead = 0;

    if (this.oneByte != null) {
      buffer.write(this.oneByte);
      sha256.update(this.oneByte);
      totalBytesRead++;
      this.oneByte = null;
    }

    while (totalBytesRead < size) {
      long bytesToRead = size - totalBytesRead;
      if (bytesToRead > this.buf16k.length) bytesToRead = this.buf16k.length;
      int bytesRead = this.stream.read(this.buf16k, 0, (int) bytesToRead);
      this.eof = (bytesRead < 0);
      if (this.eof) {
        if (this.objectSize < 0) break;
        throw new IOException("unexpected EOF");
      }
      buffer.write(this.buf16k, 0, bytesRead);
      sha256.update(this.buf16k, 0, bytesRead);
      totalBytesRead += bytesRead;
    }

    return totalBytesRead;
  }

  private long readStream(long size, MessageDigest sha256) throws IOException {
    long count = size / CHUNK_SIZE;
    long lastChunkSize = size - (count * CHUNK_SIZE);
    if (lastChunkSize > 0) {
      count++;
    } else {
      lastChunkSize = CHUNK_SIZE;
    }

    long totalBytesRead = 0;
    for (int i = 0; i < buffers.length; i++) buffers[i].reset();
    for (long i = 1; i <= count && !this.eof; i++) {
      long chunkSize = (i != count) ? CHUNK_SIZE : lastChunkSize;
      long bytesRead = this.readStreamChunk(buffers[(int) (i - 1)], chunkSize, sha256);
      totalBytesRead += bytesRead;
    }

    if (!this.eof && this.objectSize < 0) {
      this.oneByte = new byte[1];
      this.eof = this.stream.read(this.oneByte) < 0;
    }

    return totalBytesRead;
  }

  private long readFile(long size, MessageDigest sha256) throws IOException {
    long position = this.file.getFilePointer();
    long totalBytesRead = 0;

    while (totalBytesRead < size) {
      long bytesToRead = size - totalBytesRead;
      if (bytesToRead > this.buf16k.length) bytesToRead = this.buf16k.length;
      int bytesRead = this.file.read(this.buf16k, 0, (int) bytesToRead);
      if (bytesRead < 0) throw new IOException("unexpected EOF");
      sha256.update(this.buf16k, 0, bytesRead);
      totalBytesRead += bytesRead;
    }

    this.file.seek(position);
    return totalBytesRead;
  }

  private long read(long size, MessageDigest sha256) throws IOException {
    return (this.file != null) ? readFile(size, sha256) : readStream(size, sha256);
  }

  public PartSource getPart() throws NoSuchAlgorithmException, IOException {
    if (this.partNumber == this.partCount) return null;

    this.partNumber++;

    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

    long partSize = this.partSize;
    if (this.partNumber == this.partCount) partSize = this.objectSize - this.totalDataRead;
    long bytesRead = this.read(partSize, sha256);
    this.totalDataRead += bytesRead;
    if (this.objectSize < 0 && this.eof) this.partCount = this.partNumber;

    String sha256Hash = BaseEncoding.base16().encode(sha256.digest()).toLowerCase(Locale.US);

    if (this.file != null) {
      return new PartSource(this.partNumber, this.file, bytesRead, null, sha256Hash);
    }

    return new PartSource(this.partNumber, this.buffers, bytesRead, null, sha256Hash);
  }

  public int partCount() {
    return this.partCount;
  }
}
