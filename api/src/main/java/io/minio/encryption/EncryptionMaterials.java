/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2017 Minio, Inc.
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

package io.minio.encryption;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import io.minio.ObjectStat;

public class EncryptionMaterials {

  private final static String cipherModeString = "AES/CBC/PKCS5Padding";
  private MasterKey masterKey = null;

  public EncryptionMaterials(SecretKey symmetricMasterKey)
      throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {

    // Setup symmetric keys and related ciphers
    masterKey = new SymmetricKey(symmetricMasterKey);

  }

  public EncryptionMaterials(KeyPair aSymmetricKeyPair)
      throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {

    // Setup asymmetric keys and related ciphers
    masterKey = new AsymmetricKey(aSymmetricKeyPair);

  }

  public CipherInputStream decryptInputStream(InputStream encryptedInputStream, SecretKey contentKey, byte[] iv)
      throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
      BadPaddingException, InvalidAlgorithmParameterException {

    // Get the cipher
    Cipher inputStrDecryptionCipher = Cipher.getInstance(cipherModeString);

    // init cipher with mode and data encryption key
    inputStrDecryptionCipher.init(Cipher.DECRYPT_MODE, contentKey, new IvParameterSpec(iv));

    // create cipherinputstream with encrypted stream and new initialized cipher
    CipherInputStream cipherInputStream = new CipherInputStream(encryptedInputStream, inputStrDecryptionCipher);

    return cipherInputStream;
  }

  public CipherInputStream encryptInputStream(InputStream plainInputStream, SecretKey contentKey, byte[] iv)
      throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
      BadPaddingException, InvalidAlgorithmParameterException, IOException {

    // Get the cipher
    Cipher inputStrEncryptionCipher = Cipher.getInstance(cipherModeString);

    // init cipher with mode and data encryption key
    inputStrEncryptionCipher.init(Cipher.ENCRYPT_MODE, contentKey, new IvParameterSpec(iv));

    // create cipherinputstream with plain stream and new initialized cipher
    CipherInputStream cipherInputStream = new CipherInputStream(plainInputStream, inputStrEncryptionCipher);

    return cipherInputStream;
  }

  public byte[] encryptContentKeys(byte[] plainTextKeys) throws IllegalBlockSizeException, BadPaddingException {
    // Encrypt the encrypted data key using master key
    byte[] encryptedDataKey = masterKey.encrypt(plainTextKeys);
    return encryptedDataKey;
  }

  public byte[] decryptContentKeys(byte[] encryptedDataKey) throws IllegalBlockSizeException, BadPaddingException,
      InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
    // Decrypt the encrypted data key using master key
    byte[] plainDataKey = masterKey.decrypt(encryptedDataKey);
    return plainDataKey;
  }
}
