/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2017 MinIO, Inc.
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

import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;

class ChunkedInputStream extends InputStream {
  // Chunk size in chunked upload for PUT object is 64KiB
  private static final int CHUNK_SIZE = 64 * 1024;
  // Each chunk body should be like
  // CHUNK_SIZE_IN_HEX_STRING + ";chunk-signature=" + SIGNATURE + "\r\n" + CHUNK_DATA + "\r\n"
  // e.g. for 64KiB of chunk
  // 10000;chunk-signature=ad80c730a21e5b8d04586a2213dd63b9a0e99e0e2307b0ade35a65485a288648\r\n<65536-bytes>\r\n
  // From the above value, a full chunk size 65626 is by
  // len(hex string of 64KiB) = 5 (+)
  // len(;chunk-signature=ad80c730a21e5b8d04586a2213dd63b9a0e99e0e2307b0ade35a65485a288648\r\n) = 83
  // (+)
  // <65536 bytes> = 65536 (+)
  // len(\r\n) = 2
  private static final int FULL_CHUNK_LEN = 65626;
  // Data in last chunk might be less than 64KiB.  In this case, ignoring variable length of chunk
  // body components, remaining length is constant
  private static final int CHUNK_SIGNATURE_METADATA_LEN = 85;
  // As final additional chunk must be like
  // 0;chunk-signature=b6c6ea8a5354eaf15b3cb7646744f4275b71ea724fed81ceb9323e279d449df9\r\n\r\n
  // the length is 86
  private static final int FINAL_ADDITIONAL_CHUNK_LEN = 1 + CHUNK_SIGNATURE_METADATA_LEN;

  private InputStream inputStream;
  private int streamSize;
  private int length;
  private ZonedDateTime date;
  private String region;
  private String secretKey;
  private String prevSignature;

  // Counter denotes how many bytes read from given input stream.
  private int streamBytesRead = 0;
  // Initialize to avoid findbugs warning.
  private byte[] chunkBody = new byte[0];
  private int chunkPos = 0;
  private boolean isEof = false;
  // Counter denotes how many bytes the consumer read from this stream.
  private int bytesRead = 0;

  /** Create new ChunkedInputStream for given input stream. */
  public ChunkedInputStream(
      InputStream inputStream,
      int streamSize,
      ZonedDateTime date,
      String region,
      String secretKey,
      String seedSignature)
      throws IOException {
    this.inputStream = inputStream;
    this.streamSize = streamSize;
    this.date = date;
    this.region = region;
    this.secretKey = secretKey;
    this.prevSignature = seedSignature;

    // Calculate stream length.
    int fullChunks = this.streamSize / CHUNK_SIZE;
    this.length = fullChunks * FULL_CHUNK_LEN;
    int lastChunkLen = this.streamSize % CHUNK_SIZE;
    if (lastChunkLen > 0) {
      this.length += Integer.toHexString(lastChunkLen).getBytes(StandardCharsets.UTF_8).length;
      this.length += CHUNK_SIGNATURE_METADATA_LEN;
      this.length += lastChunkLen;
    }
    this.length += FINAL_ADDITIONAL_CHUNK_LEN;
  }

  private int readData(byte[] buf) throws IOException {
    if (this.isEof) {
      return -1;
    }

    int pos = 0;
    int len = buf.length;
    int totalBytesRead = 0;
    int bytesRead = 0;
    while (totalBytesRead < buf.length) {
      bytesRead = inputStream.read(buf, pos, len);
      if (bytesRead < 0) {
        this.isEof = true;
        break;
      }

      totalBytesRead += bytesRead;
      pos += bytesRead;
      len = buf.length - totalBytesRead;
    }

    return totalBytesRead;
  }

  private void createChunkBody(byte[] chunk)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, InsufficientDataException,
          InternalException {
    String chunkSha256 = Digest.sha256Hash(chunk, chunk.length);
    String signature =
        Signer.getChunkSignature(
            chunkSha256, this.date, this.region, this.secretKey, this.prevSignature);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    // Add metadata.
    os.write(Integer.toHexString(chunk.length).getBytes(StandardCharsets.UTF_8));
    os.write(";chunk-signature=".getBytes(StandardCharsets.UTF_8));
    os.write(signature.getBytes(StandardCharsets.UTF_8));
    os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    // Add chunk data.
    os.write(chunk, 0, chunk.length);
    os.write("\r\n".getBytes(StandardCharsets.UTF_8));

    this.chunkBody = os.toByteArray();
    this.chunkPos = 0;
    this.prevSignature = signature;
  }

  private int readChunk(int chunkSize)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, InsufficientDataException,
          InternalException {
    byte[] chunk = new byte[chunkSize];
    int len = readData(chunk);
    if (len < 0) {
      return -1;
    }

    if (len != chunkSize) {
      throw new InsufficientDataException(
          "Insufficient data.  read = " + len + " expected = " + chunkSize);
    }

    createChunkBody(chunk);
    return this.chunkBody.length;
  }

  /** read single byte from chunk body. */
  public int read() throws IOException {
    if (this.bytesRead == this.length) {
      // All chunks and final additional chunk are read.
      // This means we have reached EOF.
      return -1;
    }

    try {
      // Read a chunk from given input stream when
      // it is first chunk or all bytes in chunk body is read
      if (this.streamBytesRead == 0 || this.chunkPos == this.chunkBody.length) {
        // Check if there are data available to read from given input stream.
        if (this.streamBytesRead != this.streamSize) {
          // Send all data chunks.
          int chunkSize = CHUNK_SIZE;
          if (this.streamBytesRead + chunkSize > this.streamSize) {
            chunkSize = this.streamSize - this.streamBytesRead;
          }

          if (readChunk(chunkSize) < 0) {
            return -1;
          }

          this.streamBytesRead += chunkSize;
        } else {
          // Send final additional chunk to complete chunk upload.
          byte[] chunk = new byte[0];
          createChunkBody(chunk);
        }
      }

      this.bytesRead++;
      // Value must be between 0 to 255.
      int value = this.chunkBody[this.chunkPos] & 0xFF;
      this.chunkPos++;
      return value;
    } catch (NoSuchAlgorithmException
        | InvalidKeyException
        | InsufficientDataException
        | InternalException e) {
      throw new IOException(e.getCause());
    }
  }

  /** return length of data ChunkedInputStream supposes to produce. */
  public int length() {
    return this.length;
  }
}
