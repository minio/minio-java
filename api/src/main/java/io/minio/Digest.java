/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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

import com.google.common.io.BaseEncoding;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Various global static functions used. */
public class Digest {
  /** Private constructor. */
  private Digest() {}

  /** Returns SHA-256 hash of given string. */
  public static String sha256Hash(String string) throws NoSuchAlgorithmException {
    byte[] data = string.getBytes(StandardCharsets.UTF_8);
    MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
    sha256Digest.update((byte[]) data, 0, data.length);
    return BaseEncoding.base16().encode(sha256Digest.digest()).toLowerCase(Locale.US);
  }

  /**
   * Returns SHA-256 hash of given data and it's length.
   *
   * @param data must be {@link RandomAccessFile}, {@link BufferedInputStream} or byte array.
   * @param len length of data to be read for hash calculation.
   */
  public static String sha256Hash(Object data, int len)
      throws NoSuchAlgorithmException, IOException, InsufficientDataException, InternalException {
    MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

    if (data instanceof BufferedInputStream || data instanceof RandomAccessFile) {
      updateDigests(data, len, sha256Digest, null);
    } else if (data instanceof byte[]) {
      sha256Digest.update((byte[]) data, 0, len);
    } else {
      throw new InternalException(
          "Unknown data source to calculate sha256 hash. This should not happen, "
              + "please report this issue at https://github.com/minio/minio-java/issues");
    }

    return BaseEncoding.base16().encode(sha256Digest.digest()).toLowerCase(Locale.US);
  }

  /**
   * Returns SHA-256 and MD5 hashes of given data and it's length.
   *
   * @param data must be {@link RandomAccessFile}, {@link BufferedInputStream} or byte array.
   * @param len length of data to be read for hash calculation.
   */
  public static String[] sha256Md5Hashes(Object data, int len)
      throws NoSuchAlgorithmException, IOException, InsufficientDataException, InternalException {
    MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
    MessageDigest md5Digest = MessageDigest.getInstance("MD5");

    if (data instanceof BufferedInputStream || data instanceof RandomAccessFile) {
      updateDigests(data, len, sha256Digest, md5Digest);
    } else if (data instanceof byte[]) {
      sha256Digest.update((byte[]) data, 0, len);
      md5Digest.update((byte[]) data, 0, len);
    } else {
      throw new InternalException(
          "Unknown data source to calculate sha256 hash. This should not happen, "
              + "please report this issue at https://github.com/minio/minio-java/issues");
    }

    return new String[] {
      BaseEncoding.base16().encode(sha256Digest.digest()).toLowerCase(Locale.US),
      BaseEncoding.base64().encode(md5Digest.digest())
    };
  }

  /**
   * Returns MD5 hash of given data and it's length.
   *
   * @param data must be {@link RandomAccessFile}, {@link BufferedInputStream} or byte array.
   * @param len length of data to be read for hash calculation.
   */
  public static String md5Hash(Object data, int len)
      throws NoSuchAlgorithmException, IOException, InsufficientDataException, InternalException {
    MessageDigest md5Digest = MessageDigest.getInstance("MD5");

    if (data instanceof BufferedInputStream || data instanceof RandomAccessFile) {
      updateDigests(data, len, null, md5Digest);
    } else if (data instanceof byte[]) {
      md5Digest.update((byte[]) data, 0, len);
    } else {
      throw new InternalException(
          "Unknown data source to calculate sha256 hash. This should not happen, "
              + "please report this issue at https://github.com/minio/minio-java/issues");
    }

    return BaseEncoding.base64().encode(md5Digest.digest());
  }

  /** Updated MessageDigest with bytes read from file and stream. */
  private static int updateDigests(
      Object inputStream, int len, MessageDigest sha256Digest, MessageDigest md5Digest)
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
        throw new InsufficientDataException(
            "Insufficient data.  bytes read " + totalBytesRead + " expected " + len);
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
