/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2026 MinIO, Inc.
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ChecksumTest {
  /** Size of the scratch array {@link Checksum} and {@link PartReader} hash from. */
  private static final int SCRATCH_SIZE = 16384;

  private static byte[] payload(int length) {
    byte[] data = new byte[length];
    for (int i = 0; i < length; i++) data[i] = (byte) (i * 31 + (i >> 8));
    return data;
  }

  private static String sum(Checksum.Hasher hasher) {
    return Checksum.hexString(hasher.sum());
  }

  /** Standard CRC-64/NVME check value for the string "123456789". */
  @Test
  public void testCrc64NvmeCheckVector() {
    Checksum.CRC64NVME hasher = new Checksum.CRC64NVME();
    byte[] data = "123456789".getBytes(StandardCharsets.UTF_8);
    hasher.update(data, 0, data.length);
    Assert.assertEquals("ae8b14860a799888", sum(hasher));
  }

  /**
   * Hashing n bytes held in a larger scratch array must equal hashing the same n bytes in an
   * exactly sized array.
   *
   * <p>This is how {@link PartReader} and {@link Checksum#update(Map, java.io.RandomAccessFile,
   * long)} call the hashers: a fixed 16 KiB buffer with a short final read. Bounding the hash loop
   * on the array length rather than the requested length read past the end of the data.
   */
  @Test
  public void testCrc64NvmePartiallyFilledBuffer() {
    for (int n : new int[] {0, 1, 7, 8, 9, 63, 64, 65, 100, 5000, SCRATCH_SIZE - 1, SCRATCH_SIZE}) {
      byte[] exact = payload(n);

      byte[] scratch = new byte[SCRATCH_SIZE];
      System.arraycopy(exact, 0, scratch, 0, n);
      // Fill the unused tail so stale bytes would change the digest if they were hashed.
      for (int i = n; i < scratch.length; i++) scratch[i] = (byte) 0xAB;

      Checksum.CRC64NVME exactHasher = new Checksum.CRC64NVME();
      exactHasher.update(exact, 0, n);

      Checksum.CRC64NVME scratchHasher = new Checksum.CRC64NVME();
      scratchHasher.update(scratch, 0, n);

      Assert.assertEquals(
          "scratch buffer digest differs for " + n + " bytes",
          sum(exactHasher),
          sum(scratchHasher));
    }
  }

  /** Hashing at a non-zero offset must cover exactly the requested range. */
  @Test
  public void testCrc64NvmeOffset() {
    byte[] data = payload(512);
    for (int[] range : new int[][] {{0, 512}, {64, 64}, {1, 200}, {100, 7}, {448, 64}, {3, 61}}) {
      int off = range[0];
      int len = range[1];

      byte[] slice = new byte[len];
      System.arraycopy(data, off, slice, 0, len);

      Checksum.CRC64NVME sliceHasher = new Checksum.CRC64NVME();
      sliceHasher.update(slice, 0, len);

      Checksum.CRC64NVME offsetHasher = new Checksum.CRC64NVME();
      offsetHasher.update(data, off, len);

      Assert.assertEquals(
          "digest differs for offset " + off + " length " + len,
          sum(sliceHasher),
          sum(offsetHasher));
    }
  }

  /** Feeding data in arbitrary chunks must equal hashing it in one call. */
  @Test
  public void testCrc64NvmeIncremental() {
    byte[] data = payload(70000);

    Checksum.CRC64NVME oneShot = new Checksum.CRC64NVME();
    oneShot.update(data, 0, data.length);

    Checksum.CRC64NVME incremental = new Checksum.CRC64NVME();
    int position = 0;
    for (int chunk : new int[] {1, 7, 64, 4096, 13, SCRATCH_SIZE, 8, 9, 33333}) {
      int n = Math.min(chunk, data.length - position);
      if (n <= 0) break;
      incremental.update(data, position, n);
      position += n;
    }
    if (position < data.length) incremental.update(data, position, data.length - position);

    Assert.assertEquals(sum(oneShot), sum(incremental));
  }

  /** {@link Checksum#update(Map, byte[], int)} drives the hashers the same way. */
  @Test
  public void testUpdateHasherMapWithPartiallyFilledBuffer() throws MinioException {
    for (Checksum.Algorithm algorithm :
        new Checksum.Algorithm[] {
          Checksum.Algorithm.CRC64NVME,
          Checksum.Algorithm.CRC32C,
          Checksum.Algorithm.CRC32,
          Checksum.Algorithm.SHA256,
          Checksum.Algorithm.SHA1
        }) {
      int n = 5000;
      byte[] exact = payload(n);
      byte[] scratch = new byte[SCRATCH_SIZE];
      System.arraycopy(exact, 0, scratch, 0, n);
      for (int i = n; i < scratch.length; i++) scratch[i] = (byte) 0xAB;

      Checksum.Algorithm[] algorithms = new Checksum.Algorithm[] {algorithm};

      Map<Checksum.Algorithm, Checksum.Hasher> exactHashers = Checksum.newHasherMap(algorithms);
      Checksum.update(exactHashers, exact, n);

      Map<Checksum.Algorithm, Checksum.Hasher> scratchHashers = Checksum.newHasherMap(algorithms);
      Checksum.update(scratchHashers, scratch, n);

      Assert.assertEquals(
          algorithm + " digest differs between exact and scratch buffers",
          sum(exactHashers.get(algorithm)),
          sum(scratchHashers.get(algorithm)));
    }
  }
}
