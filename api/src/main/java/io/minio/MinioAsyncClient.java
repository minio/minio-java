/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2022 MinIO, Inc.
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.io.ByteStreams;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;
import io.minio.errors.BucketPolicyTooLargeException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.MinioException;
import io.minio.errors.XmlParserException;
import io.minio.messages.AccessControlPolicy;
import io.minio.messages.CORSConfiguration;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.GetObjectAttributesOutput;
import io.minio.messages.Item;
import io.minio.messages.LegalHold;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListBucketResultV2;
import io.minio.messages.ListObjectsResult;
import io.minio.messages.ListVersionsResult;
import io.minio.messages.NotificationConfiguration;
import io.minio.messages.NotificationRecords;
import io.minio.messages.ObjectLockConfiguration;
import io.minio.messages.Part;
import io.minio.messages.ReplicationConfiguration;
import io.minio.messages.Retention;
import io.minio.messages.SelectObjectContentRequest;
import io.minio.messages.SseConfiguration;
import io.minio.messages.Tags;
import io.minio.messages.VersioningConfiguration;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.xerial.snappy.SnappyFramedOutputStream;

/**
 * Simple Storage Service (aka S3) client to perform bucket and object operations asynchronously.
 *
 * <h2>Bucket operations</h2>
 *
 * <ul>
 *   <li>Create, list and delete buckets.
 *   <li>Put, get and delete bucket lifecycle configuration.
 *   <li>Put, get and delete bucket policy configuration.
 *   <li>Put, get and delete bucket encryption configuration.
 *   <li>Put and get bucket default retention configuration.
 *   <li>Put and get bucket notification configuration.
 *   <li>Enable and disable bucket versioning.
 * </ul>
 *
 * <h2>Object operations</h2>
 *
 * <ul>
 *   <li>Put, get, delete and list objects.
 *   <li>Create objects by combining existing objects.
 *   <li>Put and get object retention and legal hold.
 *   <li>Filter object content by SQL statement.
 * </ul>
 *
 * <p>If access/secret keys are provided, all S3 operation requests are signed using AWS Signature
 * Version 4; else they are performed anonymously.
 *
 * <p>Examples on using this library are available <a
 * href="https://github.com/minio/minio-java/tree/master/src/test/java/io/minio/examples">here</a>.
 *
 * <p>Use {@code MinioAsyncClient.builder()} to create S3 client.
 *
 * <pre>{@code
 * // Create client with anonymous access.
 * MinioAsyncClient minioAsyncClient =
 *     MinioAsyncClient.builder().endpoint("https://play.min.io").build();
 *
 * // Create client with credentials.
 * MinioAsyncClient minioAsyncClient =
 *     MinioAsyncClient.builder()
 *         .endpoint("https://play.min.io")
 *         .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
 *         .build();
 * }</pre>
 */
public class MinioAsyncClient extends BaseS3Client {
  /** Argument builder of {@link MinioAsyncClient}. */
  public static final class Builder {
    private Http.BaseUrl baseUrl = null;
    private String region;
    private Provider provider;
    private OkHttpClient httpClient;
    private boolean closeHttpClient;

    public Builder baseUrl(Http.BaseUrl baseUrl) {
      if (baseUrl.region() == null) {
        baseUrl.setRegion(region);
      }
      region = null;
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder endpoint(String endpoint) {
      return this.baseUrl(new Http.BaseUrl(endpoint));
    }

    public Builder endpoint(String endpoint, int port, boolean secure) {
      return this.baseUrl(new Http.BaseUrl(endpoint, port, secure));
    }

    public Builder endpoint(URL url) {
      return this.baseUrl(new Http.BaseUrl(url));
    }

    public Builder endpoint(HttpUrl url) {
      return this.baseUrl(new Http.BaseUrl(url));
    }

    public Builder region(String region) {
      if (region != null && !Utils.REGION_REGEX.matcher(region).find()) {
        throw new IllegalArgumentException("invalid region " + region);
      }
      if (baseUrl != null) {
        baseUrl.setRegion(region);
      } else {
        this.region = region;
      }
      return this;
    }

    public Builder credentials(String accessKey, String secretKey) {
      provider = new StaticProvider(accessKey, secretKey, null);
      return this;
    }

    public Builder credentialsProvider(Provider provider) {
      this.provider = provider;
      return this;
    }

    public Builder httpClient(OkHttpClient httpClient) {
      Utils.validateNotNull(httpClient, "http client");
      this.httpClient = httpClient;
      return this;
    }

    public Builder httpClient(OkHttpClient httpClient, boolean close) {
      Utils.validateNotNull(httpClient, "http client");
      this.httpClient = httpClient;
      this.closeHttpClient = close;
      return this;
    }

    public MinioAsyncClient build() {
      Utils.validateNotNull(baseUrl, "endpoint");

      if (baseUrl.awsDomainSuffix() != null
          && baseUrl.awsDomainSuffix().endsWith(".cn")
          && !baseUrl.awsS3Prefix().endsWith("s3-accelerate.")
          && baseUrl.region() == null) {
        throw new IllegalArgumentException("Region missing in Amazon S3 China endpoint " + baseUrl);
      }

      if (httpClient == null) {
        closeHttpClient = true;
        httpClient = Http.newDefaultClient();
      }

      return new MinioAsyncClient(baseUrl, provider, httpClient, closeHttpClient);
    }
  }

  /** Creates new {@link MinioAsyncClient.Builder}. */
  public static Builder builder() {
    return new Builder();
  }

  private MinioAsyncClient(
      Http.BaseUrl baseUrl, Provider provider, OkHttpClient httpClient, boolean closeHttpClient) {
    super(baseUrl, provider, httpClient, closeHttpClient);
  }

  protected MinioAsyncClient(MinioAsyncClient client) {
    super(client);
  }

  /**
   * Gets information of an object asynchronously.
   *
   * <pre>Example:{@code
   * // Get information of an object.
   * CompletableFuture<StatObjectResponse> future =
   *     minioAsyncClient.statObject(
   *         StatObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   *
   * // Get information of SSE-C encrypted object.
   * CompletableFuture<StatObjectResponse> future =
   *     minioAsyncClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .ssec(ssec)
   *             .build());
   *
   * // Get information of a versioned object.
   * CompletableFuture<StatObjectResponse> future =
   *     minioAsyncClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .versionId("version-id")
   *             .build());
   *
   * // Get information of a SSE-C encrypted versioned object.
   * CompletableFuture<StatObjectResponse> future =
   *     minioAsyncClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .versionId("version-id")
   *             .ssec(ssec)
   *             .build());
   * }</pre>
   *
   * @param args {@link StatObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link StatObjectResponse}&gt; object.
   * @see StatObjectResponse
   */
  public CompletableFuture<StatObjectResponse> statObject(StatObjectArgs args) {
    return headObject(new HeadObjectArgs(args))
        .thenApply(response -> new StatObjectResponse(response));
  }

  /**
   * Gets data from offset to length of a SSE-C encrypted object asynchronously.
   *
   * <pre>Example:{@code
   * CompletableFuture<GetObjectResponse> future = minioAsyncClient.getObject(
   *     GetObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .offset(offset)
   *         .length(len)
   *         .ssec(ssec)
   *         .build()
   * }</pre>
   *
   * @param args Object of {@link GetObjectArgs}
   * @return {@link CompletableFuture}&lt;{@link GetObjectResponse}&gt; object.
   * @see GetObjectResponse
   */
  public CompletableFuture<GetObjectResponse> getObject(GetObjectArgs args) {
    checkArgs(args);
    args.validateSsec(this.baseUrl.isHttps());
    return executeGetAsync(
            args,
            args.makeHeaders(),
            (args.versionId() != null)
                ? new Http.QueryParameters("versionId", args.versionId())
                : null)
        .thenApply(
            response -> {
              return new GetObjectResponse(
                  response.headers(),
                  args.bucket(),
                  args.region(),
                  args.object(),
                  response.body().byteStream());
            });
  }

  private void downloadObject(
      String filename,
      boolean overwrite,
      HeadObjectResponse headObjectResponse,
      GetObjectResponse getObjectResponse)
      throws MinioException {
    OutputStream os = null;
    try {
      Path filePath = Paths.get(filename);
      String tempFilename =
          filename + "." + Utils.encode(headObjectResponse.etag()) + ".part.minio";
      Path tempFilePath = Paths.get(tempFilename);
      if (Files.exists(tempFilePath)) Files.delete(tempFilePath);
      os = Files.newOutputStream(tempFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      long bytesWritten = ByteStreams.copy(getObjectResponse, os);
      if (bytesWritten != headObjectResponse.size()) {
        throw new IOException(
            tempFilename
                + ": unexpected data written.  expected = "
                + headObjectResponse.size()
                + ", written = "
                + bytesWritten);
      }

      if (overwrite) {
        Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
      } else {
        Files.move(tempFilePath, filePath);
      }
    } catch (IOException e) {
      throw new MinioException(e);
    } finally {
      try {
        getObjectResponse.close();
        if (os != null) os.close();
      } catch (IOException e) {
        throw new MinioException(e);
      }
    }
  }

  /**
   * Downloads data of a SSE-C encrypted object to file.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.downloadObject(
   *     DownloadObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .ssec(ssec)
   *         .filename("my-filename")
   *         .build());
   * }</pre>
   *
   * @param args Object of {@link DownloadObjectArgs}
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> downloadObject(DownloadObjectArgs args) {
    String filename = args.filename();
    Path filePath = Paths.get(filename);
    if (!args.overwrite() && Files.exists(filePath)) {
      throw new IllegalArgumentException("Destination file " + filename + " already exists");
    }

    return headObject(new HeadObjectArgs(args))
        .thenCombine(
            getObject(new GetObjectArgs(args)),
            (headObjectResponse, getObjectResponse) -> {
              try {
                downloadObject(filename, args.overwrite(), headObjectResponse, getObjectResponse);
                return null;
              } catch (MinioException e) {
                return Utils.failedFuture(e);
              }
            })
        .thenAccept(nullValue -> {});
  }

  /**
   * Creates an object by server-side copying data from another object.
   *
   * <pre>Example:{@code
   * // Create object "my-objectname" in bucket "my-bucketname" by copying from object
   * // "my-objectname" in bucket "my-source-bucketname".
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .source(
   *             SourceObject.builder()
   *                 .bucket("my-source-bucketname")
   *                 .object("my-objectname")
   *                 .build())
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" by copying from object
   * // "my-source-objectname" in bucket "my-source-bucketname".
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .source(
   *             SourceObject.builder()
   *                 .bucket("my-source-bucketname")
   *                 .object("my-source-objectname")
   *                 .build())
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" with SSE-KMS server-side
   * // encryption by copying from object "my-objectname" in bucket "my-source-bucketname".
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .source(
   *             SourceObject.builder()
   *                 .bucket("my-source-bucketname")
   *                 .object("my-objectname")
   *                 .build())
   *         .sse(sseKms) // Replace with actual key.
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" with SSE-S3 server-side
   * // encryption by copying from object "my-objectname" in bucket "my-source-bucketname".
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .source(
   *             SourceObject.builder()
   *                 .bucket("my-source-bucketname")
   *                 .object("my-objectname")
   *                 .build())
   *         .sse(sseS3) // Replace with actual key.
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" with SSE-C server-side encryption
   * // by copying from object "my-objectname" in bucket "my-source-bucketname".
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .source(
   *             SourceObject.builder()
   *                 .bucket("my-source-bucketname")
   *                 .object("my-objectname")
   *                 .build())
   *         .sse(ssec) // Replace with actual key.
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" by copying from SSE-C encrypted
   * // object "my-source-objectname" in bucket "my-source-bucketname".
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .source(
   *             SourceObject.builder()
   *                 .bucket("my-source-bucketname")
   *                 .object("my-source-objectname")
   *                 .ssec(ssec) // Replace with actual key.
   *                 .build())
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" with custom headers conditionally
   * // by copying from object "my-objectname" in bucket "my-source-bucketname".
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .source(
   *             SourceObject.builder()
   *                 .bucket("my-source-bucketname")
   *                 .object("my-objectname")
   *                 .matchETag(etag) // Replace with actual etag.
   *                 .build())
   *         .headers(headers) // Replace with actual headers.
   *         .build());
   * }</pre>
   *
   * @param args {@link CopyObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> copyObject(CopyObjectArgs args) {
    checkArgs(args);
    args.validateSse(this.baseUrl.isHttps());

    CompletableFuture<HeadObjectResponse> future =
        args.source().objectSize() == null
            ? headObject(new HeadObjectArgs(args.source()))
            : CompletableFuture.completedFuture((HeadObjectResponse) null);
    return future
        .thenApply(
            response ->
                response == null
                    ? args
                    : new CopyObjectArgs(
                        args, new SourceObject(args.source(), response.size(), response.etag())))
        .thenCompose(
            copyArgs -> {
              long size = copyArgs.source().objectSize();
              if (size < ObjectWriteArgs.MAX_PART_SIZE
                  && copyArgs.source().offset() == null
                  && copyArgs.source().length() == null) {
                return super.copyObject(copyArgs);
              }

              if (size > ObjectWriteArgs.MAX_PART_SIZE) {
                if (copyArgs.metadataDirective() == Directive.COPY) {
                  throw new IllegalArgumentException(
                      "COPY metadata directive is not applicable to source object size greater"
                          + " than 5 GiB");
                }
              }
              if (copyArgs.taggingDirective() == Directive.COPY) {
                throw new IllegalArgumentException(
                    "COPY tagging directive is not applicable to source object size greater than"
                        + " 5 GiB");
              }

              return composeObject(new ComposeObjectArgs(copyArgs));
            });
  }

  /** Calculates part count for given list of {@link SourceObject}. */
  protected CompletableFuture<Integer> calculatePartCount(List<SourceObject> sources) {
    long[] objectSize = {0};

    CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> 0);

    int sourceSize = sources.size();
    for (int i = 0; i < sourceSize; i++) {
      final int index = i;
      final boolean interimPart = sourceSize != 1 && sourceSize != (i + 1);

      CompletableFuture<HeadObjectResponse> future =
          sources.get(index).objectSize() == null
              ? headObject(new HeadObjectArgs(sources.get(index)))
              : CompletableFuture.completedFuture((HeadObjectResponse) null);
      completableFuture =
          completableFuture.thenCombine(
              future,
              (partCount, response) -> {
                SourceObject source = sources.get(index);
                if (response != null) {
                  source = new SourceObject(source, response.size(), response.etag());
                  sources.set(index, source);
                }

                long size = source.length() != null ? source.length() : source.objectSize();
                size -= source.offset() != null ? source.offset() : 0;
                if (size < ObjectWriteArgs.MIN_MULTIPART_SIZE && interimPart) {
                  throw new IllegalArgumentException(
                      "compose source "
                          + source.bucket()
                          + "/"
                          + source.object()
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

                  if (lastPartSize < ObjectWriteArgs.MIN_MULTIPART_SIZE && interimPart) {
                    throw new IllegalArgumentException(
                        "compose source "
                            + source.bucket()
                            + "/"
                            + source.object()
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

  private CompletableFuture<Part[]> uploadParts(
      ComposeObjectArgs args, int partCount, String uploadId) {
    Http.Headers ssecHeaders =
        (args.sse() != null && args.sse() instanceof ServerSideEncryption.CustomerKey)
            ? args.sse().headers()
            : null;

    int partNumber = 0;
    CompletableFuture<Part[]> future = CompletableFuture.supplyAsync(() -> new Part[partCount]);
    for (SourceObject source : args.sources()) {
      long size = source.objectSize();
      if (source.length() != null) {
        size = source.length();
      } else if (source.offset() != null) {
        size -= source.offset();
      }

      long offset = source.offset() == null ? 0 : source.offset();
      Http.Headers sourceHeaders = null;
      try {
        sourceHeaders = source.headers();
      } catch (MinioException e) {
        return Utils.failedFuture(e);
      }
      final Http.Headers headers = Http.Headers.merge(sourceHeaders, ssecHeaders);

      if (size <= ObjectWriteArgs.MAX_PART_SIZE) {
        partNumber++;
        if (source.length() != null) {
          headers.put(
              Http.Headers.X_AMZ_COPY_SOURCE_RANGE,
              "bytes=" + offset + "-" + (offset + source.length() - 1));
        } else if (source.offset() != null) {
          headers.put(
              Http.Headers.X_AMZ_COPY_SOURCE_RANGE, "bytes=" + offset + "-" + (offset + size - 1));
        }

        final int finalPartNumber = partNumber;
        future =
            future.thenCombine(
                uploadPartCopy(new UploadPartCopyArgs(args, uploadId, finalPartNumber, headers)),
                (parts, response) -> {
                  parts[response.partNumber() - 1] = response.part();
                  return parts;
                });
        continue;
      }

      while (size > 0) {
        partNumber++;

        long length = Math.min(size, ObjectWriteArgs.MAX_PART_SIZE);
        long endBytes = offset + length - 1;

        Http.Headers finalHeaders =
            new Http.Headers(
                Http.Headers.X_AMZ_COPY_SOURCE_RANGE, "bytes=" + offset + "-" + endBytes);
        finalHeaders.putAll(headers);

        final int finalPartNumber = partNumber;
        future =
            future.thenCombine(
                uploadPartCopy(new UploadPartCopyArgs(args, uploadId, finalPartNumber, headers)),
                (parts, response) -> {
                  parts[response.partNumber() - 1] = response.part();
                  return parts;
                });
        offset += length;
        size -= length;
      }
    }

    return future;
  }

  private CompletableFuture<ObjectWriteResponse> composeObject(
      ComposeObjectArgs args, int partCount) {
    String[] uploadId = {null};
    return createMultipartUpload(new CreateMultipartUploadArgs(args))
        .thenCompose(
            response -> {
              uploadId[0] = response.result().uploadId();
              return uploadParts(args, partCount, uploadId[0]);
            })
        .thenCompose(
            parts ->
                completeMultipartUpload(new CompleteMultipartUploadArgs(args, uploadId[0], parts)))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (uploadId[0] != null) {
                try {
                  abortMultipartUpload(new AbortMultipartUploadArgs(args, uploadId[0])).join();
                } catch (CompletionException ex) {
                  e.addSuppressed(ex.getCause());
                }
              }
              throw new CompletionException(e);
            });
  }

  /**
   * Creates an object by combining data from different source objects using server-side copy.
   *
   * <pre>Example:{@code
   * List<SourceObject> sourceObjectList = new ArrayList<SourceObject>();
   *
   * sourceObjectList.add(
   *    SourceObject.builder().bucket("my-job-bucket").object("my-objectname-part-one").build());
   * sourceObjectList.add(
   *    SourceObject.builder().bucket("my-job-bucket").object("my-objectname-part-two").build());
   * sourceObjectList.add(
   *    SourceObject.builder().bucket("my-job-bucket").object("my-objectname-part-three").build());
   *
   * // Create my-bucketname/my-objectname by combining source object list.
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.composeObject(
   *    ComposeObjectArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .sources(sourceObjectList)
   *        .build());
   *
   * // Create my-bucketname/my-objectname with user metadata by combining source object
   * // list.
   * Map<String, String> userMetadata = new HashMap<>();
   * userMetadata.put("My-Project", "Project One");
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.composeObject(
   *     ComposeObjectArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .sources(sourceObjectList)
   *        .userMetadata(userMetadata)
   *        .build());
   *
   * // Create my-bucketname/my-objectname with user metadata and server-side encryption
   * // by combining source object list.
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.composeObject(
   *   ComposeObjectArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .sources(sourceObjectList)
   *        .userMetadata(userMetadata)
   *        .ssec(sse)
   *        .build());
   * }</pre>
   *
   * @param args {@link ComposeObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> composeObject(ComposeObjectArgs args) {
    checkArgs(args);
    args.validateSse(this.baseUrl.isHttps());

    return calculatePartCount(args.sources())
        .thenCompose(
            partCount -> {
              if (partCount == 1
                  && args.sources().get(0).offset() == null
                  && args.sources().get(0).length() == null) {
                return copyObject(new CopyObjectArgs(args));
              }
              return composeObject(args, partCount);
            });
  }

  /**
   * Gets presigned URL of an object for HTTP method, expiry time and custom request parameters.
   *
   * <pre>Example:{@code
   * // Get presigned URL string to delete 'my-objectname' in 'my-bucketname' and its life time
   * // is one day.
   * String url =
   *    minioAsyncClient.getPresignedObjectUrl(
   *        GetPresignedObjectUrlArgs.builder()
   *            .method(Http.Method.DELETE)
   *            .bucket("my-bucketname")
   *            .object("my-objectname")
   *            .expiry(24 * 60 * 60)
   *            .build());
   * System.out.println(url);
   *
   * // Get presigned URL string to upload 'my-objectname' in 'my-bucketname'
   * // with response-content-type as application/json and life time as one day.
   * Map<String, String> reqParams = new HashMap<String, String>();
   * reqParams.put("response-content-type", "application/json");
   *
   * String url =
   *    minioAsyncClient.getPresignedObjectUrl(
   *        GetPresignedObjectUrlArgs.builder()
   *            .method(Http.Method.PUT)
   *            .bucket("my-bucketname")
   *            .object("my-objectname")
   *            .expiry(1, TimeUnit.DAYS)
   *            .extraQueryParams(reqParams)
   *            .build());
   * System.out.println(url);
   *
   * // Get presigned URL string to download 'my-objectname' in 'my-bucketname' and its life time
   * // is 2 hours.
   * String url =
   *    minioAsyncClient.getPresignedObjectUrl(
   *        GetPresignedObjectUrlArgs.builder()
   *            .method(Http.Method.GET)
   *            .bucket("my-bucketname")
   *            .object("my-objectname")
   *            .expiry(2, TimeUnit.HOURS)
   *            .build());
   * System.out.println(url);
   * }</pre>
   *
   * @param args {@link GetPresignedObjectUrlArgs} object.
   * @return String - URL string.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args) throws MinioException {
    checkArgs(args);

    String region = null;
    try {
      region = getRegion(args.bucket(), args.region()).join();
    } catch (CompletionException e) {
      throwMinioException(e);
    }

    Http.QueryParameters queryParams = new Http.QueryParameters();
    if (args.versionId() == null) queryParams.put("versionId", args.versionId());

    Credentials credentials = provider == null ? null : provider.fetch();
    if (credentials != null && credentials.sessionToken() != null) {
      queryParams.put(Http.Headers.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }

    return Http.S3Request.builder()
        .userAgent(userAgent)
        .method(args.method())
        .args(args)
        .queryParams(queryParams)
        .build()
        .toPresignedRequest(baseUrl, region, credentials, args.expiry())
        .httpRequest()
        .url()
        .toString();
  }

  /**
   * Gets form-data of {@link PostPolicy} of an object to upload its data using POST method.
   *
   * <pre>Example:{@code
   * // Create new post policy for 'my-bucketname' with 7 days expiry from now.
   * PostPolicy policy = new PostPolicy("my-bucketname", ZonedDateTime.now().plusDays(7));
   *
   * // Add condition that 'key' (object name) equals to 'my-objectname'.
   * policy.addEqualsCondition("key", "my-objectname");
   *
   * // Add condition that 'Content-Type' starts with 'image/'.
   * policy.addStartsWithCondition("Content-Type", "image/");
   *
   * // Add condition that 'content-length-range' is between 64kiB to 10MiB.
   * policy.addContentLengthRangeCondition(64 * 1024, 10 * 1024 * 1024);
   *
   * Map<String, String> formData = minioAsyncClient.getPresignedPostFormData(policy);
   *
   * // Upload an image using POST object with form-data.
   * MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
   * multipartBuilder.setType(MultipartBody.FORM);
   * for (Map.Entry<String, String> entry : formData.entrySet()) {
   *   multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
   * }
   * multipartBuilder.addFormDataPart("key", "my-objectname");
   * multipartBuilder.addFormDataPart("Content-Type", "image/png");
   *
   * // "file" must be added at last.
   * multipartBuilder.addFormDataPart(
   *     "file", "my-objectname", RequestBody.create(new File("Pictures/avatar.png"), null));
   *
   * Request request =
   *     new Request.Builder()
   *         .url("https://play.min.io/my-bucketname")
   *         .post(multipartBuilder.build())
   *         .build();
   * OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
   * Response response = httpClient.newCall(request).execute();
   * if (response.isSuccessful()) {
   *   System.out.println("Pictures/avatar.png is uploaded successfully using POST object");
   * } else {
   *   System.out.println("Failed to upload Pictures/avatar.png");
   * }
   * }</pre>
   *
   * @param policy Post policy of an object.
   * @return {@code Map<String, String>} - Contains form-data to upload an object using POST method.
   * @throws MinioException thrown to indicate SDK exception.
   * @see PostPolicy
   */
  public Map<String, String> getPresignedPostFormData(PostPolicy policy) throws MinioException {
    if (provider == null) {
      throw new IllegalArgumentException(
          "Anonymous access does not require presigned post form-data");
    }

    String region = null;
    try {
      region = getRegion(policy.bucket(), null).join();
    } catch (CompletionException e) {
      throwMinioException(e);
    }
    return policy.formData(provider.fetch(), region);
  }

  /**
   * Removes an object.
   *
   * <pre>Example:{@code
   * // Remove object.
   * CompletableFuture<Void> future = minioAsyncClient.removeObject(
   *     RemoveObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   *
   * // Remove versioned object.
   * CompletableFuture<Void> future = minioAsyncClient.removeObject(
   *     RemoveObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-versioned-objectname")
   *         .versionId("my-versionid")
   *         .build());
   *
   * // Remove versioned object bypassing Governance mode.
   * CompletableFuture<Void> future = minioAsyncClient.removeObject(
   *     RemoveObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-versioned-objectname")
   *         .versionId("my-versionid")
   *         .bypassRetentionMode(true)
   *         .build());
   * }</pre>
   *
   * @param args {@link RemoveObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> removeObject(RemoveObjectArgs args) {
    checkArgs(args);
    return executeDeleteAsync(
            args,
            args.bypassGovernanceMode()
                ? new Http.Headers("x-amz-bypass-governance-retention", "true")
                : null,
            (args.versionId() != null)
                ? new Http.QueryParameters("versionId", args.versionId())
                : null)
        .thenAccept(response -> response.close());
  }

  /**
   * Removes multiple objects lazily. Its required to iterate the returned Iterable to perform
   * removal.
   *
   * <pre>Example:{@code
   * List<DeleteObject> objects = new ArrayList<>();
   * objects.add(new DeleteObject("my-objectname1"));
   * objects.add(new DeleteObject("my-objectname2"));
   * objects.add(new DeleteObject("my-objectname3"));
   * Iterable<Result<DeleteResult.Error>> results =
   *     minioAsyncClient.removeObjects(
   *         RemoveObjectsArgs.builder().bucket("my-bucketname").objects(objects).build());
   * for (Result<DeleteResult.Error> result : results) {
   *   DeleteResult.Error error = result.get();
   *   System.out.println(
   *       "Error in deleting object " + error.objectName() + "; " + error.message());
   * }
   * }</pre>
   *
   * @param args {@link RemoveObjectsArgs} object.
   * @return {@code Iterable<Result<DeleteResult.Error>>} - Lazy iterator contains object removal
   *     status.
   */
  public Iterable<Result<DeleteResult.Error>> removeObjects(RemoveObjectsArgs args) {
    checkArgs(args);

    return new Iterable<Result<DeleteResult.Error>>() {
      @Override
      public Iterator<Result<DeleteResult.Error>> iterator() {
        return new Iterator<Result<DeleteResult.Error>>() {
          private Result<DeleteResult.Error> error = null;
          private Iterator<DeleteResult.Error> errorIterator = null;
          private boolean completed = false;
          private Iterator<DeleteRequest.Object> objectIter = args.objects().iterator();

          private void setError() {
            error = null;
            while (errorIterator.hasNext()) {
              DeleteResult.Error deleteError = errorIterator.next();
              if (!"NoSuchVersion".equals(deleteError.code())) {
                error = new Result<>(deleteError);
                break;
              }
            }
          }

          private synchronized void populate() {
            if (completed) {
              return;
            }

            try {
              List<DeleteRequest.Object> objectList = new ArrayList<>();
              while (objectIter.hasNext() && objectList.size() < 1000) {
                objectList.add(objectIter.next());
              }

              completed = objectList.isEmpty();
              if (completed) return;
              DeleteObjectsResponse response = null;
              try {
                response =
                    deleteObjects(
                            DeleteObjectsArgs.builder()
                                .extraHeaders(args.extraHeaders())
                                .extraQueryParams(args.extraQueryParams())
                                .bucket(args.bucket())
                                .region(args.region())
                                .objects(objectList)
                                .quiet(true)
                                .bypassGovernanceMode(args.bypassGovernanceMode())
                                .build())
                        .join();
              } catch (CompletionException e) {
                throwMinioException(e);
              }
              if (!response.result().errors().isEmpty()) {
                errorIterator = response.result().errors().iterator();
                setError();
                completed = true;
              }
            } catch (MinioException e) {
              error = new Result<>(e);
              completed = true;
            }
          }

          @Override
          public boolean hasNext() {
            while (error == null && errorIterator == null && !completed) {
              populate();
            }

            if (error == null && errorIterator != null) setError();
            if (error != null) return true;
            if (completed) return false;

            errorIterator = null;
            return hasNext();
          }

          @Override
          public Result<DeleteResult.Error> next() {
            if (!hasNext()) throw new NoSuchElementException();

            if (this.error != null) {
              Result<DeleteResult.Error> error = this.error;
              this.error = null;
              return error;
            }

            // This never happens.
            throw new NoSuchElementException();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /**
   * Restores an object asynchronously.
   *
   * <pre>Example:{@code
   * // Restore object.
   * CompletableFuture<Void> future = minioAsyncClient.restoreObject(
   *     RestoreObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .request(new RestoreRequest(null, null, null, null, null, null))
   *         .build());
   *
   * // Restore versioned object.
   * CompletableFuture<Void> future = minioAsyncClient.restoreObject(
   *     RestoreObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-versioned-objectname")
   *         .versionId("my-versionid")
   *         .request(new RestoreRequest(null, null, null, null, null, null))
   *         .build());
   * }</pre>
   *
   * @param args {@link RestoreObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> restoreObject(RestoreObjectArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.request(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePostAsync(args, null, new Http.QueryParameters("restore", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Lists objects information optionally with versions of a bucket. Supports both the versions 1
   * and 2 of the S3 API. By default, the <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html">version 2</a> API
   * is used. <br>
   * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">Version 1</a>
   * can be used by passing the optional argument {@code useVersion1} as {@code true}.
   *
   * <pre>Example:{@code
   * // Lists objects information.
   * Iterable<Result<Item>> results = minioAsyncClient.listObjects(
   *     ListObjectsArgs.builder().bucket("my-bucketname").build());
   *
   * // Lists objects information recursively.
   * Iterable<Result<Item>> results = minioAsyncClient.listObjects(
   *     ListObjectsArgs.builder().bucket("my-bucketname").recursive(true).build());
   *
   * // Lists maximum 100 objects information whose names starts with 'E' and after
   * // 'ExampleGuide.pdf'.
   * Iterable<Result<Item>> results = minioAsyncClient.listObjects(
   *     ListObjectsArgs.builder()
   *         .bucket("my-bucketname")
   *         .startAfter("ExampleGuide.pdf")
   *         .prefix("E")
   *         .maxKeys(100)
   *         .build());
   *
   * // Lists maximum 100 objects information with version whose names starts with 'E' and after
   * // 'ExampleGuide.pdf'.
   * Iterable<Result<Item>> results = minioAsyncClient.listObjects(
   *     ListObjectsArgs.builder()
   *         .bucket("my-bucketname")
   *         .startAfter("ExampleGuide.pdf")
   *         .prefix("E")
   *         .maxKeys(100)
   *         .includeVersions(true)
   *         .build());
   * }</pre>
   *
   * @param args Instance of {@link ListObjectsArgs} built using the builder
   * @return {@code Iterable<Result<Item>>} - Lazy iterator contains object information.
   */
  public Iterable<Result<Item>> listObjects(ListObjectsArgs args) {
    if (args.includeVersions() || args.versionIdMarker() != null) {
      return objectVersionLister(new ListObjectVersionsArgs(args));
    }

    if (args.useApiVersion1()) {
      return objectV1Lister(new ListObjectsV1Args(args));
    }

    return objectV2Lister(new ListObjectsV2Args(args));
  }

  /**
   * Lists bucket information of all buckets.
   *
   * <pre>Example:{@code
   * CompletableFuture<List<ListAllMyBucketsResult.Bucket>> future = minioAsyncClient.listBuckets();
   * }</pre>
   *
   * @return {@link CompletableFuture}&lt;{@link List}&lt;{@link
   *     ListAllMyBucketsResult.Bucket}&gt;&gt; object.
   */
  public CompletableFuture<List<ListAllMyBucketsResult.Bucket>> listBuckets() {
    return listBucketsAPI(ListBucketsArgs.builder().build())
        .thenApply(
            response -> {
              return response.result().buckets();
            });
  }

  /**
   * Lists bucket information of all buckets.
   *
   * <pre>Example:{@code
   * Iterable<Result<ListAllMyBucketsResult.Bucket>> results = minioAsyncClient.listBuckets(ListBucketsArgs.builder().build());
   * for (Result<ListAllMyBucketsResult.Bucket> result : results) {
   *   Bucket bucket = result.get();
   *   System.out.println(String.format("Bucket: %s, Region: %s, CreationDate: %s", bucket.name(), bucket.bucketRegion(), bucket.creationDate()));
   * }
   * }</pre>
   *
   * @return {@link Iterable}&lt;{@link List}&lt;{@link ListAllMyBucketsResult.Bucket}&gt;&gt;
   *     object.
   */
  public Iterable<Result<ListAllMyBucketsResult.Bucket>> listBuckets(ListBucketsArgs args) {
    return new Iterable<Result<ListAllMyBucketsResult.Bucket>>() {
      @Override
      public Iterator<Result<ListAllMyBucketsResult.Bucket>> iterator() {
        return new Iterator<Result<ListAllMyBucketsResult.Bucket>>() {
          private ListAllMyBucketsResult result = null;
          private Result<ListAllMyBucketsResult.Bucket> error = null;
          private Iterator<ListAllMyBucketsResult.Bucket> iterator = null;
          private boolean completed = false;

          private synchronized void populate() {
            if (completed) return;

            try {
              this.iterator = Collections.emptyIterator();
              try {
                ListBucketsResponse response =
                    listBucketsAPI(
                            ListBucketsArgs.builder()
                                .extraHeaders(args.extraHeaders())
                                .extraQueryParams(args.extraQueryParams())
                                .bucketRegion(args.bucketRegion())
                                .maxBuckets(args.maxBuckets())
                                .prefix(args.prefix())
                                .continuationToken(
                                    result == null
                                        ? args.continuationToken()
                                        : result.continuationToken())
                                .build())
                        .join();
                this.result = response.result();
              } catch (CompletionException e) {
                throwMinioException(e);
              }
              this.iterator = this.result.buckets().iterator();
            } catch (MinioException e) {
              this.error = new Result<>(e);
              completed = true;
            }
          }

          @Override
          public boolean hasNext() {
            if (this.completed) return false;

            if (this.error == null && this.iterator == null) {
              populate();
            }

            if (this.error == null
                && !this.iterator.hasNext()
                && this.result.continuationToken() != null
                && !this.result.continuationToken().isEmpty()) {
              populate();
            }

            if (this.error != null) return true;
            if (this.iterator.hasNext()) return true;

            this.completed = true;
            return false;
          }

          @Override
          public Result<ListAllMyBucketsResult.Bucket> next() {
            if (this.completed) throw new NoSuchElementException();
            if (this.error == null && this.iterator == null) {
              populate();
            }

            if (this.error == null
                && !this.iterator.hasNext()
                && this.result.continuationToken() != null
                && !this.result.continuationToken().isEmpty()) {
              populate();
            }

            if (this.error != null) {
              this.completed = true;
              return this.error;
            }

            ListAllMyBucketsResult.Bucket item = null;
            if (this.iterator.hasNext()) {
              item = this.iterator.next();
            }

            if (item != null) {
              return new Result<>(item);
            }

            this.completed = true;
            throw new NoSuchElementException();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /**
   * Checks if a bucket exists.
   *
   * <pre>Example:{@code
   * CompletableFuture<Boolean> future =
   *      minioAsyncClient.bucketExists(BucketExistsArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link BucketExistsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Boolean}&gt; object.
   */
  public CompletableFuture<Boolean> bucketExists(BucketExistsArgs args) {
    return executeHeadAsync(args, null, null)
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e).errorResponse().code().equals(NO_SUCH_BUCKET)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              try {
                return response != null;
              } finally {
                if (response != null) response.close();
              }
            });
  }

  /**
   * Creates a bucket with region and object lock.
   *
   * <pre>Example:{@code
   * // Create bucket with default region.
   * CompletableFuture<Void> future = minioAsyncClient.makeBucket(
   *     MakeBucketArgs.builder()
   *         .bucket("my-bucketname")
   *         .build());
   *
   * // Create bucket with specific region.
   * CompletableFuture<Void> future = minioAsyncClient.makeBucket(
   *     MakeBucketArgs.builder()
   *         .bucket("my-bucketname")
   *         .region("us-west-1")
   *         .build());
   *
   * // Create object-lock enabled bucket with specific region.
   * CompletableFuture<Void> future = minioAsyncClient.makeBucket(
   *     MakeBucketArgs.builder()
   *         .bucket("my-bucketname")
   *         .region("us-west-1")
   *         .objectLock(true)
   *         .build());
   * }</pre>
   *
   * @param args Object with bucket name, region and lock functionality
   * @return {@link CompletableFuture}&lt;{@link GenericResponse}&gt; object.
   */
  public CompletableFuture<GenericResponse> makeBucket(MakeBucketArgs args) {
    checkArgs(args);
    return createBucket(new CreateBucketArgs(args));
  }

  /**
   * Sets versioning configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.setBucketVersioning(
   *     SetBucketVersioningArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketVersioningArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketVersioning(SetBucketVersioningArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("versioning", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Gets versioning configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<VersioningConfiguration> future =
   *     minioAsyncClient.getBucketVersioning(
   *         GetBucketVersioningArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketVersioningArgs} object.
   * @return {@link CompletableFuture}&lt;{@link VersioningConfiguration}&gt; object.
   */
  public CompletableFuture<VersioningConfiguration> getBucketVersioning(
      GetBucketVersioningArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("versioning", ""))
        .thenApply(
            response -> {
              try {
                return Xml.unmarshal(VersioningConfiguration.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets default object retention in a bucket.
   *
   * <pre>Example:{@code
   * ObjectLockConfiguration config = new ObjectLockConfiguration(
   *     RetentionMode.COMPLIANCE, new RetentionDurationDays(100));
   * CompletableFuture<Void> future = minioAsyncClient.setObjectLockConfiguration(
   *     SetObjectLockConfigurationArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetObjectLockConfigurationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setObjectLockConfiguration(SetObjectLockConfigurationArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("object-lock", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Deletes default object retention in a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.deleteObjectLockConfiguration(
   *     DeleteObjectLockConfigurationArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteObjectLockConfigurationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteObjectLockConfiguration(
      DeleteObjectLockConfigurationArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(new ObjectLockConfiguration(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("object-lock", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Gets default object retention in a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<ObjectLockConfiguration> future =
   *     minioAsyncClient.getObjectLockConfiguration(
   *         GetObjectLockConfigurationArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetObjectLockConfigurationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectLockConfiguration}&gt; object.
   */
  public CompletableFuture<ObjectLockConfiguration> getObjectLockConfiguration(
      GetObjectLockConfigurationArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("object-lock", ""))
        .thenApply(
            response -> {
              try {
                return Xml.unmarshal(ObjectLockConfiguration.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets retention configuration to an object.
   *
   * <pre>Example:{@code
   *  Retention retention = new Retention(
   *       RetentionMode.COMPLIANCE, ZonedDateTime.now().plusYears(1));
   *  CompletableFuture<Void> future = minioAsyncClient.setObjectRetention(
   *      SetObjectRetentionArgs.builder()
   *          .bucket("my-bucketname")
   *          .object("my-objectname")
   *          .config(config)
   *          .bypassGovernanceMode(true)
   *          .build());
   * }</pre>
   *
   * @param args {@link SetObjectRetentionArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setObjectRetention(SetObjectRetentionArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("retention", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(
            args,
            args.bypassGovernanceMode()
                ? new Http.Headers("x-amz-bypass-governance-retention", "True")
                : null,
            queryParams,
            body)
        .thenAccept(response -> response.close());
  }

  /**
   * Gets retention configuration of an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<Retention> future =
   *     minioAsyncClient.getObjectRetention(GetObjectRetentionArgs.builder()
   *        .bucket(bucketName)
   *        .object(objectName)
   *        .versionId(versionId)
   *        .build());
   * }</pre>
   *
   * @param args {@link GetObjectRetentionArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Retention}&gt; object.
   */
  public CompletableFuture<Retention> getObjectRetention(GetObjectRetentionArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("retention", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    return executeGetAsync(args, null, queryParams)
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals(NO_SUCH_OBJECT_LOCK_CONFIGURATION)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return null;
              try {
                return Xml.unmarshal(Retention.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Enables legal hold on an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.enableObjectLegalHold(
   *    EnableObjectLegalHoldArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .versionId("object-versionId")
   *        .build());
   * }</pre>
   *
   * @param args {@link EnableObjectLegalHoldArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> enableObjectLegalHold(EnableObjectLegalHoldArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("legal-hold", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    Http.Body body = null;
    try {
      body = new Http.Body(new LegalHold(true), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, queryParams, body).thenAccept(response -> response.close());
  }

  /**
   * Disables legal hold on an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.disableObjectLegalHold(
   *    DisableObjectLegalHoldArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .versionId("object-versionId")
   *        .build());
   * }</pre>
   *
   * @param args {@link DisableObjectLegalHoldArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> disableObjectLegalHold(DisableObjectLegalHoldArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("legal-hold", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    Http.Body body = null;
    try {
      body = new Http.Body(new LegalHold(false), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, queryParams, body).thenAccept(response -> response.close());
  }

  /**
   * Returns true if legal hold is enabled on an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<Boolean> future =
   *     s3Client.isObjectLegalHoldEnabled(
   *        IsObjectLegalHoldEnabledArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .versionId("object-versionId")
   *             .build());
   * }</pre>
   *
   * @param args {@link IsObjectLegalHoldEnabledArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Boolean}&gt; object.
   */
  public CompletableFuture<Boolean> isObjectLegalHoldEnabled(IsObjectLegalHoldEnabledArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("legal-hold", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    return executeGetAsync(args, null, queryParams)
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals(NO_SUCH_OBJECT_LOCK_CONFIGURATION)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return false;
              try {
                LegalHold result = Xml.unmarshal(LegalHold.class, response.body().charStream());
                return result.status();
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Removes an empty bucket using arguments
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future =
   *     minioAsyncClient.removeBucket(RemoveBucketArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link RemoveBucketArgs} bucket.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> removeBucket(RemoveBucketArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, null)
        .thenAccept(response -> regionCache.remove(args.bucket()));
  }

  private CompletableFuture<List<UploadPartResponse>> uploadPartsSequentially(
      PutObjectBaseArgs args,
      String uploadId,
      PartReader partReader,
      boolean addContentSha256,
      boolean addSha256Checksum,
      ByteBuffer buffer,
      long partSize,
      List<UploadPartResponse> responses) {
    return CompletableFuture.supplyAsync(
            () ->
                new UploadPartArgs(
                    args,
                    uploadId,
                    partReader.partNumber(),
                    buffer,
                    Checksum.makeHeaders(
                        partReader.hashers(), addContentSha256, addSha256Checksum)))
        .thenCompose(partArgs -> uploadPart(partArgs))
        .thenCompose(
            response -> {
              responses.add(response);
              try {
                buffer.reset();
                if (partReader.partNumber() == partReader.partCount()) {
                  return CompletableFuture.completedFuture(responses);
                }
                partReader.read(buffer);
              } catch (MinioException e) {
                return Utils.failedFuture(e);
              }
              return uploadPartsSequentially(
                  args,
                  uploadId,
                  partReader,
                  addContentSha256,
                  addSha256Checksum,
                  buffer,
                  partSize,
                  responses);
            });
  }

  private CompletableFuture<List<UploadPartResponse>> uploadPartsParallelly(
      PutObjectBaseArgs args,
      String uploadId,
      PartReader partReader,
      boolean addContentSha256,
      boolean addSha256Checksum,
      ByteBuffer buffer,
      long partSize,
      int parallelUploads) {
    ByteBufferPool bufferPool = new ByteBufferPool(parallelUploads, partSize);
    ExecutorService uploadExecutor = Executors.newFixedThreadPool(parallelUploads);
    BlockingQueue<UploadPartArgs.Wrapper> queue = new ArrayBlockingQueue<>(parallelUploads);
    CountDownLatch doneLatch = new CountDownLatch(parallelUploads);
    List<UploadPartResponse> uploadResults = Collections.synchronizedList(new ArrayList<>());
    AtomicBoolean errorOccurred = new AtomicBoolean(false);
    ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Start uploader workers
            for (int i = 0; i < parallelUploads; i++) {
              Future<?> result =
                  uploadExecutor.submit(
                      () -> {
                        try {
                          while (!errorOccurred.get()) {
                            UploadPartArgs.Wrapper part = queue.take();
                            if (part.args() == null) break; // poison pill
                            UploadPartResponse response = uploadPart(part.args()).join();
                            bufferPool.put(part.args().buffer());
                            uploadResults.add(response);
                          }
                        } catch (InterruptedException e) {
                          errorOccurred.set(true); // signal to all threads
                          exceptions.add(e);
                        } finally {
                          doneLatch.countDown();
                        }
                      });
              if (result == null) {
                throw new RuntimeException(
                    "uploadExecutor.submit() returns null; this should not happen");
              }
            }

            // Reader: submit initial buffer
            queue.put(
                new UploadPartArgs.Wrapper(
                    new UploadPartArgs(
                        args,
                        uploadId,
                        partReader.partNumber(),
                        buffer,
                        Checksum.makeHeaders(
                            partReader.hashers(), addContentSha256, addSha256Checksum))));

            // Reader: loop to submit remaining parts
            while (partReader.partNumber() != partReader.partCount() && !errorOccurred.get()) {
              ByteBuffer buf = bufferPool.take();
              partReader.read(buf);
              queue.put(
                  new UploadPartArgs.Wrapper(
                      new UploadPartArgs(
                          args,
                          uploadId,
                          partReader.partNumber(),
                          buf,
                          Checksum.makeHeaders(
                              partReader.hashers(), addContentSha256, addSha256Checksum))));
            }

            // Signal all workers to stop with poison pills
            for (int i = 0; i < parallelUploads; i++) {
              queue.put(new UploadPartArgs.Wrapper(null));
            }

            doneLatch.await();
            uploadExecutor.shutdown();

            if (!exceptions.isEmpty()) {
              CompletionException combined =
                  new CompletionException("uploadPartsParallelly failed", exceptions.peek());
              exceptions.stream().skip(1).forEach(combined::addSuppressed);
              throw combined;
            }

            uploadResults.sort(Comparator.comparingInt(r -> r.part().partNumber()));
            return uploadResults;

          } catch (InterruptedException | MinioException e) {
            throw new CompletionException(e);
          } finally {
            uploadExecutor.shutdownNow(); // ensure executor exits on error
          }
        });
  }

  private CompletableFuture<ObjectWriteResponse> putObject(
      PutObjectBaseArgs args,
      Object fileStreamData,
      MediaType contentType,
      boolean addContentSha256) {
    RandomAccessFile file = null;
    InputStream stream = null;
    byte[] data = null;
    if (fileStreamData instanceof RandomAccessFile) file = (RandomAccessFile) fileStreamData;
    if (fileStreamData instanceof InputStream) stream = (InputStream) fileStreamData;
    if (fileStreamData instanceof byte[]) data = (byte[]) fileStreamData;

    Checksum.Algorithm algorithm =
        args.checksum() != null ? args.checksum() : Checksum.Algorithm.CRC32C;
    boolean addSha256Checksum = algorithm == Checksum.Algorithm.SHA256;
    Checksum.Algorithm[] algorithms;
    if (addContentSha256 && !addSha256Checksum) {
      algorithms = new Checksum.Algorithm[] {algorithm, Checksum.Algorithm.SHA256};
    } else {
      algorithms = new Checksum.Algorithm[] {algorithm};
    }

    PartReader partReader = null;
    ByteBuffer buffer = null;
    int partCount = args.partCount();

    if (stream != null) {
      try {
        partReader =
            new PartReader(
                stream, args.objectSize(), args.partSize(), args.partCount(), algorithms);
        buffer = new ByteBuffer(partReader.partCount() == 1 ? args.objectSize() : args.partSize());
        partReader.read(buffer);
        partCount = partReader.partCount();
      } catch (MinioException e) {
        return Utils.failedFuture(e);
      }
    }

    if (partCount == 1) {
      if (stream != null) {
        return putObject(
            new PutObjectAPIArgs(
                args,
                buffer,
                contentType,
                Checksum.makeHeaders(partReader.hashers(), addContentSha256, addSha256Checksum)));
      }

      if (args.objectSize() == null) {
        return Utils.failedFuture(
            new MinioException("object size is null; this should not happen"));
      }
      long length = args.objectSize();

      try {
        Map<Checksum.Algorithm, Checksum.Hasher> hashers = Checksum.newHasherMap(algorithms);

        if (file != null) {
          long position = file.getFilePointer();
          Checksum.update(hashers, file, length);
          file.seek(position);
          return putObject(
              new PutObjectAPIArgs(
                  args,
                  file,
                  length,
                  contentType,
                  Checksum.makeHeaders(hashers, addContentSha256, addSha256Checksum)));
        }

        Checksum.update(hashers, data, (int) length);
        return putObject(
            new PutObjectAPIArgs(
                args,
                data,
                (int) length,
                contentType,
                Checksum.makeHeaders(hashers, addContentSha256, addSha256Checksum)));
      } catch (MinioException e) {
        return Utils.failedFuture(e);
      } catch (IOException e) {
        return Utils.failedFuture(new MinioException(e));
      }
    }

    // Multipart upload starts here

    if (args.checksum() != null && !args.checksum().compositeSupport()) {
      throw new IllegalArgumentException(
          "unsupported checksum " + args.checksum() + " for multipart upload");
    }

    if (file != null) {
      try {
        partReader =
            new PartReader(file, args.objectSize(), args.partSize(), args.partCount(), algorithms);
        buffer = new ByteBuffer(args.partSize());
        partReader.read(buffer);
      } catch (MinioException e) {
        return Utils.failedFuture(e);
      }
    }

    int parallelUploads = args.parallelUploads();
    if (parallelUploads <= 0) parallelUploads = 1;
    if (partReader.partCount() > 0 && parallelUploads > partReader.partCount()) {
      parallelUploads = partReader.partCount();
    }

    String[] uploadId = {null};
    final PartReader finalPartReader = partReader;
    final ByteBuffer finalBuffer = buffer;
    final int finalParallelUploads = parallelUploads;
    return createMultipartUpload(new CreateMultipartUploadArgs(args, contentType, algorithm))
        .thenCompose(
            response -> {
              uploadId[0] = response.result().uploadId();
              // Do sequential multipart uploads
              if (finalParallelUploads == 1) {
                return uploadPartsSequentially(
                    args,
                    uploadId[0],
                    finalPartReader,
                    addContentSha256,
                    addSha256Checksum,
                    finalBuffer,
                    args.partSize(),
                    new ArrayList<UploadPartResponse>());
              }

              // Do sequential multipart uploads
              return uploadPartsParallelly(
                  args,
                  uploadId[0],
                  finalPartReader,
                  addContentSha256,
                  addSha256Checksum,
                  finalBuffer,
                  args.partSize(),
                  finalParallelUploads);
            })
        .thenCompose(
            responses ->
                completeMultipartUpload(
                    new CompleteMultipartUploadArgs(
                        args,
                        uploadId[0],
                        responses.stream()
                            .map(UploadPartResponse::part)
                            .toArray(io.minio.messages.Part[]::new))))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (uploadId[0] != null) {
                try {
                  abortMultipartUpload(new AbortMultipartUploadArgs(args, uploadId[0])).join();
                } catch (CompletionException ex) {
                  e.addSuppressed(ex.getCause());
                }
              }
              throw new CompletionException(e);
            });
  }

  /**
   * Uploads data from a stream to an object.
   *
   * <pre>Example:{@code
   * // Upload known sized input stream.
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, size, -1)
   *         .contentType("video/mp4")
   *         .build());
   *
   * // Upload unknown sized input stream.
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, -1, 10485760)
   *         .contentType("video/mp4")
   *         .build());
   *
   * // Create object ends with '/' (also called as folder or directory).
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("path/to/").stream(
   *             new ByteArrayInputStream(new byte[] {}), 0, -1)
   *         .build());
   *
   * // Upload input stream with headers and user metadata.
   * Map<String, String> headers = new HashMap<>();
   * headers.put("X-Amz-Storage-Class", "REDUCED_REDUNDANCY");
   * Map<String, String> userMetadata = new HashMap<>();
   * userMetadata.put("My-Project", "Project One");
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, size, -1)
   *         .headers(headers)
   *         .userMetadata(userMetadata)
   *         .build());
   *
   * // Upload input stream with server-side encryption.
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, size, -1)
   *         .sse(sse)
   *         .build());
   * }</pre>
   *
   * @param args {@link PutObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> putObject(PutObjectArgs args) {
    checkArgs(args);
    args.validateSse(this.baseUrl.isHttps());
    try {
      return putObject(
          args,
          args.stream() != null ? args.stream() : args.data(),
          args.contentType(),
          !this.baseUrl.isHttps());
    } catch (IOException e) {
      return Utils.failedFuture(new MinioException(e));
    }
  }

  /**
   * Uploads data from a file to an object.
   *
   * <pre>Example:{@code
   * // Upload an JSON file.
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.uploadObject(
   *     UploadObjectArgs.builder()
   *         .bucket("my-bucketname").object("my-objectname").filename("person.json").build());
   *
   * // Upload a video file.
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.uploadObject(
   *     UploadObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .filename("my-video.avi")
   *         .contentType("video/mp4")
   *         .build());
   * }</pre>
   *
   * @param args {@link UploadObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> uploadObject(UploadObjectArgs args) {
    checkArgs(args);
    args.validateSse(this.baseUrl.isHttps());
    try {
      final RandomAccessFile file = new RandomAccessFile(args.filename(), "r");
      return putObject(args, file, args.contentType(), !this.baseUrl.isHttps())
          .exceptionally(
              e -> {
                e = e.getCause();
                try {
                  file.close();
                } catch (IOException ex) {
                  e.addSuppressed(new MinioException(ex));
                }
                throw new CompletionException(e);
              })
          .thenApply(
              objectWriteResponse -> {
                try {
                  file.close();
                } catch (IOException e) {
                  throw new CompletionException(new MinioException(e));
                }
                return objectWriteResponse;
              });
    } catch (IOException e) {
      return Utils.failedFuture(new MinioException(e));
    }
  }

  /**
   * Gets bucket policy configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<String> future =
   *     minioAsyncClient.getBucketPolicy(
   *         GetBucketPolicyArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketPolicyArgs} object.
   * @return {@link CompletableFuture}&lt;{@link String}&gt; object.
   */
  public CompletableFuture<String> getBucketPolicy(GetBucketPolicyArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("policy", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals(NO_SUCH_BUCKET_POLICY)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return "";
              try {
                byte[] buf = new byte[MAX_BUCKET_POLICY_SIZE];
                int bytesRead = 0;
                bytesRead = response.body().byteStream().read(buf, 0, MAX_BUCKET_POLICY_SIZE);
                if (bytesRead < 0) {
                  throw new CompletionException(
                      new IOException("unexpected EOF when reading bucket policy"));
                }

                // Read one byte extra to ensure only MAX_BUCKET_POLICY_SIZE data is sent by the
                // server.
                if (bytesRead == MAX_BUCKET_POLICY_SIZE) {
                  int byteRead = 0;
                  while (byteRead == 0) {
                    byteRead = response.body().byteStream().read();
                    if (byteRead < 0) {
                      break; // reached EOF which is fine.
                    }

                    if (byteRead > 0) {
                      throw new CompletionException(
                          new BucketPolicyTooLargeException(args.bucket()));
                    }
                  }
                }

                return new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
              } catch (IOException e) {
                throw new CompletionException(new MinioException(e));
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets bucket policy configuration to a bucket.
   *
   * <pre>Example:{@code
   * // Assume policyJson contains below JSON string;
   * // {
   * //     "Statement": [
   * //         {
   * //             "Action": [
   * //                 "s3:GetBucketLocation",
   * //                 "s3:ListBucket"
   * //             ],
   * //             "Effect": "Allow",
   * //             "Principal": "*",
   * //             "Resource": "arn:aws:s3:::my-bucketname"
   * //         },
   * //         {
   * //             "Action": "s3:GetObject",
   * //             "Effect": "Allow",
   * //             "Principal": "*",
   * //             "Resource": "arn:aws:s3:::my-bucketname/myobject*"
   * //         }
   * //     ],
   * //     "Version": "2012-10-17"
   * // }
   * //
   * CompletableFuture<Void> future = minioAsyncClient.setBucketPolicy(
   *     SetBucketPolicyArgs.builder().bucket("my-bucketname").config(policyJson).build());
   * }</pre>
   *
   * @param args {@link SetBucketPolicyArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketPolicy(SetBucketPolicyArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), Http.JSON_MEDIA_TYPE, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("policy", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Deletes bucket policy configuration to a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future =
   *     minioAsyncClient.deleteBucketPolicy(
   *         DeleteBucketPolicyArgs.builder().bucket("my-bucketname"));
   * }</pre>
   *
   * @param args {@link DeleteBucketPolicyArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteBucketPolicy(DeleteBucketPolicyArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, new Http.QueryParameters("policy", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals(NO_SUCH_BUCKET_POLICY)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenAccept(
            response -> {
              if (response != null) response.close();
            });
  }

  /**
   * Sets lifecycle configuration to a bucket.
   *
   * <pre>Example:{@code
   * List<LifecycleRule> rules = new ArrayList<>();
   * rules.add(
   *     new LifecycleRule(
   *         Status.ENABLED,
   *         null,
   *         new Expiration((ZonedDateTime) null, 365, null),
   *         new RuleFilter("logs/"),
   *         "rule2",
   *         null,
   *         null,
   *         null));
   * LifecycleConfiguration config = new LifecycleConfiguration(rules);
   * CompletableFuture<Void> future = minioAsyncClient.setBucketLifecycle(
   *     SetBucketLifecycleArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketLifecycleArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketLifecycle(SetBucketLifecycleArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("lifecycle", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Deletes lifecycle configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = deleteBucketLifecycle(
   *     DeleteBucketLifecycleArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketLifecycleArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteBucketLifecycle(DeleteBucketLifecycleArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, new Http.QueryParameters("lifecycle", ""))
        .thenAccept(response -> response.close());
  }

  /**
   * Gets lifecycle configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<LifecycleConfiguration> future =
   *     minioAsyncClient.getBucketLifecycle(
   *         GetBucketLifecycleArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketLifecycleArgs} object.
   * @return {@link LifecycleConfiguration} object.
   * @return {@link CompletableFuture}&lt;{@link LifecycleConfiguration}&gt; object.
   */
  public CompletableFuture<LifecycleConfiguration> getBucketLifecycle(GetBucketLifecycleArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("lifecycle", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals("NoSuchLifecycleConfiguration")) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return null;
              try {
                return Xml.unmarshal(LifecycleConfiguration.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Gets notification configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<NotificationConfiguration> future =
   *     minioAsyncClient.getBucketNotification(
   *         GetBucketNotificationArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketNotificationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link NotificationConfiguration}&gt; object.
   */
  public CompletableFuture<NotificationConfiguration> getBucketNotification(
      GetBucketNotificationArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("notification", ""))
        .thenApply(
            response -> {
              try {
                return Xml.unmarshal(NotificationConfiguration.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets notification configuration to a bucket.
   *
   * <pre>Example:{@code
   * List<EventType> eventList = new ArrayList<>();
   * eventList.add(EventType.OBJECT_CREATED_PUT);
   * eventList.add(EventType.OBJECT_CREATED_COPY);
   *
   * QueueConfiguration queueConfiguration = new QueueConfiguration();
   * queueConfiguration.setQueue("arn:minio:sqs::1:webhook");
   * queueConfiguration.setEvents(eventList);
   * queueConfiguration.setPrefixRule("images");
   * queueConfiguration.setSuffixRule("pg");
   *
   * List<QueueConfiguration> queueConfigurationList = new ArrayList<>();
   * queueConfigurationList.add(queueConfiguration);
   *
   * NotificationConfiguration config = new NotificationConfiguration();
   * config.setQueueConfigurationList(queueConfigurationList);
   *
   * CompletableFuture<Void> future = minioAsyncClient.setBucketNotification(
   *     SetBucketNotificationArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketNotificationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketNotification(SetBucketNotificationArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("notification", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Deletes notification configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.deleteBucketNotification(
   *     DeleteBucketNotificationArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketNotificationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteBucketNotification(DeleteBucketNotificationArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(new NotificationConfiguration(null, null, null, null), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("notification", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Gets bucket replication configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<ReplicationConfiguration> future =
   *     minioAsyncClient.getBucketReplication(
   *         GetBucketReplicationArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketReplicationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ReplicationConfiguration}&gt; object.
   */
  public CompletableFuture<ReplicationConfiguration> getBucketReplication(
      GetBucketReplicationArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("replication", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals("ReplicationConfigurationNotFoundError")) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return null;
              try {
                return Xml.unmarshal(ReplicationConfiguration.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets bucket replication configuration to a bucket.
   *
   * <pre>Example:{@code
   * Map<String, String> tags = new HashMap<>();
   * tags.put("key1", "value1");
   * tags.put("key2", "value2");
   *
   * ReplicationRule rule =
   *     new ReplicationRule(
   *         new DeleteMarkerReplication(Status.DISABLED),
   *         new ReplicationDestination(
   *             null, null, "REPLACE-WITH-ACTUAL-DESTINATION-BUCKET-ARN", null, null, null, null),
   *         null,
   *         new RuleFilter(new AndOperator("TaxDocs", tags)),
   *         "rule1",
   *         null,
   *         1,
   *         null,
   *         Status.ENABLED);
   *
   * List<ReplicationRule> rules = new ArrayList<>();
   * rules.add(rule);
   *
   * ReplicationConfiguration config =
   *     new ReplicationConfiguration("REPLACE-WITH-ACTUAL-ROLE", rules);
   *
   * CompletableFuture<Void> future = minioAsyncClient.setBucketReplication(
   *     SetBucketReplicationArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketReplicationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketReplication(SetBucketReplicationArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(
            args,
            (args.objectLockToken() != null)
                ? new Http.Headers("x-amz-bucket-object-lock-token", args.objectLockToken())
                : null,
            new Http.QueryParameters("replication", ""),
            body)
        .thenAccept(response -> response.close());
  }

  /**
   * Deletes bucket replication configuration from a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.deleteBucketReplication(
   *     DeleteBucketReplicationArgs.builder().bucket("my-bucketname"));
   * }</pre>
   *
   * @param args {@link DeleteBucketReplicationArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteBucketReplication(DeleteBucketReplicationArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, new Http.QueryParameters("replication", ""))
        .thenAccept(response -> response.close());
  }

  /**
   * Listens events of object prefix and suffix of a bucket. The returned closable iterator is
   * lazily evaluated hence its required to iterate to get new records and must be used with
   * try-with-resource to release underneath network resources.
   *
   * <pre>Example:{@code
   * String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
   * try (CloseableIterator<Result<NotificationRecords>> ci =
   *     minioAsyncClient.listenBucketNotification(
   *         ListenBucketNotificationArgs.builder()
   *             .bucket("bucketName")
   *             .prefix("")
   *             .suffix("")
   *             .events(events)
   *             .build())) {
   *   while (ci.hasNext()) {
   *     NotificationRecords records = ci.next().get();
   *     for (Event event : records.events()) {
   *       System.out.println("Event " + event.eventType() + " occurred at "
   *           + event.eventTime() + " for " + event.bucketName() + "/"
   *           + event.objectName());
   *     }
   *   }
   * }
   * }</pre>
   *
   * @param args {@link ListenBucketNotificationArgs} object.
   * @return {@code CloseableIterator<Result<NotificationRecords>>} - Lazy closable iterator
   *     contains event records.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public CloseableIterator<Result<NotificationRecords>> listenBucketNotification(
      ListenBucketNotificationArgs args) throws MinioException {
    checkArgs(args);

    Http.QueryParameters queryParams =
        new Http.QueryParameters("prefix", args.prefix(), "suffix", args.suffix());
    for (String event : args.events()) {
      queryParams.put("events", event);
    }

    Response response = null;
    try {
      response = executeGetAsync(args, null, queryParams).join();
    } catch (CompletionException e) {
      throwMinioException(e);
    }
    NotificationResultRecords result = new NotificationResultRecords(response);
    return result.closeableIterator();
  }

  /**
   * Selects content of an object by SQL expression.
   *
   * <pre>Example:{@code
   * String sqlExpression = "select * from S3Object";
   * InputSerialization is =
   *     new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null,
   *         null);
   * OutputSerialization os =
   *     new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);
   * SelectResponseStream stream =
   *     minioAsyncClient.selectObjectContent(
   *       SelectObjectContentArgs.builder()
   *       .bucket("my-bucketname")
   *       .object("my-objectname")
   *       .sqlExpression(sqlExpression)
   *       .inputSerialization(is)
   *       .outputSerialization(os)
   *       .requestProgress(true)
   *       .build());
   *
   * byte[] buf = new byte[512];
   * int bytesRead = stream.read(buf, 0, buf.length);
   * System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
   *
   * Stats stats = stream.stats();
   * System.out.println("bytes scanned: " + stats.bytesScanned());
   * System.out.println("bytes processed: " + stats.bytesProcessed());
   * System.out.println("bytes returned: " + stats.bytesReturned());
   *
   * stream.close();
   * }</pre>
   *
   * @param args instance of {@link SelectObjectContentArgs}
   * @return {@link SelectResponseStream} - Contains filtered records and progress.
   * @throws MinioException thrown to indicate SDK exception.
   */
  public SelectResponseStream selectObjectContent(SelectObjectContentArgs args)
      throws MinioException {
    checkArgs(args);
    args.validateSsec(this.baseUrl.isHttps());
    Response response = null;
    try {
      response =
          executePostAsync(
                  args,
                  args.ssec() == null ? null : args.ssec().headers(),
                  new Http.QueryParameters("select", "", "select-type", "2"),
                  new Http.Body(
                      new SelectObjectContentRequest(
                          args.sqlExpression(),
                          args.requestProgress(),
                          args.inputSerialization(),
                          args.outputSerialization(),
                          args.scanStartRange(),
                          args.scanEndRange()),
                      null,
                      null,
                      null))
              .join();
    } catch (CompletionException e) {
      throwMinioException(e);
    }
    return new SelectResponseStream(response.body().byteStream());
  }

  /**
   * Sets encryption configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.setBucketEncryption(
   *     SetBucketEncryptionArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketEncryptionArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketEncryption(SetBucketEncryptionArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("encryption", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Gets encryption configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<SseConfiguration> future =
   *     minioAsyncClient.getBucketEncryption(
   *         GetBucketEncryptionArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketEncryptionArgs} object.
   * @return {@link CompletableFuture}&lt;{@link SseConfiguration}&gt; object.
   */
  public CompletableFuture<SseConfiguration> getBucketEncryption(GetBucketEncryptionArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("encryption", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals(SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return new SseConfiguration(null);
              try {
                return Xml.unmarshal(SseConfiguration.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Deletes encryption configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.deleteBucketEncryption(
   *     DeleteBucketEncryptionArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketEncryptionArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteBucketEncryption(DeleteBucketEncryptionArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, new Http.QueryParameters("encryption", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals(SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR)) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenAccept(
            response -> {
              if (response != null) response.close();
            });
  }

  /**
   * Gets tags of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Tags> future =
   *     minioAsyncClient.getBucketTags(GetBucketTagsArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketTagsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Tags}&gt; object.
   */
  public CompletableFuture<Tags> getBucketTags(GetBucketTagsArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("tagging", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e).errorResponse().code().equals("NoSuchTagSet")) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return new Tags();
              try {
                return Xml.unmarshal(Tags.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets tags to a bucket.
   *
   * <pre>Example:{@code
   * Map<String, String> map = new HashMap<>();
   * map.put("Project", "Project One");
   * map.put("User", "jsmith");
   * CompletableFuture<Void> future = minioAsyncClient.setBucketTags(
   *     SetBucketTagsArgs.builder().bucket("my-bucketname").tags(map).build());
   * }</pre>
   *
   * @param args {@link SetBucketTagsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketTags(SetBucketTagsArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.tags(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("tagging", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Deletes tags of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.deleteBucketTags(
   *     DeleteBucketTagsArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketTagsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteBucketTags(DeleteBucketTagsArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, new Http.QueryParameters("tagging", ""))
        .thenAccept(response -> response.close());
  }

  /**
   * Gets tags of an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<Tags> future =
   *     minioAsyncClient.getObjectTags(
   *         GetObjectTagsArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   * }</pre>
   *
   * @param args {@link GetObjectTagsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Tags}&gt; object.
   */
  public CompletableFuture<Tags> getObjectTags(GetObjectTagsArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("tagging", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    return executeGetAsync(args, null, queryParams)
        .thenApply(
            response -> {
              try {
                return Xml.unmarshal(Tags.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets tags to an object.
   *
   * <pre>Example:{@code
   * Map<String, String> map = new HashMap<>();
   * map.put("Project", "Project One");
   * map.put("User", "jsmith");
   * CompletableFuture<Void> future = minioAsyncClient.setObjectTags(
   *     SetObjectTagsArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .tags((map)
   *         .build());
   * }</pre>
   *
   * @param args {@link SetObjectTagsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setObjectTags(SetObjectTagsArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.tags(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    Http.QueryParameters queryParams = new Http.QueryParameters("tagging", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    return executePutAsync(args, null, queryParams, body).thenAccept(response -> response.close());
  }

  /**
   * Deletes tags of an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.deleteObjectTags(
   *     DeleteObjectTags.builder().bucket("my-bucketname").object("my-objectname").build());
   * }</pre>
   *
   * @param args {@link DeleteObjectTagsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteObjectTags(DeleteObjectTagsArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("tagging", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    return executeDeleteAsync(args, null, queryParams).thenAccept(response -> response.close());
  }

  /**
   * Gets CORS configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<CORSConfiguration> future =
   *     minioAsyncClient.getBucketCors(GetBucketCorsArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketCorsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link CORSConfiguration}&gt; object.
   */
  public CompletableFuture<CORSConfiguration> getBucketCors(GetBucketCorsArgs args) {
    checkArgs(args);
    return executeGetAsync(args, null, new Http.QueryParameters("cors", ""))
        .exceptionally(
            e -> {
              e = e.getCause();
              if (e instanceof ErrorResponseException) {
                if (((ErrorResponseException) e)
                    .errorResponse()
                    .code()
                    .equals("NoSuchCORSConfiguration")) {
                  return null;
                }
              }
              throw new CompletionException(e);
            })
        .thenApply(
            response -> {
              if (response == null) return new CORSConfiguration(null);
              try {
                return Xml.unmarshal(CORSConfiguration.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Sets CORS configuration to a bucket.
   *
   * <pre>Example:{@code
   * CORSConfiguration config =
   *     new CORSConfiguration(
   *         Arrays.asList(
   *             new CORSConfiguration.CORSRule[] {
   *               // Rule 1
   *               new CORSConfiguration.CORSRule(
   *                   Arrays.asList(new String[] {"*"}), // Allowed headers
   *                   Arrays.asList(new String[] {"PUT", "POST", "DELETE"}), // Allowed methods
   *                   Arrays.asList(new String[] {"http://www.example.com"}), // Allowed origins
   *                   Arrays.asList(
   *                       new String[] {"x-amz-server-side-encryption"}), // Expose headers
   *                   null, // ID
   *                   3000), // Maximum age seconds
   *               // Rule 2
   *               new CORSConfiguration.CORSRule(
   *                   null, // Allowed headers
   *                   Arrays.asList(new String[] {"GET"}), // Allowed methods
   *                   Arrays.asList(new String[] {"*"}), // Allowed origins
   *                   null, // Expose headers
   *                   null, // ID
   *                   null // Maximum age seconds
   *                   )
   *             }));
   * CompletableFuture<Void> future = minioAsyncClient.setBucketCors(
   *     SetBucketCorsArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketCorsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> setBucketCors(SetBucketCorsArgs args) {
    checkArgs(args);
    Http.Body body = null;
    try {
      body = new Http.Body(args.config(), null, null, null);
    } catch (MinioException e) {
      return Utils.failedFuture(e);
    }
    return executePutAsync(args, null, new Http.QueryParameters("cors", ""), body)
        .thenAccept(response -> response.close());
  }

  /**
   * Deletes CORS configuration of a bucket.
   *
   * <pre>Example:{@code
   * CompletableFuture<Void> future = minioAsyncClient.deleteBucketCors(
   *     DeleteBucketCorsArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketCorsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link Void}&gt; object.
   */
  public CompletableFuture<Void> deleteBucketCors(DeleteBucketCorsArgs args) {
    checkArgs(args);
    return executeDeleteAsync(args, null, new Http.QueryParameters("cors", ""))
        .thenAccept(response -> response.close());
  }

  /**
   * Gets access control policy of an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<AccessControlPolicy> future =
   *     minioAsyncClient.getObjectAcl(
   *         GetObjectAclArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   * }</pre>
   *
   * @param args {@link GetObjectAclArgs} object.
   * @return {@link CompletableFuture}&lt;{@link AccessControlPolicy}&gt; object.
   */
  public CompletableFuture<AccessControlPolicy> getObjectAcl(GetObjectAclArgs args) {
    checkArgs(args);
    Http.QueryParameters queryParams = new Http.QueryParameters("acl", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    return executeGetAsync(args, null, queryParams)
        .thenApply(
            response -> {
              try {
                return Xml.unmarshal(AccessControlPolicy.class, response.body().charStream());
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Gets attributes of an object.
   *
   * <pre>Example:{@code
   * CompletableFuture<GetObjectAttributesResponse> future =
   *     minioAsyncClient.getObjectAttributes(
   *         GetObjectAttributesArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .objectAttributes(
   *                 new String[] {
   *                   "ETag", "Checksum", "ObjectParts", "StorageClass", "ObjectSize"
   *                 })
   *             .build());
   * }</pre>
   *
   * @param args {@link GetObjectAttributesArgs} object.
   * @return {@link CompletableFuture}&lt;{@link GetObjectAttributesResponse}&gt; object.
   */
  public CompletableFuture<GetObjectAttributesResponse> getObjectAttributes(
      GetObjectAttributesArgs args) {
    checkArgs(args);

    Http.QueryParameters queryParams = new Http.QueryParameters("attributes", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());

    Http.Headers headers = new Http.Headers();
    if (args.maxParts() != null) headers.put("x-amz-max-parts", args.maxParts().toString());
    if (args.partNumberMarker() != null) {
      headers.put("x-amz-part-number-marker", args.partNumberMarker().toString());
    }
    for (String attribute : args.objectAttributes()) {
      if (attribute != null) headers.put("x-amz-object-attributes", attribute);
    }

    return executeGetAsync(args, headers, queryParams)
        .thenApply(
            response -> {
              try {
                GetObjectAttributesOutput result =
                    Xml.unmarshal(GetObjectAttributesOutput.class, response.body().charStream());

                String value = response.headers().get("x-amz-delete-marker");
                if (value != null) result.setDeleteMarker(Boolean.valueOf(value));
                value = response.headers().get("Last-Modified");
                if (value != null) {
                  result.setLastModified(ZonedDateTime.parse(value, Time.HTTP_HEADER_DATE_FORMAT));
                }
                result.setVersionId(response.headers().get("x-amz-version-id"));

                return new GetObjectAttributesResponse(
                    response.headers(), args.bucket(), args.region(), args.object(), result);
              } catch (XmlParserException e) {
                throw new CompletionException(e);
              } finally {
                response.close();
              }
            });
  }

  /**
   * Uploads multiple objects in a single put call. It is done by creating intermediate TAR file
   * optionally compressed which is uploaded to S3 service.
   *
   * <pre>Example:{@code
   * // Upload snowball objects.
   * List<SnowballObject> objects = new ArrayList<SnowballObject>();
   * objects.add(
   *     new SnowballObject(
   *         "my-object-one",
   *         new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
   *         5,
   *         null));
   * objects.add(
   *     new SnowballObject(
   *         "my-object-two",
   *         new ByteArrayInputStream("java".getBytes(StandardCharsets.UTF_8)),
   *         4,
   *         null));
   * CompletableFuture<ObjectWriteResponse> future = minioAsyncClient.uploadSnowballObjects(
   *     UploadSnowballObjectsArgs.builder().bucket("my-bucketname").objects(objects).build());
   * }</pre>
   *
   * @param args {@link UploadSnowballObjectsArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> uploadSnowballObjects(
      UploadSnowballObjectsArgs args) {
    checkArgs(args);

    return CompletableFuture.supplyAsync(
            () -> {
              FileOutputStream fos = null;
              BufferedOutputStream bos = null;
              SnappyFramedOutputStream sos = null;
              ByteArrayOutputStream baos = null;
              TarArchiveOutputStream tarOutputStream = null;

              try {
                OutputStream os = null;
                if (args.stagingFilename() != null) {
                  fos = new FileOutputStream(args.stagingFilename());
                  bos = new BufferedOutputStream(fos);
                  os = bos;
                } else {
                  baos = new ByteArrayOutputStream();
                  os = baos;
                }

                if (args.compression()) {
                  sos = new SnappyFramedOutputStream(os);
                  os = sos;
                }

                tarOutputStream = new TarArchiveOutputStream(os);
                tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                for (SnowballObject object : args.objects()) {
                  if (object.filename() != null) {
                    Path filePath = Paths.get(object.filename());
                    TarArchiveEntry entry = new TarArchiveEntry(filePath.toFile(), object.name());
                    tarOutputStream.putArchiveEntry(entry);
                    Files.copy(filePath, tarOutputStream);
                  } else {
                    TarArchiveEntry entry = new TarArchiveEntry(object.name());
                    if (object.modificationTime() != null) {
                      entry.setModTime(Date.from(object.modificationTime().toInstant()));
                    }
                    entry.setSize(object.size());
                    tarOutputStream.putArchiveEntry(entry);
                    ByteStreams.copy(object.stream(), tarOutputStream);
                  }
                  tarOutputStream.closeArchiveEntry();
                }
                tarOutputStream.finish();
              } catch (IOException e) {
                throw new CompletionException(new MinioException(e));
              } finally {
                try {
                  if (tarOutputStream != null) tarOutputStream.flush();
                  if (sos != null) sos.flush();
                  if (bos != null) bos.flush();
                  if (fos != null) fos.flush();
                  if (tarOutputStream != null) tarOutputStream.close();
                  if (sos != null) sos.close();
                  if (bos != null) bos.close();
                  if (fos != null) fos.close();
                } catch (IOException e) {
                  throw new CompletionException(new MinioException(e));
                }
              }
              return baos;
            })
        .thenCompose(
            baos -> {
              Http.Headers headers = args.makeHeaders();
              headers.put("X-Amz-Meta-Snowball-Auto-Extract", "true");

              if (args.stagingFilename() == null) {
                byte[] data = baos.toByteArray();
                return putObject(new PutObjectAPIArgs(args, data, data.length, headers));
              }

              long length = Paths.get(args.stagingFilename()).toFile().length();
              if (length > ObjectWriteArgs.MAX_OBJECT_SIZE) {
                throw new IllegalArgumentException(
                    "tarball size " + length + " is more than maximum allowed 5TiB");
              }
              try (RandomAccessFile file = new RandomAccessFile(args.stagingFilename(), "r")) {
                return putObject(new PutObjectAPIArgs(args, file, length, headers));
              } catch (IOException e) {
                throw new CompletionException(new MinioException(e));
              }
            });
  }

  /**
   * Uploads multiple objects with same content from single stream with optional metadata and tags.
   *
   * <pre>Example:{@code
   * Map<String, String> map = new HashMap<>();
   * map.put("Project", "Project One");
   * map.put("User", "jsmith");
   * CompletableFuture<PutObjectFanOutResponse> future =
   *     minioAsyncClient.putObjectFanOut(
   *         PutObjectFanOutArgs.builder().bucket("my-bucketname").stream(
   *                 new ByteArrayInputStream("somedata".getBytes(StandardCharsets.UTF_8)), 8)
   *             .entries(
   *                 Arrays.asList(
   *                     new PutObjectFanOutEntry[] {
   *                       PutObjectFanOutEntry.builder().key("fan-out.0").build(),
   *                       PutObjectFanOutEntry.builder().key("fan-out.1").tags(map).build()
   *                     }))
   *             .build());
   * }</pre>
   *
   * @param args {@link PutObjectFanOutArgs} object.
   * @return {@link CompletableFuture}&lt;{@link PutObjectFanOutResponse}&gt; object.
   */
  public CompletableFuture<PutObjectFanOutResponse> putObjectFanOut(PutObjectFanOutArgs args) {
    checkArgs(args);
    args.validateSse(this.baseUrl.isHttps());

    return CompletableFuture.supplyAsync(
            () -> {
              byte[] buf16k = new byte[16384]; // 16KiB buffer for optimization.
              ByteBuffer buffer = new ByteBuffer(args.size());
              long bytesWritten = 0;
              while (bytesWritten != args.size()) {
                try {
                  int length = args.stream().read(buf16k);
                  if (length < 0) {
                    throw new InsufficientDataException(
                        "insufficient data; expected=" + args.size() + ", got=" + bytesWritten);
                  }
                  buffer.write(buf16k, 0, length);
                  bytesWritten += length;
                } catch (IOException e) {
                  throw new CompletionException(new MinioException(e));
                } catch (MinioException e) {
                  throw new CompletionException(e);
                }
              }

              // Build POST object data
              String objectName =
                  "pan-out-"
                      + new BigInteger(32, RANDOM).toString(32)
                      + "-"
                      + System.currentTimeMillis();
              PostPolicy policy =
                  new PostPolicy(args.bucket(), ZonedDateTime.now().plusMinutes(15));
              policy.addEqualsCondition("key", objectName);
              if (args.sse() != null) {
                for (Map.Entry<String, String> entry : args.sse().headers().entrySet()) {
                  policy.addEqualsCondition(entry.getKey(), entry.getValue());
                }
              }
              if (args.checksum() != null) {
                for (Map.Entry<String, String> entry : args.checksum().headers().entries()) {
                  policy.addEqualsCondition(entry.getKey(), entry.getValue());
                }
              }

              try {
                Map<String, String> formData = this.getPresignedPostFormData(policy);

                // Build MultipartBody
                MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
                multipartBuilder.setType(MultipartBody.FORM);
                for (Map.Entry<String, String> entry : formData.entrySet()) {
                  multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
                }
                multipartBuilder.addFormDataPart("key", objectName);
                multipartBuilder.addFormDataPart("x-minio-fanout-list", args.fanOutList());
                // "file" must be added at last.
                multipartBuilder.addFormDataPart(
                    "file",
                    "fanout-content",
                    new Http.RequestBody(buffer, Http.DEFAULT_MEDIA_TYPE));

                return multipartBuilder.build();
              } catch (JsonProcessingException e) {
                throw new CompletionException(new MinioException(e));
              } catch (MinioException e) {
                throw new CompletionException(e);
              }
            })
        .thenCompose(body -> executePostAsync(args, null, null, new Http.Body(body)))
        .thenApply(
            response -> {
              try {
                JsonFactory jsonFactory = new JsonFactory();
                Iterator<PutObjectFanOutResponse.Result> iterator =
                    OBJECT_MAPPER.readValues(
                        jsonFactory.createParser(response.body().byteStream()),
                        PutObjectFanOutResponse.Result.class);
                List<PutObjectFanOutResponse.Result> results = new ArrayList<>();
                iterator.forEachRemaining(results::add);
                return new PutObjectFanOutResponse(
                    response.headers(), args.bucket(), args.region(), results);
              } catch (IOException e) {
                throw new CompletionException(new MinioException(e));
              } finally {
                response.close();
              }
            });
  }

  /**
   * Performs language model inference with the prompt and referenced object as context.
   *
   * @param args {@link PromptObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link PromptObjectResponse}&gt; object.
   */
  public CompletableFuture<PromptObjectResponse> promptObject(PromptObjectArgs args) {
    checkArgs(args);

    Http.QueryParameters queryParams = new Http.QueryParameters("lambdaArn", args.lambdaArn());
    if (args.versionId() == null) queryParams.put("versionId", args.versionId());
    Http.Headers headers =
        Http.Headers.merge(
            new Http.Headers(args.headers()),
            new Http.Headers(Http.Headers.CONTENT_TYPE, Http.JSON_MEDIA_TYPE.toString()));

    Map<String, Object> promptArgs = args.promptArgs();
    if (promptArgs == null) promptArgs = new HashMap<>();
    promptArgs.put("prompt", args.prompt());
    try {
      byte[] data = OBJECT_MAPPER.writeValueAsString(promptArgs).getBytes(StandardCharsets.UTF_8);
      return executePostAsync(
              args, headers, queryParams, new Http.Body(data, data.length, null, null, null))
          .thenApply(
              response -> {
                return new PromptObjectResponse(
                    response.headers(),
                    args.bucket(),
                    args.region(),
                    args.object(),
                    response.body().byteStream());
              });
    } catch (JsonProcessingException e) {
      return Utils.failedFuture(new MinioException(e));
    }
  }

  private CompletableFuture<ObjectWriteResponse> appendObject(
      AppendObjectArgs args,
      long writeOffset,
      PartReader partReader,
      ByteBuffer buffer,
      byte[] data,
      Long length,
      RandomAccessFile file,
      Long partSize,
      Map<Checksum.Algorithm, Checksum.Hasher> hashers,
      boolean addContentSha256,
      boolean addSha256Checksum) {
    Http.Headers headers =
        new Http.Headers("x-amz-write-offset-bytes", String.valueOf(writeOffset));

    if (data != null) {
      if (hashers != null) {
        Checksum.update(hashers, data, length.intValue());
        headers.putAll(Checksum.makeHeaders(hashers, addContentSha256, addSha256Checksum));
      }
      return putObject(new PutObjectAPIArgs(args, data, length.intValue(), headers));
    }

    if (partReader != null) {
      if (partReader.hashers() != null) {
        headers.putAll(
            Checksum.makeHeaders(partReader.hashers(), addContentSha256, addSha256Checksum));
      }
      return putObject(new PutObjectAPIArgs(args, buffer, headers))
          .thenCompose(
              response -> {
                long finalWriteOffset = writeOffset + buffer.length();
                try {
                  buffer.reset();
                  if (partReader.partNumber() == partReader.partCount()) {
                    return CompletableFuture.completedFuture(response);
                  }
                  partReader.read(buffer);
                } catch (MinioException e) {
                  return Utils.failedFuture(e);
                }
                return appendObject(
                    args,
                    finalWriteOffset,
                    partReader,
                    buffer,
                    null,
                    null,
                    null,
                    null,
                    null,
                    addContentSha256,
                    addSha256Checksum);
              });
    }

    long size = Math.min(length, partSize);
    if (hashers != null) {
      for (Map.Entry<Checksum.Algorithm, Checksum.Hasher> entry : hashers.entrySet()) {
        entry.getValue().reset();
      }

      try {
        long position = file.getFilePointer();
        Checksum.update(hashers, file, size);
        file.seek(position);
      } catch (MinioException e) {
        return Utils.failedFuture(e);
      } catch (IOException e) {
        return Utils.failedFuture(new MinioException(e));
      }

      headers.putAll(Checksum.makeHeaders(hashers, addContentSha256, addSha256Checksum));
    }

    return putObject(new PutObjectAPIArgs(args, file, size, headers))
        .thenCompose(
            response -> {
              long finalWriteOffset = writeOffset + size;
              long finalLength = length - size;
              if (finalLength == 0) {
                return CompletableFuture.completedFuture(response);
              }
              return appendObject(
                  args,
                  finalWriteOffset,
                  null,
                  null,
                  null,
                  finalLength,
                  file,
                  partSize,
                  hashers,
                  addContentSha256,
                  addSha256Checksum);
            });
  }

  /**
   * Appends from a file, stream or data to existing object in a bucket.
   *
   * @param args {@link AppendObjectArgs} object.
   * @return {@link CompletableFuture}&lt;{@link ObjectWriteResponse}&gt; object.
   */
  public CompletableFuture<ObjectWriteResponse> appendObject(AppendObjectArgs args) {
    checkArgs(args);
    return headObject(new HeadObjectArgs(args))
        .thenCompose(
            response -> {
              Checksum.Algorithm algorithm = null;
              if (response.checksumType() != null) {
                if (response.checksumType() != Checksum.Type.FULL_OBJECT) {
                  return Utils.failedFuture(
                      new MinioException(
                          "append object does not support checksum type "
                              + response.checksumType()));
                }
                List<Checksum.Algorithm> algorithms = response.algorithms();
                if (algorithms != null) algorithm = algorithms.get(0);
              }

              long writeOffset = response.size();

              long partSize =
                  args.chunkSize() != null ? args.chunkSize() : ObjectWriteArgs.MIN_MULTIPART_SIZE;

              boolean addContentSha256 = !this.baseUrl.isHttps();
              boolean addSha256Checksum = algorithm == Checksum.Algorithm.SHA256;
              Checksum.Algorithm[] algorithms;
              if (addContentSha256 && !addSha256Checksum) {
                algorithms =
                    (algorithm != null)
                        ? new Checksum.Algorithm[] {algorithm, Checksum.Algorithm.SHA256}
                        : new Checksum.Algorithm[] {Checksum.Algorithm.SHA256};
              } else {
                algorithms =
                    (algorithm != null)
                        ? new Checksum.Algorithm[] {algorithm}
                        : new Checksum.Algorithm[0];
              }

              if (args.stream() != null) {
                try {
                  int partCount =
                      args.length() == null
                          ? -1
                          : (int) Math.max((args.length() + partSize - 1) / partSize, 1);
                  PartReader partReader =
                      new PartReader(args.stream(), args.length(), partSize, partCount, algorithms);
                  ByteBuffer buffer =
                      new ByteBuffer(partReader.partCount() == 1 ? args.length() : partSize);
                  partReader.read(buffer);
                  return appendObject(
                      args,
                      writeOffset,
                      partReader,
                      buffer,
                      null,
                      null,
                      null,
                      null,
                      null,
                      addContentSha256,
                      addSha256Checksum);
                } catch (MinioException e) {
                  return Utils.failedFuture(e);
                }
              }

              try {
                Map<Checksum.Algorithm, Checksum.Hasher> hashers =
                    Checksum.newHasherMap(algorithms);
                if (args.data() != null) {
                  return appendObject(
                      args,
                      writeOffset,
                      null,
                      null,
                      args.data(),
                      args.length(),
                      null,
                      null,
                      hashers,
                      addContentSha256,
                      addSha256Checksum);
                }

                RandomAccessFile file = new RandomAccessFile(args.filename(), "r");
                return appendObject(
                    args,
                    writeOffset,
                    null,
                    null,
                    null,
                    args.length(),
                    file,
                    partSize,
                    hashers,
                    addContentSha256,
                    addSha256Checksum);
              } catch (MinioException e) {
                return Utils.failedFuture(e);
              } catch (IOException e) {
                return Utils.failedFuture(new MinioException(e));
              }
            });
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////// Higher level ListObjects implementation ///////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////

  /** Throws encapsulated exception wrapped by {@link CompletionException}. */
  public void throwMinioException(CompletionException e) throws MinioException {
    if (e == null) return;
    Throwable ex = e.getCause();
    if (ex instanceof MinioException) throw (MinioException) ex;
    Throwable exc = ex.getCause();
    throw new RuntimeException(exc != null ? exc : ex);
  }

  private abstract class ObjectIterator implements Iterator<Result<Item>> {
    protected Result<Item> error;
    protected Iterator<? extends Item> itemIterator;
    protected Iterator<ListVersionsResult.DeleteMarker> deleteMarkerIterator;
    protected Iterator<ListObjectsResult.Prefix> prefixIterator;
    protected boolean completed = false;
    protected ListObjectsResult listObjectsResult;
    protected String lastObjectName;

    protected abstract void populateResult() throws MinioException;

    protected synchronized void populate() {
      try {
        populateResult();
      } catch (MinioException e) {
        this.error = new Result<>(e);
      }

      if (this.listObjectsResult != null) {
        this.itemIterator = this.listObjectsResult.contents().iterator();
        this.deleteMarkerIterator = this.listObjectsResult.deleteMarkers().iterator();
        this.prefixIterator = this.listObjectsResult.commonPrefixes().iterator();
      } else {
        this.itemIterator = Collections.emptyIterator();
        this.deleteMarkerIterator = Collections.emptyIterator();
        this.prefixIterator = Collections.emptyIterator();
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
  protected Iterable<Result<Item>> objectV1Lister(ListObjectsV1Args args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          private ListBucketResultV1 result = null;

          @Override
          protected void populateResult() throws MinioException {
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
                      .join();
              result = response.result();
              this.listObjectsResult = response.result();
            } catch (CompletionException e) {
              throwMinioException(e);
            }
          }
        };
      }
    };
  }

  /** Execute list objects v2. */
  protected Iterable<Result<Item>> objectV2Lister(ListObjectsV2Args args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          private ListBucketResultV2 result = null;

          @Override
          protected void populateResult() throws MinioException {
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
                      .join();
              result = response.result();
              this.listObjectsResult = response.result();
            } catch (CompletionException e) {
              throwMinioException(e);
            }
          }
        };
      }
    };
  }

  /** Execute list object versions. */
  protected Iterable<Result<Item>> objectVersionLister(ListObjectVersionsArgs args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          private ListVersionsResult result = null;

          @Override
          protected void populateResult() throws MinioException {
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
                      .join();
              result = response.result();
              this.listObjectsResult = response.result();
            } catch (CompletionException e) {
              throwMinioException(e);
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
          } catch (JsonMappingException | JsonParseException e) {
            return new Result<>(new MinioException(e));
          } catch (IOException e) {
            return new Result<>(new MinioException(e));
          } finally {
            recordsString = null;
            records = null;
          }
        }
      };
    }
  }
}
