/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Multipart data reader for {@link RandomAccessFile} or {@link InputStream} to given {@link
 * ByteBuffer}.
 */
public class PartReader {
  byte[] buf16k = new byte[16384];

  RandomAccessFile file;
  InputStream stream;
  long objectSize;
  long partSize;
  int partCount;
  Map<Checksum.Algorithm, Checksum.Hasher> hashers;

  long totalBytesRead = 0;
  int partNumber = 0;
  byte[] oneByte = null;
  boolean eof;

  public PartReader(
      @Nonnull RandomAccessFile file,
      long objectSize,
      long partSize,
      int partCount,
      Checksum.Algorithm... algorithms)
      throws MinioException {
    this.file = Objects.requireNonNull(file, "file must not be null");
    if (objectSize < 0) throw new IllegalArgumentException("valid object size must be provided");
    if (partCount < 0) throw new IllegalArgumentException("part count must be provided");
    set(objectSize, partSize, partCount, algorithms);
  }

  public PartReader(
      @Nonnull InputStream stream,
      Long objectSize,
      long partSize,
      int partCount,
      Checksum.Algorithm... algorithms)
      throws MinioException {
    this.stream = Objects.requireNonNull(stream, "stream must not be null");
    if (partCount == -1) {
      objectSize = -1L;
    } else if (objectSize < 0) {
      throw new IllegalArgumentException("object size must be provided for part count");
    }
    set(objectSize, partSize, partCount, algorithms);
  }

  private void set(Long objectSize, long partSize, int partCount, Checksum.Algorithm[] algorithms)
      throws MinioException {
    if (partCount == 0) partCount = -1;
    this.objectSize = objectSize == null ? -1 : objectSize;
    this.partSize = partSize;
    this.partCount = partCount;
    this.hashers = Checksum.newHasherMap(algorithms);
  }

  private int readBuf16k(int length) throws MinioException {
    try {
      return file != null ? file.read(buf16k, 0, length) : stream.read(buf16k, 0, length);
    } catch (IOException e) {
      throw new MinioException(e);
    }
  }

  private void readOneByte() throws MinioException {
    if (eof) return;

    oneByte = new byte[] {0};
    int n = 0;

    try {
      while ((n = file != null ? file.read(oneByte) : stream.read(oneByte)) == 0) ;
    } catch (IOException e) {
      throw new MinioException(e);
    }

    if ((eof = n < 0)) oneByte = null;
  }

  public void read(ByteBuffer buffer) throws MinioException {
    if (buffer == null) throw new IllegalArgumentException("valid buffer must be provided");
    if (eof) throw new MinioException("EOF reached");
    if (partNumber == partCount) throw new MinioException("data fully read");

    long size = partSize;
    if (partCount == 1) {
      size = objectSize;
    } else if (partNumber == partCount - 1) {
      size = objectSize - totalBytesRead;
    }
    if (buffer.size() < size) {
      throw new IllegalArgumentException(
          "insufficient buffer size " + buffer.size() + " for data size " + size);
    }

    if (hashers != null) {
      for (Map.Entry<Checksum.Algorithm, Checksum.Hasher> entry : hashers.entrySet()) {
        entry.getValue().reset();
      }
    }

    long bytesRead = 0;

    if (oneByte != null) {
      try {
        buffer.write(oneByte);
      } catch (IOException e) {
        throw new MinioException(e);
      }
      if (hashers != null) Checksum.update(hashers, oneByte, oneByte.length);
      bytesRead++;
      oneByte = null;
    }

    while (bytesRead < size) {
      int n = readBuf16k((int) Math.min(size - bytesRead, this.buf16k.length));
      if ((eof = n < 0)) {
        if (partCount < 0) break;
        throw new MinioException("unexpected EOF");
      }
      try {
        buffer.write(this.buf16k, 0, n);
      } catch (IOException e) {
        throw new MinioException(e);
      }
      if (hashers != null) Checksum.update(hashers, this.buf16k, n);
      bytesRead += n;
    }

    totalBytesRead += bytesRead;
    partNumber++;
    readOneByte();
    if (eof && partCount < 0) partCount = partNumber;
  }

  public Map<Checksum.Algorithm, Checksum.Hasher> hashers() {
    return hashers;
  }

  public int partNumber() {
    return partNumber;
  }

  public int partCount() {
    return partCount;
  }
}
