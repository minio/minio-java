/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2022 MinIO, Inc.
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

import com.google.common.collect.Multimap;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Part;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class PutObjectOutputStream extends OutputStream {

  private static final int SIZE_16_K = 16 * 1024;
  private final MinioClient client;

  /** The buffer where data is stored. */
  protected byte buf[];

  /** The number of valid bytes in the buffer. */
  protected int count;

  private final int flushSize;
  private final PutObjectOutputStreamArgs args;
  private final Multimap<String, String> headers;
  private int partNumber;
  private String uploadId;
  private Part[] parts = null;
  private CompletableFuture<String> uploadIdFuture;
  private CompletableFuture<ObjectWriteResponse> futureClose;
  private final boolean asyncClose;
  private boolean closed = false;
  private boolean aborted = false;
  private final List<PutPart> putParts = new ArrayList<>();
  private Throwable exception;
  private Exception abortException;
  private int currentRequests;
  private final Object lock = new Object();

  public PutObjectOutputStream(
      MinioClient client, PutObjectOutputStreamArgs args, boolean asyncClose) {
    this.client = client;
    this.flushSize = (int) args.partSize();
    if (flushSize < PutObjectArgs.MIN_MULTIPART_SIZE) {
      throw new IllegalArgumentException("partSize too small: " + flushSize);
    }
    this.args = args;
    this.headers = client.newMultimap(args.extraHeaders());
    headers.putAll(args.genHeaders());
    this.asyncClose = asyncClose;
    if (args.partCount > 1) {
      buf = new byte[flushSize];
      initMultipartUpload();
    } else {
      buf = new byte[SIZE_16_K];
    }
  }

  private void ensureCapacity(int minCapacity) {
    int oldCapacity = buf.length;
    int minGrowth = minCapacity - oldCapacity;
    if (minGrowth > 0) {
      buf = Arrays.copyOf(buf, Math.min(Math.max(oldCapacity * 2, minCapacity), flushSize));
    }
  }

  /** Write method is not thread safe. */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    checkState();
    int remains = flushSize - count;
    if (remains > len) {
      doWrite(b, off, len);
    } else {
      doWrite(b, off, remains);
      sendPut(true);
      doWrite(b, off + remains, len - remains);
    }
  }

  private void doWrite(byte[] b, int off, int len) throws IOException {
    System.arraycopy(b, off, buf, count, len);
    count += len;
  }

  private void initMultipartUpload() {
    try {
      uploadIdFuture =
          client
              .createMultipartUploadAsync(
                  args.bucket(), args.region(), args.object(), headers, args.extraQueryParams())
              .handle(
                  (response, e) -> {
                    if (e != null) {
                      doAbort(false);
                      exception = getCause(e);
                    } else {
                      try {
                        synchronized (putParts) {
                          uploadId = response.result().uploadId();
                          if (exception != null || aborted) {
                            doAbort(true);
                          } else {
                            for (PutPart part : putParts) {
                              part.putPartObject(uploadId);
                            }
                          }
                        }
                      } catch (InvalidKeyException
                          | NoSuchAlgorithmException
                          | InsufficientDataException
                          | ServerException
                          | XmlParserException
                          | ErrorResponseException
                          | InternalException
                          | InvalidResponseException
                          | IOException e2) {
                        exception = e2;
                      }
                    }
                    return uploadId;
                  });

      parts =
          new Part[args.partCount() > 0 ? args.partCount() : ObjectWriteArgs.MAX_MULTIPART_COUNT];
    } catch (InvalidKeyException
        | NoSuchAlgorithmException
        | InsufficientDataException
        | ServerException
        | XmlParserException
        | ErrorResponseException
        | InternalException
        | InvalidResponseException
        | IOException e) {
      exception = getCause(e);
    }
  }

  public void abort() throws Exception {
    doAbort(false);
    if (abortException != null) {
      throw abortException;
    }
  }

  public boolean isAborted() {
    return aborted;
  }

  private void doAbort(boolean force) {
    if (!force && aborted) {
      // Ignore secondary calls to abort
      return;
    }
    aborted = true;
    putParts.clear();
    if (uploadId != null) {
      try {
        client.abortMultipartUpload(
            args.bucket(), args.region(), args.object(), uploadId, null, null);
      } catch (InvalidKeyException
          | NoSuchAlgorithmException
          | InsufficientDataException
          | ServerException
          | XmlParserException
          | ErrorResponseException
          | InternalException
          | InvalidResponseException
          | IOException e) {
        abortException = e;
      }
    }
  }

  private void sendPut(boolean async) throws IOException {
    try {
      if (parts == null) {
        initMultipartUpload();
      }
      partNumber++;
      byte[] body;
      if (async) {
        synchronized (lock) {
          currentRequests++;
          while (args.maxParallelRequests() > 0 && currentRequests >= args.maxParallelRequests()) {
            lock.wait();
          }
        }
        body = new byte[count];
        System.arraycopy(buf, 0, body, 0, count);
      } else {
        body = buf;
      }
      PutPart part = new PutPart(partNumber, count, body);
      synchronized (putParts) {
        putParts.add(part);
        if (uploadId != null) {
          part.putPartObject(uploadId);
        }
      }
      count = 0;
    } catch (InvalidKeyException
        | NoSuchAlgorithmException
        | InsufficientDataException
        | ServerException
        | XmlParserException
        | ErrorResponseException
        | InternalException
        | InvalidResponseException
        | IOException
        | InterruptedException e) {
      doAbort(false);
      exception = e;
      throw new IOException(e);
    }
  }

  /** Write method is not thread safe. */
  @Override
  public void write(int b) throws IOException {
    checkState();
    if (flushSize == count) {
      sendPut(true);
    }
    buf[count] = (byte) b;
    count += 1;
  }

  private void checkState() throws IOException {
    if (exception != null) {
      throw new IOException(exception);
    }
    if (aborted) {
      throw new IOException("Aborted");
    }
    ensureCapacity(count + 1);
  }

  @Override
  public void close() throws IOException {
    if (exception != null || aborted) {
      buf = null;
      closed = true;
      putParts.clear();
      if (exception != null) {
        throw new IOException(exception);
      }
      throw new IOException("Aborted");
    }
    if (!closed) {
      closed = true;
      futureClose = initClose();
      buf = null;
      try {
        if (!asyncClose) {
          futureClose.get();
        }
      } catch (ExecutionException | InterruptedException e) {
        exception = getCause(e);
        throw new IOException(exception);
      }
    }
  }

  private static Throwable getCause(Throwable e) {
    if ((e instanceof ExecutionException || e instanceof CompletionException)
        && e.getCause() != null) {
      return e.getCause();
    } else {
      return e;
    }
  }

  public CompletableFuture<ObjectWriteResponse> getFutureClose() {
    if (!closed) {
      throw new IllegalArgumentException("Not closed");
    }
    return futureClose;
  }

  public Throwable getException() {
    return exception;
  }

  public Exception getAbortException() {
    return abortException;
  }

  private CompletableFuture<ObjectWriteResponse> initClose() {
    try {
      if (parts == null) {
        return new PutPart(-1, count, buf).putPartObject(null).future;
      } else {
        sendPut(false);
        return uploadIdFuture
            .thenCompose(
                s -> {
                  @SuppressWarnings("unchecked")
                  CompletableFuture<ObjectWriteResponse>[] array =
                      putParts.stream().map(part -> part.future).toArray(CompletableFuture[]::new);
                  return CompletableFuture.allOf(array);
                })
            .thenCompose(
                (v) -> {
                  CompletableFuture<ObjectWriteResponse> future = new CompletableFuture<>();
                  try {
                    if (exception != null) {
                      future.completeExceptionally(exception);
                    } else {
                      future.complete(
                          client.completeMultipartUpload(
                              args.bucket(),
                              args.region(),
                              args.object(),
                              uploadId,
                              parts,
                              null,
                              null));
                    }
                  } catch (InvalidKeyException
                      | NoSuchAlgorithmException
                      | InsufficientDataException
                      | ServerException
                      | XmlParserException
                      | ErrorResponseException
                      | InternalException
                      | InvalidResponseException
                      | IOException e) {
                    future.completeExceptionally(e);
                  }
                  return future;
                });
      }
    } catch (InvalidKeyException
        | NoSuchAlgorithmException
        | InsufficientDataException
        | ServerException
        | XmlParserException
        | ErrorResponseException
        | InternalException
        | InvalidResponseException
        | IOException e) {
      CompletableFuture<ObjectWriteResponse> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }

  private class PutPart {

    private final int partId;
    private final int length;
    private final byte[] body;
    private CompletableFuture<ObjectWriteResponse> future;

    public PutPart(int partId, int length, byte[] body) {
      this.partId = partId;
      this.length = length;
      this.body = body;
    }

    private PutPart putPartObject(String uploadId)
        throws NoSuchAlgorithmException, InsufficientDataException, IOException,
            InvalidKeyException, ServerException, XmlParserException, ErrorResponseException,
            InternalException, InvalidResponseException {
      if (future != null) {
        throw new IllegalStateException("Already called");
      }
      Multimap<String, String> extraQueryParams;
      if (uploadId == null) {
        extraQueryParams = args.extraQueryParams();
      } else {
        extraQueryParams =
            client.merge(
                args.extraQueryParams(),
                client.newMultimap("partNumber", Integer.toString(partId), "uploadId", uploadId));
      }

      future =
          client
              .sendAsync(
                  Method.PUT,
                  args.bucket(),
                  args.object(),
                  client.getRegion(args.bucket(), args.region()),
                  client.httpHeaders(headers),
                  extraQueryParams,
                  body,
                  length)
              .thenApply(
                  response -> {
                    synchronized (lock) {
                      int current = --currentRequests;
                      if (current < args.maxParallelRequests()) {
                        lock.notifyAll();
                      }
                    }
                    return new ObjectWriteResponse(
                        response.headers(),
                        args.bucket(),
                        client.region,
                        args.object(),
                        response.header("ETag").replaceAll("\"", ""),
                        response.header("x-amz-version-id"));
                  });
      if (partId > 0) {
        future =
            future.whenComplete(
                (response, e) -> {
                  if (e != null) {
                    exception = getCause(e);
                    doAbort(false);
                  } else {
                    String etag = response.etag();
                    parts[partId - 1] = new Part(partId, etag);
                  }
                });
      }
      return this;
    }
  }
}
