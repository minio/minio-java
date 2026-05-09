/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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

import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Collection of checksum algorithms. */
public class Checksum {
  /** MD5 hash of zero length byte array. */
  public static final String ZERO_MD5_HASH = "1B2M2Y8AsgTpgAmY7PhCfg==";
  /** SHA-256 hash of zero length byte array. */
  public static final String ZERO_SHA256_HASH =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

  private Checksum() {}

  /** Encodes the specified bytes to Base64 string. */
  public static String base64String(byte[] sum) {
    return Base64.getEncoder().encodeToString(sum);
  }

  /** Decodes the specified base64 encoded string to bytes. */
  public static byte[] base64StringToSum(String sum) {
    return Base64.getDecoder().decode(sum);
  }

  /** Encodes the specified bytes to Base16 string. */
  public static String hexString(byte[] sum) {
    StringBuilder builder = new StringBuilder();
    for (byte b : sum) builder.append(String.format("%02x", b));
    return builder.toString();
  }

  /** Decodes the specified Base16 encoded string to bytes. */
  public static byte[] hexStringToSum(String sum) {
    byte[] data = new byte[sum.length() / 2];
    for (int i = 0; i < sum.length(); i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(sum.charAt(i), 16) << 4) + Character.digit(sum.charAt(i + 1), 16));
    }
    return data;
  }

  /** Creates hasher map for the specified algorithms. */
  public static Map<Algorithm, Hasher> newHasherMap(Algorithm[] algorithms) throws MinioException {
    Map<Algorithm, Hasher> hashers = null;
    if (algorithms != null) {
      for (Checksum.Algorithm algorithm : algorithms) {
        if (algorithm != null) {
          if (hashers == null) hashers = new HashMap<>();
          if (!hashers.containsKey(algorithm)) {
            hashers.put(algorithm, algorithm.hasher());
          }
        }
      }
    }
    return (hashers == null || hashers.size() == 0) ? null : hashers;
  }

  /** Updates each hasher using the specified array of bytes, ending at the specified length. */
  public static void update(Map<Algorithm, Hasher> hashers, byte[] data, int length) {
    if (hashers == null || hashers.size() == 0) return;
    for (Map.Entry<Algorithm, Hasher> entry : hashers.entrySet()) {
      entry.getValue().update(data, 0, length);
    }
  }

  private static void update(
      Map<Algorithm, Hasher> hashers, ByteBuffer buffer, RandomAccessFile file, Long size)
      throws MinioException {
    if (hashers == null || hashers.size() == 0) return;

    InputStream stream = null;
    if (buffer != null) {
      stream = buffer.inputStream();
      size = buffer.length();
    }

    byte[] buf16k = new byte[16384];
    long bytesRead = 0;
    while (bytesRead != size) {
      try {
        int length = (int) Math.min(size - bytesRead, buf16k.length);
        int n = file != null ? file.read(buf16k, 0, length) : stream.read(buf16k, 0, length);
        if (n < 0) throw new MinioException("unexpected EOF");
        if (n != 0) {
          bytesRead += n;
          update(hashers, buf16k, n);
        }
      } catch (IOException e) {
        throw new MinioException(e);
      }
    }
  }

  /** Updates each hasher using the specified file, ending at the specified size. */
  public static void update(Map<Algorithm, Hasher> hashers, RandomAccessFile file, long size)
      throws MinioException {
    update(hashers, null, file, size);
  }

  /** Updates each hasher using the specified byte buffer. */
  public static void update(Map<Algorithm, Hasher> hashers, ByteBuffer buffer)
      throws MinioException {
    update(hashers, buffer, null, null);
  }

  /** Makes checksum headers for given hashers. */
  public static Http.Headers makeHeaders(
      Map<Algorithm, Hasher> hashers, boolean addContentSha256, boolean addSha256Checksum) {
    if (hashers == null) return null;

    Http.Headers checksumHeaders = new Http.Headers();
    for (Map.Entry<Algorithm, Hasher> entry : hashers.entrySet()) {
      byte[] sum = entry.getValue().sum();
      if (entry.getKey() == Checksum.Algorithm.SHA256) {
        if (addContentSha256) {
          checksumHeaders.put(Http.Headers.X_AMZ_CONTENT_SHA256, Checksum.hexString(sum));
        }
        if (!addSha256Checksum) continue;
      }
      checksumHeaders.put("x-amz-sdk-checksum-algorithm", entry.getKey().toString());
      checksumHeaders.put(entry.getKey().header(), Checksum.base64String(sum));
    }

    return checksumHeaders;
  }

  /** Algorithm type. */
  public static enum Type {
    COMPOSITE,
    FULL_OBJECT;
  }

  /** Checksum algorithm. */
  public static enum Algorithm {
    CRC32,
    CRC32C,
    CRC64NVME,
    SHA1,
    SHA256,
    MD5;

    /** Converts this algorithm to string. */
    @Override
    public String toString() {
      return name().toLowerCase(Locale.US);
    }

    /** Gets HTTP header key for this algorithm. */
    public String header() {
      if (this == MD5) return Http.Headers.CONTENT_MD5;
      return "x-amz-checksum-" + name().toLowerCase(Locale.US);
    }

    /** Returns whether this algorithm supports full object checksum. */
    public boolean fullObjectSupport() {
      return this == CRC32 || this == CRC32C || this == CRC64NVME;
    }

    /** Returns whether this algorithm supports composite object checksum. */
    public boolean compositeSupport() {
      return this == CRC32 || this == CRC32C || this == SHA1 || this == SHA256;
    }

    /** Validates this algorithm for the specified type. */
    public void validate(Type type) {
      if (!(compositeSupport() && type == Type.COMPOSITE
          || fullObjectSupport() && type == Type.FULL_OBJECT)) {
        throw new IllegalArgumentException(
            "algorithm " + name() + " does not support " + type + " type");
      }
    }

    /** Gets hasher for this algorithm. */
    public Hasher hasher() throws MinioException {
      if (this == CRC32) return new CRC32();
      if (this == CRC32C) return new CRC32C();
      if (this == CRC64NVME) return new CRC64NVME();
      if (this == SHA1) return new SHA1();
      if (this == SHA256) return new SHA256();
      if (this == MD5) return new MD5();
      return null;
    }
  }

  /** Checksum hasher. */
  public static interface Hasher {
    /** Updates len bytes from the specified byte array starting at offset off to this hasher. */
    public abstract void update(byte[] b, int off, int len);

    /** Completes the hash computation by performing final operations such as padding. */
    public abstract byte[] sum();

    /** Resets the hasher for further use. */
    public abstract void reset();
  }

  /** CRC32 {@link Hasher}. */
  public static class CRC32 implements Hasher {
    private java.util.zip.CRC32 hasher;

    public CRC32() {
      hasher = new java.util.zip.CRC32();
    }

    @Override
    public void update(byte[] b, int off, int len) {
      hasher.update(b, off, len);
    }

    @Override
    public byte[] sum() {
      int value = (int) hasher.getValue();
      return new byte[] {
        (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value
      };
    }

    @Override
    public void reset() {
      hasher.reset();
    }

    @Override
    public String toString() {
      return "CRC32{" + hexString(sum()) + "}";
    }
  }

  /** CRC32C {@link Hasher}. */
  public static class CRC32C implements java.util.zip.Checksum, Hasher {
    private static final int[] CRC32C_TABLE = new int[256];
    private int crc = 0xFFFFFFFF;

    static {
      for (int i = 0; i < 256; i++) {
        int crc = i;
        for (int j = 0; j < 8; j++) {
          crc = (crc >>> 1) ^ ((crc & 1) != 0 ? 0x82F63B78 : 0);
        }
        CRC32C_TABLE[i] = crc;
      }
    }

    @Override
    public void update(int b) {
      crc = CRC32C_TABLE[(crc ^ b) & 0xFF] ^ (crc >>> 8);
    }

    @Override
    public void update(byte[] b, int off, int len) {
      for (int i = off; i < off + len; i++) {
        update(b[i]);
      }
    }

    @Override
    public long getValue() {
      return (crc ^ 0xFFFFFFFFL) & 0xFFFFFFFFL;
    }

    @Override
    public void reset() {
      crc = 0xFFFFFFFF;
    }

    @Override
    public byte[] sum() {
      int value = (int) this.getValue();
      return new byte[] {
        (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value
      };
    }

    @Override
    public String toString() {
      return "CRC32C{" + hexString(sum()) + "}";
    }
  }

  /** CRC64NVME {@link Hasher} copied from https://github.com/minio/crc64nvme. */
  public static class CRC64NVME implements java.util.zip.Checksum, Hasher {
    private static final long[] CRC64_TABLE = new long[256];
    private static final long[][] SLICING8_TABLE_NVME = new long[8][256];

    static {
      long polynomial = 0x9A6C9329AC4BC9B5L;
      for (int i = 0; i < 256; i++) {
        long crc = i;
        for (int j = 0; j < 8; j++) {
          if ((crc & 1) == 1) {
            crc = (crc >>> 1) ^ polynomial;
          } else {
            crc >>>= 1;
          }
        }
        CRC64_TABLE[i] = crc;
      }

      SLICING8_TABLE_NVME[0] = CRC64_TABLE;
      for (int i = 0; i < 256; i++) {
        long crc = CRC64_TABLE[i];
        for (int j = 1; j < 8; j++) {
          crc = CRC64_TABLE[(int) crc & 0xFF] ^ (crc >>> 8);
          SLICING8_TABLE_NVME[j][i] = crc;
        }
      }
    }

    private long crc = 0;

    public CRC64NVME() {}

    @Override
    public void update(byte[] p, int off, int len) {
      java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.wrap(p, off, len);
      byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      int offset = byteBuffer.position();

      crc = ~crc;
      while (p.length >= 64 && (p.length - offset) > 8) {
        long value = byteBuffer.getLong();
        crc ^= value;
        crc =
            SLICING8_TABLE_NVME[7][(int) (crc & 0xFF)]
                ^ SLICING8_TABLE_NVME[6][(int) ((crc >>> 8) & 0xFF)]
                ^ SLICING8_TABLE_NVME[5][(int) ((crc >>> 16) & 0xFF)]
                ^ SLICING8_TABLE_NVME[4][(int) ((crc >>> 24) & 0xFF)]
                ^ SLICING8_TABLE_NVME[3][(int) ((crc >>> 32) & 0xFF)]
                ^ SLICING8_TABLE_NVME[2][(int) ((crc >>> 40) & 0xFF)]
                ^ SLICING8_TABLE_NVME[1][(int) ((crc >>> 48) & 0xFF)]
                ^ SLICING8_TABLE_NVME[0][(int) (crc >>> 56)];
        offset = byteBuffer.position();
      }

      for (; offset < len; offset++) {
        crc = CRC64_TABLE[(int) ((crc ^ (long) p[offset]) & 0xFF)] ^ (crc >>> 8);
      }

      crc = ~crc;
    }

    @Override
    public void update(int b) {
      update(new byte[] {(byte) b}, 0, 1);
    }

    @Override
    public long getValue() {
      return crc;
    }

    @Override
    public void reset() {
      crc = 0;
    }

    @Override
    public byte[] sum() {
      long value = this.getValue();
      return new byte[] {
        (byte) (value >>> 56),
        (byte) (value >>> 48),
        (byte) (value >>> 40),
        (byte) (value >>> 32),
        (byte) (value >>> 24),
        (byte) (value >>> 16),
        (byte) (value >>> 8),
        (byte) value
      };
    }

    @Override
    public String toString() {
      return "CRC64NVME{" + hexString(sum()) + "}";
    }
  }

  /** SHA1 {@link Hasher}. */
  public static class SHA1 implements Hasher {
    MessageDigest hasher;

    public SHA1() throws MinioException {
      try {
        this.hasher = MessageDigest.getInstance("SHA-1");
      } catch (NoSuchAlgorithmException e) {
        throw new MinioException(e);
      }
    }

    public void update(byte[] b, int off, int len) {
      hasher.update(b, off, len);
    }

    public void update(byte[] b) {
      hasher.update(b);
    }

    public byte[] sum() {
      return hasher.digest();
    }

    public void reset() {
      hasher.reset();
    }

    @Override
    public String toString() {
      return "SHA1{" + hexString(sum()) + "}";
    }
  }

  /** SHA256 {@link Hasher}. */
  public static class SHA256 implements Hasher {
    MessageDigest hasher;

    public SHA256() throws MinioException {
      try {
        this.hasher = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
        throw new MinioException(e);
      }
    }

    public void update(byte[] b, int off, int len) {
      hasher.update(b, off, len);
    }

    public void update(byte[] b) {
      hasher.update(b);
    }

    public byte[] sum() {
      return hasher.digest();
    }

    public void reset() {
      hasher.reset();
    }

    @Override
    public String toString() {
      return "SHA256{" + hexString(sum()) + "}";
    }

    public static byte[] sum(byte[] b, int off, int len) throws MinioException {
      SHA256 sha256 = new SHA256();
      sha256.update(b, off, len);
      return sha256.sum();
    }

    public static byte[] sum(byte[] b) throws MinioException {
      return sum(b, 0, b.length);
    }

    public static byte[] sum(String value) throws MinioException {
      return sum(value.getBytes(StandardCharsets.UTF_8));
    }
  }

  /** MD5 {@link Hasher}. */
  public static class MD5 implements Hasher {
    MessageDigest hasher;

    public MD5() throws MinioException {
      try {
        this.hasher = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        throw new MinioException(e);
      }
    }

    public void update(byte[] b, int off, int len) {
      hasher.update(b, off, len);
    }

    public void update(byte[] b) {
      hasher.update(b);
    }

    public byte[] sum() {
      return hasher.digest();
    }

    public void reset() {
      hasher.reset();
    }

    @Override
    public String toString() {
      return "MD5{" + base64String(sum()) + "}";
    }

    public static byte[] sum(byte[] b, int off, int len) throws MinioException {
      MD5 md5 = new MD5();
      md5.update(b, off, len);
      return md5.sum();
    }

    public static byte[] sum(byte[] b) throws MinioException {
      return sum(b, 0, b.length);
    }
  }
}
