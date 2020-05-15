/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015-2019 MinIO, Inc.
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

import static java.nio.file.StandardOpenOption.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
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

  /** Test: makeBucket(MakeBucketArgs args). */
  public static void makeBucket_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: makeBucket(MakeBucketArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      client.removeBucket(name);
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
      System.out.println(
          "Test: with region and object lock functionality : makeBucket(MakeBucketArgs args)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(
          MakeBucketArgs.builder().bucket(name).region("eu-west-1").objectLock(true).build());
      client.removeBucket(name);
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
      client.removeBucket(name);
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
      client.removeBucket(name);
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
      client.removeBucket(bucketName);
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

  /** Test: bucketExists(String bucketName). */
  public static void bucketExists_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: bucketExists(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      if (!client.bucketExists(name)) {
        throw new Exception("[FAILED] bucket does not exist");
      }
      client.removeBucket(name);
      mintSuccessLog("bucketExists(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "bucketExists(String bucketName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: removeBucket(String bucketName). */
  public static void removeBucket_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: removeBucket(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(name).build());
      client.removeBucket(name);
      mintSuccessLog("removeBucket(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "removeBucket(String bucketName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Tear down test setup. */
  public static void setup() throws Exception {
    client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
  }

  /** Tear down test setup. */
  public static void teardown() throws Exception {
    client.removeBucket(bucketName);
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
      client.removeObject(bucketName, filename);
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
      client.removeObject(bucketName, filename);
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
      client.removeObject(bucketName, filename);
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

      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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
      client.removeObject(bucketName, objectName);
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

      client.removeObject(bucketName, objectName);
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
      ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());

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

      client.removeObject(bucketName, objectName);
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

      client.removeObject(bucketName, objectName);
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
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client.getObject(bucketName, objectName).close();

      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: getObject(String bucketName, String objectName, long offset). */
  public static void getObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, long offset)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client.getObject(bucketName, objectName, 1000L).close();
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "getObject(String bucketName, String objectName, long offset)",
          "offset: 1000",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, long offset)",
          "offset: 1000",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: getObject(String bucketName, String objectName, long offset, Long length). */
  public static void getObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: getObject(String bucketName, String objectName, long offset, Long length)");
    }
    long startTime = System.currentTimeMillis();

    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(6 * MB, -1));
      }

      client.getObject(bucketName, objectName, 1000L, 64 * 1024L).close();
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "getObject(String bucketName, String objectName, long offset, Long length)",
          "offset: 1000, length: 64 KB",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, long offset, Long length)",
          "offset: 1000, length: 64 KB",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: getObject(String bucketName, String objectName, String filename). */
  public static void getObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, String filename)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client.getObject(bucketName, objectName, objectName + ".downloaded");
      Files.delete(Paths.get(objectName + ".downloaded"));
      client.removeObject(bucketName, objectName);

      mintSuccessLog(
          "getObject(String bucketName, String objectName, String filename)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, String filename)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, String filename). where objectName has
   * multiple path segments.
   */
  public static void getObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: objectName with multiple path segments: "
              + "getObject(String bucketName, String objectName, String filename)");
    }

    long startTime = System.currentTimeMillis();
    String baseObjectName = getRandomName();
    String objectName = "path/to/" + baseObjectName;
    try {
      try (final InputStream is = new ContentInputStream(1 * KB)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(1 * KB, -1));
      }

      client.getObject(bucketName, objectName, baseObjectName + ".downloaded");
      Files.delete(Paths.get(baseObjectName + ".downloaded"));
      client.removeObject(bucketName, objectName);

      mintSuccessLog(
          "getObject(String bucketName, String objectName, String filename)",
          "objectName: " + objectName,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, String filename)",
          "objectName: " + objectName,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: getObject(String bucketName, String objectName) zero size object. */
  public static void getObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName) zero size object");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(0)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(0, -1));
      }

      client.getObject(bucketName, objectName).close();
      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, ServerSideEncryption sse). To test
   * getObject when object is put using SSE_C.
   */
  public static void getObject_test7() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: getObject(String bucketName, String objectName, ServerSideEncryption sse) using SSE_C");
    }

    long startTime = System.currentTimeMillis();
    // Generate a new 256 bit AES key - This key must be remembered by the client.
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());

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

      InputStream stream = client.getObject(bucketName, objectName, sse);
      byte[] getbyteArray = new byte[stream.available()];
      int bytes_read_get = stream.read(getbyteArray);
      String getString = new String(getbyteArray, StandardCharsets.UTF_8);
      stream.close();

      // Compare if contents received are same as the initial uploaded object.
      if ((!putString.equals(getString)) || (bytes_read_put != bytes_read_get)) {
        throw new Exception("Contents received from getObject doesn't match initial contents.");
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "getObject(String bucketName, String objectName, ServerSideEncryption sse)"
              + " using SSE_C.",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, ServerSideEncryption sse)"
              + " using SSE_C.",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset, Long length) with offset=0.
   */
  public static void getObject_test8() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: getObject(String bucketName, String objectName, long offset, Long length) with offset=0");
    }

    final long startTime = System.currentTimeMillis();
    final int fullLength = 1024;
    final int partialLength = 256;
    final long offset = 0L;
    final String objectName = getRandomName();
    try {
      try (final InputStream is = new ContentInputStream(fullLength)) {
        client.putObject(bucketName, objectName, is, new PutObjectOptions(fullLength, -1));
      }

      try (final InputStream partialObjectStream =
          client.getObject(bucketName, objectName, offset, Long.valueOf(partialLength))) {
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
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "getObject(String bucketName, String objectName, long offset, Long length) with offset=0",
          String.format("offset: %d, length: %d bytes", offset, partialLength),
          startTime);
    } catch (final Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, long offset, Long length) with offset=0",
          String.format("offset: %d, length: %d bytes", offset, partialLength),
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, ServerSideEncryption sse, String
   * fileName).
   */
  public static void getObject_test9() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: getObject(String bucketName, String objectName, ServerSideEncryption sse, String fileName)");
    }

    long startTime = System.currentTimeMillis();
    // Generate a new 256 bit AES key - This key must be remembered by the client.
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
    try {
      String objectName = getRandomName();
      String filename = createFile1Kb();
      PutObjectOptions options = new PutObjectOptions(1 * KB, -1);
      options.setSse(sse);
      client.putObject(bucketName, objectName, filename, options);
      client.getObject(bucketName, objectName, sse, objectName + ".downloaded");
      Files.delete(Paths.get(objectName + ".downloaded"));
      client.removeObject(bucketName, objectName);

      mintSuccessLog(
          "getObject(String bucketName, String objectName, ServerSideEncryption sse, "
              + "String filename). To test SSE_C",
          "size: 1 KB",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, ServerSideEncryption sse, "
              + "String filename). To test SSE_C",
          "size: 1 KB",
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: listObjects(final String bucketName). */
  public static void listObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listObjects(final String bucketName)");
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
      for (Result<?> r : client.listObjects(bucketName)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog("listObjects(final String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(final String bucketName)",
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
      System.out.println("Test: listObjects(final String bucketName, final String prefix)");
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
      for (Result<?> r : client.listObjects(bucketName, "minio")) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog(
          "listObjects(final String bucketName, final String prefix)", "prefix :minio", startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(final String bucketName, final String prefix)",
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
      System.out.println(
          "Test: listObjects(final String bucketName, final String prefix, final boolean recursive)");
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
      for (Result<?> r : client.listObjects(bucketName, "minio", true)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog(
          "listObjects(final String bucketName, final String prefix, final boolean recursive)",
          "prefix :minio, recursive: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(final String bucketName, final String prefix, final boolean recursive)",
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
          "Test: empty bucket: listObjects(final String bucketName, final String prefix,"
              + " final boolean recursive)");
    }

    long startTime = System.currentTimeMillis();
    try {
      int i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minioemptybucket", true)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }
      mintSuccessLog(
          "listObjects(final String bucketName, final String prefix, final boolean recursive)",
          "prefix :minioemptybucket, recursive: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(final String bucketName, final String prefix, final boolean recursive)",
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
          "Test: recursive: listObjects(final String bucketName, final String prefix, "
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
      for (Result<?> r : client.listObjects(bucketName, "minio", true)) {
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
          "listObjects(final String bucketName, final String prefix, final boolean recursive)",
          "prefix :minio, recursive: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(final String bucketName, final String prefix, final boolean recursive)",
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
          "Test: listObjects(final String bucketName, final String prefix, final boolean recursive, "
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
      for (Result<?> r : client.listObjects(bucketName, "minio", true, true)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObjects(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog(
          "listObjects(final String bucketName, final String prefix, "
              + "final boolean recursive, final boolean useVersion1)",
          "prefix :minio, recursive: true, useVersion1: true",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "listObjects(final String bucketName, final String prefix, "
              + "final boolean recursive, final boolean useVersion1)",
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

      client.removeObject(bucketName, objectName);
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

      client.removeIncompleteUpload(bucketName, objectName);
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

      client.removeIncompleteUpload(bucketName, objectName);
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

      client.removeIncompleteUpload(bucketName, objectName);
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
    if (!mintEnv) {
      System.out.println("Test: removeIncompleteUpload(String bucketName, String objectName)");
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

      client.removeIncompleteUpload(bucketName, objectName);
      mintSuccessLog(
          "removeIncompleteUpload(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "removeIncompleteUpload(String bucketName, String objectName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** public String presignedGetObject(String bucketName, String objectName). */
  public static void presignedGetObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedGetObject(String bucketName, String objectName)");
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

      String urlString = client.presignedGetObject(bucketName, objectName);

      byte[] outBytes = readObject(urlString);
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("presignedGetObject(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "presignedGetObject(String bucketName, String objectName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: presignedGetObject(String bucketName, String objectName, Integer expires). */
  public static void presignedGetObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: presignedGetObject(String bucketName, String objectName, Integer expires)");
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

      String urlString = client.presignedGetObject(bucketName, objectName, 3600);
      byte[] outBytes = readObject(urlString);
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "presignedGetObject(String bucketName, String objectName, Integer expires)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "presignedGetObject(String bucketName, String objectName, Integer expires)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * public String presignedGetObject(String bucketName, String objectName, Integer expires, Map
   * reqParams).
   */
  public static void presignedGetObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: presignedGetObject(String bucketName, String objectName, Integer expires, "
              + "Map<String, String> reqParams)");
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

      String urlString = client.presignedGetObject(bucketName, objectName, 3600, reqParams);
      byte[] outBytes = readObject(urlString);
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,"
              + " String> reqParams)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,"
              + " String> reqParams)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** public String presignedPutObject(String bucketName, String objectName). */
  public static void presignedPutObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedPutObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      String urlString = client.presignedPutObject(bucketName, objectName);
      byte[] data = "hello, world".getBytes(StandardCharsets.UTF_8);
      writeObject(urlString, data);
      client.removeObject(bucketName, objectName);
      mintSuccessLog("presignedPutObject(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "presignedPutObject(String bucketName, String objectName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: presignedPutObject(String bucketName, String objectName, Integer expires). */
  public static void presignedPutObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: presignedPutObject(String bucketName, String objectName, Integer expires)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      String urlString = client.presignedPutObject(bucketName, objectName, 3600);
      byte[] data = "hello, world".getBytes(StandardCharsets.UTF_8);
      writeObject(urlString, data);
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "presignedPutObject(String bucketName, String objectName, Integer expires)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "presignedPutObject(String bucketName, String objectName, Integer expires)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
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

      client.removeObject(bucketName, objectName);
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
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName)");
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
      client.getObject(destBucketName, objectName).close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName)",
          null,
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

      client.removeObject(bucketName, objectName);
      client.removeBucket(destBucketName);

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
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions) with Matching ETag (Positive Case)");
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
      client.getObject(destBucketName, objectName).close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName,"
              + " CopyConditions copyConditions)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName,"
              + " CopyConditions copyConditions)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, CopyConditions
   * copyConditions) with ETag to not match.
   */
  public static void copyObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions) with not matching ETag"
              + " (Positive Case)");
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
      client.getObject(destBucketName, objectName).close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);

      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName,"
              + " CopyConditions copyConditions)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName,"
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

      client.removeObject(bucketName, objectName);
      client.removeBucket(destBucketName);

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
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, String destBucketName,"
              + "CopyConditions copyConditions) with modified after "
              + "condition (Positive Case)");
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
      client.getObject(destBucketName, objectName).close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions)",
          "CopyCondition: modifiedDateCondition",
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, String destBucketName, "
              + "CopyConditions copyConditions)",
          "CopyCondition: modifiedDateCondition",
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

      client.removeObject(bucketName, objectName);
      // Destination bucket is expected to be empty, otherwise it will trigger an
      // exception.
      client.removeBucket(destBucketName);
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

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
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

      client.removeObject(bucketName, objectName);
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

      ServerSideEncryption sseTarget = ServerSideEncryption.withCustomerKey(secretKeySpecTarget);

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

      client.removeObject(bucketName, objectName);
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

      client.removeObject(bucketName, objectName);
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
    if (!mintEnv) {
      System.out.println(
          "Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_KMS. ");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();

      Map<String, String> myContext = new HashMap<>();
      myContext.put("key1", "value1");

      String keyId = "";
      keyId = System.getenv("MINT_KEY_ID");
      if (keyId.equals("")) {
        mintIgnoredLog("getBucketPolicy(String bucketName)", null, startTime);
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

      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_KMS.",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
              + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
              + " using SSE_KMS.",
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

      client.removeObject(bucketName, filename1);
      client.removeObject(bucketName, filename2);
      client.removeObject(bucketName, destinationObjectName);

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

      client.removeObject(bucketName, filename1);
      client.removeObject(bucketName, filename2);
      client.removeObject(bucketName, destinationObjectName);

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

      client.removeObject(bucketName, filename1);
      client.removeObject(bucketName, destinationObjectName);

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

      ServerSideEncryption ssePut = ServerSideEncryption.withCustomerKey(secretKeySpec);

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

      client.removeObject(bucketName, filename1);
      client.removeObject(bucketName, filename2);
      client.removeObject(bucketName, objectName);
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

      ServerSideEncryption ssePut = ServerSideEncryption.withCustomerKey(secretKeySpec);

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

      client.removeObject(bucketName, filename1);
      client.removeObject(bucketName, filename2);
      client.removeObject(bucketName, objectName);
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

      client.removeObject(bucketName, filename1);
      client.removeObject(bucketName, filename2);
      client.removeObject(bucketName, objectName);
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

  /** Test: enableObjectLegalHold(String bucketName, String objectName, String versionId) */
  public static void enableObjectLegalHold_test() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: enableObjectLegalHold(String bucketName, String objectName, String versionId)");
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

        client.enableObjectLegalHold(bucketName, objectName, null);
        if (!client.isObjectLegalHoldEnabled(bucketName, objectName, null)) {
          throw new Exception("[FAILED] isObjectLegalHoldEnabled(): expected: true, got: false");
        }
        client.disableObjectLegalHold(bucketName, objectName, null);
        mintSuccessLog(
            "enableObjectLegalHold(String bucketName, String objectName, String versionId)",
            null,
            startTime);
      } finally {
        client.removeObject(bucketName, objectName);
        client.removeBucket(bucketName);
      }
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "enableObjectLegalHold(String bucketName, String objectName, String versionId)",
            null,
            startTime);
      } else {
        mintFailedLog(
            "enableObjectLegalHold(String bucketName, String objectName, String versionId)",
            null,
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: disableObjectLegalHold(String bucketName, String objectName, String versionId) */
  public static void disableObjectLegalHold_test() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: disableObjectLegalHold(String bucketName, String objectName, String versionId)");
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
        client.enableObjectLegalHold(bucketName, objectName, null);
        client.disableObjectLegalHold(bucketName, objectName, null);
        if (client.isObjectLegalHoldEnabled(bucketName, objectName, null)) {
          throw new Exception("[FAILED] isObjectLegalHoldEnabled(): expected: false, got: true");
        }
      } finally {
        client.removeObject(bucketName, objectName);
        client.removeBucket(bucketName);
      }
      mintSuccessLog(
          "disableObjectLegalHold(String bucketName, String objectName, String versionId)",
          null,
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
            "disableObjectLegalHold(String bucketName, String objectName, String versionId)",
            null,
            startTime);
      } else {
        mintFailedLog(
            "disableObjectLegalHold(String bucketName, String objectName, String versionId)",
            null,
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: setDefaultRetention(String bucketName). */
  public static void setDefaultRetention_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: setDefaultRetention(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();

    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        ObjectLockConfiguration config =
            new ObjectLockConfiguration(RetentionMode.COMPLIANCE, new RetentionDurationDays(10));
        client.setDefaultRetention(bucketName, config);
      } finally {
        client.removeBucket(bucketName);
      }
      mintSuccessLog("setDefaultRetention (String bucketName)", null, startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog("setDefaultRetention (String bucketName)", null, startTime);
      } else {
        mintFailedLog(
            "setDefaultRetention (String bucketName)",
            null,
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: getDefaultRetention(String bucketName). */
  public static void getDefaultRetention_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getDefaultRetention(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    String bucketName = getRandomName();
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(true).build());
      try {
        ObjectLockConfiguration expectedConfig =
            new ObjectLockConfiguration(RetentionMode.COMPLIANCE, new RetentionDurationDays(10));
        client.setDefaultRetention(bucketName, expectedConfig);
        ObjectLockConfiguration config = client.getDefaultRetention(bucketName);

        if ((!(config.duration().unit() == expectedConfig.duration().unit()
                && config.duration().duration() == expectedConfig.duration().duration()))
            || (config.mode() != expectedConfig.mode())) {
          throw new Exception(
              "[FAILED] Expected: expected duration : "
                  + expectedConfig.duration()
                  + ", got: "
                  + config.duration()
                  + " expected mode :"
                  + expectedConfig.mode()
                  + ", got: "
                  + config.mode());
        }

        expectedConfig =
            new ObjectLockConfiguration(RetentionMode.GOVERNANCE, new RetentionDurationYears(1));
        client.setDefaultRetention(bucketName, expectedConfig);
        config = client.getDefaultRetention(bucketName);

        if ((!(config.duration().unit() == expectedConfig.duration().unit()
                && config.duration().duration() == expectedConfig.duration().duration()))
            || (config.mode() != expectedConfig.mode())) {
          throw new Exception(
              "[FAILED] Expected: expected duration : "
                  + expectedConfig.duration()
                  + ", got: "
                  + config.duration()
                  + " expected mode :"
                  + expectedConfig.mode()
                  + ", got: "
                  + config.mode());
        }
      } finally {
        client.removeBucket(bucketName);
      }

      mintSuccessLog("getDefaultRetention (String bucketName)", null, startTime);

    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog("getDefaultRetention (String bucketName)", null, startTime);
      } else {
        mintFailedLog(
            "getDefaultRetention (String bucketName)",
            null,
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: getBucketPolicy(String bucketName). */
  public static void getBucketPolicy_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getBucketPolicy(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String policy =
          "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Action\":[\"s3:GetObject\"],\"Effect\":\"Allow\","
              + "\"Principal\":{\"AWS\":[\"*\"]},\"Resource\":[\"arn:aws:s3:::"
              + bucketName
              + "/myobject*\"],\"Sid\":\"\"}]}";
      client.setBucketPolicy(bucketName, policy);
      client.getBucketPolicy(bucketName);
      mintSuccessLog("getBucketPolicy(String bucketName)", null, startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog("getBucketPolicy(String bucketName)", null, startTime);
      } else {
        mintFailedLog(
            "getBucketPolicy(String bucketName)",
            null,
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: setBucketPolicy(String bucketName, String policy). */
  public static void setBucketPolicy_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: setBucketPolicy(String bucketName, String policy)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String policy =
          "{\"Statement\":[{\"Action\":\"s3:GetObject\",\"Effect\":\"Allow\",\"Principal\":"
              + "\"*\",\"Resource\":\"arn:aws:s3:::"
              + bucketName
              + "/myobject*\"}],\"Version\": \"2012-10-17\"}";
      client.setBucketPolicy(bucketName, policy);
      mintSuccessLog("setBucketPolicy(String bucketName, String policy)", null, startTime);
    } catch (Exception e) {
      ErrorResponse errorResponse = null;
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        errorResponse = exp.errorResponse();
      }

      // Ignore NotImplemented error
      if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
        mintIgnoredLog(
            "setBucketPolicy(String bucketName, String objectPrefix, " + "PolicyType policyType)",
            null,
            startTime);
      } else {
        mintFailedLog(
            "setBucketPolicy(String bucketName, String objectPrefix, " + "PolicyType policyType)",
            null,
            startTime,
            null,
            e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /** Test: setBucketLifeCycle(String bucketName, String lifeCycle). */
  public static void setBucketLifeCycle_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: setBucketLifeCycle(String bucketName, String lifeCycle)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String lifeCycle =
          "<LifecycleConfiguration><Rule><ID>expire-bucket</ID><Prefix></Prefix>"
              + "<Status>Enabled</Status><Expiration><Days>365</Days></Expiration>"
              + "</Rule></LifecycleConfiguration>";
      client.setBucketLifeCycle(bucketName, lifeCycle);
      mintSuccessLog("setBucketLifeCycle(String bucketName, String lifeCycle)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "setBucketLifeCycle(String bucketName, String lifeCycle) ",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: deleteBucketLifeCycle(String bucketName). */
  public static void deleteBucketLifeCycle_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: deleteBucketLifeCycle(String bucketNam)");
    }

    long startTime = System.currentTimeMillis();
    try {
      client.deleteBucketLifeCycle(bucketName);
      mintSuccessLog("deleteBucketLifeCycle(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "deleteBucketLifeCycle(String bucketName) ",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: getBucketLifeCycle(String bucketName). */
  public static void getBucketLifeCycle_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getBucketLifeCycle(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      client.getBucketLifeCycle(bucketName);
      mintSuccessLog("getBucketLifeCycle(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getBucketLifeCycle(String bucketName) ",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: setBucketNotification(String bucketName, NotificationConfiguration
   * notificationConfiguration).
   */
  public static void setBucketNotification_test1() throws Exception {
    // This test requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION'
    // environment variables.
    String topic = System.getenv("MINIO_JAVA_TEST_TOPIC");
    String region = System.getenv("MINIO_JAVA_TEST_REGION");
    if (topic == null || topic.equals("") || region == null || region.equals("")) {
      // do not run functional test as required environment variables are missing.
      return;
    }

    if (!mintEnv) {
      System.out.println(
          "Test: setBucketNotification(String bucketName, "
              + "NotificationConfiguration notificationConfiguration)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).region(region).build());

      NotificationConfiguration notificationConfiguration = new NotificationConfiguration();

      // Add a new topic configuration.
      List<TopicConfiguration> topicConfigurationList =
          notificationConfiguration.topicConfigurationList();
      TopicConfiguration topicConfiguration = new TopicConfiguration();
      topicConfiguration.setTopic(topic);

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      eventList.add(EventType.OBJECT_CREATED_COPY);
      topicConfiguration.setEvents(eventList);
      topicConfiguration.setPrefixRule("images");
      topicConfiguration.setSuffixRule("pg");

      topicConfigurationList.add(topicConfiguration);
      notificationConfiguration.setTopicConfigurationList(topicConfigurationList);

      client.setBucketNotification(destBucketName, notificationConfiguration);

      client.removeBucket(destBucketName);
      mintSuccessLog(
          "setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)",
          null,
          startTime);
    } catch (Exception e) {
      mintFailedLog(
          "setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: getBucketNotification(String bucketName). */
  public static void getBucketNotification_test1() throws Exception {
    // This test requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION'
    // environment variables.
    String topic = System.getenv("MINIO_JAVA_TEST_TOPIC");
    String region = System.getenv("MINIO_JAVA_TEST_REGION");
    if (topic == null || topic.equals("") || region == null || region.equals("")) {
      // do not run functional test as required environment variables are missing.
      return;
    }

    if (!mintEnv) {
      System.out.println("Test: getBucketNotification(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).region(region).build());

      NotificationConfiguration notificationConfiguration = new NotificationConfiguration();

      // Add a new topic configuration.
      List<TopicConfiguration> topicConfigurationList =
          notificationConfiguration.topicConfigurationList();
      TopicConfiguration topicConfiguration = new TopicConfiguration();
      topicConfiguration.setTopic(topic);

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      topicConfiguration.setEvents(eventList);

      topicConfigurationList.add(topicConfiguration);
      notificationConfiguration.setTopicConfigurationList(topicConfigurationList);

      client.setBucketNotification(destBucketName, notificationConfiguration);
      String expectedResult = notificationConfiguration.toString();

      notificationConfiguration = client.getBucketNotification(destBucketName);

      topicConfigurationList = notificationConfiguration.topicConfigurationList();
      topicConfiguration = topicConfigurationList.get(0);
      topicConfiguration.setId(null);
      String result = notificationConfiguration.toString();

      if (!result.equals(expectedResult)) {
        System.out.println("FAILED. expected: " + expectedResult + ", got: " + result);
      }

      client.removeBucket(destBucketName);
      mintSuccessLog("getBucketNotification(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "getBucketNotification(String bucketName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: removeAllBucketNotification(String bucketName). */
  public static void removeAllBucketNotification_test1() throws Exception {
    // This test requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION'
    // environment variables.
    String topic = System.getenv("MINIO_JAVA_TEST_TOPIC");
    String region = System.getenv("MINIO_JAVA_TEST_REGION");
    if (topic == null || topic.equals("") || region == null || region.equals("")) {
      // do not run functional test as required environment variables are missing.
      return;
    }

    if (!mintEnv) {
      System.out.println("Test: removeAllBucketNotification(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String destBucketName = getRandomName();
      client.makeBucket(MakeBucketArgs.builder().bucket(destBucketName).region(region).build());

      NotificationConfiguration notificationConfiguration = new NotificationConfiguration();

      // Add a new topic configuration.
      List<TopicConfiguration> topicConfigurationList =
          notificationConfiguration.topicConfigurationList();
      TopicConfiguration topicConfiguration = new TopicConfiguration();
      topicConfiguration.setTopic(topic);

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      eventList.add(EventType.OBJECT_CREATED_COPY);
      topicConfiguration.setEvents(eventList);
      topicConfiguration.setPrefixRule("images");
      topicConfiguration.setSuffixRule("pg");

      topicConfigurationList.add(topicConfiguration);
      notificationConfiguration.setTopicConfigurationList(topicConfigurationList);

      client.setBucketNotification(destBucketName, notificationConfiguration);

      notificationConfiguration = new NotificationConfiguration();
      String expectedResult = notificationConfiguration.toString();

      client.removeAllBucketNotification(destBucketName);

      notificationConfiguration = client.getBucketNotification(destBucketName);
      String result = notificationConfiguration.toString();
      if (!result.equals(expectedResult)) {
        throw new Exception("[FAILED] Expected: " + expectedResult + ", Got: " + result);
      }

      client.removeBucket(destBucketName);
      mintSuccessLog("removeAllBucketNotification(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          "removeAllBucketNotification(String bucketName)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /** Test: listenBucketNotification(String bucketName). */
  public static void listenBucketNotification_test1() throws Exception {
    if (!mintEnv) {
      System.out.println(
          "Test: listenBucketNotification(String bucketName, String prefix, "
              + "String suffix, String[] events)");
    }

    long startTime = System.currentTimeMillis();
    String file = createFile1Kb();
    String bucketName = getRandomName();
    CloseableIterator<Result<NotificationRecords>> ci = null;
    try {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region(region).build());

      String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
      ci = client.listenBucketNotification(bucketName, "prefix", "suffix", events);

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

      mintSuccessLog(
          "listenBucketNotification(String bucketName, String prefix, "
              + "String suffix, String[] events)",
          null,
          startTime);
    } catch (Exception e) {
      if (e instanceof ErrorResponseException) {
        ErrorResponseException exp = (ErrorResponseException) e;
        ErrorResponse errorResponse = exp.errorResponse();
        if (errorResponse != null && errorResponse.errorCode() == ErrorCode.NOT_IMPLEMENTED) {
          mintIgnoredLog(
              "listenBucketNotification(String bucketName, String prefix, "
                  + "String suffix, String[] events)",
              null,
              startTime);
          return;
        }
      }

      mintFailedLog(
          "listenBucketNotification(String bucketName, String prefix, "
              + "String suffix, String[] events)",
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    } finally {
      if (ci != null) {
        ci.close();
      }

      Files.delete(Paths.get(file));
      client.removeObject(bucketName, "prefix-random-suffix");
      client.removeBucket(bucketName);
    }
  }

  /**
   * Test: selectObjectContent(String bucketName, String objectName, String sqlExpression,
   * InputSerialization is, OutputSerialization os, boolean requestProgress, Long scanStartRange,
   * Long scanEndRange, ServerSideEncryption sse).
   */
  public static void selectObjectContent_test1() throws Exception {
    String testName =
        "selectObjectContent(String bucketName, String objectName, String sqlExpression,"
            + " InputSerialization is, OutputSerialization os, boolean requestProgress,"
            + " Long scanStartRange, Long scanEndRange, ServerSideEncryption sse)";

    if (!mintEnv) {
      System.out.println("Test: " + testName);
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

      String sqlExpression = "select * from S3Object";
      InputSerialization is =
          new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null);
      OutputSerialization os =
          new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);

      responseStream =
          client.selectObjectContent(
              bucketName, objectName, sqlExpression, is, os, true, null, null, null);

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

      mintSuccessLog(testName, null, startTime);
    } catch (Exception e) {
      mintFailedLog(
          testName,
          null,
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    } finally {
      if (responseStream != null) {
        responseStream.close();
      }
      client.removeObject(bucketName, objectName);
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

    removeBucket_test();

    setup();

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

    presignedGetObject_test1();
    presignedGetObject_test2();
    presignedGetObject_test3();

    presignedPutObject_test1();
    presignedPutObject_test2();

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

    selectObjectContent_test1();

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

    // SSE_S3 and SSE_KMS only work with endpoint="s3.amazonaws.com"
    String requestUrl = endpoint;
    if (requestUrl.equals("s3.amazonaws.com")) {
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

    listenBucketNotification_test1();

    threadedPutObject();

    teardown();

    // notification tests requires 'MINIO_JAVA_TEST_TOPIC' and
    // 'MINIO_JAVA_TEST_REGION' environment variables
    // to be set appropriately.
    setBucketNotification_test1();
    getBucketNotification_test1();
    removeAllBucketNotification_test1();
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
    presignedGetObject_test1();
    presignedPutObject_test1();
    presignedPostPolicy_test();
    copyObject_test1();
    getBucketPolicy_test1();
    setBucketPolicy_test1();
    selectObjectContent_test1();
    listenBucketNotification_test1();

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
      e.printStackTrace();
      exitValue = -1;
    } finally {
      if (minioProcess != null) {
        minioProcess.destroy();
      }
    }

    System.exit(exitValue);
  }
}
