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

import com.google.common.base.Preconditions;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * MinIO <a href="https://github.com/minio/madmin-go/blob/main/encrypt.go#L38">encrypts/decrypts</a>
 * any payloads containing access or secret keys. The encryption scheme used is from a library
 * called <a href="https://github.com/secure-io/sio-go">sio-go</a>. The library encrypts/decrypts
 * data in chunks, which allows it handle large amounts of data, without sacrificing security. In
 * addition, MinIO itself formats the data into specific format, to allow encryption/decryption
 * between client and server.
 */
public class Crypto {
  private static final byte ARGON2ID_AES_GCM = 0;
  private static final int NONCE_LENGTH = 8;
  private static final int SALT_LENGTH = 32;
  private static final int BUFFER_SIZE = 16384; // 16 KiB
  private static final SecureRandom RANDOM = new SecureRandom();

  private static byte[] random(int length) {
    byte[] data = new byte[length];
    RANDOM.nextBytes(data);
    return data;
  }

  /**
   * Generates a 256-bit Argon2ID key
   *
   * @param password Password to derive unique key from
   * @param salt Salt to be used for hash generation
   * @return 256-bit key that can be used for encryption/decryption
   */
  private static byte[] generateKey(byte[] password, byte[] salt) {
    byte[] key = new byte[32];
    Argon2BytesGenerator generator = new Argon2BytesGenerator();
    Argon2Parameters params =
        new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(65536) // 64 KiB
            .withParallelism(4)
            .withIterations(1)
            .build();
    generator.init(params);
    generator.generateBytes(password, key);
    return key;
  }

  /**
   * Generates the additional data which is used per chunk.
   *
   * @param key Encryption key
   * @param paddedNonce 12-byte NONCE
   * @return Additional data (128-bit) that can be used along side encryption/decryption
   * @throws InvalidCipherTextException
   */
  private static byte[] generateAdditionalData(byte[] key, byte[] paddedNonce)
      throws InvalidCipherTextException {
    GCMModeCipher cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
    cipher.init(true, new AEADParameters(new KeyParameter(key), 128, paddedNonce));
    int outputLength = cipher.getMac().length;
    byte[] additionalData = new byte[outputLength];
    cipher.doFinal(additionalData, 0);
    byte[] finalAdditionalData = new byte[outputLength + 1];
    System.arraycopy(additionalData, 0, finalAdditionalData, 1, additionalData.length);
    finalAdditionalData[0] = (byte) 0x80;
    return finalAdditionalData;
  }

  /**
   * Encrypts data in {@link Crypto#BUFFER_SIZE} chunks using AES-GCM using a 256-bit Argon2ID key.
   * The format returned is compatible with MinIO servers and clients. Header format: salt [string
   * 32] | aead id [byte 1] | nonce [byte 8] | encrypted_data [byte len(encrypted_data)] To see the
   * original implementation in Go, check out the <a
   * href="https://github.com/minio/madmin-go/blob/main/encrypt.go#L38">madmin-go library</a>.
   *
   * @param password Plaintext password
   * @param data The data to encrypt
   * @return Encrypted data
   * @throws UnsupportedEncodingException
   * @throws InvalidCipherTextException
   */
  public static byte[] encrypt(String password, byte[] data)
      throws UnsupportedEncodingException, InvalidCipherTextException {
    Preconditions.checkArgument(
        data.length <= BUFFER_SIZE,
        "Cannot encrypt data of length %d that is greater than block size %d, currently only n = 1"
            + " blocks (chunks) are supported.",
        data.length,
        BUFFER_SIZE);

    byte[] nonce = random(NONCE_LENGTH);

    /**
     * NONCE is expected to be 12-bytes for AES-GCM. We add 4 empty bytes, which we increment in
     * Little Endian format per chunk
     */
    byte[] paddedNonce = new byte[NONCE_LENGTH + 4];
    System.arraycopy(nonce, 0, paddedNonce, 0, nonce.length);
    byte[] salt = random(SALT_LENGTH);

    byte[] key = generateKey(password.getBytes("utf-8"), salt);
    byte[] additionalData = generateAdditionalData(key, paddedNonce);

    /** Increment IV (nonce) by 1 as we used it for generating a tag for additional data. */
    paddedNonce[8] = 1;

    GCMModeCipher cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
    cipher.init(true, new AEADParameters(new KeyParameter(key), 128, paddedNonce, additionalData));
    int outputLength = cipher.getOutputSize(data.length);
    byte[] encryptedData = new byte[outputLength];
    int outputOffset = cipher.processBytes(data, 0, data.length, encryptedData, 0);
    cipher.doFinal(encryptedData, outputOffset);
    ByteBuffer payload = ByteBuffer.allocate(1 + salt.length + nonce.length + outputLength);
    payload.put(salt);
    payload.put(ARGON2ID_AES_GCM);
    payload.put(nonce);
    payload.put(encryptedData);
    return payload.array();
  }

  /**
   * Decrypts data in {@link Crypto#BUFFER_SIZE} chunks using AES-GCM using a 256-bit Argon2ID key.
   *
   * @param password Plaintext password
   * @param payload Data to decrypt, including headers
   * @return Decrypted data
   * @throws UnsupportedEncodingException
   * @throws InvalidCipherTextException
   */
  public static byte[] decrypt(String password, byte[] payload)
      throws UnsupportedEncodingException, InvalidCipherTextException {
    ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
    byte[] nonce = new byte[NONCE_LENGTH];
    byte[] salt = new byte[SALT_LENGTH];
    payloadBuffer.get(salt);
    /** One byte to determine which encryption format to use. We only allow for Argon2ID AES-GCM. */
    payloadBuffer.get();
    payloadBuffer.get(nonce);
    byte[] encryptedData = new byte[payloadBuffer.remaining()];
    payloadBuffer.get(encryptedData);

    byte[] key = generateKey(password.getBytes("UTF-8"), salt);

    /**
     * Nonce for AES-GCM is expected to be 12 bytes, but we keep it at 8-bytes to allow up to
     * 4-bytes (int32) chunks
     */
    byte[] paddedNonce = new byte[NONCE_LENGTH + 4];
    System.arraycopy(nonce, 0, paddedNonce, 0, nonce.length);
    byte[] additionalData = generateAdditionalData(key, paddedNonce);

    /** Increment IV (nonce) by 1 as we used it for generating a tag for additional data. */
    paddedNonce[8] = 1;

    GCMModeCipher cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
    cipher.init(false, new AEADParameters(new KeyParameter(key), 128, paddedNonce, additionalData));
    int outputLength = cipher.getOutputSize(encryptedData.length);
    byte[] decryptedData = new byte[outputLength];
    int outputOffset =
        cipher.processBytes(encryptedData, 0, encryptedData.length, decryptedData, 0);
    cipher.doFinal(decryptedData, outputOffset);
    return ByteBuffer.wrap(decryptedData).array();
  }
}
