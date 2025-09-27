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

package io.minio.admin;

import io.minio.errors.MinioException;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Cryptography to read and write encrypted MinIO Admin payload.
 *
 * <pre>
 * Encrypted Message Format:
 *
 * |    41 bytes HEADER      |
 * |-------------------------|
 * | 16 KiB encrypted chunk  |
 * |     + 16 bytes TAG      |
 * |-------------------------|
 * |          ....           |
 * |-------------------------|
 * | ~16 KiB encrypted chunk |
 * |     + 16 bytes TAG      |
 * |-------------------------|
 *
 * HEADER:
 *
 * | 32 bytes salt  |
 * |----------------|
 * | 1 byte AEAD ID |
 * |----------------|
 * | 8 bytes NONCE  |
 * |----------------|
 * </pre>
 */
public class Crypto {
  private static final int TAG_LENGTH = 16;
  private static final int CHUNK_SIZE = 16 * 1024;
  private static final int MAX_CHUNK_SIZE = CHUNK_SIZE + TAG_LENGTH;
  private static final int SALT_LENGTH = 32;
  private static final int NONCE_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private static byte[] random(int length) {
    byte[] data = new byte[length];
    RANDOM.nextBytes(data);
    return data;
  }

  private static byte[] appendBytes(byte[]... args) {
    if (args.length == 1) {
      return args[0];
    }

    int length = 0;
    for (byte[] arg : args) {
      length += arg.length;
    }

    ByteBuffer buf = ByteBuffer.allocate(length);
    for (byte[] arg : args) {
      buf.put(arg);
    }
    return buf.array();
  }

  private static int[] readFully(InputStream inputStream, byte[] buf, boolean raiseEof)
      throws EOFException, IOException {
    int totalBytesRead = 0;
    int eof = 0;
    int offset = 0;

    while (totalBytesRead < buf.length) {
      int bytesToRead = buf.length - totalBytesRead;
      int bytesRead = inputStream.read(buf, offset, bytesToRead);
      if (bytesRead < 0) {
        if (raiseEof) throw new EOFException("EOF occurred");
        eof = 1;
        break;
      }
      totalBytesRead += bytesRead;
      offset += bytesRead;
    }

    return new int[] {totalBytesRead, eof};
  }

  private static AEADCipher getEncryptDecryptCipher(
      boolean encryptFlag, int aeadId, byte[] key, byte[] paddedNonce) {
    AEADCipher cipher = null;
    switch (aeadId) {
      case 0:
        cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
        break;
      case 1:
        cipher = new ChaCha20Poly1305();
        break;
      default:
        throw new IllegalArgumentException("unknown AEAD ID " + aeadId);
    }
    cipher.init(encryptFlag, new AEADParameters(new KeyParameter(key), 128, paddedNonce));
    return cipher;
  }

  private static AEADCipher getEncryptCipher(int aeadId, byte[] key, byte[] paddedNonce) {
    return getEncryptDecryptCipher(true, aeadId, key, paddedNonce);
  }

  private static AEADCipher getDecryptCipher(int aeadId, byte[] key, byte[] paddedNonce) {
    return getEncryptDecryptCipher(false, aeadId, key, paddedNonce);
  }

  private static byte[] generateKey(byte[] secret, byte[] salt) {
    Argon2BytesGenerator generator = new Argon2BytesGenerator();
    generator.init(
        new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(65536)
            .withParallelism(4)
            .withIterations(1)
            .build());

    byte[] key = new byte[32];
    generator.generateBytes(secret, key);
    return key;
  }

  private static byte[] generateEncryptDecryptAdditionalData(
      boolean encryptFlag, int aeadId, byte[] key, byte[] paddedNonce) throws MinioException {
    try {
      AEADCipher cipher = getEncryptCipher(aeadId, key, paddedNonce);
      int outputLength = cipher.getMac().length;
      byte[] additionalData = new byte[outputLength];
      cipher.doFinal(additionalData, 0);
      return appendBytes(new byte[] {0}, additionalData);
    } catch (InvalidCipherTextException e) {
      throw new MinioException(e);
    }
  }

  private static byte[] generateEncryptAdditionalData(int aeadId, byte[] key, byte[] paddedNonce)
      throws MinioException {
    return generateEncryptDecryptAdditionalData(true, aeadId, key, paddedNonce);
  }

  private static byte[] generateDecryptAdditionalData(int aeadId, byte[] key, byte[] paddedNonce)
      throws MinioException {
    return generateEncryptDecryptAdditionalData(false, aeadId, key, paddedNonce);
  }

  private static byte[] markAsLast(byte[] additionalData) {
    additionalData[0] = (byte) 0x80;
    return additionalData;
  }

  private static byte[] updateNonceId(byte[] nonce, int idx) {
    byte[] idxLittleEndian = new byte[4];
    idxLittleEndian[0] = (byte) (idx & 0xFF);
    idxLittleEndian[1] = (byte) ((idx >> 8) & 0xFF);
    idxLittleEndian[2] = (byte) ((idx >> 16) & 0xFF);
    idxLittleEndian[3] = (byte) ((idx >> 24) & 0xFF);
    return appendBytes(nonce, idxLittleEndian);
  }

  /** Encrypt data payload. */
  public static byte[] encrypt(byte[] payload, String password) throws MinioException {
    byte[] nonce = random(NONCE_LENGTH);
    byte[] salt = random(SALT_LENGTH);

    byte[] key = generateKey(password.getBytes(StandardCharsets.UTF_8), salt);
    byte[] aeadId = new byte[] {0x0};
    byte[] paddedNonce = appendBytes(nonce, new byte[] {0, 0, 0, 0});
    byte[] additionalData = generateEncryptAdditionalData(aeadId[0], key, paddedNonce);

    byte[] result = appendBytes(salt, aeadId, nonce);

    int from = 0;
    boolean done = false;
    for (int nonceId = 1; !done; nonceId++) {
      int to = from + CHUNK_SIZE;
      if (to > payload.length) {
        additionalData = markAsLast(additionalData);
        to = payload.length;
        done = true;
      }
      byte[] chunk = Arrays.copyOfRange(payload, from, to);
      paddedNonce = updateNonceId(nonce, nonceId);

      AEADCipher cipher = getEncryptCipher(aeadId[0], key, paddedNonce);
      cipher.processAADBytes(additionalData, 0, additionalData.length);

      int outputLength = cipher.getOutputSize(chunk.length);
      byte[] encryptedData = new byte[outputLength];
      int outputOffset = cipher.processBytes(chunk, 0, chunk.length, encryptedData, 0);
      try {
        cipher.doFinal(encryptedData, outputOffset);
      } catch (InvalidCipherTextException e) {
        throw new MinioException(e);
      }

      result = appendBytes(result, encryptedData);

      from = to;
    }

    return result;
  }

  /** Reader crypts MinioAdmin API response. */
  public static class DecryptReader {
    private InputStream inputStream;
    private byte[] secret;
    private byte[] salt = new byte[32];
    private byte[] aeadId = new byte[1];
    private byte[] nonce = new byte[8];
    private byte[] key = null;
    private byte[] additionalData = null;
    private int count = 0;
    private byte[] chunk = new byte[MAX_CHUNK_SIZE];
    private byte[] oneByte = null;
    private boolean eof = false;

    public DecryptReader(InputStream inputStream, byte[] secret) throws MinioException {
      this.inputStream = inputStream;
      this.secret = secret;
      try {
        readFully(this.inputStream, this.salt, true);
        readFully(this.inputStream, this.aeadId, true);
        readFully(this.inputStream, this.nonce, true);
      } catch (EOFException e) {
        throw new MinioException(e);
      } catch (IOException e) {
        throw new MinioException(e);
      }
      this.key = generateKey(this.secret, this.salt);
      byte[] paddedNonce = appendBytes(this.nonce, new byte[] {0, 0, 0, 0});
      this.additionalData = generateDecryptAdditionalData(this.aeadId[0], this.key, paddedNonce);
    }

    private byte[] decrypt(byte[] encryptedData, boolean lastChunk) throws MinioException {
      this.count++;
      if (lastChunk) {
        this.additionalData = markAsLast(this.additionalData);
      }
      byte[] paddedNonce = updateNonceId(this.nonce, this.count);
      AEADCipher cipher = getDecryptCipher(this.aeadId[0], this.key, paddedNonce);
      cipher.processAADBytes(this.additionalData, 0, this.additionalData.length);
      int outputLength = cipher.getOutputSize(encryptedData.length);
      byte[] decryptedData = new byte[outputLength];
      int outputOffset =
          cipher.processBytes(encryptedData, 0, encryptedData.length, decryptedData, 0);
      try {
        cipher.doFinal(decryptedData, outputOffset);
      } catch (InvalidCipherTextException e) {
        throw new MinioException(e);
      }
      return decryptedData;
    }

    /** Read a chunk at least one byte more than chunk size. */
    private byte[] readChunk() throws EOFException, IOException {
      if (this.eof) {
        return new byte[] {};
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      if (this.oneByte != null) {
        baos.write(this.oneByte);
      }

      int[] result = readFully(this.inputStream, this.chunk, false);
      int bytesRead = result[0];
      this.eof = result[1] == 1;
      if (bytesRead == this.chunk.length) {
        if (this.oneByte != null) {
          bytesRead--;
          this.oneByte[0] = this.chunk[bytesRead];
        } else if (!this.eof) {
          this.oneByte = new byte[] {0};
          result = readFully(this.inputStream, this.oneByte, false);
          this.eof = result[1] == 1;
          if (this.eof) this.oneByte = null;
        }
      }

      baos.write(this.chunk, 0, bytesRead);
      return baos.toByteArray();
    }

    public byte[] readAllBytes() throws MinioException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while (!this.eof) {
        try {
          byte[] payload = this.readChunk();
          baos.write(this.decrypt(payload, this.eof));
        } catch (EOFException e) {
          throw new MinioException(e);
        } catch (IOException e) {
          throw new MinioException(e);
        }
      }
      return baos.toByteArray();
    }
  }

  /** Decrypt data stream. */
  public static byte[] decrypt(InputStream inputStream, String password) throws MinioException {
    DecryptReader reader =
        new DecryptReader(inputStream, password.getBytes(StandardCharsets.UTF_8));
    return reader.readAllBytes();
  }
}
