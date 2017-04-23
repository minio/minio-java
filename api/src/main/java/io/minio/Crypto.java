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

package io.minio;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

class Crypto {

  /**
   * Decrypts an encrypted input stream using secret key and IV.
   * 
   * @param encryptedInputStream
   *          Encrypted {@link InputStream}
   * @param contentKey
   *          {@link SecretKey} for decryption
   * @param iv
   *          IV for decryption
   * 
   * @return {@link CipherInputStream}
   * 
   * @throws InvalidKeyException
   *           upon invalid keys
   * @throws InvalidAlgorithmParameterException
   *           upon invalid algorithm
   * @throws NoSuchAlgorithmException
   *           upon requested algorithm was not found
   * @throws NoSuchPaddingException
   *           upon incorrect padding
   * @throws IllegalBlockSizeException
   *           upon incorrect block size
   * @throws BadPaddingException
   *           upon wrong padding
   */
  public static CipherInputStream decrypt(InputStream encryptedInputStream, SecretKey contentKey,
      String transformation, byte[] iv) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
      IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {

    // Get the cipher
    Cipher cipher = Cipher.getInstance(transformation);

    // init cipher with mode and data encryption key
    cipher.init(Cipher.DECRYPT_MODE, contentKey, new IvParameterSpec(iv));

    // create cipherinputstream with encrypted stream and new initialized cipher
    return new CipherInputStream(encryptedInputStream, cipher);
  }

  /**
   * Decrypts cipher text byte array using RSA Private key. Returns plain data byte array.
   * 
   * @param cipherText
   *          cipher text data to be decrypted
   * 
   * @return byte array with the plain text data
   * 
   * @throws BadPaddingException
   *           upon wrong padding
   * @throws IllegalBlockSizeException
   *           upon incorrect block size
   * @throws NoSuchAlgorithmException
   *           upon requested algorithm was not found during signature calculation
   * @throws BadPaddingException
   *           upon wrong padding
   * @throws NoSuchPaddingException
   *           upon incorrect padding
   */
  public static byte[] decrypt(byte[] cipherText, Key key, String transformation) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    // Create encryption cipher
    Cipher cipher = Cipher.getInstance(transformation);

    // init encryption cipher with mode and public key
    cipher.init(Cipher.DECRYPT_MODE, key);

    // decrypt cipher text data
    return cipher.doFinal(cipherText);
  }

  /**
   * Encrypts an plain input stream using secret key and IV.
   * 
   * @param plainInputStream
   *          Plain {@link InputStream}
   * @param contentKey
   *          {@link SecretKey} for encryption
   * @param iv
   *          IV for encryption
   * 
   * @return {@link CipherInputStream}
   * 
   * @throws InvalidKeyException
   *           upon invalid keys
   * @throws InvalidAlgorithmParameterException
   *           upon invalid algorithm
   * @throws NoSuchAlgorithmException
   *           upon requested algorithm was not found
   * @throws NoSuchPaddingException
   *           upon incorrect padding
   * @throws IllegalBlockSizeException
   *           upon incorrect block size
   * @throws BadPaddingException
   *           upon wrong padding
   * @throws IOException
   *           upon IO error
   */
  public static CipherInputStream encrypt(InputStream plainInputStream, SecretKey contentKey,
      String transformation, byte[] iv) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
      IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {

    // Get the cipher
    Cipher cipher = Cipher.getInstance(transformation);

    // init cipher with mode and data encryption key
    cipher.init(Cipher.ENCRYPT_MODE, contentKey, new IvParameterSpec(iv));

    // create cipherinputstream with plain stream and new initialized cipher
    return new CipherInputStream(plainInputStream, cipher);
  }

  /**
   * Encrypts plain text byte array using RSA Public key. Returns encrypted data byte array.
   * 
   * @param plainText
   *          plain text data to be encrypted
   * 
   * @return byte array with the encrypted data
   * 
   * @throws BadPaddingException
   *           upon wrong padding
   * @throws IllegalBlockSizeException
   *           upon incorrect block size
   * 
   * @throws NoSuchPaddingException
   *           In case of wrong padding scheme
   * @throws NoSuchAlgorithmException
   *           In case of wrong algorithm
   * @throws InvalidKeyException
   *           In case of wrong key
   * 
   */
  public static byte[] encrypt(byte[] plainText, Key key, String transformation)
      throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
      InvalidKeyException {

    // Create encryption cipher
    Cipher cipher = Cipher.getInstance(transformation);

    // init encryption cipher with mode and public key
    cipher.init(Cipher.ENCRYPT_MODE, key);

    // encrypt plain text data and return
    return cipher.doFinal(plainText);
  }
}
