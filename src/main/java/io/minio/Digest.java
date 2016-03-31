/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

import java.nio.charset.StandardCharsets;
import java.io.BufferedInputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.io.BaseEncoding;

import io.minio.errors.InsufficientDataException;


public class Digest {
  public static String sha256Hash(String string) throws NoSuchAlgorithmException {
    return sha256Hash(string.getBytes(StandardCharsets.UTF_8));
  }


  public static String sha256Hash(byte[] data) throws NoSuchAlgorithmException {
    return sha256Hash(data, data.length);
  }


  /**
   * returns SHA-256 hash string.
   */
  public static String sha256Hash(byte[] data, int length) throws NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

    messageDigest.update(data, 0, length);

    return BaseEncoding.base16().encode(messageDigest.digest()).toLowerCase();
  }


  public static String md5Hash(String string) throws NoSuchAlgorithmException {
    return md5Hash(string.getBytes(StandardCharsets.UTF_8));
  }


  public static String md5Hash(byte[] data) throws NoSuchAlgorithmException {
    return md5Hash(data, data.length);
  }


  /**
   * returns MD5 hash string.
   */
  public static String md5Hash(byte[] data, int length) throws NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance("MD5");

    messageDigest.update(data, 0, length);

    return BaseEncoding.base64().encode(messageDigest.digest());
  }


  /**
   * returns MD5 hash string.
   */
  public static String md5Hash(Object inputStream, int len)
    throws IllegalArgumentException, NoSuchAlgorithmException, IOException, InsufficientDataException {
    RandomAccessFile file = null;
    BufferedInputStream stream = null;
    if (inputStream instanceof RandomAccessFile) {
      file = (RandomAccessFile) inputStream;
    } else if (inputStream instanceof BufferedInputStream) {
      stream = (BufferedInputStream) inputStream;
    } else {
      throw new IllegalArgumentException("unsupported input stream object");
    }

    MessageDigest md5Digest;
    md5Digest = MessageDigest.getInstance("MD5");

    // 16KiB buffer for optimization
    byte[] buf = new byte[16384];
    int bytesToRead = buf.length;
    int bytesRead;
    int totalBytesRead = 0;
    int length = len;
    long pos = 0;

    if (file != null) {
      pos = file.getFilePointer();
    } else {
      stream.mark(len);
    }

    do {
      if ((length - totalBytesRead) < bytesToRead) {
        bytesToRead = length - totalBytesRead;
      }

      if (file != null) {
        bytesRead = file.read(buf, 0, bytesToRead);
      } else {
        bytesRead = stream.read(buf, 0, bytesToRead);
      }

      if (bytesRead < 0) {
        throw new InsufficientDataException("Insufficient data.  bytes read " + totalBytesRead + " expected "
                                              + length);
      } else if (bytesRead == 0) {
        continue;
      }

      md5Digest.update(buf, 0, bytesRead);

      totalBytesRead += bytesRead;
    } while (totalBytesRead < length);

    if (file != null) {
      file.seek(pos);
    } else {
      stream.reset();
    }

    return BaseEncoding.base64().encode(md5Digest.digest());
  }


  public static String[] sha256md5Hashes(String string) throws NoSuchAlgorithmException {
    return sha256md5Hashes(string.getBytes(StandardCharsets.UTF_8));
  }


  public static String[] sha256md5Hashes(byte[] data) throws NoSuchAlgorithmException {
    return sha256md5Hashes(data, data.length);
  }


  /**
   * returns SHA-256 and MD5 hash strings.
   */
  public static String[] sha256md5Hashes(byte[] data, int length) throws NoSuchAlgorithmException {
    String[] hashes = { sha256Hash(data, length), md5Hash(data, length) };

    return hashes;
  }


  /**
   * returns SHA-256 and MD5 hash strings.
   */
  public static String[] sha256md5Hashes(Object inputStream, int len)
    throws IllegalArgumentException, NoSuchAlgorithmException, IOException, InsufficientDataException {
    RandomAccessFile file = null;
    BufferedInputStream stream = null;
    if (inputStream instanceof RandomAccessFile) {
      file = (RandomAccessFile) inputStream;
    } else if (inputStream instanceof BufferedInputStream) {
      stream = (BufferedInputStream) inputStream;
    } else {
      throw new IllegalArgumentException("unsupported input stream object");
    }

    MessageDigest sha256Digest;
    MessageDigest md5Digest;

    sha256Digest = MessageDigest.getInstance("SHA-256");
    md5Digest = MessageDigest.getInstance("MD5");

    // 16KiB buffer for optimization
    byte[] buf = new byte[16384];
    int bytesToRead = buf.length;
    int bytesRead;
    int totalBytesRead = 0;
    int length = len;
    long pos = 0;

    if (file != null) {
      pos = file.getFilePointer();
    } else {
      stream.mark(len);
    }

    do {
      if ((length - totalBytesRead) < bytesToRead) {
        bytesToRead = length - totalBytesRead;
      }

      if (file != null) {
        bytesRead = file.read(buf, 0, bytesToRead);
      } else {
        bytesRead = stream.read(buf, 0, bytesToRead);
      }

      if (bytesRead < 0) {
        throw new InsufficientDataException("Insufficient data.  bytes read " + totalBytesRead + " expected "
                                              + length);
      } else if (bytesRead == 0) {
        continue;
      }

      sha256Digest.update(buf, 0, bytesRead);
      md5Digest.update(buf, 0, bytesRead);

      totalBytesRead += bytesRead;
    } while (totalBytesRead < length);

    if (file != null) {
      file.seek(pos);
    } else {
      stream.reset();
    }

    String[] hashes = { BaseEncoding.base16().encode(sha256Digest.digest()).toLowerCase(),
                        BaseEncoding.base64().encode(md5Digest.digest()) };

    return hashes;
  }
}
