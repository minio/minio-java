/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2019 MinIO, Inc.
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

import com.google.common.io.ByteStreams;
import io.minio.errors.InternalException;
import io.minio.errors.MinioException;
import io.minio.messages.Progress;
import io.minio.messages.Stats;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/** Select object content response stream. */
public class SelectResponseStream extends InputStream {
  private InputStream inputStream;
  private CRC32 crcHasher;
  private byte[] prelude = new byte[8];
  private byte[] preludeCrc = new byte[4];
  private byte[] messageCrc = new byte[4];
  private byte[] headerValueLen = new byte[2];

  private Stats stats = null;
  private ByteArrayInputStream payloadStream = null;

  public SelectResponseStream(InputStream inputStream) {
    this.inputStream = inputStream;
    this.crcHasher = new CRC32();
  }

  private Map<String, String> decodeHeaderData(ByteArrayInputStream bais) throws IOException {
    Map<String, String> headerMap = new HashMap<>();
    int n;
    while ((n = bais.read()) >= 0) {
      int len = n & 0xFF;
      byte[] b = new byte[len];
      if (bais.read(b, 0, len) < 0) {
        throw new IOException("insufficient data");
      }
      String name = new String(b, StandardCharsets.UTF_8);

      n = bais.read();
      if (n < 0) {
        throw new IOException("insufficient data");
      }
      if ((n & 0xFF) != 7) {
        throw new IOException("header value type is not 7");
      }

      if (bais.read(headerValueLen, 0, 2) < 0) {
        throw new IOException("insufficient data");
      }
      len = (0xff & headerValueLen[0]) << 8 | (0xff & headerValueLen[1]);
      b = new byte[len];
      if (bais.read(b, 0, len) < 0) {
        throw new IOException("insufficient data");
      }
      String value = new String(b, StandardCharsets.UTF_8);

      headerMap.put(name, value);
    }

    return headerMap;
  }

  private boolean populate() throws EOFException, IOException, InternalException, MinioException {
    ByteStreams.readFully(inputStream, prelude);
    ByteStreams.readFully(inputStream, preludeCrc);
    crcHasher.reset();
    crcHasher.update(prelude);
    if ((int) crcHasher.getValue() != ByteBuffer.wrap(preludeCrc).getInt()) {
      throw new IOException(
          "prelude CRC mismatch; expected: "
              + (int) crcHasher.getValue()
              + ", got: "
              + ByteBuffer.wrap(preludeCrc).getInt());
    }

    int totalLength = ByteBuffer.wrap(prelude, 0, 4).getInt();
    byte[] data = new byte[totalLength - 12 - 4];
    ByteStreams.readFully(inputStream, data);
    ByteStreams.readFully(inputStream, messageCrc);
    crcHasher.reset();
    crcHasher.update(prelude);
    crcHasher.update(preludeCrc);
    crcHasher.update(data);
    if ((int) crcHasher.getValue() != ByteBuffer.wrap(messageCrc).getInt()) {
      throw new IOException(
          "message CRC mismatch; expected: "
              + (int) crcHasher.getValue()
              + ", got: "
              + ByteBuffer.wrap(messageCrc).getInt());
    }

    int headerLength = ByteBuffer.wrap(prelude, 4, 4).getInt();
    Map<String, String> headerMap = null;
    try {
      headerMap = decodeHeaderData(new ByteArrayInputStream(data, 0, headerLength));
    } catch (EOFException e) {
      throw new IOException("invalid header read");
    }

    if (headerMap.get(":message-type").equals("error")) {
      throw new MinioException(
          headerMap.get(":error-code") + ":" + headerMap.get(":error-message"));
    }

    if (headerMap.get(":event-type").equals("End")) {
      throw new EOFException();
    }

    int payloadLength = totalLength - headerLength - 16;

    if (headerMap.get(":event-type").equals("Cont") || payloadLength < 1) {
      return false;
    }

    ByteArrayInputStream payloadStream =
        new ByteArrayInputStream(data, headerLength, payloadLength);

    if (headerMap.get(":event-type").equals("Progress")) {
      Stats stats =
          (Stats)
              Xml.unmarshal(
                  Progress.class, new InputStreamReader(payloadStream, StandardCharsets.UTF_8));
      this.stats = stats;
      return false;
    }

    if (headerMap.get(":event-type").equals("Stats")) {
      Stats stats =
          Xml.unmarshal(Stats.class, new InputStreamReader(payloadStream, StandardCharsets.UTF_8));
      this.stats = stats;
      return false;
    }

    if (headerMap.get(":event-type").equals("Records")) {
      this.payloadStream = payloadStream;
      return true;
    }

    throw new InternalException("unknown event-type '" + headerMap.get(":event-type") + "'");
  }

  /** read single byte from payload. */
  public int read() throws IOException {
    if (payloadStream == null) {
      try {
        while (!this.populate()) {}
        ;
      } catch (EOFException e) {
        return -1;
      } catch (MinioException e) {
        throw new IOException(e);
      }
    }

    int n = payloadStream.read();
    if (n < 0) {
      payloadStream = null;
      return read();
    }

    return n;
  }

  public void close() throws IOException {
    inputStream.close();
  }

  /** get progress stats. */
  public Stats stats() {
    return this.stats;
  }
}
