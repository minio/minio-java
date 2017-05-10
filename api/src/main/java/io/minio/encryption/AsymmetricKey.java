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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class AsymmetricKey implements MasterKey {

  private static KeyPair aSymMasterKeyPair = null;
  private final static String cipherModeString = "RSA";
  private static Cipher decryptionCipher = null;
  private static Cipher encryptionCipher = null;

  /*
   * Set Asymmetric key pair for RSA algorithm.
   */
  AsymmetricKey(KeyPair aSymmetricMasterKeyPair)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    // create master key from given byte array
    aSymMasterKeyPair = aSymmetricMasterKeyPair;

    // Create encryption cipher
    encryptionCipher = Cipher.getInstance(cipherModeString);

    // init encryption cipher with mode and public key
    encryptionCipher.init(Cipher.ENCRYPT_MODE, aSymMasterKeyPair.getPublic());

    // Get a cipher
    decryptionCipher = Cipher.getInstance(cipherModeString);

    // init decryption cipher with mode and private key
    decryptionCipher.init(Cipher.DECRYPT_MODE, aSymMasterKeyPair.getPrivate());
  }

  /*
   * Encrypts plain data encryption keys using symmetric master key. Returns encrypted data encryption key.
   */
  public byte[] encrypt(byte[] plainTextKeys) throws IllegalBlockSizeException, BadPaddingException {

    if (encryptionCipher != null) {
      // encrypt plain text data key and return
      return encryptionCipher.doFinal(plainTextKeys);
    }

    // else return null
    return null;
  }

  /*
   * Decrypts ciphered data encryption keys using symmetric master key. Returns plain data encryption key.
   */
  public byte[] decrypt(byte[] cipherTextKeys) throws NoSuchAlgorithmException, NoSuchPaddingException,
      InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    if (decryptionCipher != null) {
      // decrypt cipher text data key
      return decryptionCipher.doFinal(cipherTextKeys);
    }

    // else return null
    return null;
  }

}
