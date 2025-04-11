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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.CompleteMultipartUpload;
import io.minio.messages.CompleteMultipartUploadResult;
import io.minio.messages.CopyPartResult;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.ErrorResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.Item;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListBucketResultV2;
import io.minio.messages.ListMultipartUploadsResult;
import io.minio.messages.ListObjectsResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.ListVersionsResult;
import io.minio.messages.LocationConstraint;
import io.minio.messages.NotificationRecords;
import io.minio.messages.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Core S3 API client. */
public abstract class BaseClient extends BaseS3Client {
  /////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////// Higher level ListObjects implementation ///////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////

  /** Throws encapsulated exception wrapped by {@link ExecutionException}. */
  public void throwEncapsulatedException(ExecutionException e)
      throws ErrorResponseException, InsufficientDataException, InternalException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {
    if (e == null) return;

    Throwable ex = e.getCause();

    if (ex instanceof CompletionException) {
      ex = ((CompletionException) ex).getCause();
    }

    if (ex instanceof ExecutionException) {
      ex = ((ExecutionException) ex).getCause();
    }

    try {
      throw ex;
    } catch (IllegalArgumentException
        | ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException exc) {
      throw exc;
    } catch (Throwable exc) {
      throw new RuntimeException(exc.getCause() == null ? exc : exc.getCause());
    }
  }

  private abstract class ObjectIterator implements Iterator<Result<Item>> {
    protected Result<Item> error;
    protected Iterator<? extends Item> itemIterator;
    protected Iterator<ListVersionsResult.DeleteMarker> deleteMarkerIterator;
    protected Iterator<ListObjectsResult.Prefix> prefixIterator;
    protected boolean completed = false;
    protected ListObjectsResult listObjectsResult;
    protected String lastObjectName;

    protected abstract void populateResult()
        throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException;

    protected synchronized void populate() {
      try {
        populateResult();
      } catch (ErrorResponseException
          | InsufficientDataException
          | InternalException
          | InvalidKeyException
          | InvalidResponseException
          | IOException
          | NoSuchAlgorithmException
          | ServerException
          | XmlParserException e) {
        this.error = new Result<>(e);
      }

      if (this.listObjectsResult != null) {
        this.itemIterator = this.listObjectsResult.contents().iterator();
        this.deleteMarkerIterator = this.listObjectsResult.deleteMarkers().iterator();
        this.prefixIterator = this.listObjectsResult.commonPrefixes().iterator();
      } else {
        this.itemIterator = new LinkedList<Item>().iterator();
        this.deleteMarkerIterator = new LinkedList<ListVersionsResult.DeleteMarker>().iterator();
        this.prefixIterator = new LinkedList<ListObjectsResult.Prefix>().iterator();
      }
    }

    @Override
    public boolean hasNext() {
      if (this.completed) return false;

      if (this.error == null
          && this.itemIterator == null
          && this.deleteMarkerIterator == null
          && this.prefixIterator == null) {
        populate();
      }

      if (this.error == null
          && !this.itemIterator.hasNext()
          && !this.deleteMarkerIterator.hasNext()
          && !this.prefixIterator.hasNext()
          && this.listObjectsResult.isTruncated()) {
        populate();
      }

      if (this.error != null) return true;
      if (this.itemIterator.hasNext()) return true;
      if (this.deleteMarkerIterator.hasNext()) return true;
      if (this.prefixIterator.hasNext()) return true;

      this.completed = true;
      return false;
    }

    @Override
    public Result<Item> next() {
      if (this.completed) throw new NoSuchElementException();
      if (this.error == null
          && this.itemIterator == null
          && this.deleteMarkerIterator == null
          && this.prefixIterator == null) {
        populate();
      }

      if (this.error == null
          && !this.itemIterator.hasNext()
          && !this.deleteMarkerIterator.hasNext()
          && !this.prefixIterator.hasNext()
          && this.listObjectsResult.isTruncated()) {
        populate();
      }

      if (this.error != null) {
        this.completed = true;
        return this.error;
      }

      Item item = null;
      if (this.itemIterator.hasNext()) {
        item = this.itemIterator.next();
        item.setEncodingType(this.listObjectsResult.encodingType());
        this.lastObjectName = item.objectName();
      } else if (this.deleteMarkerIterator.hasNext()) {
        item = this.deleteMarkerIterator.next();
      } else if (this.prefixIterator.hasNext()) {
        item = this.prefixIterator.next().toItem();
      }

      if (item != null) {
        item.setEncodingType(this.listObjectsResult.encodingType());
        return new Result<>(item);
      }

      this.completed = true;
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /** Execute list objects v1. */
  protected Iterable<Result<Item>> listObjectsV1(ListObjectsV1Args args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          private ListBucketResultV1 result = null;

          @Override
          protected void populateResult()
              throws ErrorResponseException, InsufficientDataException, InternalException,
                  InvalidKeyException, InvalidResponseException, IOException,
                  NoSuchAlgorithmException, ServerException, XmlParserException {
            this.listObjectsResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            String nextMarker = (result == null) ? args.marker() : result.nextMarker();
            if (nextMarker == null) nextMarker = this.lastObjectName;

            try {
              ListObjectsV1Response response =
                  listObjectsV1(
                          ListObjectsV1Args.builder()
                              .extraHeaders(args.extraHeaders())
                              .extraQueryParams(args.extraQueryParams())
                              .bucket(args.bucket())
                              .region(args.region())
                              .delimiter(args.delimiter())
                              .encodingType(args.encodingType())
                              .maxKeys(args.maxKeys())
                              .prefix(args.prefix())
                              .marker(nextMarker)
                              .build())
                      .get();
              result = response.result();
              this.listObjectsResult = response.result();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            } catch (ExecutionException e) {
              throwEncapsulatedException(e);
            }
          }
        };
      }
    };
  }

  /** Execute list objects v2. */
  protected Iterable<Result<Item>> listObjectsV2(ListObjectsV2Args args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          private ListBucketResultV2 result = null;

          @Override
          protected void populateResult()
              throws ErrorResponseException, InsufficientDataException, InternalException,
                  InvalidKeyException, InvalidResponseException, IOException,
                  NoSuchAlgorithmException, ServerException, XmlParserException {
            this.listObjectsResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            try {
              ListObjectsV2Response response =
                  listObjectsV2(
                          ListObjectsV2Args.builder()
                              .extraHeaders(args.extraHeaders())
                              .extraQueryParams(args.extraQueryParams())
                              .bucket(args.bucket())
                              .region(args.region())
                              .delimiter(args.delimiter())
                              .encodingType(args.encodingType())
                              .maxKeys(args.maxKeys())
                              .prefix(args.prefix())
                              .startAfter(args.startAfter())
                              .continuationToken(
                                  result == null
                                      ? args.continuationToken()
                                      : result.nextContinuationToken())
                              .fetchOwner(args.fetchOwner())
                              .includeUserMetadata(args.includeUserMetadata())
                              .build())
                      .get();
              result = response.result();
              this.listObjectsResult = response.result();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            } catch (ExecutionException e) {
              throwEncapsulatedException(e);
            }
          }
        };
      }
    };
  }

  /** Execute list object versions. */
  protected Iterable<Result<Item>> listObjectVersions(ListObjectVersionsArgs args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          private ListVersionsResult result = null;

          @Override
          protected void populateResult()
              throws ErrorResponseException, InsufficientDataException, InternalException,
                  InvalidKeyException, InvalidResponseException, IOException,
                  NoSuchAlgorithmException, ServerException, XmlParserException {
            this.listObjectsResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            try {
              ListObjectVersionsResponse response =
                  listObjectVersions(
                          ListObjectVersionsArgs.builder()
                              .extraHeaders(args.extraHeaders())
                              .extraQueryParams(args.extraQueryParams())
                              .bucket(args.bucket())
                              .region(args.region())
                              .delimiter(args.delimiter())
                              .encodingType(args.encodingType())
                              .maxKeys(args.maxKeys())
                              .prefix(args.prefix())
                              .keyMarker(result == null ? args.keyMarker() : result.nextKeyMarker())
                              .versionIdMarker(
                                  result == null
                                      ? args.versionIdMarker()
                                      : result.nextVersionIdMarker())
                              .build())
                      .get();
              result = response.result();
              this.listObjectsResult = response.result();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            } catch (ExecutionException e) {
              throwEncapsulatedException(e);
            }
          }
        };
      }
    };
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////// ListenBucketNotification API implementation /////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////

  /** Notification result records representation. */
  protected static class NotificationResultRecords {
    Response response = null;
    Scanner scanner = null;
    ObjectMapper mapper = null;

    public NotificationResultRecords(Response response) {
      this.response = response;
      this.scanner = new Scanner(response.body().charStream()).useDelimiter("\n");
      this.mapper =
          JsonMapper.builder()
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
              .build();
    }

    /** returns closeable iterator of result of notification records. */
    public CloseableIterator<Result<NotificationRecords>> closeableIterator() {
      return new CloseableIterator<Result<NotificationRecords>>() {
        String recordsString = null;
        NotificationRecords records = null;
        boolean isClosed = false;

        @Override
        public void close() throws IOException {
          if (!isClosed) {
            try {
              response.body().close();
              scanner.close();
            } finally {
              isClosed = true;
            }
          }
        }

        public boolean populate() {
          if (isClosed) return false;
          if (recordsString != null) return true;

          while (scanner.hasNext()) {
            recordsString = scanner.next().trim();
            if (!recordsString.equals("")) break;
          }

          if (recordsString == null || recordsString.equals("")) {
            try {
              close();
            } catch (IOException e) {
              isClosed = true;
            }
            return false;
          }
          return true;
        }

        @Override
        public boolean hasNext() {
          return populate();
        }

        @Override
        public Result<NotificationRecords> next() {
          if (isClosed) throw new NoSuchElementException();
          if ((recordsString == null || recordsString.equals("")) && !populate()) {
            throw new NoSuchElementException();
          }

          try {
            records = mapper.readValue(recordsString, NotificationRecords.class);
            return new Result<>(records);
          } catch (JsonMappingException e) {
            return new Result<>(e);
          } catch (JsonParseException e) {
            return new Result<>(e);
          } catch (IOException e) {
            return new Result<>(e);
          } finally {
            recordsString = null;
            records = null;
          }
        }
      };
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////

  private Part[] uploadParts(
      PutObjectBaseArgs args, String uploadId, PartReader partReader, PartSource firstPartSource)
      throws InterruptedException, ExecutionException, InsufficientDataException, InternalException,
          InvalidKeyException, IOException, NoSuchAlgorithmException, XmlParserException {
    Part[] parts = new Part[ObjectWriteArgs.MAX_MULTIPART_COUNT];
    int partNumber = 0;
    PartSource partSource = firstPartSource;
    while (true) {
      partNumber++;

      Multimap<String, String> ssecHeaders = null;
      // set encryption headers in the case of SSE-C.
      if (args.sse() != null && args.sse() instanceof ServerSideEncryption.CustomerKey) {
        ssecHeaders = Multimaps.forMap(args.sse().headers());
      }

      UploadPartResponse response =
          uploadPart(
                  UploadPartArgs.builder()
                      .bucket(args.bucket())
                      .region(args.region())
                      .object(args.object())
                      .buffer(partSource, partSource.size())
                      .partNumber(partNumber)
                      .uploadId(uploadId)
                      .headers(ssecHeaders)
                      .build())
              .get();
      parts[partNumber - 1] = new Part(partNumber, response.etag());

      partSource = partReader.getPart();
      if (partSource == null) break;
    }

    return parts;
  }

  private CompletableFuture<ObjectWriteResponse> putMultipartObjectAsync(
      PutObjectBaseArgs args,
      Multimap<String, String> headers,
      PartReader partReader,
      PartSource firstPartSource)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    return CompletableFuture.supplyAsync(
        () -> {
          String uploadId = null;
          ObjectWriteResponse response = null;
          try {
            CreateMultipartUploadResponse createMultipartUploadResponse =
                createMultipartUpload(
                        CreateMultipartUploadArgs.builder()
                            .extraQueryParams(args.extraQueryParams())
                            .bucket(args.bucket())
                            .region(args.region())
                            .object(args.object())
                            .headers(headers)
                            .build())
                    .get();
            uploadId = createMultipartUploadResponse.result().uploadId();
            Part[] parts = uploadParts(args, uploadId, partReader, firstPartSource);
            response =
                completeMultipartUpload(
                        CompleteMultipartUploadArgs.builder()
                            .bucket(args.bucket())
                            .region(args.region())
                            .object(args.object())
                            .uploadId(uploadId)
                            .parts(parts)
                            .build())
                    .get();
          } catch (InsufficientDataException
              | InternalException
              | InvalidKeyException
              | IOException
              | NoSuchAlgorithmException
              | XmlParserException
              | InterruptedException
              | ExecutionException e) {
            Throwable throwable = e;
            if (throwable instanceof ExecutionException) {
              throwable = ((ExecutionException) throwable).getCause();
            }
            if (throwable instanceof CompletionException) {
              throwable = ((CompletionException) throwable).getCause();
            }
            if (uploadId == null) {
              throw new CompletionException(throwable);
            }
            try {
              abortMultipartUpload(
                      AbortMultipartUploadArgs.builder()
                          .bucket(args.bucket())
                          .region(args.region())
                          .object(args.object())
                          .uploadId(uploadId)
                          .build())
                  .get();
            } catch (InsufficientDataException
                | InternalException
                | InvalidKeyException
                | IOException
                | NoSuchAlgorithmException
                | XmlParserException
                | InterruptedException
                | ExecutionException ex) {
              throwable = ex;
              if (throwable instanceof ExecutionException) {
                throwable = ((ExecutionException) throwable).getCause();
              }
              if (throwable instanceof CompletionException) {
                throwable = ((CompletionException) throwable).getCause();
              }
            }
            throw new CompletionException(throwable);
          }
          return response;
        });
  }

  protected PartReader newPartReader(
      Object data, long objectSize, long partSize, int partCount, Checksum.Algorithm... algorithms)
      throws NoSuchAlgorithmException {
    if (data instanceof RandomAccessFile) {
      return new PartReader((RandomAccessFile) data, objectSize, partSize, partCount, algorithms);
    }

    if (data instanceof InputStream) {
      return new PartReader((InputStream) data, objectSize, partSize, partCount, algorithms);
    }

    return null;
  }

  protected CompletableFuture<Integer> calculatePartCountAsync(List<ComposeSource> sources)
      throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
          NoSuchAlgorithmException, XmlParserException {
    long[] objectSize = {0};
    int index = 0;

    CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> 0);
    for (ComposeSource src : sources) {
      index++;
      final int i = index;
      completableFuture =
          completableFuture.thenCombine(
              statObject(new StatObjectArgs((ObjectReadArgs) src)),
              (partCount, statObjectResponse) -> {
                src.buildHeaders(statObjectResponse.size(), statObjectResponse.etag());

                long size = statObjectResponse.size();
                if (src.length() != null) {
                  size = src.length();
                } else if (src.offset() != null) {
                  size -= src.offset();
                }

                if (size < ObjectWriteArgs.MIN_MULTIPART_SIZE
                    && sources.size() != 1
                    && i != sources.size()) {
                  throw new IllegalArgumentException(
                      "source "
                          + src.bucket()
                          + "/"
                          + src.object()
                          + ": size "
                          + size
                          + " must be greater than "
                          + ObjectWriteArgs.MIN_MULTIPART_SIZE);
                }

                objectSize[0] += size;
                if (objectSize[0] > ObjectWriteArgs.MAX_OBJECT_SIZE) {
                  throw new IllegalArgumentException(
                      "destination object size must be less than "
                          + ObjectWriteArgs.MAX_OBJECT_SIZE);
                }

                if (size > ObjectWriteArgs.MAX_PART_SIZE) {
                  long count = size / ObjectWriteArgs.MAX_PART_SIZE;
                  long lastPartSize = size - (count * ObjectWriteArgs.MAX_PART_SIZE);
                  if (lastPartSize > 0) {
                    count++;
                  } else {
                    lastPartSize = ObjectWriteArgs.MAX_PART_SIZE;
                  }

                  if (lastPartSize < ObjectWriteArgs.MIN_MULTIPART_SIZE
                      && sources.size() != 1
                      && i != sources.size()) {
                    throw new IllegalArgumentException(
                        "source "
                            + src.bucket()
                            + "/"
                            + src.object()
                            + ": "
                            + "for multipart split upload of "
                            + size
                            + ", last part size is less than "
                            + ObjectWriteArgs.MIN_MULTIPART_SIZE);
                  }
                  partCount += (int) count;
                } else {
                  partCount++;
                }

                if (partCount > ObjectWriteArgs.MAX_MULTIPART_COUNT) {
                  throw new IllegalArgumentException(
                      "Compose sources create more than allowed multipart count "
                          + ObjectWriteArgs.MAX_MULTIPART_COUNT);
                }
                return partCount;
              });
    }

    return completableFuture;
  }
}
