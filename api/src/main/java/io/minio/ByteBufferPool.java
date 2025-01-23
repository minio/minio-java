/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2025 MinIO, Inc.
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** Pool of {@link ByteBuffer} to be used in parallel part uploads. */
public class ByteBufferPool {
  private final BlockingQueue<ByteBuffer> pool;
  long bufferSize;

  /** Creates given capacity pool of ByteBuffer with buffer size. */
  public ByteBufferPool(int capacity, long bufferSize) {
    this.pool = new ArrayBlockingQueue<>(capacity);
    this.bufferSize = bufferSize;
    // Optionally pre-fill
    for (int i = 0; i < capacity - 1; i++) {
      if (!pool.offer(new ByteBuffer(bufferSize))) {
        throw new RuntimeException("unable to allocate byte buffer; this should not happen");
      }
    }
  }

  /**
   * Retrieves and removes the head of this pool, or returns new ByteBuffer if this pool is empty.
   */
  public ByteBuffer take() {
    ByteBuffer buffer = pool.poll(); // non-blocking
    return (buffer != null) ? buffer : new ByteBuffer(bufferSize);
  }

  /** Inserts the specified buffer into this pool, ignore if the pool is full. */
  public void put(ByteBuffer buffer) {
    try {
      buffer.reset();
      if (!pool.offer(buffer)) return; // ignore if pool is full
    } catch (MinioException e) {
      throw new RuntimeException(e);
    }
  }
}
