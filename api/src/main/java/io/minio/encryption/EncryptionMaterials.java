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
	private final byte AESBlockSize = 16;
	private Map<String, String> headerMap = new HashMap<>();
	
	public EncryptionMaterials(SecretKey symmetricMasterKey) throws 
	InvalidKeyException, NoSuchAlgorithmException, 
	NoSuchPaddingException, InvalidAlgorithmParameterException{
		
		// Setup symmetric keys and related ciphers
		SymmetricKey.setupSymmetricKeys(symmetricMasterKey);
		
	}
	
	public CipherInputStream decryptInputStream(InputStream encryptedInputStream, ObjectStat metadata) throws 
	InvalidKeyException, NoSuchAlgorithmException, 
	NoSuchPaddingException, IllegalBlockSizeException, 
	BadPaddingException, InvalidAlgorithmParameterException {
		// Get the encrypted key from response metadata
		byte[] encryptedDataKey = Base64.decodeBase64(metadata.key().getBytes());
		
		// Get the iv from the response metadata
		byte[] iv = Base64.decodeBase64(metadata.iv().getBytes());
		
		// Decrypt the encrypted data key using master key
		byte[] plainDataKey = SymmetricKey.decryptDataKeys(encryptedDataKey);
		
		// Create secret key from byte array
		SecretKey dataEncryptionKey = new SecretKeySpec(plainDataKey, 0, plainDataKey.length, "AES");
		
		// Get the cipher
		Cipher inputStrDecryptionCipher = Cipher.getInstance(cipherModeString);
		
		// init cipher with mode and data encryption key
		inputStrDecryptionCipher.init(Cipher.DECRYPT_MODE, dataEncryptionKey, new IvParameterSpec(iv));

		// create cipherinputstream with encrypted stream and new initialized cipher
		CipherInputStream cipherInputStream = new CipherInputStream(encryptedInputStream, inputStrDecryptionCipher);
		
		return cipherInputStream;		
	}
	
	public CipherInputStream encryptInputStream(InputStream plainInputStream) throws 
	InvalidKeyException, NoSuchAlgorithmException, 
	NoSuchPaddingException, IllegalBlockSizeException, 
	BadPaddingException, InvalidAlgorithmParameterException, IOException{
		
		// Generate symmetric 128 bit AES key.
	    KeyGenerator symKeyGenerator = KeyGenerator.getInstance("AES");
	    symKeyGenerator.init(128);
	    SecretKey dataEncryptionKey = symKeyGenerator.generateKey();
	    
	    // Generate an iv to be used for encryption
 		SecureRandom ivSeed = new SecureRandom();
 		byte[] iv = new byte[AESBlockSize];
 		ivSeed.nextBytes(iv);
 		
	    // Get the cipher
 		Cipher inputStrEncryptionCipher = Cipher.getInstance(cipherModeString);
 		
 		// init cipher with mode and data encryption key
 		inputStrEncryptionCipher.init(Cipher.ENCRYPT_MODE, dataEncryptionKey, new IvParameterSpec(iv));
 		
 		// create cipherinputstream with plain stream and new initialized cipher
 		CipherInputStream cipherInputStream = new CipherInputStream(plainInputStream, inputStrEncryptionCipher);
 		
 		// Encrypt the data encryption key and iv and set headers
 		byte[] plainDataKey = dataEncryptionKey.getEncoded();
		
		// encrypt the plain data key using master key
		byte[] encryptedDataKey = SymmetricKey.encryptDataKeys(plainDataKey);  

	    // Prepare data to be put to the object header
		String encDataKey = new String(Base64.encodeBase64(encryptedDataKey));
		String ivString = new String(Base64.encodeBase64(iv));
		
		// Set the headermap with encryption related metadata
		headerMap.put("x-amz-meta-x-amz-key", encDataKey);
	    headerMap.put("x-amz-meta-x-amz-iv", ivString);
	    // matdesc is unused
	    headerMap.put("x-amz-meta-x-amz-matdesc", "");
	    
		return cipherInputStream;
	}
	
	public Map<String, String> getHeaderMap(){
		return headerMap;
	}
}
