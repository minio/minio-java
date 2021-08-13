package io.minio.security;

import com.google.common.base.Preconditions;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class EncryptionUtils {

  public static final byte argon2idAESGCM = 0x00;

  public static final int NONCE_LENGTH = 8;
  public static final int SALT_LENGTH = 32;

  public static final int BUFFER_SIZE = 1 << 14; // 0x4000, 16384

  private static final SecureRandom random = new SecureRandom();

  private static byte[] random(int length) {
    byte[] data = new byte[length];
    random.nextBytes(data);
    return data;
  }

  public static ByteBuffer encrypt(String password, byte[] data)
      throws UnsupportedEncodingException, InvalidCipherTextException {
    Preconditions.checkArgument(
        data.length <= BUFFER_SIZE,
        "Cannot encrypt data of length %d that is greater than block size %d, currently only n = 1 blocks (chunks) are supported.",
        data.length,
        BUFFER_SIZE);

    byte[] nonce = random(NONCE_LENGTH);
    byte[] paddedNonce = new byte[NONCE_LENGTH + 4];
    System.arraycopy(nonce, 0, paddedNonce, 0, nonce.length);
    byte[] salt = random(SALT_LENGTH);

    byte[] key = generateKey(password.getBytes("utf-8"), salt);
    byte[] additionalData = generateAdditionalData(key, paddedNonce);

    /** Increment IV (nonce) by 1 as we used it for generating a tag for additional data. */
    paddedNonce[8] = 1;

    GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
    cipher.init(true, new AEADParameters(new KeyParameter(key), 128, paddedNonce, additionalData));
    int outputLength = cipher.getOutputSize(data.length);
    byte[] encryptedData = new byte[outputLength];
    int outputOffset = cipher.processBytes(data, 0, data.length, encryptedData, 0);
    cipher.doFinal(encryptedData, outputOffset);
    ByteBuffer payload = ByteBuffer.allocate(1 + salt.length + nonce.length + outputLength);
    payload.put(salt);
    payload.put(argon2idAESGCM);
    payload.put(nonce);
    payload.put(encryptedData);
    return payload;
  }

  public static ByteBuffer decrypt(byte[] key, byte[] nonce, byte[] cipherText)
      throws InvalidCipherTextException {
    /**
     * Nonce for AES-GCM is expected to be 12 bytes, but we keep it at 8-bytes to allow up to
     * 4-bytes (int32) chunks
     */
    byte[] paddedNonce = new byte[NONCE_LENGTH + 4];
    System.arraycopy(nonce, 0, paddedNonce, 0, nonce.length);
    byte[] additionalData = generateAdditionalData(key, paddedNonce);

    /** Increment IV (nonce) by 1 as we used it for generating a tag for additional data. */
    paddedNonce[8] = 1;

    GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
    cipher.init(false, new AEADParameters(new KeyParameter(key), 128, paddedNonce, additionalData));
    int outputLength = cipher.getOutputSize(cipherText.length);
    byte[] decryptedData = new byte[outputLength];
    int outputOffset = cipher.processBytes(cipherText, 0, cipherText.length, decryptedData, 0);
    cipher.doFinal(decryptedData, outputOffset);
    return ByteBuffer.wrap(decryptedData);
  }

  public static ByteBuffer decrypt(String password, byte[] payload)
      throws UnsupportedEncodingException, InvalidCipherTextException {
    ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
    byte[] nonce = new byte[NONCE_LENGTH];
    byte[] salt = new byte[SALT_LENGTH];
    payloadBuffer.get(salt);
    byte encryptionAlgo = payloadBuffer.get();
    payloadBuffer.get(nonce);
    byte[] encryptedData = new byte[payloadBuffer.remaining()];
    payloadBuffer.get(encryptedData);

    byte[] key = generateKey(password.getBytes("UTF-8"), salt);

    return decrypt(key, nonce, encryptedData);
  }

  public static byte[] generateAdditionalData(byte[] key, byte[] paddedNonce)
      throws InvalidCipherTextException {
    GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
    cipher.init(true, new AEADParameters(new KeyParameter(key), 128, paddedNonce));
    int outputLength = cipher.getMac().length;
    byte[] additionalData = new byte[outputLength];
    cipher.doFinal(additionalData, 0);
    byte[] finalAdditionalData = new byte[outputLength + 1];
    System.arraycopy(additionalData, 0, finalAdditionalData, 1, additionalData.length);
    finalAdditionalData[0] = (byte) 0x80;
    return finalAdditionalData;
  }

  public static byte[] generateKey(byte[] password, byte[] salt) {
    byte[] key = new byte[32];
    Argon2BytesGenerator generator = new Argon2BytesGenerator();
    Argon2Parameters params =
        new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(64 * 1024)
            .withParallelism(4)
            .withIterations(1)
            .build();
    generator.init(params);
    generator.generateBytes(password, key);
    return key;
  }
}
