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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class SymmetricKey {

	private static SecretKey symMasterKey = null;
	private final static String cipherModeString = "AES/ECB/PKCS5Padding";
	private static Cipher decryptionCipher = null;
	private static Cipher encryptionCipher = null;
	
	/* Private Constructor to prevent 
     * the instantiation of this class */
	private SymmetricKey(){
	      
	}
	 
	/*
	 * Set Symmetric master key for AES algorithm from the given byte array. 
	 */
	static void setupSymmetricKeys(SecretKey symmetricMasterKey) 
			throws NoSuchAlgorithmException, NoSuchPaddingException, 
			InvalidKeyException, InvalidAlgorithmParameterException{
		// create master key from given byte array
		symMasterKey = symmetricMasterKey;
 		
		// Create encryption cipher
		encryptionCipher = Cipher.getInstance(cipherModeString);
		
		// init cipher with mode and master key
		encryptionCipher.init(Cipher.ENCRYPT_MODE, symMasterKey);
		
		// Get a cipher
		decryptionCipher = Cipher.getInstance(cipherModeString);

		// init cipher with mode and master key
		decryptionCipher.init(Cipher.DECRYPT_MODE, symMasterKey);
	}
	
	/*
	 * Encrypts plain data encryption keys using symmetric master key. Returns encrypted 
	 * data encryption key.
	 */
	static byte[] encryptDataKeys(byte[] plainTextKeys) throws 
	IllegalBlockSizeException, BadPaddingException {
				
		if(encryptionCipher != null){
			// encrypt plain text data key and return
	        return encryptionCipher.doFinal(plainTextKeys);	        
		}
		
		// else return null
		return null;
	}

	/*
	 * Decrypts ciphered data encryption keys using symmetric master key. Returns plain 
	 * data encryption key.
	 */
	static byte[] decryptDataKeys(byte[] cipherTextKeys) throws 
	NoSuchAlgorithmException, NoSuchPaddingException, 
	InvalidKeyException, IllegalBlockSizeException, 
	BadPaddingException {
		
		if(decryptionCipher != null){
			// decrypt cipher text data key
	        return decryptionCipher.doFinal(cipherTextKeys);	        
		}
		
		// else return null
		return null;		
	}

}

	