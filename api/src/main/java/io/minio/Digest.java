/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.io.BaseEncoding;

import io.minio.errors.InsufficientDataException;


/**
 * Various global static functions used.
 */
class Digest {
  /**
   * Private constructor.
   */
  private Digest() {}


  /**
   * Returns SHA-256 hash of given string.
   */
  public static String sha256Hash(String string) throws NoSuchAlgorithmException {
    return sha256Hash(string.getBytes(StandardCharsets.UTF_8));
  }


  /**
   * Returns SHA-256 hash of given byte array.
   */
  public static String sha256Hash(byte[] data) throws NoSuchAlgorithmException {
    return sha256Hash(data, data.length);
  }


  /**
   * Returns SHA-256 hash string of given byte array and it's length.
   */
  public static String sha256Hash(byte[] data, int length) throws NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

    messageDigest.update(data, 0, length);

    return BaseEncoding.base16().encode(messageDigest.digest()).toLowerCase();
  }


  /**
   * Returns SHA-256 of given input stream and it's length.
   *
   * @param inputStream  Input stream whose type is either {@link RandomAccessFile} or {@link BufferedInputStream}.
   * @param len          Length of Input stream.
   */
  public static String sha256Hash(Object inputStream, int len)
    throws NoSuchAlgorithmException, IOException, InsufficientDataException {
    MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
    updateDigests(inputStream, len, sha256Digest, null);
    return BaseEncoding.base16().encode(sha256Digest.digest()).toLowerCase();
  }

  /**
   * Returns MD5 hash of given string.
   */
  public static String md5Hash(String string) throws NoSuchAlgorithmException {
    return md5Hash(string.getBytes(StandardCharsets.UTF_8));
  }


  /**
   * Returns MD5 hash of given byte array.
   */
  public static String md5Hash(byte[] data) throws NoSuchAlgorithmException {
    return md5Hash(data, data.length);
  }


  /**
   * Returns MD5 hash of given byte array and it's length.
   */
  public static String md5Hash(byte[] data, int length) throws NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance("MD5");

    messageDigest.update(data, 0, length);

    return BaseEncoding.base64().encode(messageDigest.digest());
  }


  /**
   * Returns MD5 hash of given input stream and it's length.
   *
   * @param inputStream  Input stream whose type is either {@link RandomAccessFile} or {@link BufferedInputStream}.
   * @param len          Length of Input stream.
   */
  public static String md5Hash(Object inputStream, int len)
    throws NoSuchAlgorithmException, IOException, InsufficientDataException {
    MessageDigest md5Digest = MessageDigest.getInstance("MD5");
    updateDigests(inputStream, len, null, md5Digest);
    return BaseEncoding.base64().encode(md5Digest.digest());
  }

  /**
   * Updated MessageDigest with bytes read from file and stream.
   */
  private static int updateDigests(Object inputStream, int len, MessageDigest sha256Digest, MessageDigest md5Digest)
    throws IOException, InsufficientDataException {
    RandomAccessFile file = null;
    BufferedInputStream stream = null;
    if (inputStream instanceof RandomAccessFile) {
      file = (RandomAccessFile) inputStream;
    } else if (inputStream instanceof BufferedInputStream) {
      stream = (BufferedInputStream) inputStream;
    }

    // hold current position of file/stream to reset back to this position.
    long pos = 0;
    if (file != null) {
      pos = file.getFilePointer();
    } else {
      stream.mark(len);
    }

    // 16KiB buffer for optimization
    byte[] buf = new byte[16384];
    int bytesToRead = buf.length;
    int bytesRead = 0;
    int totalBytesRead = 0;
    while (totalBytesRead < len) {
      if ((len - totalBytesRead) < bytesToRead) {
        bytesToRead = len - totalBytesRead;
      }

      if (file != null) {
        bytesRead = file.read(buf, 0, bytesToRead);
      } else {
        bytesRead = stream.read(buf, 0, bytesToRead);
      }

      if (bytesRead < 0) {
        // reached EOF 
        throw new InsufficientDataException("Insufficient data.  bytes read " + totalBytesRead + " expected "
                                            + len);
      }

      if (bytesRead > 0) {
        if (sha256Digest != null) {
          sha256Digest.update(buf, 0, bytesRead);
        }

        if (md5Digest != null) {
          md5Digest.update(buf, 0, bytesRead);
        }

        totalBytesRead += bytesRead;
      }
    }

    // reset back to saved position.
    if (file != null) {
      file.seek(pos);
    } else {
      stream.reset();
    }

    return totalBytesRead;
  }
}
