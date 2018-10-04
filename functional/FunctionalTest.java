/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015, 2016, 2017, 2018 Minio, Inc.
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

import java.security.*;
import java.math.BigInteger;
import java.util.*;
import java.io.*;

import static java.nio.file.StandardOpenOption.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.joda.time.DateTime;

import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import okhttp3.Response;

import io.minio.*;
import io.minio.messages.*;
import io.minio.errors.*;

@SuppressFBWarnings(value = "REC", justification = "Allow catching super class Exception since it's tests")
public class FunctionalTest {
  private static final String PASS = "PASS";
  private static final String FAILED = "FAIL";
  private static final String IGNORED = "NA";
  private static final int MB = 1024 * 1024;
  private static final Random random = new Random(new SecureRandom().nextLong());
  private static final String customContentType = "application/javascript";
  private static final String nullContentType = null;
  private static String bucketName = getRandomName();
  private static boolean mintEnv = false;
  private static Path dataFile1Mb;
  private static Path dataFile65Mb;
  private static String endpoint;
  private static String accessKey;
  private static String secretKey;
  private static String region;
  private static MinioClient client = null;

  /**
   * Do no-op.
   */
  public static void ignore(Object ...args) {
  }

  /**
   * Create given sized file and returns its name.
   */
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

  /**
   * Create 1 MB temporary file.
   */
  public static String createFile1Mb() throws IOException {
    if (mintEnv) {
      String filename = getRandomName();
      Files.createSymbolicLink(Paths.get(filename).toAbsolutePath(), dataFile1Mb);
      return filename;
    }

    return createFile(1 * MB);
  }

  /**
   * Create 65 MB temporary file.
   */
  public static String createFile65Mb() throws IOException {
    if (mintEnv) {
      String filename = getRandomName();
      Files.createSymbolicLink(Paths.get(filename).toAbsolutePath(), dataFile65Mb);
      return filename;
    }

    return createFile(65 * MB);
  }

  /**
   * Generate random name.
   */
  public static String getRandomName() {
    return "minio-java-test-" + new BigInteger(32, random).toString(32);
  }

  /**
   * Returns byte array contains all data in given InputStream.
   */

  public static byte[] readAllBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];
    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    return buffer.toByteArray();
  }

  /**
   * Prints a success log entry in JSON format.
   */
  public static void mintSuccessLog(String function, String args, long startTime) {
    if (mintEnv) {
      System.out.println(new MintLogger(function, args, System.currentTimeMillis() - startTime,
                                        PASS, null, null, null));
    }
  }

  /**
   * Prints a failure log entry in JSON format.
   */
  public static void mintFailedLog(String function, String args, long startTime, String message, String error) {
    if (mintEnv) {
      System.out.println(new MintLogger(function, args, System.currentTimeMillis() - startTime,
                                        FAILED, null, message, error));
    }
  }

  /**
   * Prints a ignore log entry in JSON format.
   */
  public static void mintIgnoredLog(String function, String args, long startTime) {
    if (mintEnv) {
      System.out.println(new MintLogger(function, args, System.currentTimeMillis() - startTime,
                                        IGNORED, null, null, null));
    }
  }

  /**
   * Read object content of the given url.
   */
  public static byte[] readObject(String urlString) throws Exception {
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("GET", null)
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response == null) {
      throw new Exception("empty response");
    }

    if (!response.isSuccessful()) {
      String errorXml = "";

      // read entire body stream to string.
      Scanner scanner = new Scanner(response.body().charStream());
      scanner.useDelimiter("\\A");
      if (scanner.hasNext()) {
        errorXml = scanner.next();
      }
      scanner.close();
      response.body().close();

      throw new Exception("failed to read object. Response: " + response + ", Response body: " + errorXml);
    }

    return readAllBytes(response.body().byteStream());
  }

  /**
   * Write data to given object url.
   */
  public static void writeObject(String urlString, byte[] dataBytes) throws Exception {
    Request.Builder requestBuilder = new Request.Builder();
    // Set header 'x-amz-acl' to 'bucket-owner-full-control', so objects created
    // anonymously, can be downloaded by bucket owner in AWS S3.
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("PUT", RequestBody.create(null, dataBytes))
        .addHeader("x-amz-acl", "bucket-owner-full-control")
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response == null) {
      throw new Exception("empty response");
    }

    if (!response.isSuccessful()) {
      String errorXml = "";

      // read entire body stream to string.
      Scanner scanner = new Scanner(response.body().charStream());
      scanner.useDelimiter("\\A");
      if (scanner.hasNext()) {
        errorXml = scanner.next();
      }
      scanner.close();
      response.body().close();

      throw new Exception("failed to create object. Response: " + response + ", Response body: " + errorXml);
    }
  }

  /**
   * Test: makeBucket(String bucketName).
   */
  public static void makeBucket_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: makeBucket(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(name);
      client.removeBucket(name);
      mintSuccessLog("makeBucket(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("makeBucket(String bucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: makeBucket(String bucketName, String region).
   */
  public static void makeBucketwithRegion_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: makeBucket(String bucketName, String region)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(name, "eu-west-1");
      client.removeBucket(name);
      mintSuccessLog("makeBucket(String bucketName, String region)", "region: eu-west-1", startTime);
    } catch (Exception e) {
      mintFailedLog("makeBucket(String bucketName, String region)", "region: eu-west-1", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: makeBucket(String bucketName, String region) where bucketName has
   * periods in its name.
   */
  public static void makeBucketWithPeriod_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: makeBucket(String bucketName, String region)");
    }

    long startTime = System.currentTimeMillis();
    String name = getRandomName() + ".withperiod";
    try {
      client.makeBucket(name, "eu-central-1");
      client.removeBucket(name);
      mintSuccessLog("makeBucket(String bucketName, String region)",
                     "name: " + name + ", region: eu-central-1", startTime);
    } catch (Exception e) {
      mintFailedLog("makeBucket(String bucketName, String region)", "name: " + name + ", region: eu-central-1",
                    startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listBuckets().
   */
  public static void listBuckets_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listBuckets()");
    }

    long startTime = System.currentTimeMillis();
    try {
      Date expectedTime = new Date();
      String bucketName = getRandomName();
      boolean found = false;
      client.makeBucket(bucketName);
      for (Bucket bucket : client.listBuckets()) {
        if (bucket.name().equals(bucketName)) {
          if (found) {
            throw new Exception("[FAILED] duplicate entry " +  bucketName + " found in list buckets");
          }

          found = true;
          Date time = bucket.creationDate();
          long diff = time.getTime() - expectedTime.getTime();
          // excuse 15 minutes
          if (diff > (15 * 60 * 1000)) {
            throw new Exception("[FAILED] bucket creation time too apart. expected: " + expectedTime
                                + ", got: " + time);
          }
        }
      }
      client.removeBucket(bucketName);
      if (!found) {
        throw new Exception("[FAILED] created bucket not found in list buckets");
      }
      mintSuccessLog("listBuckets()", null, startTime);
    } catch (Exception e) {
      mintFailedLog("listBuckets()", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: bucketExists(String bucketName).
   */
  public static void bucketExists_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: bucketExists(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(name);
      if (!client.bucketExists(name)) {
        throw new Exception("[FAILED] bucket does not exist");
      }
      client.removeBucket(name);
      mintSuccessLog("bucketExists(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("bucketExists(String bucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: removeBucket(String bucketName).
   */
  public static void removeBucket_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: removeBucket(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String name = getRandomName();
      client.makeBucket(name);
      client.removeBucket(name);
      mintSuccessLog("removeBucket(String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("removeBucket(String bucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Tear down test setup.
   */
  public static void setup() throws Exception {
    client.makeBucket(bucketName);
  }

  /**
   * Tear down test setup.
   */
  public static void teardown() throws Exception {
    client.removeBucket(bucketName);
  }

  /**
   * Test: putObject(String bucketName, String objectName, String filename).
   */
  public static void putObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, String filename)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String filename = createFile1Mb();
      client.putObject(bucketName, filename, filename);
      Files.delete(Paths.get(filename));
      client.removeObject(bucketName, filename);
      mintSuccessLog("putObject(String bucketName, String objectName, String filename)", "filename: 1MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, String filename)", "filename: 1MB", startTime,
                    null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: multipart: putObject(String bucketName, String objectName, String filename).
   */
  public static void putObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: multipart: putObject(String bucketName, String objectName, String filename)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String filename = createFile65Mb();
      client.putObject(bucketName, filename, filename);
      Files.delete(Paths.get(filename));
      client.removeObject(bucketName, filename);
      mintSuccessLog("putObject(String bucketName, String objectName, String filename)", "filename: 65MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, String filename)", "filename: 65MB", startTime,
                    null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: With content-type: putObject(String bucketName, String objectName, String filename, String contentType).
   */
  public static void putObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, String filename,"
                        + " String contentType)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String filename = createFile1Mb();
      client.putObject(bucketName, filename, filename, customContentType);
      Files.delete(Paths.get(filename));
      client.removeObject(bucketName, filename);
      mintSuccessLog("putObject(String bucketName, String objectName, String filename, String contentType)",
                     "contentType: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, String filename, String contentType)",
                    "contentType: " + customContentType, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream body, long size, String contentType).
   */
  public static void putObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream body, "
                        + "long size, String contentType)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * MB)) {
        client.putObject(bucketName, objectName, is, 1 * MB, customContentType);
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream body, long size,"
                     + " String contentType)",
                     "size: 1 MB, objectName: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream body, long size, String contentType)",
                    "size: 1 MB, objectName: " + customContentType, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: multipart resume: putObject(String bucketName, String objectName, InputStream body, long size,
   *                                   String contentType).
   */
  public static void putObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: multipart resume: putObject(String bucketName, String objectName, InputStream body, "
                         + "long size, String contentType)");
    }

    long startTime = System.currentTimeMillis();
    long size = 20 * MB;
    try {
      String objectName = getRandomName();
      try (InputStream is = new ContentInputStream(13 * MB)) {
        client.putObject(bucketName, objectName, is, size, nullContentType);
      } catch (EOFException e) {
        ignore();
      }

      size = 13 * MB;
      try (final InputStream is = new ContentInputStream(size)) {
        client.putObject(bucketName, objectName, is, size, nullContentType);
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream body, long size,"
                      + " String contentType)",
                    "contentType: " + nullContentType + ", size: " + String.valueOf(size), startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream body, long size, String contentType)",
                    "contentType: " + nullContentType + ", size: " + String.valueOf(size), startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream body, long size, String contentType).
   * where objectName has multiple path segments.
   */
  public static void putObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: objectName with path segments: "
                        + "putObject(String bucketName, String objectName, InputStream body, "
                        + "long size, String contentType)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = "/path/to/" + getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 1 * MB, customContentType);
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream body, long size,"
                      + " String contentType)",
                      "size: 1 MB, contentType: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream body, long size,"
                    + " String contentType)",
                    "size: 1 MB, contentType: " + customContentType, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test put object with unknown sized stream.
   */
  public static void testPutObjectUnknownStreamSize(long size) throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream body, "
                        + "String contentType)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(size)) {
        client.putObject(bucketName, objectName, is, customContentType);
      }

      client.removeObject(bucketName, objectName);

      mintSuccessLog("putObject(String bucketName, String objectName, InputStream body, String contentType)",
                    "contentType: " + customContentType, startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream body, long size, String contentType)",
                    "contentType: " + customContentType, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream body, String contentType).
   */
  public static void putObject_test7() throws Exception {
    testPutObjectUnknownStreamSize(3 * MB);
  }

  /**
   * Test: multipart: putObject(String bucketName, String objectName, InputStream body, String contentType).
   */
  public static void putObject_test8() throws Exception {
    testPutObjectUnknownStreamSize(537 * MB);
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 Map&lt;String, String&gt; headerMap).
   */
  public static void putObject_test9() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, Map<String, String> headerMap).");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      try (final InputStream is = new ContentInputStream(13 * MB)) {
        client.putObject(bucketName, objectName, is, 13 * MB, headerMap);
      }

      client.removeObject(bucketName, objectName);

      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, Map<String, String> headerMap)",
                      "size: 13 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, Map<String, String> headerMap)",
                    "size: 13 MB", startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 Map&lt;String, String&gt; headerMap) with Storage Class REDUCED_REDUNDANCY.
   */
  @SuppressFBWarnings("UCF")
  public static void putObject_test10() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, Map<String, String> headerMap). with Storage Class REDUCED_REDUNDANCY set");
    }

    long startTime = System.currentTimeMillis();
    try {
      String storageClass = "REDUCED_REDUNDANCY";
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      headerMap.put("X-Amz-Storage-Class", storageClass);
      try (final InputStream is = new ContentInputStream(13 * MB)) {
        client.putObject(bucketName, objectName, is, 13 * MB, headerMap);
      }

      ObjectStat objectStat = client.statObject(bucketName, objectName);
      Map<String, List<String>> returnHeader = objectStat.httpHeaders();
      List<String> returnStorageClass = returnHeader.get("X-Amz-Storage-Class");

      if ((returnStorageClass != null) && (!storageClass.equals(returnStorageClass.get(0)))) {
        throw new Exception("Metadata mismatch");
      }

      client.removeObject(bucketName, objectName);

      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, Map<String, String> headerMap)",
                      "size: 13 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, Map<String, String> headerMap)",
                    "size: 13 MB", startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                  Map&lt;String, String&gt; headerMap) with Storage Class STANDARD.
   */
  public static void putObject_test11() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, Map<String, String> headerMap). with Storage Class STANDARD set");
    }

    long startTime = System.currentTimeMillis();
    try {
      String storageClass = "STANDARD";
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      headerMap.put("X-Amz-Storage-Class", storageClass);
      try (final InputStream is = new ContentInputStream(13 * MB)) {
        client.putObject(bucketName, objectName, is, 13 * MB, headerMap);
      }

      ObjectStat objectStat = client.statObject(bucketName, objectName);

      Map<String, List<String>> returnHeader = objectStat.httpHeaders();
      List<String> returnStorageClass = returnHeader.get("X-Amz-Storage-Class");

      // Standard storage class shouldn't be present in metadata response
      if (returnStorageClass != null) {
        throw new Exception("Did not expect: " + storageClass + " in response metadata");
      }

      client.removeObject(bucketName, objectName);

      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, Map<String, String> headerMap)",
                      "size: 13 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, Map<String, String> headerMap)",
                    "size: 13 MB", startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 Map&lt;String, String&gt; headerMap). with invalid Storage Class set
   */
  public static void putObject_test12() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
          + "long size, Map<String, String> headerMap). with invalid Storage Class set");
    }

    long startTime = System.currentTimeMillis();
    try {
      String storageClass = "INVALID";
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      headerMap.put("X-Amz-Storage-Class", storageClass);

      try (final InputStream is = new ContentInputStream(13 * MB)) {
        client.putObject(bucketName, objectName, is, 13 * MB, headerMap);
      } catch (ErrorResponseException e) {
        if (!e.errorResponse().code().equals("InvalidStorageClass")) {
          throw e;
        }
      }

      client.removeObject(bucketName, objectName);

      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                   + "long size, Map<String, String> headerMap)",
                  "size: 13 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                   + "long size, Map<String, String> headerMap)",
                "size: 13 MB", startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 ServerSideEncryption sse). To test SSE_C
   */
  public static void putObject_test13() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, ServerSideEncryption sse) using SSE_C. ");
    }
    long startTime = System.currentTimeMillis();
    // Generate a new 256 bit AES key - This key must be remembered by the client.
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());

    
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * MB)) {
        client.putObject(bucketName, objectName, is, 1 * MB, sse);
      }

      client.removeObject(bucketName, objectName);
      
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, ServerSideEncryption sse) using SSE_C.",
                      "size: 1 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, ServerSideEncryption sse) using SSE_C.",
                    "size: 1 MB", startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

/**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 ServerSideEncryption sse). To test SSE_S3
   */
  public static void putObject_test14() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, ServerSideEncryption sse) using SSE_S3.");
    }
    long startTime = System.currentTimeMillis();
    
    ServerSideEncryption sse = ServerSideEncryption.atRest();

    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * MB)) {
        client.putObject(bucketName, objectName, is, 1 * MB, sse);
      }

      client.removeObject(bucketName, objectName);
      
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, ServerSideEncryption sse) using SSE_S3.",
                      "size: 1 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, ServerSideEncryption sse) using SSE_S3.",
                    "size: 1 MB", startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

/**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 ServerSideEncryption sse). To test SSE_KMS
   */
  public static void putObject_test15() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, ServerSideEncryption sse) using SSE_KMS.");
    }
    long startTime = System.currentTimeMillis();
    
    Map<String,String> myContext = new HashMap<>();
    myContext.put("key1","value1");

    String keyId = "";
    keyId = System.getenv("MINT_KEY_ID");
    if (keyId.equals("")) {
      mintIgnoredLog("getBucketPolicy(String bucketName)", null, startTime); 
    }
    ServerSideEncryption sse = ServerSideEncryption.withManagedKeys("keyId", myContext);

    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1 * MB)) {
        client.putObject(bucketName, objectName, is, 1 * MB, sse);
      }

      client.removeObject(bucketName, objectName);
      
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, ServerSideEncryption sse) using SSE_KMS.",
                      "size: 1 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                       + "long size, ServerSideEncryption sse) using SSE_KMS.",
                    "size: 1 MB", startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: statObject(String bucketName, String objectName).
   */
  public static void statObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: statObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      headerMap.put("x-amz-meta-my-custom-data", "foo");
      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, 1, headerMap);
      }

      ObjectStat objectStat = client.statObject(bucketName, objectName);

      if (!(objectName.equals(objectStat.name()) && (objectStat.length() == 1)
            && bucketName.equals(objectStat.bucketName()) && objectStat.contentType().equals(customContentType))) {
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
      mintSuccessLog("statObject(String bucketName, String objectName)",null, startTime);
    } catch (Exception e) {
      mintFailedLog("statObject(String bucketName, String objectName)",null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: statObject(String bucketName, String objectName, ServerSideEncryption sse).
   * To test statObject using SSE_C.
   */
  public static void statObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: statObject(String bucketName, String objectName, ServerSideEncryption sse)"
          + " using SSE_C.");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      // Generate a new 256 bit AES key - This key must be remembered by the client.
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256);
      ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());

      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, 1, sse);
      }
        
      ObjectStat objectStat = client.statObject(bucketName, objectName, sse);
      
      if (!(objectName.equals(objectStat.name()) && (objectStat.length() == 1)
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
        throw new Exception("[FAILED] wrong metadata value. expected: AES256, got: " + values.get(0));
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("statObject(String bucketName, String objectName, ServerSideEncryption sse)"
          + " using SSE_C.",null, startTime);
    } catch (Exception e) {
      mintFailedLog("statObject(String bucketName, String objectName, ServerSideEncryption sse)"
          + " using SSE_C.",null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  } 

  /**
   * Test: getObject(String bucketName, String objectName).
   */
  public static void getObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      client.getObject(bucketName, objectName)
          .close();
      
      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName)",null, startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName)",null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset).
   */
  public static void getObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, long offset)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      client.getObject(bucketName, objectName, 1000L)
          .close();
      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName, long offset)", "offset: 1000", startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName, long offset)", "offset: 1000", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset, Long length).
   */
  public static void getObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, long offset, Long length)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      client.getObject(bucketName, objectName, 1000L, 1024 * 1024L)
          .close();
      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName, long offset, Long length)",
                     "offset: 1000, length: 1 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName, long offset, Long length)",
                    "offset: 1000, length: 1 MB", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, String filename).
   */
  public static void getObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, String filename)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      client.getObject(bucketName, objectName, objectName + ".downloaded");
      Files.delete(Paths.get(objectName + ".downloaded"));
      client.removeObject(bucketName, objectName);

      mintSuccessLog("getObject(String bucketName, String objectName, String filename)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName, String filename)",
                    null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, String filename).
   * where objectName has multiple path segments.
   */
  public static void getObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: objectName with multiple path segments: "
                        + "getObject(String bucketName, String objectName, String filename)");
    }

    long startTime = System.currentTimeMillis();
    String baseObjectName = getRandomName();
    String objectName = "path/to/" + baseObjectName;
    try {
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      client.getObject(bucketName, objectName, baseObjectName + ".downloaded");
      Files.delete(Paths.get(baseObjectName + ".downloaded"));
      client.removeObject(bucketName, objectName);

      mintSuccessLog("getObject(String bucketName, String objectName, String filename)",
                      "objectName: " + objectName, startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName, String filename)",
                    "objectName: " + objectName, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName) zero size object.
   */
  public static void getObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName) zero size object");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(0)) {
        client.putObject(bucketName, objectName, is, 0, nullContentType);
      }

      client.getObject(bucketName, objectName)
          .close();
      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, ServerSideEncryption sse). 
   * To test getObject when object is put using SSE_C.
   */
  public static void getObject_test7() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, ServerSideEncryption sse) using SSE_C");
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
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        
        client.putObject(bucketName, objectName, is, 3 * MB, sse);
        byte [] putbyteArray = new byte[is.available()];
        bytes_read_put = is.read(putbyteArray);
        putString = new String(putbyteArray, StandardCharsets.UTF_8);
      }

      InputStream stream = client.getObject(bucketName, objectName, sse);
      byte [] getbyteArray = new byte[stream.available()];
      int bytes_read_get = stream.read(getbyteArray);
      String getString = new String(getbyteArray, StandardCharsets.UTF_8);
      stream.close();

      //client.getObject(bucketName, objectName, sse)
      //  .close();

      // Compare if contents received are same as the initial uploaded object.
      if ((!putString.equals(getString)) || (bytes_read_put != bytes_read_get) ) {
        throw new Exception("Contents received from getObject doesn't match initial contents.");
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName, ServerSideEncryption sse)"
          + " using SSE_C.",null, startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName, ServerSideEncryption sse)"
          + " using SSE_C.",null, startTime, null,
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
          "Test: getObject(String bucketName, String objectName, long offset, Long length) with offset=0"
      );
    }

    final long startTime = System.currentTimeMillis();
    final int fullLength = 1024;
    final int partialLength = 256;
    final long offset = 0L;
    final String objectName = getRandomName();
    try {
      try (final InputStream is = new ContentInputStream(fullLength)) {
        client.putObject(bucketName, objectName, is, fullLength, nullContentType);
      }

      try (
          final InputStream partialObjectStream = client.getObject(
              bucketName,
              objectName,
              offset,
              Long.valueOf(partialLength)
          )
      ) {
        byte[] result = new byte[fullLength];
        final int read = partialObjectStream.read(result);
        result = Arrays.copyOf(result, read);
        if (result.length != partialLength) {
          throw new Exception(
              String.format(
                  "Expecting only the first %d bytes from partial getObject request; received %d bytes instead.",
                  partialLength,
                  read
              )
          );
        }
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog(
          "getObject(String bucketName, String objectName, long offset, Long length) with offset=0",
          String.format("offset: %d, length: %d bytes", offset, partialLength),
          startTime
      );
    } catch (final Exception e) {
      mintFailedLog(
          "getObject(String bucketName, String objectName, long offset, Long length) with offset=0",
          String.format("offset: %d, length: %d bytes", offset, partialLength),
          startTime,
          null,
          e.toString() + " >>> " + Arrays.toString(e.getStackTrace())
      );
      throw e;
    }
  }

  /**
   * Test: listObjects(final String bucketName).
   */
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
          client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        }
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObject(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog("listObjects(final String bucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("listObjects(final String bucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix).
   */
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
          client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        }
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minio")) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObject(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog("listObjects(final String bucketName, final String prefix)", "prefix :minio", startTime);
    } catch (Exception e) {
      mintFailedLog("listObjects(final String bucketName, final String prefix)", "prefix :minio", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix, final boolean recursive).
   */
  public static void listObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listObjects(final String bucketName, final String prefix, final boolean recursive)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[3];
      int i = 0;
      for (i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        }
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minio", true)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObject(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog("listObjects(final String bucketName, final String prefix, final boolean recursive)",
                    "prefix :minio, recursive: true", startTime);
    } catch (Exception e) {
      mintFailedLog("listObjects(final String bucketName, final String prefix, final boolean recursive)",
                    "prefix :minio, recursive: true", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listObjects(final string bucketName).
   */
  public static void listObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: empty bucket: listObjects(final String bucketName, final String prefix,"
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
      mintSuccessLog("listObjects(final String bucketName, final String prefix, final boolean recursive)",
                      "prefix :minioemptybucket, recursive: true", startTime);
    } catch (Exception e) {
      mintFailedLog("listObjects(final String bucketName, final String prefix, final boolean recursive)",
                    "prefix :minioemptybucket, recursive: true", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: recursive: listObjects(bucketName, final String prefix, final boolean recursive).
   */
  public static void listObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: recursive: listObjects(final String bucketName, final String prefix, "
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
          client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
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

      for (Result<?> r : client.removeObject(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog("listObjects(final String bucketName, final String prefix, final boolean recursive)",
                     "prefix :minio, recursive: true", startTime);
    } catch (Exception e) {
      mintFailedLog("listObjects(final String bucketName, final String prefix, final boolean recursive)",
                    "prefix :minio, recursive: true", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix, final boolean recursive, final boolean useVersion1).
   */
  public static void listObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listObjects(final String bucketName, final String prefix, final boolean recursive, "
                        + "final boolean useVersion1)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[3];
      int i = 0;
      for (i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        }
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minio", true, true)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (Result<?> r : client.removeObject(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }

      mintSuccessLog("listObjects(final String bucketName, final String prefix, "
                      + "final boolean recursive, final boolean useVersion1)",
                      "prefix :minio, recursive: true, useVersion1: true", startTime);
    } catch (Exception e) {
      mintFailedLog("listObjects(final String bucketName, final String prefix, "
                    + "final boolean recursive, final boolean useVersion1)",
                    "prefix :minio, recursive: true, useVersion1: true", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: removeObject(String bucketName, String objectName).
   */
  public static void removeObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: removeObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, 1, nullContentType);
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("removeObject(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("removeObject(String bucketName, String objectName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: removeObject(final String bucketName, final Iterable&lt;String&gt; objectNames).
   */
  public static void removeObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: removeObject(final String bucketName, final Iterable<String> objectNames)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String[] objectNames = new String[4];
      for (int i = 0; i < 3; i++) {
        objectNames[i] = getRandomName();
        try (final InputStream is = new ContentInputStream(1)) {
          client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        }
      }
      objectNames[3] = "nonexistent-object";

      for (Result<?> r : client.removeObject(bucketName, Arrays.asList(objectNames))) {
        ignore(r.get());
      }
      mintSuccessLog("removeObject(final String bucketName, final Iterable<String> objectNames)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("removeObject(final String bucketName, final Iterable<String> objectNames)",
                    null, startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listIncompleteUploads(String bucketName).
   */
  public static void listIncompleteUploads_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listIncompleteUploads(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
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
      mintFailedLog("listIncompleteUploads(String bucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listIncompleteUploads(String bucketName, String prefix).
   */
  public static void listIncompleteUploads_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listIncompleteUploads(String bucketName, String prefix)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
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
      mintFailedLog("listIncompleteUploads(String bucketName, String prefix)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive).
   */
  public static void listIncompleteUploads_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: listIncompleteUploads(final String bucketName, final String prefix, "
                        + "final boolean recursive)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
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
      mintSuccessLog("listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive)",
                      "prefix: minio, recursive: true", startTime);
    } catch (Exception e) {
      mintFailedLog("listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive)",
                    "prefix: minio, recursive: true", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: removeIncompleteUpload(String bucketName, String objectName).
   */
  public static void removeIncompleteUploads_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: removeIncompleteUpload(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(6 * MB)) {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
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
      mintSuccessLog("removeIncompleteUpload(String bucketName, String objectName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("removeIncompleteUpload(String bucketName, String objectName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * public String presignedGetObject(String bucketName, String objectName).
   */
  public static void presignedGetObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedGetObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      byte[] inBytes;
      try (final InputStream is = new ContentInputStream(3 * MB)) {
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
      mintFailedLog("presignedGetObject(String bucketName, String objectName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: presignedGetObject(String bucketName, String objectName, Integer expires).
   */
  public static void presignedGetObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedGetObject(String bucketName, String objectName, Integer expires)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      byte[] inBytes;
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        inBytes = readAllBytes(is);
      }

      String urlString = client.presignedGetObject(bucketName, objectName, 3600);
      byte[] outBytes = readObject(urlString);
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog("presignedGetObject(String bucketName, String objectName, Integer expires)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("presignedGetObject(String bucketName, String objectName, Integer expires)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * public String presignedGetObject(String bucketName, String objectName, Integer expires, Map reqParams).
   */
  public static void presignedGetObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedGetObject(String bucketName, String objectName, Integer expires, "
                        + "Map<String, String> reqParams)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      byte[] inBytes;
      try (final InputStream is = new ContentInputStream(3 * MB)) {
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
      mintSuccessLog("presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,"
                     + " String> reqParams)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,"
                    + " String> reqParams)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * public String presignedPutObject(String bucketName, String objectName).
   */
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
      mintFailedLog("presignedPutObject(String bucketName, String objectName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: presignedPutObject(String bucketName, String objectName, Integer expires).
   */
  public static void presignedPutObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedPutObject(String bucketName, String objectName, Integer expires)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      String urlString = client.presignedPutObject(bucketName, objectName, 3600);
      byte[] data = "hello, world".getBytes(StandardCharsets.UTF_8);
      writeObject(urlString, data);
      client.removeObject(bucketName, objectName);
      mintSuccessLog("presignedPutObject(String bucketName, String objectName, Integer expires)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("presignedPutObject(String bucketName, String objectName, Integer expires)", null, startTime,
                    null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: presignedPostPolicy(PostPolicy policy).
   */
  public static void presignedPostPolicy_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: presignedPostPolicy(PostPolicy policy)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      PostPolicy policy = new PostPolicy(bucketName, objectName, DateTime.now().plusDays(7));
      policy.setContentRange(1 * MB, 4 * MB);
      Map<String, String> formData = client.presignedPostPolicy(policy);

      MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
      multipartBuilder.setType(MultipartBody.FORM);
      for (Map.Entry<String, String> entry : formData.entrySet()) {
        multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
      }
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        multipartBuilder.addFormDataPart("file", objectName, RequestBody.create(null, readAllBytes(is)));
      }

      Request.Builder requestBuilder = new Request.Builder();
      String urlString = client.getObjectUrl(bucketName, "");
      Request request = requestBuilder.url(urlString).post(multipartBuilder.build()).build();
      OkHttpClient transport = new OkHttpClient();
      Response response = transport.newCall(request).execute();
      if (response == null) {
        throw new Exception("no response from server");
      }

      if (!response.isSuccessful()) {
        String errorXml = "";
        // read entire body stream to string.
        Scanner scanner = new Scanner(response.body().charStream());
        scanner.useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }
        scanner.close();
        throw new Exception("failed to upload object. Response: " + response + ", Error: " + errorXml);
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("presignedPostPolicy(PostPolicy policy)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("presignedPostPolicy(PostPolicy policy)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: PutObject(): do put object using multi-threaded way in parallel.
   */
  public static void threadedPutObject() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: threadedPutObject");
    }

    long startTime = System.currentTimeMillis();
    try {
      Thread[] threads = new Thread[7];

      for (int i = 0; i < 7; i++) {
        threads[i] = new Thread(new PutObjectRunnable(client, bucketName, createFile65Mb()));
      }

      for (int i = 0; i < 7; i++) {
        threads[i].start();
      }

      // Waiting for threads to complete.
      for (int i = 0; i < 7; i++) {
        threads[i].join();
      }

      // All threads are completed.
      mintSuccessLog("putObject(String bucketName, String objectName, String filename)",
                    "filename: threaded65MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, String filename)",
                    "filename: threaded65MB", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName).
   */
  public static void copyObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);
      client.copyObject(bucketName, objectName, destBucketName);
      client.getObject(destBucketName, objectName)
          .close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to match.
   */
  public static void copyObject_test2() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions) with Matching ETag (Negative Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      CopyConditions invalidETag = new CopyConditions();
      invalidETag.setMatchETag("TestETag");

      try {
        client.copyObject(bucketName, objectName, destBucketName, invalidETag);
      } catch (ErrorResponseException e) {
        if (!e.errorResponse().code().equals("PreconditionFailed")) {
          throw e;
        }
      }

      client.removeObject(bucketName, objectName);
      client.removeBucket(destBucketName);

      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName,"
                     + " CopyConditions copyConditions)", "CopyConditions: invalidETag",startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName, "
                    + "CopyConditions copyConditions)",
                    "CopyConditions: invalidETag", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to match.
   */
  public static void copyObject_test3() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions) with Matching ETag (Positive Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      ObjectStat stat = client.statObject(bucketName, objectName);
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setMatchETag(stat.etag());

      // File should be copied as ETag set in copyConditions matches object's ETag.
      client.copyObject(bucketName, objectName, destBucketName, copyConditions);
      client.getObject(destBucketName, objectName)
          .close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName,"
                      + " CopyConditions copyConditions)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName,"
                    + " CopyConditions copyConditions)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to not match.
   */
  public static void copyObject_test4() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions) with not matching ETag"
                        + " (Positive Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setMatchETagNone("TestETag");

      // File should be copied as ETag set in copyConditions doesn't match object's ETag.
      client.copyObject(bucketName, objectName, destBucketName, copyConditions);
      client.getObject(destBucketName, objectName)
          .close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);

      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName,"
                    + " CopyConditions copyConditions)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName,"
                    + "CopyConditions copyConditions)",
                    null, startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to not match.
   */
  public static void copyObject_test5() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions) with not matching ETag"
                        + " (Negative Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      ObjectStat stat = client.statObject(bucketName, objectName);
      CopyConditions matchingETagNone = new CopyConditions();
      matchingETagNone.setMatchETagNone(stat.etag());

      try {
        client.copyObject(bucketName, objectName, destBucketName, matchingETagNone);
      } catch (ErrorResponseException e) {
        // File should not be copied as ETag set in copyConditions matches object's ETag.
        if (!e.errorResponse().code().equals("PreconditionFailed")) {
          throw e;
        }
      }

      client.removeObject(bucketName, objectName);
      client.removeBucket(destBucketName);

      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName, "
                     + "CopyConditions copyConditions)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName, "
                    + "CopyConditions copyConditions)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with object modified after condition.
   */
  public static void copyObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions) with modified after "
                        + "condition (Positive Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      CopyConditions modifiedDateCondition = new CopyConditions();
      DateTime dateRepresentation = new DateTime(2015, Calendar.MAY, 3, 10, 10);

      modifiedDateCondition.setModified(dateRepresentation);

      // File should be copied as object was modified after the set date.
      client.copyObject(bucketName, objectName, destBucketName, modifiedDateCondition);
      client.getObject(destBucketName, objectName)
          .close();

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName, "
                    + "CopyConditions copyConditions)",
                    "CopyCondition: modifiedDateCondition", startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName, "
                    + "CopyConditions copyConditions)",
                    "CopyCondition: modifiedDateCondition", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with object modified after condition.
   */
  public static void copyObject_test7() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions) with modified after"
                        + " condition (Negative Case)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      CopyConditions invalidUnmodifiedCondition = new CopyConditions();
      DateTime dateRepresentation = new DateTime(2015, Calendar.MAY, 3, 10, 10);

      invalidUnmodifiedCondition.setUnmodified(dateRepresentation);

      try {
        client.copyObject(bucketName, objectName, destBucketName, invalidUnmodifiedCondition);
      } catch (ErrorResponseException e) {
        // File should not be copied as object was modified after date set in copyConditions.
        if (!e.errorResponse().code().equals("PreconditionFailed")) {
          throw e;
        }
      }

      client.removeObject(bucketName, objectName);
      // Destination bucket is expected to be empty, otherwise it will trigger an exception.
      client.removeBucket(destBucketName);
      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName, "
                     + "CopyConditions copyConditions)",
                     "CopyCondition: invalidUnmodifiedCondition", startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName, "
                    + "CopyConditions copyConditions)",
                    "CopyCondition: invalidUnmodifiedCondition",  startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

   /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions, Map metadata) replace
   * object metadata.
   */
  public static void copyObject_test8() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions, Map<String, String> metadata)"
                        + " replace object metadata");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      try (final InputStream is = new ContentInputStream(3 * MB)) {
        client.putObject(bucketName, objectName, is, 3 * MB, "application/octet-stream");
      }

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();

      Map<String, String> metadata = new HashMap<>();
      metadata.put("Content-Type", customContentType);

      client.copyObject(bucketName, objectName, destBucketName, objectName, copyConditions, metadata);

      ObjectStat objectStat = client.statObject(destBucketName, objectName);
      if (!customContentType.equals(objectStat.contentType())) {
        throw new Exception("content type differs. expected: " + customContentType + ", got: "
                            + objectStat.contentType());
      }

      client.removeObject(bucketName, objectName);
      client.removeObject(destBucketName, objectName);
      client.removeBucket(destBucketName);
      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName, "
                     + "CopyConditions copyConditions, Map<String, String> metadata)",
                     null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName, "
                    + "CopyConditions copyConditions, Map<String, String> metadata)",
                    null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

   /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions, Map metadata) remove
   * object metadata.
   */
  public static void copyObject_test9() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                        + "CopyConditions copyConditions, Map<String, String> metadata)"
                        + " remove object metadata");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("X-Amz-Meta-Test", "testValue");

      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, 1, headerMap);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();

      client.copyObject(bucketName, objectName, bucketName,
          objectName, copyConditions, new HashMap<String,String>());
      ObjectStat objectStat = client.statObject(bucketName, objectName);
      if (objectStat.httpHeaders().containsKey("X-Amz-Meta-Test")) {
        throw new Exception("expected user-defined metadata has been removed");
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("copyObject(String bucketName, String objectName, String destBucketName, "
                     + "CopyConditions copyConditions, Map<String, String> metadata)",
                     null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, String destBucketName, "
                    + "CopyConditions copyConditions, Map<String, String> metadata)",
                    null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String destBucketName,
   * CopyConditions copyConditions, ServerSideEncryption sseTarget) 
   * To test using SSE_C.
   */
  public static void copyObject_test10() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
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
      ServerSideEncryption sseSource = ServerSideEncryption.copyWithCustomerKey(secretKeySpec);
        
      byte[] keyTarget = "98765432100123456789012345678901".getBytes(StandardCharsets.UTF_8); 
      SecretKeySpec secretKeySpecTarget = new SecretKeySpec(keyTarget, "AES");

      ServerSideEncryption sseTarget = ServerSideEncryption.withCustomerKey(secretKeySpecTarget);

      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, 1, ssePut);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();
        
   
      client.copyObject(bucketName, objectName, sseSource, bucketName,
          objectName, copyConditions, sseTarget);
      ObjectStat objectStat = client.statObject(bucketName, objectName, sseTarget);

      client.removeObject(bucketName, objectName);
      mintSuccessLog("copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                     + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                     + " using SSE_C.",
                     null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                    + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                    + " using SSE_C.",
                    null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String destBucketName,
   * CopyConditions copyConditions, ServerSideEncryption sseTarget) 
   * To test using SSE_S3.
   */
  public static void copyObject_test11() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                        + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                        + " using SSE_S3. ");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      
      ServerSideEncryption sse = ServerSideEncryption.atRest();

      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, 1, sse);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();
        
   
      client.copyObject(bucketName, objectName, null, bucketName,
          objectName, copyConditions, sse);
      ObjectStat objectStat = client.statObject(bucketName, objectName);
      if (objectStat.httpHeaders().containsKey("X-Amz-Meta-Test")) {
        throw new Exception("expected user-defined metadata has been removed");
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                     + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                     + " using SSE_S3.",
                     null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                    + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                    + " using SSE_S3.",
                    null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

/**
   * Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String destBucketName,
   * CopyConditions copyConditions, ServerSideEncryption sseTarget) 
   * To test using SSE_KMS.
   */
  public static void copyObject_test12() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                        + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                        + " using SSE_KMS. ");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      
      Map<String,String> myContext = new HashMap<>();
      myContext.put("key1","value1");

      String keyId = "";
      keyId = System.getenv("MINT_KEY_ID");
      if (keyId.equals("")) {
        mintIgnoredLog("getBucketPolicy(String bucketName)", null, startTime); 
      }
      ServerSideEncryption sse = ServerSideEncryption.withManagedKeys("keyId", myContext);

      try (final InputStream is = new ContentInputStream(1)) {
        client.putObject(bucketName, objectName, is, 1, sse);
      }

      // Attempt to remove the user-defined metadata from the object
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setReplaceMetadataDirective();
        
      client.copyObject(bucketName, objectName, null, bucketName,
          objectName, copyConditions, sse);
      ObjectStat objectStat = client.statObject(bucketName, objectName);
      if (objectStat.httpHeaders().containsKey("X-Amz-Meta-Test")) {
        throw new Exception("expected user-defined metadata has been removed");
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                     + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                     + " using SSE_KMS.",
                     null, startTime);
    } catch (Exception e) {
      mintFailedLog("copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, "
                    + "String destBucketName, CopyConditions copyConditions, ServerSideEncryption sseTarget)"
                    + " using SSE_KMS.",
                    null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getBucketPolicy(String bucketName).
   */
  public static void getBucketPolicy_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getBucketPolicy(String bucketName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Action\":[\"s3:GetObject\"],\"Effect\":\"Allow\","
          + "\"Principal\":{\"AWS\":[\"*\"]},\"Resource\":[\"arn:aws:s3:::" + bucketName
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
        mintFailedLog("getBucketPolicy(String bucketName)", null, startTime,
                      null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /**
   * Test: setBucketPolicy(String bucketName, String policy).
   */
  public static void setBucketPolicy_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: setBucketPolicy(String bucketName, String policy)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String policy = "{\"Statement\":[{\"Action\":\"s3:GetObject\",\"Effect\":\"Allow\",\"Principal\":"
          + "\"*\",\"Resource\":\"arn:aws:s3:::" + bucketName + "/myobject*\"}],\"Version\": \"2012-10-17\"}";
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
        mintIgnoredLog("setBucketPolicy(String bucketName, String objectPrefix, "
                       + "PolicyType policyType)", null, startTime);
      } else {
        mintFailedLog("setBucketPolicy(String bucketName, String objectPrefix, "
                      + "PolicyType policyType)", null, startTime, null,
                      e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
        throw e;
      }
    }
  }

  /**
   * Test: setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration).
   */
  public static void setBucketNotification_test1() throws Exception {
    // This test requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION' environment variables.
    String topic = System.getenv("MINIO_JAVA_TEST_TOPIC");
    String region = System.getenv("MINIO_JAVA_TEST_REGION");
    if (topic == null || topic.equals("") || region == null || region.equals("")) {
      // do not run functional test as required environment variables are missing.
      return;
    }

    if (!mintEnv) {
      System.out.println("Test: setBucketNotification(String bucketName, "
                        + "NotificationConfiguration notificationConfiguration)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String destBucketName = getRandomName();
      client.makeBucket(destBucketName, region);

      NotificationConfiguration notificationConfiguration = new NotificationConfiguration();

      // Add a new topic configuration.
      List<TopicConfiguration> topicConfigurationList = notificationConfiguration.topicConfigurationList();
      TopicConfiguration topicConfiguration = new TopicConfiguration();
      topicConfiguration.setTopic(topic);

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      eventList.add(EventType.OBJECT_CREATED_COPY);
      topicConfiguration.setEvents(eventList);

      Filter filter = new Filter();
      filter.setPrefixRule("images");
      filter.setSuffixRule("pg");
      topicConfiguration.setFilter(filter);

      topicConfigurationList.add(topicConfiguration);
      notificationConfiguration.setTopicConfigurationList(topicConfigurationList);

      client.setBucketNotification(destBucketName, notificationConfiguration);

      client.removeBucket(destBucketName);
      mintSuccessLog("setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)",
                      null, startTime);
    } catch (Exception e) {
      mintFailedLog("setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)",
                    null, startTime, null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getBucketNotification(String bucketName).
   */
  public static void getBucketNotification_test1() throws Exception {
    // This test requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION' environment variables.
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
      client.makeBucket(destBucketName, region);

      NotificationConfiguration notificationConfiguration = new NotificationConfiguration();

      // Add a new topic configuration.
      List<TopicConfiguration> topicConfigurationList = notificationConfiguration.topicConfigurationList();
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
      mintFailedLog("getBucketNotification(String bucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }


  /**
   * Test: removeAllBucketNotification(String bucketName).
   */
  public static void removeAllBucketNotification_test1() throws Exception {
    // This test requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION' environment variables.
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
      client.makeBucket(destBucketName, region);

      NotificationConfiguration notificationConfiguration = new NotificationConfiguration();

      // Add a new topic configuration.
      List<TopicConfiguration> topicConfigurationList = notificationConfiguration.topicConfigurationList();
      TopicConfiguration topicConfiguration = new TopicConfiguration();
      topicConfiguration.setTopic(topic);

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      eventList.add(EventType.OBJECT_CREATED_COPY);
      topicConfiguration.setEvents(eventList);

      Filter filter = new Filter();
      filter.setPrefixRule("images");
      filter.setSuffixRule("pg");
      topicConfiguration.setFilter(filter);

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
      mintFailedLog("removeAllBucketNotification(String bucketName)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * runTests: runs as much as possible of test combinations.
   */
  public static void runTests() throws Exception {
    makeBucket_test1();
    if (endpoint.toLowerCase(Locale.US).contains("s3")) {
      makeBucketwithRegion_test();
      makeBucketWithPeriod_test();
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
    putObject_test12();
    
    statObject_test1();

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

    // SSE_C tests will only work over TLS connection
    Locale locale = Locale.ENGLISH;
    boolean tlsEnabled = endpoint.toLowerCase(locale).contains("https://");
    if (tlsEnabled) {
      statObject_test2();
      getObject_test7();
      putObject_test13();
      copyObject_test10();
    }

    // SSE_S3 and SSE_KMS only work with endpoint="s3.amazonaws.com"
    String requestUrl = endpoint;
    if (requestUrl.equals("s3.amazonaws.com")) {
      putObject_test14();
      putObject_test15();
      copyObject_test11();
      copyObject_test12();
    }
    
    getBucketPolicy_test1();
    setBucketPolicy_test1();

    threadedPutObject();

    teardown();

    // notification tests requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION' environment variables
    // to be set appropriately.
    setBucketNotification_test1();
    getBucketNotification_test1();
    removeAllBucketNotification_test1();
  }

  /**
   * runQuickTests: runs tests those completely quicker.
   */
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

    teardown();
  }


  /**
   * main().
   */
  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY> <REGION>");
      System.exit(-1);
    }

    String dataDir = System.getenv("MINT_DATA_DIR");
    if (dataDir != null && !dataDir.equals("")) {
      mintEnv = true;
      dataFile1Mb = Paths.get(dataDir, "datafile-1-MB");
      dataFile65Mb = Paths.get(dataDir, "datafile-65-MB");
    }

    String mintMode = null;
    if (mintEnv) {
      mintMode = System.getenv("MINT_MODE");
    }

    endpoint = args[0];
    accessKey = args[1];
    secretKey = args[2];
    region = args[3];

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
      System.exit(-1);
    }
  }
}
