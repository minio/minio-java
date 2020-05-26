/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015-2020 MinIO, Inc.
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

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.BucketExistsArgs;
import io.minio.CloseableIterator;
import io.minio.ComposeSource;
import io.minio.CopyConditions;
import io.minio.DeleteBucketEncryptionArgs;
import io.minio.DeleteBucketLifeCycleArgs;
import io.minio.DeleteBucketNotificationArgs;
import io.minio.DeleteBucketPolicyArgs;
import io.minio.DeleteBucketTagsArgs;
import io.minio.DeleteDefaultRetentionArgs;
import io.minio.DeleteObjectTagsArgs;
import io.minio.DisableObjectLegalHoldArgs;
import io.minio.DisableVersioningArgs;
import io.minio.DownloadObjectArgs;
import io.minio.EnableObjectLegalHoldArgs;
import io.minio.EnableVersioningArgs;
import io.minio.ErrorCode;
import io.minio.GetBucketEncryptionArgs;
import io.minio.GetBucketLifeCycleArgs;
import io.minio.GetBucketNotificationArgs;
import io.minio.GetBucketPolicyArgs;
import io.minio.GetBucketTagsArgs;
import io.minio.GetDefaultRetentionArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectRetentionArgs;
import io.minio.GetObjectTagsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.IsObjectLegalHoldEnabledArgs;
import io.minio.IsVersioningEnabledArgs;
import io.minio.ListObjectsArgs;
import io.minio.ListenBucketNotificationArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.PostPolicy;
import io.minio.PutObjectOptions;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveIncompleteUploadArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SelectObjectContentArgs;
import io.minio.SelectResponseStream;
import io.minio.ServerSideEncryption;
import io.minio.ServerSideEncryptionCustomerKey;
import io.minio.SetBucketEncryptionArgs;
import io.minio.SetBucketLifeCycleArgs;
import io.minio.SetBucketNotificationArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.SetBucketTagsArgs;
import io.minio.SetDefaultRetentionArgs;
import io.minio.SetObjectRetentionArgs;
import io.minio.SetObjectTagsArgs;
import io.minio.StatObjectArgs;
import io.minio.Time;
import io.minio.Xml;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Event;
import io.minio.messages.EventType;
import io.minio.messages.FileHeaderInfo;
import io.minio.messages.InputSerialization;
import io.minio.messages.NotificationConfiguration;
import io.minio.messages.NotificationRecords;
import io.minio.messages.ObjectLockConfiguration;
import io.minio.messages.OutputSerialization;
import io.minio.messages.QueueConfiguration;
import io.minio.messages.QuoteFields;
import io.minio.messages.Retention;
import io.minio.messages.RetentionDuration;
import io.minio.messages.RetentionDurationDays;
import io.minio.messages.RetentionDurationYears;
import io.minio.messages.RetentionMode;
import io.minio.messages.SseAlgorithm;
import io.minio.messages.SseConfiguration;
import io.minio.messages.SseConfigurationRule;
import io.minio.messages.Stats;
import io.minio.messages.Tags;
import io.minio.messages.Upload;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

@SuppressFBWarnings(
    value = "REC",
    justification = "Allow catching super class Exception since it's tests")
public class FunctionalTest {
  private static final String OS = System.getProperty("os.name").toLowerCase(Locale.US);
  private static final String MINIO_BINARY;
  private static final String PASS = "PASS";
  private static final String FAILED = "FAIL";
  private static final String IGNORED = "NA";
  private static final int KB = 1024;
  private static final int MB = 1024 * 1024;
  private static final Random random = new Random(new SecureRandom().nextLong());
  private static final String customContentType = "application/javascript";
  private static final String nullContentType = null;
  private static String bucketName = getRandomName();
  private static boolean mintEnv = false;
  private static Path dataFile1Kb;
  private static Path dataFile6Mb;
  private static String endpoint;
  private static String accessKey;
  private static String secretKey;
  private static String region;
  private static String sqsArn = null;
  private static MinioClient client = null;

  static {
    String binaryName = "minio";
    if (OS.contains("windows")) {
      binaryName = "minio.exe";
    }

    MINIO_BINARY = binaryName;
  }

  /** Do no-op. */
  public static void ignore(Object... args) {}

  /** Create given sized file and returns its name. */
  public static String createFile(int size) throws IOException {
    String filename = getRandomName();

    try (OutputStream os = Files.newOutputStream(Paths.get(filename), CREATE, APPEND)) {
      int totalBytesWritten = 0;
      int bytesToWrite = 0;
      byte[] buf = new byte[1 * MB];
      while (totalBytesWritten < size) {
        random.nextBytes(buf);
        bytesToWrite = size - totalBytesWritten;
        if (bytesToWrite > buf.length) {
          bytesToWrite = buf.length;
        }

        os.write(buf, 0, bytesToWrite);
        totalBytesWritten += bytesToWrite;
      }
    }

    return filename;
  }

  /** Create 1 KB temporary file. */
  public static String createFile1Kb() throws IOException {
    if (mintEnv) {
      String filename = getRandomName();
      Files.createSymbolicLink(Paths.get(filename).toAbsolutePath(), dataFile1Kb);
      return filename;
    }

    return createFile(1 * KB);
  }

  /** Create 6 MB temporary file. */
  public static String createFile6Mb() throws IOException {
    if (mintEnv) {
      String filename = getRandomName();
      Files.createSymbolicLink(Paths.get(filename).toAbsolutePath(), dataFile6Mb);
      return filename;
    }

    return createFile(6 * MB);
  }

  /** Generate random name. */
  public static String getRandomName() {
    return "minio-java-test-" + new BigInteger(32, random).toString(32);
  }

  /** Returns byte array contains all data in given InputStream. */
  public static byte[] readAllBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];
    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    return buffer.toByteArray();
  }

  /** Prints a success log entry in JSON format. */
  public static void mintSuccessLog(String function, String args, long startTime) {
    if (mintEnv) {
      System.out.println(
          new MintLogger(
              function, args, System.currentTimeMillis() - startTime, PASS, null, null, null));
    }
  }

  /** Prints a failure log entry in JSON format. */
  public static void mintFailedLog(
      String function, String args, long startTime, String message, String error) {
    if (mintEnv) {
      System.out.println(
          new MintLogger(
              function,
              args,
              System.currentTimeMillis() - startTime,
              FAILED,
              null,
              message,
              error));
    }
  }

  /** Prints a ignore log entry in JSON format. */
  public static void mintIgnoredLog(String function, String args, long startTime) {
    if (mintEnv) {
      System.out.println(
          new MintLogger(
              function, args, System.currentTimeMillis() - startTime, IGNORED, null, null, null));
    }
  }

  /** Read object content of the given url. */
  public static byte[] readObject(String urlString) throws Exception {
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder.url(HttpUrl.parse(urlString)).method("GET", null).build();
    OkHttpClient transport =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    Response response = transport.newCall(request).execute();

    try {
      if (response.isSuccessful()) {
        return response.body().bytes();
      }

      String errorXml = new String(response.body().bytes(), StandardCharsets.UTF_8);
      throw new Exception(
          "failed to create object. Response: " + response + ", Response body: " + errorXml);
    } finally {
      response.close();
    }
  }

  /** Write data to given object url. */
  public static void writeObject(String urlString, byte[] dataBytes) throws Exception {
    Request.Builder requestBuilder = new Request.Builder();
    // Set header 'x-amz-acl' to 'bucket-owner-full-control', so objects created
    // anonymously, can be downloaded by bucket owner in AWS S3.
    Request request =
        requestBuilder
            .url(HttpUrl.parse(urlString))
            .method("PUT", RequestBody.create(null, dataBytes))
            .addHeader("x-amz-acl", "bucket-owner-full-control")
            .build();
    OkHttpClient transport =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    Response response = transport.newCall(request).execute();

    try {
      if (!response.isSuccessful()) {
        String errorXml = new String(response.body().bytes(), StandardCharsets.UTF_8);
        throw new Exception(
            "failed to create object. Response: " + response + ", Response body: " + errorXml);
      }
    } finally {
      response.close();
    }
  }

  private static void handleException(String methodName, String args, long startTime, Exception e)
      throws Exception {
    if (e instanceof ErrorResponseException) {
      if (((ErrorResponseException) e).errorResponse().errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(methodName, args, startTime);
        return;
      }
    }

    mintFailedLog(
        methodName,
        args,
        startTime,
        null,
        e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
    throw e;
  }

  /** Test: makeBucket(MakeBucketArgs args). */
  public static void makeBucket_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: makeBucket(MakeBucketArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog("makeBucket(MakeBucketArgs args)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "makeBucket(MakeBucketArgs args)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: makeBucket(MakeBucketArgs args). */
  public static void makeBucket_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: with region and object lock : makeBucket(MakeBucketArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(
          MakeBucketArgs.builder().bucket(name).region("eu-west-1").objectLock(true).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog(
          "makeBucket(MakeBucketArgs args)", "region: eu-west-1, objectLock: true", startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }
      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "makeBucket(MakeBucketArgs args)", "region: eu-west-1, objectLock: true", startTime);
      } else {
        mintFailedLog(
            "makeBucket(MakeBucketArgs args)",
            "region: eu-west-1, objectLock: true",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: makeBucket(MakeBucketArgs args). */
  public static void makeBucket_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: with region: makeBucket(MakeBucketArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).region("eu-west-1").build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog("makeBucket(MakeBucketArgs args) ", "region: eu-west-1", startTime);
    } catch (Exception e) {
      mintFailedLog(
          "makeBucket(MakeBucketArgs args) ",
          "region: eu-west-1",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: makeBucket(MakeBucketArgs args) where bucketName has periods in its name. */
  public static void makeBucket_test4() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: with bucket name having periods in its name:  makeBucket(MakeBucketArgs args)");
    }

    long startTime = System.currentTimeMillis();
    String name = getRandomName() + ".withperiod";
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(name).region("eu-central-1").build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog(
          "makeBucket(MakeBucketArgs args) bucketname having periods in its name",
          "name: " + name + ", region: eu-central-1",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "makeBucket(MakeBucketArgs args) bucketname having periods in its name",
          "name: " + name + ", region: eu-central-1",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: enableVersioning(EnableVersioningArgs args). */
  public static void enableVersioning_test() throws Exception {
    String methodName = "enableVersioning(EnableVersioningArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      client.enableVersioning(EnableVersioningArgs.builder().bucket(name).build());
      if (!client.isVersioningEnabled(IsVersioningEnabledArgs.builder().bucket(name).build())) {
        throw new Exception("[FAILED] isVersioningEnabled(): expected: true, got: false");
      }
      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: disableVersioning(DisableVersioningArgs args). */
  public static void disableVersioning_test() throws Exception {
    String methodName = "disableVersioning(DisableVersioningArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      client.disableVersioning(DisableVersioningArgs.builder().bucket(name).build());
      if (client.isVersioningEnabled(IsVersioningEnabledArgs.builder().bucket(name).build())) {
        throw new Exception("[FAILED] isVersioningEnabled(): expected: false, got: true");
      }

      client.enableVersioning(EnableVersioningArgs.builder().bucket(name).build());
      client.disableVersioning(DisableVersioningArgs.builder().bucket(name).build());
      if (client.isVersioningEnabled(IsVersioningEnabledArgs.builder().bucket(name).build())) {
        throw new Exception("[FAILED] isVersioningEnabled(): expected: false, got: true");
      }

      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: listBuckets(). */
  public static void listBuckets_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listBuckets()");
    }

    long startTime = System.currentTimeMillis();
    try {
      long nowSeconds = ZonedDateTime.now().toEpochSecond();
      String bucketName = getRandomName();
      boolean found = false;
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      for (Bucket bucket : client.listBuckets()) {
        if (bucket.name().equals(bucketName)) {
          if (found) {
            throw new Exception(
                "[FAILED] duplicate entry " + bucketName + " found in list buckets");
          }

          found = true;
          // excuse 15 minutes
          if ((bucket.creationDate().toEpochSecond() - nowSeconds) > (15 * 60)) {
            throw new Exception(
                "[FAILED] bucket creation time too apart in "
                    + (bucket.creationDate().toEpochSecond() - nowSeconds)
                    + " seconds");
          }
        }
      }
      client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      if (!found) {
        throw new Exception("[FAILED] created bucket not found in list buckets");
      }
      mintSuccessLog("listBuckets()", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listBuckets()",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: bucketExists(BucketExistsArgs args). */
  public static void bucketExists_test() throws Exception {
    String methodName = "bucketExists(BucketExistsArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      if (!client.bucketExists(BucketExistsArgs.builder().bucket(name).build())) {
        throw new Exception("[FAILED] bucket does not exist");
      }
      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: removeBucket(RemoveBucketArgs args). */
  public static void removeBucket_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: removeBucket(RemoveBucketArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(name).build());
      mintSuccessLog("removeBucket(RemoveBucketArgs args)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "removeBucket(RemoveBucketArgs args)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Tear down test setup. */
  public static void setup() throws Exception {
    long startTime = System.currentTimeMillis();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    } catch (Exception e) {
      handleException("makeBucket(MakeBucketArgs args)", null, startTime, e);
    }
  }

  /** Tear down test setup. */
  public static void teardown() throws Exception {
    long startTime = System.currentTimeMillis();
    try {
      client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    } catch (Exception e) {
      handleException("removeBucket(RemoveBucketArgs args)", null, startTime, e);
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, String filename, PutObjectOptions
   * options)
   */
  public static void putObject_test1() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, String filename, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String filename = createFile1Kb();
      client.putObject(bucketName, filename, filename, null);
      Files.delete(Paths.get(filename));
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename).build());
      mintSuccessLog(methodName, "filename: 1KB", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "filename: 1KB",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: multipart: putObject(String bucketName, String objectName, String filename,
   * PutObjectOptions options)
   */
  public static void putObject_test2() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, String filename, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: multipart: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String filename = createFile6Mb();
      client.putObject(bucketName, filename, filename, new PutObjectOptions(6 * MB, 5 * MB));
      Files.delete(Paths.get(filename));
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename).build());
      mintSuccessLog(methodName, "filename: 6MB", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "filename: 6MB",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: with content-type: putObject(String bucketName, String objectName, String filename,
   * PutObjectOptions options)
   */
  public static void putObject_test3() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, String filename, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: with content-type: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String filename = createFile1Kb();
      PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
      options.setContentType(customContentType);
      client.putObject(bucketName, filename, filename, options);
      Files.delete(Paths.get(filename));
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename).build());
      mintSuccessLog(methodName, "contentType: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "contentType: " + customContentType,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions
   * options)
   */
  public static void putObject_test4() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setContentType(customContentType);
        client.putObject(bucketName, objectName, is, options);
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "size: 1 KB, objectName: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "size: 1 KB, objectName: " + customContentType,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: object name with multiple path segments: putObject(String bucketName, String objectName,
   * InputStream stream, PutObjectOptions options)
   */
  public static void putObject_test5() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: object name with path segments: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = "path/to/" + getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setContentType(customContentType);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "size: 1 KB, contentType: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "size: 1 KB, contentType: " + customContentType,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: unknown size stream: putObject(String bucketName, String objectName, InputStream stream,
   * PutObjectOptions options)
   */
  public static void putObject_test6() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: unknown size stream: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * KB)) {
        PutObjectOptions options = new PutObjectOptions(is.available(), -1);
        options.setContentType(customContentType);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "size: -1, contentType: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "size: -1, contentType: " + customContentType,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: multipart unknown size stream: putObject(String bucketName, String objectName,
   * InputStream stream, PutObjectOptions options)
   */
  public static void putObject_test7() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: multipart unknown size stream: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(11 * MB)) {
        PutObjectOptions options = new PutObjectOptions(is.available(), -1);
        options.setContentType(customContentType);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "size: -1, contentType: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "size: -1, contentType: " + customContentType,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: with user metadata: putObject(String bucketName, String objectName, InputStream stream,
   * PutObjectOptions options).
   */
  public static void putObject_test8() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: with user metadata: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("X-Amz-Meta-mykey", "myvalue");
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setHeaders(headerMap);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "X-Amz-Meta-mykey: myvalue", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "X-Amz-Meta-mykey: myvalue",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: with storage class REDUCED_REDUNDANCY: putObject(String bucketName, String objectName,
   * InputStream stream, PutObjectOptions options).
   */
  public static void putObject_test9() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: with storage class REDUCED_REDUNDANCY: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("X-Amz-Storage-Class", "REDUCED_REDUNDANCY");
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setHeaders(headerMap);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "X-Amz-Storage-Class: REDUCED_REDUNDANCY", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "X-Amz-Storage-Class: REDUCED_REDUNDANCY",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: with storage class STANDARD: putObject(String bucketName, String objectName, InputStream
   * stream, PutObjectOptions options).
   */
  public static void putObject_test10() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: with storage class STANDARD: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("X-Amz-Storage-Class", "STANDARD");
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setHeaders(headerMap);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "X-Amz-Storage-Class: STANDARD", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "X-Amz-Storage-Class: STANDARD",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: with storage class INVALID: putObject(String bucketName, String objectName, InputStream
   * stream, PutObjectOptions options).
   */
  public static void putObject_test11() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";

    if (!mintEnv) {
      System.out.println("Test: with storage class INVALID: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("X-Amz-Storage-Class", "INVALID");
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setHeaders(headerMap);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.INVALID_STORAGE_CLASS) {
        mintFailedLog(
            methodName,
            "X-Amz-Storage-Class: INVALID",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "X-Amz-Storage-Class: INVALID",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
    mintSuccessLog(methodName, "X-Amz-Storage-Class: INVALID", startTime);
  }

  /**
   * Test: with SSE_C: putObject(String bucketName, String objectName, InputStream stream,
   * PutObjectOptions options).
   */
  public static void putObject_test12() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";
    if (!mintEnv) {
      System.out.println("Test: with SSE_C: " + methodName);
    }

    long startTime = System.currentTimeMillis();

    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "Server-side encryption: SSE_C", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "Server-side encryption: SSE_C",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: multipart with SSE_C: putObject(String bucketName, String objectName, InputStream stream,
   * PutObjectOptions options).
   */
  public static void putObject_test13() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";
    if (!mintEnv) {
      System.out.println("Test: multipart with SSE_C: " + methodName);
    }

    long startTime = System.currentTimeMillis();

    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(11 * MB)) {
        PutObjectOptions options = new PutObjectOptions(-1, 5 * MB);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "Size: 11 MB, Server-side encryption: SSE_C", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "Size: 11 MB, Server-side encryption: SSE_C",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: with SSE_S3: putObject(String bucketName, String objectName, InputStream stream,
   * PutObjectOptions options).
   */
  public static void putObject_test14() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";
    if (!mintEnv) {
      System.out.println("Test: with SSE_S3: " + methodName);
    }

    long startTime = System.currentTimeMillis();

    ServerSideEncryption sse = ServerSideEncryption.atRest();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "Server-side encryption: SSE_S3", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "Server-side encryption: SSE_S3",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: with SSE_KMS: putObject(String bucketName, String objectName, InputStream stream,
   * PutObjectOptions options).
   */
  public static void putObject_test15() throws Exception {
    String methodName =
        "putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)";
    if (!mintEnv) {
      System.out.println("Test: with SSE_KMS: " + methodName);
    }

    long startTime = System.currentTimeMillis();

    if (System.getenv("MINT_KEY_ID").equals("")) {
      mintIgnoredLog(methodName, "Server-side encryption: SSE_KMS", startTime);
    }

    Map<String, String> myContext = new HashMap<>();
    myContext.put("key1", "value1");
    ServerSideEncryption sse = ServerSideEncryption.withManagedKeys("keyId", myContext);

    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "Server-side encryption: SSE_KMS", startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          "Server-side encryption: SSE_KMS",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: statObject(StatObjectArgs args). */
  public static void statObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: statObject(StatObjectArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      headerMap.put("my-custom-data", "foo");
      try (final InputStream is = new ContentInputStream(1)) {
        PutObjectOptions options = new PutObjectOptions(1, -1);
        options.setHeaders(headerMap);
        options.setContentType(customContentType);
        client.putObject(bucketName, objectName, is, options);
      }

      ObjectStat objectStat =
          client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());

      if (!(objectName.equals(objectStat.name())
          && (objectStat.length() == 1)
          && bucketName.equals(objectStat.bucketName())
          && objectStat.contentType().equals(customContentType))) {
        throw new Exception("[FAILED] object stat differs");
      }

      Map<String, List<String>> httpHeaders = objectStat.httpHeaders();
      if (!httpHeaders.containsKey("x-amz-meta-my-custom-data")) {
        throw new Exception("[FAILED] metadata not found in object stat");
      }
      List<String> values = httpHeaders.get("x-amz-meta-my-custom-data");
      if (values.size() != 1) {
        throw new Exception("[FAILED] too many metadata value. expected: 1, got: " + values.size());
      }
      if (!values.get(0).equals("foo")) {
        throw new Exception("[FAILED] wrong metadata value. expected: foo, got: " + values.get(0));
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog("statObject(StatObjectArgs args)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "statObject(StatObjectArgs args)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: with SSE-C: statObject(StatObjectArgs args). */
  public static void statObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: with SSE-C: statObject(StatObjectArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      // Generate a new 256 bit AES key - This key must be remembered by the client.
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256);
      ServerSideEncryptionCustomerKey sse =
          ServerSideEncryption.withCustomerKey(keyGen.generateKey());

      try (final InputStream is = new ContentInputStream(1)) {
        PutObjectOptions options = new PutObjectOptions(1, -1);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
      }

      ObjectStat objectStat =
          client.statObject(
              StatObjectArgs.builder().bucket(bucketName).object(objectName).ssec(sse).build());

      if (!(objectName.equals(objectStat.name())
          && (objectStat.length() == 1)
          && bucketName.equals(objectStat.bucketName()))) {
        throw new Exception("[FAILED] object stat differs");
      }

      Map<String, List<String>> httpHeaders = objectStat.httpHeaders();
      if (!httpHeaders.containsKey("X-Amz-Server-Side-Encryption-Customer-Algorithm")) {
        throw new Exception("[FAILED] metadata not found in object stat");
      }
      List<String> values = httpHeaders.get("X-Amz-Server-Side-Encryption-Customer-Algorithm");
      if (values.size() != 1) {
        throw new Exception("[FAILED] too many metadata value. expected: 1, got: " + values.size());
      }
      if (!values.get(0).equals("AES256")) {
        throw new Exception(
            "[FAILED] wrong metadata value. expected: AES256, got: " + values.get(0));
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog("statObject(StatObjectArgs args) using SSE_C.", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "statObject(StatObjectArgs args) using SSE_C.",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: wtth non-existing objecth: statObject(StatObjectArgs args). */
  public static void statObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: with non-existing object: statObject(StatObjectArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      client.statObject(
          StatObjectArgs.builder().bucket(bucketName).object(getRandomName() + "/").build());
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_KEY) {
        mintFailedLog(
            "statObject(StatObjectArgs args) with non-existing object",
            null,
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    } catch (Exception e) {
      mintFailedLog(
          "statObject(StatObjectArgs args) with non-existing object",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    } finally {
      mintSuccessLog("statObject(StatObjectArgs args) with non-existing object", null, startTime);
    }
  }

  /** Test: with extra headers/query params: statObject(StatObjectArgs args). */
  public static void statObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: with extra headers/query params: statObject(StatObjectArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      headerMap.put("my-custom-data", "foo");
      try (final InputStream is = new ContentInputStream(1)) {
        PutObjectOptions options = new PutObjectOptions(1, -1);
        options.setHeaders(headerMap);
        options.setContentType(customContentType);
        client.putObject(bucketName, objectName, is, options);
      }

      HashMap<String, String> headers = new HashMap<>();
      headers.put("x-amz-request-payer", "requester");
      HashMap<String, String> queryParams = new HashMap<>();
      queryParams.put("partNumber", "1");
      ObjectStat objectStat =
          client.statObject(
              StatObjectArgs.builder()
                  .bucket(bucketName)
                  .object(objectName)
                  .extraHeaders(headers)
                  .extraQueryParams(queryParams)
                  .build());

      if (!(objectName.equals(objectStat.name())
          && (objectStat.length() == 1)
          && bucketName.equals(objectStat.bucketName())
          && objectStat.contentType().equals(customContentType))) {
        throw new Exception("[FAILED] object stat differs");
      }

      Map<String, List<String>> httpHeaders = objectStat.httpHeaders();
      if (!httpHeaders.containsKey("x-amz-meta-my-custom-data")) {
        throw new Exception("[FAILED] metadata not found in object stat");
      }
      List<String> values = httpHeaders.get("x-amz-meta-my-custom-data");
      if (values.size() != 1) {
        throw new Exception("[FAILED] too many metadata value. expected: 1, got: " + values.size());
      }
      if (!values.get(0).equals("foo")) {
        throw new Exception("[FAILED] wrong metadata value. expected: foo, got: " + values.get(0));
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "statObject(StatObjectArgs args) with extra headers/query params", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "statObject(StatObjectArgs args) with extra headers/query params",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: getObject(String bucketName, String objectName). */
  public static void getObject_test1() throws Exception {
    String methodName = "getObject(GetObjectArgs) [bucketName, objectName]";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client
          .getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build())
          .close();

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getObject(String bucketName, String objectName, long offset). */
  public static void getObject_test2() throws Exception {
    String methodName = "getObject(GetObjectArgs) [bucketName, objectName, offset]";
    String args = "offset: 1000";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client
          .getObject(
              GetObjectArgs.builder().bucket(bucketName).object(objectName).offset(1000L).build())
          .close();
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, args, startTime);
    } catch (Exception e) {
      handleException(methodName, args, startTime, e);
    }
  }

  /** Test: getObject(String bucketName, String objectName, long offset, Long length). */
  public static void getObject_test3() throws Exception {
    String methodName = "getObject(GetObjectArgs) [bucketName, objectName, offset, length]";
    String args = "offset: 1000, length: 64 KB";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }
    long startTime = System.currentTimeMillis();

    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(6 * MB, -1));
      }

      client
          .getObject(
              GetObjectArgs.builder()
                  .bucket(bucketName)
                  .object(objectName)
                  .offset(1000L)
                  .length(64 * 1024L)
                  .build())
          .close();
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, args, startTime);
    } catch (Exception e) {
      handleException(methodName, args, startTime, e);
    }
  }

  /** Test: getObject(String bucketName, String objectName, String filename). */
  public static void getObject_test4() throws Exception {
    String methodName = "getObject(GetObjectArgs) [bucketName, objectName, fileName]";
    String args = "offset: 1000, length: 64 KB";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client.downloadObject(
          DownloadObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .fileName(objectName + ".downloaded")
              .build());
      Files.delete(Paths.get(objectName + ".downloaded"));
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());

      mintSuccessLog(methodName, args, startTime);
    } catch (Exception e) {
      handleException(methodName, args, startTime, e);
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, String filename). where objectName has
   * multiple path segments.
   */
  public static void getObject_test5() throws Exception {
    String methodName = "downloadObject(GetObjectArgs) [bucketName, objectName, fileName]";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }
    long startTime = System.currentTimeMillis();
    String baseObjectName = getRandomName();
    String objectName = "path/to/" + baseObjectName;
    String args = "objectName: " + objectName;
    try {
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client.downloadObject(
          DownloadObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .fileName(baseObjectName + ".downloaded")
              .build());
      Files.delete(Paths.get(baseObjectName + ".downloaded"));
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());

      mintSuccessLog(methodName, args, startTime);
    } catch (Exception e) {
      handleException(methodName, args, startTime, e);
    }
  }

  /** Test: getObject(String bucketName, String objectName) zero size object. */
  public static void getObject_test6() throws Exception {
    String methodName =
        "getObject(GetObjectArgs) [bucketName, objectName, fileName] zero size object";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(0)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(0, -1));
      }

      client
          .getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build())
          .close();
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, ServerSideEncryption sse). To test
   * getObject when object is put using SSE_C.
   */
  public static void getObject_test7() throws Exception {
    String methodName = "getObject(GetObjectArgs) [bucketName, objectName, sse] using SSE_C.";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    // Generate a new 256 bit AES key - This key must be remembered by the client.
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    ServerSideEncryptionCustomerKey sse =
        ServerSideEncryption.withCustomerKey(keyGen.generateKey());

    try {
      String objectName = getRandomName();
      String putString;
      int bytes_read_put;
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
        byte[] putbyteArray = new byte[is.available()];
        bytes_read_put = is.read(putbyteArray);
        putString = new String(putbyteArray, StandardCharsets.UTF_8);
      }

      InputStream stream =
          client.getObject(
              GetObjectArgs.builder().bucket(bucketName).object(objectName).ssec(sse).build());
      byte[] getbyteArray = new byte[stream.available()];
      int bytes_read_get = stream.read(getbyteArray);
      String getString = new String(getbyteArray, StandardCharsets.UTF_8);
      stream.close();

      // Compare if contents received are same as the initial uploaded object.
      if ((!putString.equals(getString)) || (bytes_read_put != bytes_read_get)) {
        throw new Exception("Contents received from getObject doesn't match initial contents.");
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset, Long length) with offset=0.
   */
  public static void getObject_test8() throws Exception {
    String methodName = "getObject(GetObjectArgs) [bucketName, objectName, offset, length]";
    if (!mintEnv) {
      System.out.println("Test: " + methodName + " with offset=0");
    }

    final long startTime = System.currentTimeMillis();
    final int fullLength = 1024;
    final int partialLength = 256;
    final long offset = 0L;
    final String objectName = getRandomName();
    String args = String.format("offset: %d, length: %d bytes", offset, partialLength);
    try {
      try (final InputStream is = new ContentInputStream(fullLength)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(fullLength, -1));
      }

      try (final InputStream partialObjectStream =
          client.getObject(
              GetObjectArgs.builder()
                  .bucket(bucketName)
                  .object(objectName)
                  .offset(offset)
                  .length(Long.valueOf(partialLength))
                  .build())) {
        byte[] result = new byte[fullLength];
        final int read = partialObjectStream.read(result);
        result = Arrays.copyOf(result, read);
        if (result.length != partialLength) {
          throw new Exception(
              String.format(
                  "Expecting only the first %d bytes from partial getObject request; received %d bytes instead.",
                  partialLength, read));
        }
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, args, startTime);
    } catch (final Exception e) {
      handleException(methodName, args, startTime, e);
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, ServerSideEncryption sse, String
   * fileName).
   */
  public static void getObject_test9() throws Exception {
    String methodName = "downloadObject(GetObjectArgs) [bucketName, objectName, sse, fileName]";
    String args = "size: 1 KB";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    // Generate a new 256 bit AES key - This key must be remembered by the client.
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    ServerSideEncryptionCustomerKey sse =
        ServerSideEncryption.withCustomerKey(keyGen.generateKey());
    try {
      String objectName = getRandomName();
      String filename = createFile1Kb();
      PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
      options.setSse(sse);
      client.putObject(bucketName, objectName, filename, options);
      client.downloadObject(
          DownloadObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .ssec(sse)
              .fileName(objectName + ".downloaded")
              .build());
      Files.delete(Paths.get(objectName + ".downloaded"));
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());

      mintSuccessLog(methodName, args, startTime);
    } catch (Exception e) {
      handleException(methodName, args, startTime, e);
    }
  }

  /** Test: listObjects(final String bucketName). */
  public static void listObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listObjects(ListObjectsArgs args) [bucket]");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[3];
      int i = 0;
      for (i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, new PutObjectOptions(1, -1));
        }
      }

      i = 0;
      for (Result<?> r : client.listObjects(ListObjectsArgs.builder().bucket(bucketName).build())) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog("listObjects(ListObjectsArgs args) [bucket]", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(ListObjectsArgs args)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: listObjects(bucketName, final String prefix). */
  public static void listObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listObjects(ListObjectsArgs args) [bucket, prefix]");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[3];
      int i = 0;
      for (i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, new PutObjectOptions(1, -1));
        }
      }

      i = 0;
      for (Result<?> r :
          client.listObjects(
              ListObjectsArgs.builder().bucket(bucketName).prefix("minio").build())) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix]", "prefix :minio", startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix]",
          "prefix :minio",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: listObjects(bucketName, final String prefix, final boolean recursive). */
  public static void listObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listObjects(ListObjectsArgs args) [bucket, prefix, recursive]");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[3];
      int i = 0;
      for (i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, new PutObjectOptions(1, -1));
        }
      }

      i = 0;
      for (Result<?> r :
          client.listObjects(
              ListObjectsArgs.builder()
                  .bucket(bucketName)
                  .prefix("minio")
                  .recursive(true)
                  .build())) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix, recursive]",
          "prefix :minio, recursive: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects with args [bucket, prefix, recursive]",
          "prefix :minio, recursive: true",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: listObjects(final string bucketName). */
  public static void listObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: empty bucket: listObjects(ListObjectsArgs args) [bucket, prefix, recursive]"
              + " final boolean recursive)");
    }

    long startTime = System.currentTimeMillis();
    try {
      int i = 0;
      for (Result<?> r :
          client.listObjects(
              ListObjectsArgs.builder()
                  .bucket(bucketName)
                  .prefix("minioemptybucket")
                  .recursive(true)
                  .build())) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }
      mintSuccessLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix, recursive]",
          "prefix :minioemptybucket, recursive: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix, recursive]",
          "prefix :minioemptybucket, recursive: true",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: recursive: listObjects(bucketName, final String prefix, final boolean recursive). */
  public static void listObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: recursive: listObjects(ListObjectsArgs args) [bucket, prefix, recursive]"
              + "final boolean recursive)");
    }

    long startTime = System.currentTimeMillis();
    try {
      int objCount = 1050;
      String[] objectNames = new String[objCount];
      int i = 0;
      for (i = 0; i < objCount; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, new PutObjectOptions(1, -1));
        }
      }

      i = 0;
      for (Result<?> r :
          client.listObjects(
              ListObjectsArgs.builder()
                  .bucket(bucketName)
                  .prefix("minio")
                  .recursive(true)
                  .build())) {
        ignore(i++, r.get());
      }

      // Check the number of uploaded objects
      if (i != objCount) {
        throw new Exception("item count differs, expected: " + objCount + ", got: " + i);
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix, recursive]",
          "prefix :minio, recursive: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix, recursive]",
          "prefix :minio, recursive: true",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix, final boolean recursive, final boolean
   * useVersion1).
   */
  public static void listObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: listObjects(ListObjectsArgs args) [bucket, prefix, recursive, useVersion1]"
              + "final boolean useVersion1)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[3];
      int i = 0;
      for (i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, new PutObjectOptions(1, -1));
        }
      }

      i = 0;
      for (Result<?> r :
          client.listObjects(
              ListObjectsArgs.builder()
                  .bucket(bucketName)
                  .prefix("minio")
                  .recursive(true)
                  .useVersion1(true)
                  .build())) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix, recursive, useVersion1]",
          "prefix :minio, recursive: true, useVersion1: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(ListObjectsArgs args) [bucket, prefix, recursive, useVersion1]",
          "prefix :minio, recursive: true, useVersion1: true",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: removeObject(String bucketName, String objectName). */
  public static void removeObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: removeObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1, -1));
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog("removeObject(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "removeObject(String bucketName, String objectName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: removeObjects(final String bucketName, final Iterable&lt;String&gt; objectNames). */
  public static void removeObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: removeObjects(final String bucketName, final Iterable<String> objectNames)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[4];
      for (int i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, new PutObjectOptions(1, -1));
        }
      }
      objectNames[3] = "nonexistent-object";

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }
      mintSuccessLog(
          "removeObjects(final String bucketName, final Iterable<String> objectNames)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "removeObjects(final String bucketName, final Iterable<String> objectNames)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: listIncompleteUploads(String bucketName). */
  public static void listIncompleteUploads_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listIncompleteUploads(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(9 * MB, -1));
      } catch (ErrorResponseException e) {
        if (e.errorResponse().errorCode() != ErrorCode.INCOMPLETE_BODY) {
          throw e;
        }
      } catch (InsufficientDataException e) {
        ignore();
      }

      int i = 0;
      for (Result<Upload> r : client.listIncompleteUploads(bucketName)) {
        ignore(i++, r.get());
        if (i == 10) {
          break;
        }
      }

      client.removeIncompleteUpload(
          RemoveIncompleteUploadArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog("listIncompleteUploads(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listIncompleteUploads(String bucketName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: listIncompleteUploads(String bucketName, String prefix). */
  public static void listIncompleteUploads_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listIncompleteUploads(String bucketName, String prefix)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(9 * MB, -1));
      } catch (ErrorResponseException e) {
        if (e.errorResponse().errorCode() != ErrorCode.INCOMPLETE_BODY) {
          throw e;
        }
      } catch (InsufficientDataException e) {
        ignore();
      }

      int i = 0;
      for (Result<Upload> r : client.listIncompleteUploads(bucketName, "minio")) {
        ignore(i++, r.get());
        if (i == 10) {
          break;
        }
      }

      client.removeIncompleteUpload(
          RemoveIncompleteUploadArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog("listIncompleteUploads(String bucketName, String prefix)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listIncompleteUploads(String bucketName, String prefix)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listIncompleteUploads(final String bucketName, final String prefix, final boolean
   * recursive).
   */
  public static void listIncompleteUploads_test3() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: listIncompleteUploads(final String bucketName, final String prefix, "
              + "final boolean recursive)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(9 * MB, -1));
      } catch (ErrorResponseException e) {
        if (e.errorResponse().errorCode() != ErrorCode.INCOMPLETE_BODY) {
          throw e;
        }
      } catch (InsufficientDataException e) {
        ignore();
      }

      int i = 0;
      for (Result<Upload> r : client.listIncompleteUploads(bucketName, "minio", true)) {
        ignore(i++, r.get());
        if (i == 10) {
          break;
        }
      }

      client.removeIncompleteUpload(
          RemoveIncompleteUploadArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive)",
          "prefix: minio, recursive: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive)",
          "prefix: minio, recursive: true",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: removeIncompleteUpload(String bucketName, String objectName). */
  public static void removeIncompleteUploads_test() throws Exception {
    String methodName = "removeIncompleteUpload(RemoveIncompleteUploadArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(9 * MB, -1));
      } catch (ErrorResponseException e) {
        if (e.errorResponse().errorCode() != ErrorCode.INCOMPLETE_BODY) {
          throw e;
        }
      } catch (InsufficientDataException e) {
        ignore();
      }

      int i = 0;
      for (Result<Upload> r : client.listIncompleteUploads(bucketName)) {
        ignore(i++, r.get());
        if (i == 10) {
          break;
        }
      }

      client.removeIncompleteUpload(
          RemoveIncompleteUploadArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args). */
  public static void getPresignedObjectUrl_test1() throws Exception {
    String methodName = "getPresignedObjectUrl(GetPresignedObjectUrlArgs args)";
    if (!mintEnv) {
      System.out.println("Test: presigned get object: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      byte[] inBytes;
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        inBytes = readAllBytes(is);
      }

      String urlString =
          client.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.GET)
                  .bucket(bucketName)
                  .object(objectName)
                  .build());

      byte[] outBytes = readObject(urlString);
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "presigned get object", startTime);
    } catch (Exception e) {
      handleException(methodName, "presigned get object", startTime, e);
    }
  }

  /** Test: getPresignedObjectUrl(GetPresignedObjectUrlArgs args). */
  public static void getPresignedObjectUrl_test2() throws Exception {
    String methodName = "getPresignedObjectUrl(GetPresignedObjectUrlArgs args)";
    if (!mintEnv) {
      System.out.println("Test: presigned get object with expiry: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      byte[] inBytes;
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        inBytes = readAllBytes(is);
      }

      String urlString =
          client.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.GET)
                  .bucket(bucketName)
                  .object(objectName)
                  .expires(3600)
                  .build());
      byte[] outBytes = readObject(urlString);
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "presigned get object expiry :3600 sec ", startTime);
    } catch (Exception e) {
      handleException(methodName, "presigned get object expiry :3600 sec ", startTime, e);
    }
  }

  /** public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args). */
  public static void getPresignedObjectUrl_test3() throws Exception {
    String methodName = "getPresignedObjectUrl(GetPresignedObjectUrlArgs args)";
    if (!mintEnv) {
      System.out.println("Test: presigned get object with expiry and params: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      byte[] inBytes;
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        inBytes = readAllBytes(is);
      }

      Map<String, String> reqParams = new HashMap<>();
      reqParams.put("response-content-type", "application/json");

      String urlString =
          client.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.GET)
                  .bucket(bucketName)
                  .object(objectName)
                  .expires(3600)
                  .extraQueryParams(reqParams)
                  .build());

      byte[] outBytes = readObject(urlString);
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          methodName,
          "presigned get object expiry : 3600 sec, reqParams : response-content-type as application/json",
          startTime);
    } catch (Exception e) {
      handleException(
          methodName,
          "presigned get object expiry : 3600 sec, reqParams : response-content-type as application/json",
          startTime,
          e);
    }
  }

  /** public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args). */
  public static void getPresignedObjectUrl_test4() throws Exception {
    String methodName = "getPresignedObjectUrl(GetPresignedObjectUrlArgs args)";
    if (!mintEnv) {
      System.out.println("Test: presigned put object: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      String urlString =
          client.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.PUT)
                  .bucket(bucketName)
                  .object(objectName)
                  .build());
      byte[] data = "hello, world".getBytes(StandardCharsets.UTF_8);
      writeObject(urlString, data);
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "presigned put object", startTime);
    } catch (Exception e) {
      handleException(methodName, "presigned put object", startTime, e);
    }
  }

  /** Test: getPresignedObjectUrl(GetPresignedObjectUrlArgs args). */
  public static void getPresignedObjectUrl_test5() throws Exception {
    String methodName = "getPresignedObjectUrl(GetPresignedObjectUrlArgs args)";
    if (!mintEnv) {
      System.out.println("Test: presigned put object with expiry: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();

      String urlString =
          client.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.PUT)
                  .bucket(bucketName)
                  .object(objectName)
                  .expires(3600)
                  .build());
      byte[] data = "hello, world".getBytes(StandardCharsets.UTF_8);
      writeObject(urlString, data);
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, "presigned put object with expiry :3600 sec", startTime);
    } catch (Exception e) {
      handleException(methodName, "presigned put object with expiry :3600 sec", startTime, e);
    }
  }

  /** Test: presignedPostPolicy(PostPolicy policy). */
  public static void presignedPostPolicy_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedPostPolicy(PostPolicy policy)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      PostPolicy policy = new PostPolicy(bucketName, objectName, ZonedDateTime.now().plusDays(7));
      policy.setContentRange(1 * MB, 4 * MB);
      Map<String, String> formData = client.presignedPostPolicy(policy);

      MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
      multipartBuilder.setType(MultipartBody.FORM);
      for (Map.Entry<String, String> entry : formData.entrySet()) {
        multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
      }
      try (final InputStream is = new ContentInputStream(1 * MB)) {
        multipartBuilder.addFormDataPart(
            "file", objectName, RequestBody.create(null, readAllBytes(is)));
      }

      Request.Builder requestBuilder = new Request.Builder();
      String urlString = client.getObjectUrl(bucketName, "x");
      // remove last two characters to get clean url string of bucket.
      urlString = urlString.substring(0, urlString.length() - 2);
      Request request = requestBuilder.url(urlString).post(multipartBuilder.build()).build();
      OkHttpClient transport =
          new OkHttpClient()
              .newBuilder()
              .connectTimeout(20, TimeUnit.SECONDS)
              .writeTimeout(20, TimeUnit.SECONDS)
              .readTimeout(20, TimeUnit.SECONDS)
              .build();
      Response response = transport.newCall(request).execute();
      if (response == null) {
        throw new Exception("no response from server");
      }

      try {
        if (!response.isSuccessful()) {
          String errorXml = new String(response.body().bytes(), StandardCharsets.UTF_8);
          throw new Exception(
              "failed to upload object. Response: " + response + ", Error: " + errorXml);
        }
      } finally {
        response.close();
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog("presignedPostPolicy(PostPolicy policy)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "presignedPostPolicy(PostPolicy policy)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: PutObject(): do put object using multi-threaded way in parallel. */
  public static void threadedPutObject() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: threadedPutObject");
    }

    long startTime = System.currentTimeMillis();
    try {
      Thread[] threads = new Thread[7];

      for (int i = 0; i < 7; i++) {
        threads[i] = new Thread(new PutObjectRunnable(client, bucketName, createFile6Mb(), 6 * MB));
      }

      for (int i = 0; i < 7; i++) {
        threads[i].start();
      }

      // Waiting for threads to complete.
      for (int i = 0; i < 7; i++) {
        threads[i].join();
      }

      // All threads are completed.
      mintSuccessLog(
          "putObject(String bucketName, String objectName, String filename)",
          "filename: threaded6MB",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "putObject(String bucketName, String objectName, String filename)",
          "filename: threaded6MB",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: copyObject(String bucketName, String objectName, String destBucketName). */
  public static void copyObject_test1() throws Exception {
    String methodName = "copyObject(String bucketName, String objectName, String destBucketName)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());
      client.copyObject(destBucketName, objectName, null, null, bucketName, null, null, null);
      client
          .getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build())
          .close();

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(destBucketName).object(objectName).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions) with ETag to match.
   */
  public static void copyObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions) with Matching ETag (Negative Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());

      CopyConditions invalidETag = new CopyConditions();
      invalidETag.setMatchETag("TestETag");

      try {
        client.copyObject(
            destBucketName, objectName, null, null, bucketName, null, null, invalidETag);
      } catch (ErrorResponseException e) {
        if (e.errorResponse().errorCode() != ErrorCode.PRECONDITION_FAILED) {
          throw e;
        }
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());

      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName,"
              + " CopyConditions copyConditions)",
          "CopyConditions: invalidETag",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions)",
          "CopyConditions: invalidETag",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions) with ETag to match.
   */
  public static void copyObject_test3() throws Exception {
    String methodName =
        "copyObject(String bucketName, String objectName, String destBucketName,"
            + " CopyConditions copyConditions) with Matching ETag (Positive Case)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());

      ObjectStat stat =
          client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setMatchETag(stat.etag());

      // File should be copied as ETag set in copyConditions matches object's ETag.
      client.copyObject(
          destBucketName, objectName, null, null, bucketName, null, null, copyConditions);
      client
          .getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build())
          .close();

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(destBucketName).object(objectName).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions) with ETag to not match.
   */
  public static void copyObject_test4() throws Exception {
    String methodName =
        "Test: copyObject(String bucketName, String objectName, String destBucketName,"
            + "CopyConditions copyConditions) with not matching ETag"
            + " (Positive Case)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());

      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setMatchETagNone("TestETag");

      // File should be copied as ETag set in copyConditions doesn't match object's
      // ETag.
      client.copyObject(
          destBucketName, objectName, null, null, bucketName, null, null, copyConditions);
      client
          .getObject(GetObjectArgs.builder().bucket(destBucketName).object(objectName).build())
          .close();

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(destBucketName).object(objectName).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());

      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions) with ETag to not match.
   */
  public static void copyObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions) with not matching ETag"
              + " (Negative Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());

      ObjectStat stat =
          client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
      CopyConditions matchingETagNone = new CopyConditions();
      matchingETagNone.setMatchETagNone(stat.etag());

      try {
        client.copyObject(
            destBucketName, objectName, null, null, bucketName, null, null, matchingETagNone);
      } catch (ErrorResponseException e) {
        // File should not be copied as ETag set in copyConditions matches object's
        // ETag.
        if (e.errorResponse().errorCode() != ErrorCode.PRECONDITION_FAILED) {
          throw e;
        }
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());

      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions) with object modified after condition.
   */
  public static void copyObject_test6() throws Exception {
    String methodName =
        "Test: copyObject(String bucketName, String objectName, String destBucketName,"
            + "CopyConditions copyConditions) with modified after "
            + "condition (Positive Case)";
    String args = "CopyCondition: modifiedDateCondition";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());

      CopyConditions modifiedDateCondition = new CopyConditions();
      modifiedDateCondition.setModified(ZonedDateTime.of(2015, 05, 3, 3, 10, 10, 0, Time.UTC));

      // File should be copied as object was modified after the set date.
      client.copyObject(
          destBucketName, objectName, null, null, bucketName, null, null, modifiedDateCondition);
      client
          .getObject(GetObjectArgs.builder().bucket(destBucketName).object(objectName).build())
          .close();

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(destBucketName).object(objectName).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());
      mintSuccessLog(methodName, args, startTime);
    } catch (Exception e) {
      handleException(methodName, args, startTime, e);
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions) with object modified after condition.
   */
  public static void copyObject_test7() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions) with modified after"
              + " condition (Negative Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());

      CopyConditions invalidUnmodifiedCondition = new CopyConditions();
      invalidUnmodifiedCondition.setUnmodified(
          ZonedDateTime.of(2015, 05, 3, 3, 10, 10, 0, Time.UTC));

      try {
        client.copyObject(
            destBucketName,
            objectName,
            null,
            null,
            bucketName,
            null,
            null,
            invalidUnmodifiedCondition);
      } catch (ErrorResponseException e) {
        // File should not be copied as object was modified after date set in
        // copyConditions.
        if (e.errorResponse().errorCode() != ErrorCode.PRECONDITION_FAILED) {
          throw e;
        }
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      // Destination bucket is expected to be empty, otherwise it will trigger an
      // exception.
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions)",
          "CopyCondition: invalidUnmodifiedCondition",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions)",
          "CopyCondition: invalidUnmodifiedCondition",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions, Map metadata) replace object metadata.
   */
  public static void copyObject_test8() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions, Map<String, String> metadata)"
              + " replace object metadata");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).build());

      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();

      Map<String, String> metadata = new HashMap<>();
      metadata.put("Content-Type", customContentType);

      client.copyObject(
          destBucketName, objectName, metadata, null, bucketName, objectName, null, copyConditions);

      ObjectStat objectStat =
          client.statObject(
              StatObjectArgs.builder().bucket(destBucketName).object(objectName).build());
      if (!customContentType.equals(objectStat.contentType())) {
        throw new Exception(
            "content type differs. expected: "
                + customContentType
                + ", got: "
                + objectStat.contentType());
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(destBucketName).object(objectName).build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(destBucketName).build());
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions, Map<String, String> metadata)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions, Map<String, String> metadata)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions, Map metadata) remove object metadata.
   */
  public static void copyObject_test9() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions, Map<String, String> metadata)"
              + " remove object metadata");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Test", "testValue");

      try (final InputStream is = new ContentInputStream(1)) {
        PutObjectOptions options = new PutObjectOptions(1, -1);
        options.setHeaders(headerMap);
        client.putObject(bucketName, objectName, is, options);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();

      client.copyObject(
          bucketName,
          objectName,
          new HashMap<String, String>(),
          null,
          bucketName,
          objectName,
          null,
          copyConditions);
      ObjectStat objectStat =
          client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
      if (objectStat.httpHeaders().containsKey("X-Amz-Meta-Test")) {
        throw new Exception("expected user-defined metadata has been removed");
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions, Map<String, String> metadata)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions, Map<String, String> metadata)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String
   * destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget) To test using
   * SSE_C.
   */
  public static void copyObject_test10() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_C. ");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();

      // Generate a new 256 bit AES key - This key must be remembered by the client.
      byte[] key = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

      ServerSideEncryption ssePut = ServerSideEncryption.withCustomerKey(secretKeySpec);
      ServerSideEncryption sseSource = ServerSideEncryption.withCustomerKey(secretKeySpec);

      byte[] keyTarget = "98765432100123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpecTarget = new SecretKeySpec(keyTarget, "AES");

      ServerSideEncryptionCustomerKey sseTarget =
          ServerSideEncryption.withCustomerKey(secretKeySpecTarget);

      try (final InputStream is = new ContentInputStream(1)) {
        PutObjectOptions options = new PutObjectOptions(1, -1);
        options.setSse(ssePut);
        client.putObject(bucketName, objectName, is, options);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();

      client.copyObject(
          bucketName,
          objectName,
          null,
          sseTarget,
          bucketName,
          objectName,
          sseSource,
          copyConditions);

      client.statObject(
          StatObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .ssec(sseTarget)
              .build()); // Check for object existence.

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_C.",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_C.",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String
   * destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget) To test using
   * SSE_S3.
   */
  public static void copyObject_test11() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_S3. ");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();

      ServerSideEncryption sse = ServerSideEncryption.atRest();

      try (final InputStream is = new ContentInputStream(1)) {
        PutObjectOptions options = new PutObjectOptions(1, -1);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();

      client.copyObject(
          bucketName, objectName, null, sse, bucketName, objectName, null, copyConditions);
      ObjectStat objectStat =
          client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
      if (objectStat.httpHeaders().containsKey("X-Amz-Meta-Test")) {
        throw new Exception("expected user-defined metadata has been removed");
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_S3.",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_S3.",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String
   * destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget) To test using
   * SSE_KMS.
   */
  public static void copyObject_test12() throws Exception {
    String methodName =
        "SSE-KMS: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
            + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();

      Map<String, String> myContext = new HashMap<>();
      myContext.put("key1", "value1");

      String keyId = "";
      keyId = System.getenv("MINT_KEY_ID");
      if (keyId.equals("")) {
        mintIgnoredLog(methodName, null, startTime);
      }
      ServerSideEncryption sse = ServerSideEncryption.withManagedKeys("keyId", myContext);

      try (final InputStream is = new ContentInputStream(1)) {
        PutObjectOptions options = new PutObjectOptions(1, -1);
        options.setSse(sse);
        client.putObject(bucketName, objectName, is, options);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();

      client.copyObject(
          bucketName, objectName, null, sse, bucketName, objectName, null, copyConditions);
      ObjectStat objectStat =
          client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
      if (objectStat.httpHeaders().containsKey("X-Amz-Meta-Test")) {
        throw new Exception("expected user-defined metadata has been removed");
      }

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          methodName,
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: composeObject(String bucketName, String objectName, List&lt;ComposeSource&gt;
   * composeSources,Map &lt;String, String&gt; headerMap, ServerSideEncryption sseTarget).
   */
  public static void composeObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget).");
    }
    long startTime = System.currentTimeMillis();

    try {
      String destinationObjectName = getRandomName();
      String filename1 = createFile6Mb();
      String filename2 = createFile6Mb();
      PutObjectOptions options = new PutObjectOptions(6 * MB, -1);
      client.putObject(bucketName, filename1, filename1, options);
      client.putObject(bucketName, filename2, filename2, options);
      ComposeSource s1 = new ComposeSource(bucketName, filename1, null, null, null, null, null);
      ComposeSource s2 = new ComposeSource(bucketName, filename2, null, null, null, null, null);

      List<ComposeSource> listSourceObjects = new ArrayList<ComposeSource>();
      listSourceObjects.add(s1);
      listSourceObjects.add(s2);

      client.composeObject(bucketName, destinationObjectName, listSourceObjects, null, null);
      Files.delete(Paths.get(filename1));
      Files.delete(Paths.get(filename2));

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename1).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename2).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(destinationObjectName).build());

      mintSuccessLog(
          "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
          "size: 6 MB & 6 MB ",
          startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
            null,
            startTime);
      } else {
        mintFailedLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
            "size: 6 MB & 6 MB",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /**
   * Test: composeObject(String bucketName, String objectName, List&lt;ComposeSource&gt;
   * composeSources,Map &lt;String, String&gt; headerMap, ServerSideEncryption sseTarget).
   */
  public static void composeObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget) with offset and length.");
    }
    long startTime = System.currentTimeMillis();

    try {
      String destinationObjectName = getRandomName();
      String filename1 = createFile6Mb();
      String filename2 = createFile6Mb();
      PutObjectOptions options = new PutObjectOptions(6 * MB, -1);
      client.putObject(bucketName, filename1, filename1, options);
      client.putObject(bucketName, filename2, filename2, options);
      ComposeSource s1 = new ComposeSource(bucketName, filename1, 10L, 6291436L, null, null, null);
      ComposeSource s2 = new ComposeSource(bucketName, filename2, null, null, null, null, null);

      List<ComposeSource> listSourceObjects = new ArrayList<ComposeSource>();
      listSourceObjects.add(s1);
      listSourceObjects.add(s2);

      client.composeObject(bucketName, destinationObjectName, listSourceObjects, null, null);
      Files.delete(Paths.get(filename1));
      Files.delete(Paths.get(filename2));

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename1).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename2).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(destinationObjectName).build());

      mintSuccessLog(
          "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
          "with offset and length.",
          startTime);

    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget)"
                + "with offset and length.",
            null,
            startTime);
      } else {
        mintFailedLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
            "with offset and length.",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /**
   * Test: composeObject(String bucketName, String objectName, List&lt;ComposeSource&gt;
   * composeSources,Map &lt;String, String&gt; headerMap, ServerSideEncryption sseTarget).
   */
  public static void composeObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget) with one source");
    }
    long startTime = System.currentTimeMillis();

    try {
      String destinationObjectName = getRandomName();
      String filename1 = createFile6Mb();
      client.putObject(bucketName, filename1, filename1, new PutObjectOptions(6 * MB, -1));
      ComposeSource s1 = new ComposeSource(bucketName, filename1, 10L, 6291436L, null, null, null);

      List<ComposeSource> listSourceObjects = new ArrayList<ComposeSource>();
      listSourceObjects.add(s1);

      client.composeObject(bucketName, destinationObjectName, listSourceObjects, null, null);
      Files.delete(Paths.get(filename1));

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename1).build());
      client.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(destinationObjectName).build());

      mintSuccessLog(
          "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
          "with one source.",
          startTime);

    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget)"
                + "with one source.",
            null,
            startTime);
      } else {
        mintFailedLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
            "with one source.",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /**
   * Test: composeObject(String bucketName, String objectName, List&lt;ComposeSource&gt;
   * composeSources,Map &lt;String, String&gt; headerMap, ServerSideEncryption sseTarget).
   */
  public static void composeObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > userMetaData, ServerSideEncryption sseTarget) with SSE_C and SSE_C Target");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();

      // Generate a new 256 bit AES key - This key must be remembered by the client.
      byte[] key = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

      ServerSideEncryptionCustomerKey ssePut = ServerSideEncryption.withCustomerKey(secretKeySpec);

      byte[] keyTarget = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpecTarget = new SecretKeySpec(keyTarget, "AES");

      ServerSideEncryption sseTarget = ServerSideEncryption.withCustomerKey(secretKeySpecTarget);

      String filename1 = createFile6Mb();
      String filename2 = createFile6Mb();
      PutObjectOptions options = new PutObjectOptions(6 * MB, -1);
      options.setSse(ssePut);
      client.putObject(bucketName, filename1, filename1, options);
      client.putObject(bucketName, filename2, filename2, options);
      ComposeSource s1 = new ComposeSource(bucketName, filename1, null, null, null, null, ssePut);
      ComposeSource s2 = new ComposeSource(bucketName, filename2, null, null, null, null, ssePut);

      List<ComposeSource> listSourceObjects = new ArrayList<ComposeSource>();
      listSourceObjects.add(s1);
      listSourceObjects.add(s2);

      client.composeObject(bucketName, objectName, listSourceObjects, null, sseTarget);
      Files.delete(Paths.get(filename1));
      Files.delete(Paths.get(filename2));

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename1).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename2).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
          "with SSE_C and SSE_C Target",
          startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget) with SSE_C and"
                + "SSE_C Target",
            null,
            startTime);
      } else {
        mintFailedLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget) with SSE_C and ",
            "SSE_C Target",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /**
   * Test: composeObject(String bucketName, String objectName, List&lt;ComposeSource&gt;
   * composeSources,Map &lt;String, String&gt; headerMap, ServerSideEncryption sseTarget).
   */
  public static void composeObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > userMetaData, ServerSideEncryption sseTarget) with SSE_C on one source object");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();

      // Generate a new 256 bit AES key - This key must be remembered by the client.
      byte[] key = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

      ServerSideEncryptionCustomerKey ssePut = ServerSideEncryption.withCustomerKey(secretKeySpec);

      String filename1 = createFile6Mb();
      String filename2 = createFile6Mb();
      PutObjectOptions options = new PutObjectOptions(6 * MB, -1);
      options.setSse(ssePut);
      client.putObject(bucketName, filename1, filename1, options);
      client.putObject(bucketName, filename2, filename2, new PutObjectOptions(6 * MB, -1));
      ComposeSource s1 = new ComposeSource(bucketName, filename1, null, null, null, null, ssePut);
      ComposeSource s2 = new ComposeSource(bucketName, filename2, null, null, null, null, null);

      List<ComposeSource> listSourceObjects = new ArrayList<ComposeSource>();
      listSourceObjects.add(s1);
      listSourceObjects.add(s2);

      client.composeObject(bucketName, objectName, listSourceObjects, null, null);
      Files.delete(Paths.get(filename1));
      Files.delete(Paths.get(filename2));

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename1).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename2).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
          "with SSE_C on one source object ",
          startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget) with SSE_C on and"
                + "one source object",
            null,
            startTime);
      } else {
        mintFailedLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget) with SSE_C on ",
            "one source object",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /**
   * Test: composeObject(String bucketName, String objectName, List&lt;ComposeSource&gt;
   * composeSources,Map &lt;String, String&gt; headerMap, ServerSideEncryption sseTarget).
   */
  public static void composeObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > userMetaData, ServerSideEncryption sseTarget) with SSE_C Target only.");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      byte[] keyTarget = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpecTarget = new SecretKeySpec(keyTarget, "AES");

      ServerSideEncryption sseTarget = ServerSideEncryption.withCustomerKey(secretKeySpecTarget);

      String filename1 = createFile6Mb();
      String filename2 = createFile6Mb();
      PutObjectOptions options = new PutObjectOptions(6 * MB, -1);
      client.putObject(bucketName, filename1, filename1, options);
      client.putObject(bucketName, filename2, filename2, options);
      ComposeSource s1 = new ComposeSource(bucketName, filename1, null, null, null, null, null);
      ComposeSource s2 = new ComposeSource(bucketName, filename2, null, null, null, null, null);

      List<ComposeSource> listSourceObjects = new ArrayList<ComposeSource>();
      listSourceObjects.add(s1);
      listSourceObjects.add(s2);

      client.composeObject(bucketName, objectName, listSourceObjects, null, sseTarget);
      Files.delete(Paths.get(filename1));
      Files.delete(Paths.get(filename2));

      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename1).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename2).build());
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
      mintSuccessLog(
          "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
              + "Map <String,String > headerMap, ServerSideEncryption sseTarget)",
          "SSE_C Target only.",
          startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget) with SSE_C only",
            null,
            startTime);
      } else {
        mintFailedLog(
            "composeObject(String bucketName, String objectName,List<ComposeSource> composeSources, "
                + "Map <String,String > headerMap, ServerSideEncryption sseTarget) SSE_C Target ",
            " only.",
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: enableObjectLegalHold(EnableObjectLegalHoldArgs args) */
  public static void enableObjectLegalHold_test() throws Exception {
    String methodName = "enableObjectLegalHold(EnableObjectLegalHoldArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }
    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String objectName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());

      try {
        try (final InputStream is = new ContentInputStream(1 * KB)) {
          client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
        }

        client.enableObjectLegalHold(
            EnableObjectLegalHoldArgs.builder().bucket(bucketName).object(objectName).build());
        if (!client.isObjectLegalHoldEnabled(
            IsObjectLegalHoldEnabledArgs.builder().bucket(bucketName).object(objectName).build())) {
          throw new Exception("[FAILED] isObjectLegalHoldEnabled(): expected: true, got: false");
        }
        client.disableObjectLegalHold(
            DisableObjectLegalHoldArgs.builder().bucket(bucketName).object(objectName).build());
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeObject(
            RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: disableObjectLegalHold(DisableObjectLegalHoldArgs args) */
  public static void disableObjectLegalHold_test() throws Exception {
    String methodName = "disableObjectLegalHold(DisableObjectLegalHoldArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }
    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String objectName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        try (final InputStream is = new ContentInputStream(1 * KB)) {
          client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
        }
        client.enableObjectLegalHold(
            EnableObjectLegalHoldArgs.builder().bucket(bucketName).object(objectName).build());
        client.disableObjectLegalHold(
            DisableObjectLegalHoldArgs.builder().bucket(bucketName).object(objectName).build());
        if (client.isObjectLegalHoldEnabled(
            IsObjectLegalHoldEnabledArgs.builder().bucket(bucketName).object(objectName).build())) {
          throw new Exception("[FAILED] isObjectLegalHoldEnabled(): expected: false, got: true");
        }
      } finally {
        client.removeObject(
            RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: setDefaultRetention(SetDefaultRetentionArgs args). */
  public static void setDefaultRetention_test() throws Exception {
    String methodName = "setDefaultRetention(SetDefaultRetentionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String mintArgs = "config={COMPLIANCE, 10 days}";
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        ObjectLockConfiguration config =
            new ObjectLockConfiguration(RetentionMode.COMPLIANCE, new RetentionDurationDays(10));
        client.setDefaultRetention(
            SetDefaultRetentionArgs.builder().bucket(bucketName).config(config).build());
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
      mintSuccessLog(methodName, mintArgs, startTime);
    } catch (Exception e) {
      handleException(methodName, mintArgs, startTime, e);
    }
  }

  public static void testGetDefaultRetention(
      String bucketName, RetentionMode mode, RetentionDuration duration) throws Exception {
    ObjectLockConfiguration expectedConfig = new ObjectLockConfiguration(mode, duration);
    client.setDefaultRetention(
        SetDefaultRetentionArgs.builder().bucket(bucketName).config(expectedConfig).build());
    ObjectLockConfiguration config =
        client.getDefaultRetention(GetDefaultRetentionArgs.builder().bucket(bucketName).build());

    if (config.mode() != expectedConfig.mode()) {
      throw new Exception(
          "[FAILED] mode: expected: " + expectedConfig.mode() + ", got: " + config.mode());
    }

    if (config.duration().unit() != expectedConfig.duration().unit()
        || config.duration().duration() != expectedConfig.duration().duration()) {
      throw new Exception(
          "[FAILED] duration: " + expectedConfig.duration() + ", got: " + config.duration());
    }
  }

  /** Test: getDefaultRetention(GetDefaultRetentionArgs args). */
  public static void getDefaultRetention_test() throws Exception {
    String methodName = "getDefaultRetention(GetDefaultRetentionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        testGetDefaultRetention(
            bucketName, RetentionMode.COMPLIANCE, new RetentionDurationDays(10));
        testGetDefaultRetention(
            bucketName, RetentionMode.GOVERNANCE, new RetentionDurationYears(1));
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }

      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: deleteDefaultRetention(DeleteDefaultRetentionArgs args). */
  public static void deleteDefaultRetention_test() throws Exception {
    String methodName = "deleteDefaultRetention(DeleteDefaultRetentionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        client.deleteDefaultRetention(
            DeleteDefaultRetentionArgs.builder().bucket(bucketName).build());
        ObjectLockConfiguration config =
            new ObjectLockConfiguration(RetentionMode.COMPLIANCE, new RetentionDurationDays(10));
        client.setDefaultRetention(
            SetDefaultRetentionArgs.builder().bucket(bucketName).config(config).build());
        client.deleteDefaultRetention(
            DeleteDefaultRetentionArgs.builder().bucket(bucketName).build());
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: setObjectRetention(SetObjectRetentionArgs args). */
  public static void setObjectRetention_test1() throws Exception {
    String methodName = "setObjectRetention(SetObjectRetentionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String objectName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        client.putObject(
            bucketName,
            objectName,
            new ContentInputStream(1 * KB),
            new PutObjectOptions(1 * KB, -1));

        ZonedDateTime retentionUntil = ZonedDateTime.now(Time.UTC).plusDays(1);
        Retention expectedConfig = new Retention(RetentionMode.GOVERNANCE, retentionUntil);
        client.setObjectRetention(
            SetObjectRetentionArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .config(expectedConfig)
                .build());

        Retention emptyConfig = new Retention();
        client.setObjectRetention(
            SetObjectRetentionArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .config(emptyConfig)
                .bypassGovernanceMode(true)
                .build());

      } finally {
        client.removeObject(
            RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getObjectRetention(GetObjectRetentionArgs args). */
  public static void getObjectRetention_test1() throws Exception {
    String methodName = "getObjectRetention(GetObjectRetentionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String objectName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        client.putObject(
            bucketName,
            objectName,
            new ContentInputStream(1 * KB),
            new PutObjectOptions(1 * KB, -1));

        ZonedDateTime retentionUntil = ZonedDateTime.now(Time.UTC).plusDays(3);
        Retention expectedConfig = new Retention(RetentionMode.GOVERNANCE, retentionUntil);
        client.setObjectRetention(
            SetObjectRetentionArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .config(expectedConfig)
                .build());

        Retention config =
            client.getObjectRetention(
                GetObjectRetentionArgs.builder().bucket(bucketName).object(objectName).build());

        if (!(config
            .retainUntilDate()
            .withNano(0)
            .equals(expectedConfig.retainUntilDate().withNano(0)))) {
          throw new Exception(
              "[FAILED] Expected: expected duration : "
                  + expectedConfig.retainUntilDate()
                  + ", got: "
                  + config.retainUntilDate());
        }

        if (config.mode() != expectedConfig.mode()) {
          throw new Exception(
              "[FAILED] Expected: expected mode: "
                  + " expected mode :"
                  + expectedConfig.mode()
                  + ", got: "
                  + config.mode());
        }

        // Check shortening retention until period
        ZonedDateTime shortenedRetentionUntil = ZonedDateTime.now(Time.UTC).plusDays(1);
        expectedConfig = new Retention(RetentionMode.GOVERNANCE, shortenedRetentionUntil);
        client.setObjectRetention(
            SetObjectRetentionArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .config(expectedConfig)
                .bypassGovernanceMode(true)
                .build());

        config =
            client.getObjectRetention(
                GetObjectRetentionArgs.builder().bucket(bucketName).object(objectName).build());

        if (!(config
            .retainUntilDate()
            .withNano(0)
            .equals(expectedConfig.retainUntilDate().withNano(0)))) {
          throw new Exception(
              "[FAILED] Expected: expected duration : "
                  + expectedConfig.retainUntilDate()
                  + ", got: "
                  + config.retainUntilDate());
        }

        if (config.mode() != expectedConfig.mode()) {
          throw new Exception(
              " [FAILED] Expected: Expected mode :"
                  + expectedConfig.mode()
                  + ", got: "
                  + config.mode());
        }

        Retention emptyConfig = new Retention();
        client.setObjectRetention(
            SetObjectRetentionArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .config(emptyConfig)
                .bypassGovernanceMode(true)
                .build());

      } finally {
        client.removeObject(
            RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getBucketPolicy(GetBucketPolicyArgs args). */
  public static void getBucketPolicy_test1() throws Exception {
    String methodName = "getBucketPolicy(GetBucketPolicyArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String policy =
          "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Action\":[\"s3:GetObject\"],\"Effect\":\"Allow\","
              + "\"Principal\":{\"AWS\":[\"*\"]},\"Resource\":[\"arn:aws:s3:::"
              + bucketName
              + "/myobject*\"],\"Sid\":\"\"}]}";
      client.setBucketPolicy(
          SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
      client.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: setBucketPolicy(SetBucketPolicyArgs args). */
  public static void setBucketPolicy_test1() throws Exception {
    String methodName = "setBucketPolicy(SetBucketPolicyArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String policy =
          "{\"Statement\":[{\"Action\":\"s3:GetObject\",\"Effect\":\"Allow\",\"Principal\":"
              + "\"*\",\"Resource\":\"arn:aws:s3:::"
              + bucketName
              + "/myobject*\"}],\"Version\": \"2012-10-17\"}";
      client.setBucketPolicy(
          SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: deleteBucketPolicy(deleteBucketPolicyArgs args). */
  public static void deleteBucketPolicy_test1() throws Exception {
    String methodName = "deleteBucketPolicy(DeleteBucketPolicyArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String policy =
          "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Action\":[\"s3:GetObject\"],\"Effect\":\"Allow\","
              + "\"Principal\":{\"AWS\":[\"*\"]},\"Resource\":[\"arn:aws:s3:::"
              + bucketName
              + "/myobject*\"],\"Sid\":\"\"}]}";
      client.setBucketPolicy(
          SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
      client.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket(bucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: setBucketLifeCycle(SetBucketLifeCycleArgs args). */
  public static void setBucketLifeCycle_test1() throws Exception {
    String methodName = "setBucketLifeCycle(SetBucketLifeCycleArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      String lifeCycle =
          "<LifecycleConfiguration><Rule><ID>expire-bucket</ID><Prefix></Prefix>"
              + "<Status>Enabled</Status><Expiration><Days>365</Days></Expiration>"
              + "</Rule></LifecycleConfiguration>";
      client.setBucketLifeCycle(
          SetBucketLifeCycleArgs.builder().bucket(bucketName).config(lifeCycle).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: deleteBucketLifeCycle(DeleteBucketLifeCycleArgs args). */
  public static void deleteBucketLifeCycle_test1() throws Exception {
    String methodName = "deleteBucketLifeCycle(DeleteBucketLifeCycleArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      client.deleteBucketLifeCycle(DeleteBucketLifeCycleArgs.builder().bucket(bucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getBucketLifeCycle(GetBucketLifeCycleArgs args). */
  public static void getBucketLifeCycle_test1() throws Exception {
    String methodName = "getBucketLifeCycle(GetBucketLifeCycleArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      client.getBucketLifeCycle(GetBucketLifeCycleArgs.builder().bucket(bucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: setBucketNotification(SetBucketNotificationArgs args). */
  public static void setBucketNotification_test1() throws Exception {
    String methodName = "setBucketNotification(SetBucketNotificationArgs args)";
    long startTime = System.currentTimeMillis();
    if (sqsArn == null) {
      mintIgnoredLog(methodName, null, startTime);
      return;
    }

    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    try {
      String bucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region(region).build());

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      eventList.add(EventType.OBJECT_CREATED_COPY);
      QueueConfiguration queueConfig = new QueueConfiguration();
      queueConfig.setQueue(sqsArn);
      queueConfig.setEvents(eventList);
      queueConfig.setPrefixRule("images");
      queueConfig.setSuffixRule("pg");

      List<QueueConfiguration> queueConfigList = new LinkedList<>();
      queueConfigList.add(queueConfig);

      NotificationConfiguration config = new NotificationConfiguration();
      config.setQueueConfigurationList(queueConfigList);

      client.setBucketNotification(
          SetBucketNotificationArgs.builder().bucket(bucketName).config(config).build());

      client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getBucketNotification(GetBucketNotificationArgs args). */
  public static void getBucketNotification_test1() throws Exception {
    String methodName = "getBucketNotification(GetBucketNotificationArgs args)";
    long startTime = System.currentTimeMillis();
    if (sqsArn == null) {
      mintIgnoredLog(methodName, null, startTime);
      return;
    }

    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    try {
      String bucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region(region).build());

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      QueueConfiguration queueConfig = new QueueConfiguration();
      queueConfig.setQueue(sqsArn);
      queueConfig.setEvents(eventList);

      List<QueueConfiguration> queueConfigList = new LinkedList<>();
      queueConfigList.add(queueConfig);

      NotificationConfiguration expectedConfig = new NotificationConfiguration();
      expectedConfig.setQueueConfigurationList(queueConfigList);

      client.setBucketNotification(
          SetBucketNotificationArgs.builder().bucket(bucketName).config(expectedConfig).build());

      NotificationConfiguration config =
          client.getBucketNotification(
              GetBucketNotificationArgs.builder().bucket(bucketName).build());

      if (config.queueConfigurationList().size() != 1
          || !sqsArn.equals(config.queueConfigurationList().get(0).queue())
          || config.queueConfigurationList().get(0).events().size() != 1
          || config.queueConfigurationList().get(0).events().get(0)
              != EventType.OBJECT_CREATED_PUT) {
        System.out.println(
            "FAILED. expected: " + Xml.marshal(expectedConfig) + ", got: " + Xml.marshal(config));
      }

      client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: deleteBucketNotification(DeleteBucketNotificationArgs args). */
  public static void deleteBucketNotification_test1() throws Exception {
    String methodName = "deleteBucketNotification(DeleteBucketNotificationArgs args)";
    long startTime = System.currentTimeMillis();
    if (sqsArn == null) {
      mintIgnoredLog(methodName, null, startTime);
      return;
    }

    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    try {
      String bucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region(region).build());

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      eventList.add(EventType.OBJECT_CREATED_COPY);
      QueueConfiguration queueConfig = new QueueConfiguration();
      queueConfig.setQueue(sqsArn);
      queueConfig.setEvents(eventList);
      queueConfig.setPrefixRule("images");
      queueConfig.setSuffixRule("pg");

      List<QueueConfiguration> queueConfigList = new LinkedList<>();
      queueConfigList.add(queueConfig);

      NotificationConfiguration config = new NotificationConfiguration();
      config.setQueueConfigurationList(queueConfigList);

      client.setBucketNotification(
          SetBucketNotificationArgs.builder().bucket(bucketName).config(config).build());

      client.deleteBucketNotification(
          DeleteBucketNotificationArgs.builder().bucket(bucketName).build());

      config =
          client.getBucketNotification(
              GetBucketNotificationArgs.builder().bucket(bucketName).build());
      if (config.queueConfigurationList().size() != 0) {
        System.out.println("FAILED. expected: <empty>, got: " + Xml.marshal(config));
      }

      client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      mintSuccessLog(methodName, null, startTime);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: listenBucketNotification(ListenBucketNotificationArgs args). */
  public static void listenBucketNotification_test1() throws Exception {
    String methodName = "listenBucketNotification(ListenBucketNotificationArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String file = createFile1Kb();
    String bucketName = getRandomName();
    CloseableIterator<Result<NotificationRecords>> ci = null;
    String mintArgs =
        "prefix=prefix, suffix=suffix, events={\"s3:ObjectCreated:*\", \"s3:ObjectAccessed:*\"}";
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region(region).build());

      String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
      ci =
          client.listenBucketNotification(
              ListenBucketNotificationArgs.builder()
                  .bucket(bucketName)
                  .prefix("prefix")
                  .suffix("suffix")
                  .events(events)
                  .build());

      client.putObject(bucketName, "prefix-random-suffix", file, new PutObjectOptions(1 * KB, -1));

      while (ci.hasNext()) {
        NotificationRecords records = ci.next().get();
        if (records.events().size() == 0) {
          continue;
        }

        boolean found = false;
        for (Event event : records.events()) {
          if (event.objectName().equals("prefix-random-suffix")) {
            found = true;
            break;
          }
        }

        if (found) {
          break;
        }
      }

      mintSuccessLog(methodName, mintArgs, startTime);
    } catch (Exception e) {
      handleException(methodName, mintArgs, startTime, e);
    } finally {
      if (ci != null) {
        ci.close();
      }

      Files.delete(Paths.get(file));
      client.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object("prefix-random-suffix").build());
      client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }
  }

  /**
   * Test: selectObjectContent(String bucketName, String objectName, String sqlExpression,
   * InputSerialization is, OutputSerialization os, boolean requestProgress, Long scanStartRange,
   * Long scanEndRange, ServerSideEncryption sse).
   */
  public static void selectObjectContent_test1() throws Exception {
    String methodName = "selectObjectContent(SelectObjectContentArgs args)";
    String sqlExpression = "select * from S3Object";
    String args = "sqlExpression: " + sqlExpression + ", requestProgress: true";

    if (!mintEnv) {
      System.out.println("Test: " + methodName + ", " + args);
    }

    long startTime = System.currentTimeMillis();
    String objectName = getRandomName();
    SelectResponseStream responseStream = null;
    try {
      String expectedResult =
          "1997,Ford,E350,\"ac, abs, moon\",3000.00\n"
              + "1999,Chevy,\"Venture \"\"Extended Edition\"\"\",,4900.00\n"
              + "1999,Chevy,\"Venture \"\"Extended Edition, Very Large\"\"\",,5000.00\n"
              + "1996,Jeep,Grand Cherokee,\"MUST SELL!\n"
              + "air, moon roof, loaded\",4799.00\n";
      byte[] data =
          ("Year,Make,Model,Description,Price\n" + expectedResult).getBytes(StandardCharsets.UTF_8);
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      client.putObject(bucketName, objectName, bais, new PutObjectOptions(data.length, -1));

      InputSerialization is =
          new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null);
      OutputSerialization os =
          new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);

      responseStream =
          client.selectObjectContent(
              SelectObjectContentArgs.builder()
                  .bucket(bucketName)
                  .object(objectName)
                  .sqlExpression(sqlExpression)
                  .inputSerialization(is)
                  .outputSerialization(os)
                  .requestProgress(true)
                  .build());

      String result = new String(readAllBytes(responseStream), StandardCharsets.UTF_8);
      if (!result.equals(expectedResult)) {
        throw new Exception("result mismatch; expected: " + expectedResult + ", got: " + result);
      }

      Stats stats = responseStream.stats();

      if (stats == null) {
        throw new Exception("stats is null");
      }

      if (stats.bytesScanned() != 256) {
        throw new Exception(
            "stats.bytesScanned mismatch; expected: 258, got: " + stats.bytesScanned());
      }

      if (stats.bytesProcessed() != 256) {
        throw new Exception(
            "stats.bytesProcessed mismatch; expected: 258, got: " + stats.bytesProcessed());
      }

      if (stats.bytesReturned() != 222) {
        throw new Exception(
            "stats.bytesReturned mismatch; expected: 222, got: " + stats.bytesReturned());
      }

      mintSuccessLog(methodName, args, startTime);
    } catch (Exception e) {
      handleException(methodName, args, startTime, e);
    } finally {
      if (responseStream != null) {
        responseStream.close();
      }
      client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }
  }

  /** Test: setBucketEncryption(SetBucketEncryptionArgs args). */
  public static void setBucketEncryption_test() throws Exception {
    String methodName = "setBucketEncryption(SetBucketEncryptionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        List<SseConfigurationRule> ruleList = new LinkedList<>();
        ruleList.add(new SseConfigurationRule(null, SseAlgorithm.AES256));
        SseConfiguration config = new SseConfiguration(ruleList);
        client.setBucketEncryption(
            SetBucketEncryptionArgs.builder().bucket(bucketName).config(config).build());
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getBucketEncryption(GetBucketEncryptionArgs args). */
  public static void getBucketEncryption_test() throws Exception {
    String methodName = "getBucketEncryption(GetBucketEncryptionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        SseConfiguration config =
            client.getBucketEncryption(
                GetBucketEncryptionArgs.builder().bucket(bucketName).build());
        if (config.rules().size() != 0) {
          throw new Exception("expected: <empty rules>, got: " + config.rules());
        }

        List<SseConfigurationRule> ruleList = new LinkedList<>();
        ruleList.add(new SseConfigurationRule(null, SseAlgorithm.AES256));
        SseConfiguration expectedConfig = new SseConfiguration(ruleList);
        client.setBucketEncryption(
            SetBucketEncryptionArgs.builder().bucket(bucketName).config(expectedConfig).build());
        config =
            client.getBucketEncryption(
                GetBucketEncryptionArgs.builder().bucket(bucketName).build());
        if (config.rules().size() != 1) {
          throw new Exception("expected: 1, got: " + config.rules().size());
        }
        if (config.rules().get(0).sseAlgorithm() != expectedConfig.rules().get(0).sseAlgorithm()) {
          throw new Exception(
              "expected: "
                  + expectedConfig.rules().get(0).sseAlgorithm()
                  + ", got: "
                  + config.rules().get(0).sseAlgorithm());
        }
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: deleteBucketEncryption(DeleteBucketEncryptionArgs args). */
  public static void deleteBucketEncryption_test() throws Exception {
    String methodName = "deleteBucketEncryption(DeleteBucketEncryptionArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        // Delete should succeed.
        client.deleteBucketEncryption(
            DeleteBucketEncryptionArgs.builder().bucket(bucketName).build());

        List<SseConfigurationRule> ruleList = new LinkedList<>();
        ruleList.add(new SseConfigurationRule(null, SseAlgorithm.AES256));
        SseConfiguration config = new SseConfiguration(ruleList);
        client.setBucketEncryption(
            SetBucketEncryptionArgs.builder().bucket(bucketName).config(config).build());
        client.deleteBucketEncryption(
            DeleteBucketEncryptionArgs.builder().bucket(bucketName).build());
        config =
            client.getBucketEncryption(
                GetBucketEncryptionArgs.builder().bucket(bucketName).build());
        if (config.rules().size() != 0) {
          throw new Exception("expected: <empty rules>, got: " + config.rules());
        }
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: setBucketTags(SetBucketTagsArgs args). */
  public static void setBucketTags_test() throws Exception {
    String methodName = "setBucketTags(SetBucketTagsArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        Map<String, String> map = new HashMap<>();
        map.put("Project", "Project One");
        map.put("User", "jsmith");
        client.setBucketTags(SetBucketTagsArgs.builder().bucket(bucketName).tags(map).build());
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getBucketTags(GetBucketTagsArgs args). */
  public static void getBucketTags_test() throws Exception {
    String methodName = "getBucketTags(GetBucketTagsArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        Map<String, String> map = new HashMap<>();
        Tags tags = client.getBucketTags(GetBucketTagsArgs.builder().bucket(bucketName).build());
        if (!map.equals(tags.get())) {
          throw new Exception("expected: " + map + ", got: " + tags.get());
        }

        map.put("Project", "Project One");
        map.put("User", "jsmith");
        client.setBucketTags(SetBucketTagsArgs.builder().bucket(bucketName).tags(map).build());
        tags = client.getBucketTags(GetBucketTagsArgs.builder().bucket(bucketName).build());
        if (!map.equals(tags.get())) {
          throw new Exception("expected: " + map + ", got: " + tags.get());
        }
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: deleteBucketTags(DeleteBucketTagsArgs args). */
  public static void deleteBucketTags_test() throws Exception {
    String methodName = "deleteBucketTags(DeleteBucketTagsArgs args)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        // Delete should succeed.
        client.deleteBucketTags(DeleteBucketTagsArgs.builder().bucket(bucketName).build());

        Map<String, String> map = new HashMap<>();
        map.put("Project", "Project One");
        map.put("User", "jsmith");
        client.setBucketTags(SetBucketTagsArgs.builder().bucket(bucketName).tags(map).build());
        client.deleteBucketTags(DeleteBucketTagsArgs.builder().bucket(bucketName).build());
        Tags tags = client.getBucketTags(GetBucketTagsArgs.builder().bucket(bucketName).build());
        if (tags.get().size() != 0) {
          throw new Exception("expected: <empty map>" + ", got: " + tags.get());
        }
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: setObjectTags(String bucketName, Tags tags). */
  public static void setObjectTags_test() throws Exception {
    String methodName = "setObjectTags(String bucketName, String bucketName, Tags tags)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String objectName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        client.putObject(
            bucketName,
            objectName,
            new ContentInputStream(1 * KB),
            new PutObjectOptions(1 * KB, -1));
        Map<String, String> map = new HashMap<>();
        map.put("Project", "Project One");
        map.put("User", "jsmith");
        client.setObjectTags(
            SetObjectTagsArgs.builder().bucket(bucketName).object(objectName).tags(map).build());
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeObject(
            RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: getObjectTags(String bucketName). */
  public static void getObjectTags_test() throws Exception {
    String methodName = "getObjectTags(String bucketName, String bucketName)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String objectName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        client.putObject(
            bucketName,
            objectName,
            new ContentInputStream(1 * KB),
            new PutObjectOptions(1 * KB, -1));
        Map<String, String> map = new HashMap<>();
        Tags tags =
            client.getObjectTags(
                GetObjectTagsArgs.builder().bucket(bucketName).object(objectName).build());
        if (!map.equals(tags.get())) {
          throw new Exception("expected: " + map + ", got: " + tags.get());
        }

        map.put("Project", "Project One");
        map.put("User", "jsmith");
        client.setObjectTags(
            SetObjectTagsArgs.builder().bucket(bucketName).object(objectName).tags(map).build());
        tags =
            client.getObjectTags(
                GetObjectTagsArgs.builder().bucket(bucketName).object(objectName).build());
        if (!map.equals(tags.get())) {
          throw new Exception("expected: " + map + ", got: " + tags.get());
        }
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeObject(
            RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** Test: deleteObjectTags(String bucketName). */
  public static void deleteObjectTags_test() throws Exception {
    String methodName = "deleteObjectTags(String bucketName, String objectName)";
    if (!mintEnv) {
      System.out.println("Test: " + methodName);
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    String objectName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      try {
        client.putObject(
            bucketName,
            objectName,
            new ContentInputStream(1 * KB),
            new PutObjectOptions(1 * KB, -1));
        // Delete should succeed.
        client.deleteObjectTags(
            DeleteObjectTagsArgs.builder().bucket(bucketName).object(objectName).build());

        Map<String, String> map = new HashMap<>();
        map.put("Project", "Project One");
        map.put("User", "jsmith");
        client.setObjectTags(
            SetObjectTagsArgs.builder().bucket(bucketName).object(objectName).tags(map).build());
        client.deleteObjectTags(
            DeleteObjectTagsArgs.builder().bucket(bucketName).object(objectName).build());
        Tags tags =
            client.getObjectTags(
                GetObjectTagsArgs.builder().bucket(bucketName).object(objectName).build());
        if (tags.get().size() != 0) {
          throw new Exception("expected: <empty map>" + ", got: " + tags.get());
        }
        mintSuccessLog(methodName, null, startTime);
      } finally {
        client.removeObject(
            RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  /** runTests: runs as much as possible of test combinations. */
  public static void runTests() throws Exception {
    makeBucket_test1();
    makeBucket_test2();
    if (endpoint.toLowerCase(Locale.US).contains("s3")) {
      makeBucket_test3();
      makeBucket_test4();
    }

    listBuckets_test();

    bucketExists_test();
    enableVersioning_test();
    disableVersioning_test();

    removeBucket_test();

    setup();

    setObjectRetention_test1();
    getObjectRetention_test1();

    putObject_test1();
    putObject_test2();
    putObject_test3();
    putObject_test4();
    putObject_test5();
    putObject_test6();
    putObject_test7();
    putObject_test8();
    putObject_test9();
    putObject_test10();
    putObject_test11();

    statObject_test1();
    if (endpoint.toLowerCase(Locale.US).contains("https://")) statObject_test2();
    statObject_test3();
    statObject_test4();

    getObject_test1();
    getObject_test2();
    getObject_test3();
    getObject_test4();
    getObject_test5();
    getObject_test6();
    getObject_test8();

    getPresignedObjectUrl_test1();
    getPresignedObjectUrl_test2();
    getPresignedObjectUrl_test3();
    getPresignedObjectUrl_test4();
    getPresignedObjectUrl_test5();

    listObject_test1();
    listObject_test2();
    listObject_test3();
    listObject_test4();
    listObject_test5();
    listObject_test6();

    removeObject_test1();
    removeObject_test2();

    listIncompleteUploads_test1();
    listIncompleteUploads_test2();
    listIncompleteUploads_test3();

    removeIncompleteUploads_test();

    presignedPostPolicy_test();

    copyObject_test1();
    copyObject_test2();
    copyObject_test3();
    copyObject_test4();
    copyObject_test5();
    copyObject_test6();
    copyObject_test7();
    copyObject_test8();
    copyObject_test9();
    composeObject_test1();
    composeObject_test2();
    composeObject_test3();

    enableObjectLegalHold_test();
    disableObjectLegalHold_test();
    setDefaultRetention_test();
    getDefaultRetention_test();

    setObjectRetention_test1();
    getObjectRetention_test1();

    selectObjectContent_test1();

    setBucketEncryption_test();
    getBucketEncryption_test();
    deleteBucketEncryption_test();

    setBucketTags_test();
    getBucketTags_test();
    deleteBucketTags_test();
    setObjectTags_test();
    getObjectTags_test();
    deleteObjectTags_test();

    // SSE_C tests will only work over TLS connection
    if (endpoint.toLowerCase(Locale.US).contains("https://")) {
      getObject_test7();
      getObject_test9();
      putObject_test12();
      putObject_test13();
      copyObject_test10();
      composeObject_test4();
      composeObject_test5();
      composeObject_test6();
    }

    // SSE_S3 and SSE_KMS only work with Amazon AWS endpoint.
    String requestUrl = endpoint;
    if (requestUrl.contains(".amazonaws.com")) {
      putObject_test14();
      putObject_test15();
      copyObject_test11();
      copyObject_test12();
      setBucketLifeCycle_test1();
      getBucketLifeCycle_test1();
      deleteBucketLifeCycle_test1();
    }

    getBucketPolicy_test1();
    setBucketPolicy_test1();
    deleteBucketPolicy_test1();

    listenBucketNotification_test1();

    threadedPutObject();

    teardown();

    setBucketNotification_test1();
    getBucketNotification_test1();
    deleteBucketNotification_test1();
  }

  /** runQuickTests: runs tests those completely quicker. */
  public static void runQuickTests() throws Exception {
    makeBucket_test1();
    listBuckets_test();
    bucketExists_test();
    removeBucket_test();

    setup();

    putObject_test1();
    statObject_test1();
    getObject_test1();
    listObject_test1();
    removeObject_test1();
    listIncompleteUploads_test1();
    removeIncompleteUploads_test();
    getPresignedObjectUrl_test1();
    getPresignedObjectUrl_test2();
    presignedPostPolicy_test();
    copyObject_test1();
    getBucketPolicy_test1();
    setBucketPolicy_test1();
    deleteBucketPolicy_test1();
    selectObjectContent_test1();
    listenBucketNotification_test1();
    setBucketTags_test();
    getBucketTags_test();
    deleteBucketTags_test();
    setObjectTags_test();
    getObjectTags_test();
    deleteObjectTags_test();

    teardown();
  }

  public static boolean downloadMinio() throws IOException {
    String url = "https://dl.min.io/server/minio/release/";
    if (OS.contains("linux")) {
      url += "linux-amd64/minio";
    } else if (OS.contains("windows")) {
      url += "windows-amd64/minio.exe";
    } else if (OS.contains("mac")) {
      url += "darwin-amd64/minio";
    } else {
      System.out.println("unknown operating system " + OS);
      return false;
    }

    File file = new File(MINIO_BINARY);
    if (file.exists()) {
      return true;
    }

    System.out.println("downloading " + MINIO_BINARY + " binary");

    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder.url(HttpUrl.parse(url)).method("GET", null).build();
    OkHttpClient transport =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    Response response = transport.newCall(request).execute();

    try {
      if (!response.isSuccessful()) {
        System.out.println("failed to download binary " + MINIO_BINARY);
        return false;
      }

      BufferedSink bufferedSink = Okio.buffer(Okio.sink(new File(MINIO_BINARY)));
      bufferedSink.writeAll(response.body().source());
      bufferedSink.flush();
      bufferedSink.close();
    } finally {
      response.close();
    }

    if (!OS.contains("windows")) {
      file.setExecutable(true);
    }

    return true;
  }

  public static Process runMinio() throws Exception {
    File binaryPath = new File(new File(System.getProperty("user.dir")), MINIO_BINARY);
    ProcessBuilder pb = new ProcessBuilder(binaryPath.getPath(), "server", "d1");

    Map<String, String> env = pb.environment();
    env.put("MINIO_ACCESS_KEY", "minio");
    env.put("MINIO_SECRET_KEY", "minio123");
    env.put("MINIO_KMS_KES_ENDPOINT", "https://play.min.io:7373");
    env.put("MINIO_KMS_KES_KEY_FILE", "play.min.io.kes.root.key");
    env.put("MINIO_KMS_KES_CERT_FILE", "play.min.io.kes.root.cert");
    env.put("MINIO_KMS_KES_KEY_NAME", "my-minio-key");
    env.put("MINIO_NOTIFY_WEBHOOK_ENABLE_miniojavatest", "on");
    env.put("MINIO_NOTIFY_WEBHOOK_ENDPOINT_miniojavatest", "http://example.org/");
    sqsArn = "arn:minio:sqs::miniojavatest:webhook";

    pb.redirectErrorStream(true);
    pb.redirectOutput(ProcessBuilder.Redirect.to(new File(MINIO_BINARY + ".log")));

    System.out.println("starting minio server");
    Process p = pb.start();
    Thread.sleep(10 * 1000); // wait for 10 seconds to do real start.
    return p;
  }

  /** main(). */
  public static void main(String[] args) throws Exception {
    String mintMode = null;
    if (mintEnv) {
      mintMode = System.getenv("MINT_MODE");
    }

    Process minioProcess = null;

    if (args.length != 4) {
      endpoint = "http://localhost:9000";
      accessKey = "minio";
      secretKey = "minio123";
      region = "us-east-1";
      sqsArn = System.getenv("MINIO_JAVA_TEST_SQS_ARN");

      if (!downloadMinio()) {
        System.out.println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY> <REGION>");
        System.exit(-1);
      }

      minioProcess = runMinio();
      try {
        int exitValue = minioProcess.exitValue();
        System.out.println("minio server process exited with " + exitValue);
        System.out.println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY> <REGION>");
        System.exit(-1);
      } catch (IllegalThreadStateException e) {
        ignore();
      }
    } else {
      String dataDir = System.getenv("MINT_DATA_DIR");
      if (dataDir != null && !dataDir.equals("")) {
        mintEnv = true;
        dataFile1Kb = Paths.get(dataDir, "datafile-1-kB");
        dataFile6Mb = Paths.get(dataDir, "datafile-6-MB");
      }

      endpoint = args[0];
      accessKey = args[1];
      secretKey = args[2];
      region = args[3];
    }

    int exitValue = 0;
    try {
      client = new MinioClient(endpoint, accessKey, secretKey);
      // Enable trace for debugging.
      // client.traceOn(System.out);

      // For mint environment, run tests based on mint mode
      if (mintEnv) {
        if (mintMode != null && mintMode.equals("full")) {
          FunctionalTest.runTests();
        } else {
          FunctionalTest.runQuickTests();
        }
      } else {
        FunctionalTest.runTests();
        // Get new bucket name to avoid minio azure gateway failure.
        bucketName = getRandomName();
        // Quick tests with passed region.
        client = new MinioClient(endpoint, accessKey, secretKey, region);
        FunctionalTest.runQuickTests();
      }
    } catch (Exception e) {
      if (!mintEnv) {
        e.printStackTrace();
      }
      exitValue = -1;
    } finally {
      if (minioProcess != null) {
        minioProcess.destroy();
      }
    }

    System.exit(exitValue);
  }
}
