/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015, 2016, 2017 Minio, Inc.
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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.io.*;

import static java.nio.file.StandardOpenOption.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

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
import io.minio.policy.*;


public class FunctionalTest {
  private static final String PASS = "PASS";
  private static final String FAILED = "FAIL";
  private static final String NA = "NA";
  private static final int MB = 1024 * 1024;
  private static final Random random = new Random(new SecureRandom().nextLong());
  private static final String bucketName = getRandomName();
  private static final String customContentType = "application/javascript";
  private static final String nullContentType = null;
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

    OutputStream os = Files.newOutputStream(Paths.get(filename), CREATE, APPEND);
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
    os.close();

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
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("PUT", RequestBody.create(null, dataBytes))
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
      for (Bucket bucket : client.listBuckets()) {
        ignore(bucket);
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
      ObjectStat objectStat = client.statObject(bucketName, filename);
      if (!customContentType.equals(objectStat.contentType())) {
        throw new Exception("content type mismatch, expected: " + customContentType + ", got: "
                            + objectStat.contentType());
      }
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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 1 * MB, customContentType);
      is.close();
      ObjectStat objectStat = client.statObject(bucketName, objectName);
      if (!customContentType.equals(objectStat.contentType())) {
        throw new Exception("content type mismatch, expected: " + customContentType + ", got: "
                            + objectStat.contentType());
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
      InputStream is = new ContentInputStream(13 * MB);
      try {
        client.putObject(bucketName, objectName, is, size, nullContentType);
      } catch (EOFException e) {
        ignore();
      }
      is.close();

      size = 13 * MB;
      is = new ContentInputStream(size);
      client.putObject(bucketName, objectName, is, size, nullContentType);
      is.close();
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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 1 * MB, customContentType);
      is.close();
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
      InputStream is = new ContentInputStream(size);
      client.putObject(bucketName, objectName, is, customContentType);
      is.close();
      ObjectStat objectStat = client.statObject(bucketName, objectName);
      if (!customContentType.equals(objectStat.contentType())) {
        throw new Exception("content type mismatch, expected: " + customContentType + ", got: "
                            + objectStat.contentType());
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
   *                 String contentType, SecretKey key).
   */
  public static void putObject_test9() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, String contentType, SecretKey key)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(128);
      SecretKey secretKey = keyGenerator.generateKey();

      InputStream is = new ContentInputStream(13 * MB);
      client.putObject(bucketName, objectName, is, 13 * MB, null, secretKey);
      is.close();

      client.removeObject(bucketName, objectName);
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                      + "long size, String contentType, SecretKey key)",
                      "size: 13 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                    + "long size, String contentType, SecretKey key)",
                    "size: 13 MB", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 String contentType, KeyPair keyPair).
   */
  public static void putObject_test10() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, String contentType, KeyPair keyPair).");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
      keyGenerator.initialize(1024, new SecureRandom());
      KeyPair keyPair = keyGenerator.generateKeyPair();

      InputStream is = new ContentInputStream(13 * MB);
      client.putObject(bucketName, objectName, is, 13 * MB, null, keyPair);
      is.close();

      client.removeObject(bucketName, objectName);
      mintSuccessLog("putObject(String bucketName, String objectName, InputStream stream, "
                      + "long size, String contentType, KeyPair keyPair)", 
                      "size: 13 MB", startTime);
    } catch (Exception e) {
      mintFailedLog("putObject(String bucketName, String objectName, InputStream stream, "
                    + "long size, String contentType, KeyPair keyPair)",
                    "size: 13 MB", startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: putObject(String bucketName, String objectName, InputStream stream, long size,
   *                 Map&lt;String, String&gt; headerMap).
   */
  public static void putObject_test11() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: putObject(String bucketName, String objectName, InputStream stream, "
                        + "long size, Map<String, String> headerMap).");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", customContentType);
      InputStream is = new ContentInputStream(13 * MB);
      client.putObject(bucketName, objectName, is, 13 * MB, headerMap);
      is.close();

      ObjectStat objectStat = client.statObject(bucketName, objectName);
      if (!customContentType.equals(objectStat.contentType())) {
        throw new Exception("content type mismatch, expected: " + customContentType + ", got: "
                            + objectStat.contentType());
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
   * Test: statObject(String bucketName, String objectName).
   */
  public static void statObject_test() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: statObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      InputStream is = new ContentInputStream(1);
      client.putObject(bucketName, objectName, is, 1, customContentType);
      is.close();

      ObjectStat objectStat = client.statObject(bucketName, objectName);

      if (!(objectName.equals(objectStat.name()) && (objectStat.length() == 1)
            && bucketName.equals(objectStat.bucketName()) && objectStat.contentType().equals(customContentType))) {
        throw new Exception("object stat differs");
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
   * Test: getObject(String bucketName, String objectName).
   */
  public static void getObject_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      is = client.getObject(bucketName, objectName);
      is.close();
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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      is = client.getObject(bucketName, objectName, 1000L);
      is.close();
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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      is = client.getObject(bucketName, objectName, 1000L, 1024 * 1024L);
      is.close();
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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

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
   * Test: getObject(String bucketName, String objectName, SecretKey key).
   */
  public static void getObject_test6() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, SecretKey key).");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(128);
      SecretKey secretKey = keyGenerator.generateKey();
      InputStream is = new ContentInputStream(13 * MB);
      client.putObject(bucketName, objectName, is, 13 * MB, null, secretKey);
      is.close();

      is = new ContentInputStream(13 * MB);
      byte[] inBytes = readAllBytes(is);
      is.close();

      is = client.getObject(bucketName, objectName, secretKey);
      byte[] outBytes = readAllBytes(is);
      is.close();
      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }
      client.removeObject(bucketName, objectName);
      mintSuccessLog("getObject(String bucketName, String objectName, SecretKey key)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName, SecretKey key)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: getObject(String bucketName, String objectName, KeyPair keyPair).
   */
  public static void getObject_test7() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: getObject(String bucketName, String objectName, KeyPair keyPair).");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectName = getRandomName();
      KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
      keyGenerator.initialize(1024, new SecureRandom());
      KeyPair keyPair = keyGenerator.generateKeyPair();
      InputStream is = new ContentInputStream(13 * MB);
      client.putObject(bucketName, objectName, is, 13 * MB, null, keyPair);
      is.close();

      is = new ContentInputStream(13 * MB);
      byte[] inBytes = readAllBytes(is);
      is.close();

      is = client.getObject(bucketName, objectName, keyPair);
      byte[] outBytes = readAllBytes(is);
      is.close();

      if (!Arrays.equals(inBytes, outBytes)) {
        throw new Exception("object content differs");
      }
      client.removeObject(bucketName, objectName);

      mintSuccessLog("getObject(String bucketName, String objectName, KeyPair keyPair)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("getObject(String bucketName, String objectName, KeyPair keyPair)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
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
        InputStream is = new ContentInputStream(1);
        client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        is.close();
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (i = 0; i < 3; i++) {
        client.removeObject(bucketName, objectNames[i]);
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
        InputStream is = new ContentInputStream(1);
        client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        is.close();
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minio")) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (i = 0; i < 3; i++) {
        client.removeObject(bucketName, objectNames[i]);
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
        InputStream is = new ContentInputStream(1);
        client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        is.close();
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minio", true)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (i = 0; i < 3; i++) {
        client.removeObject(bucketName, objectNames[i]);
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
        InputStream is = new ContentInputStream(1);
        client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        is.close();
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minio", true)) {
        ignore(i++, r.get());
      }
      
      // Check the number of uploaded objects
      if (i != objCount) {
        throw new Exception("item count differs, expected: " + objCount + ", got: " + i);
      }

      for (i = 0; i < objCount; i++) {
        client.removeObject(bucketName, objectNames[i]);
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
        InputStream is = new ContentInputStream(1);
        client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        is.close();
      }

      i = 0;
      for (Result<?> r : client.listObjects(bucketName, "minio", true, true)) {
        ignore(i++, r.get());
        if (i == 3) {
          break;
        }
      }

      for (i = 0; i < 3; i++) {
        client.removeObject(bucketName, objectNames[i]);
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
      InputStream is = new ContentInputStream(1);
      client.putObject(bucketName, objectName, is, 1, nullContentType);
      is.close();

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
        InputStream is = new ContentInputStream(1);
        client.putObject(bucketName, objectNames[i], is, 1, nullContentType);
        is.close();
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
      InputStream is = new ContentInputStream(6 * MB);
      try {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
        ignore();
      }
      is.close();

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
      InputStream is = new ContentInputStream(6 * MB);
      try {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
        ignore();
      }
      is.close();

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
      InputStream is = new ContentInputStream(6 * MB);
      try {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
        ignore();
      }
      is.close();

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
      InputStream is = new ContentInputStream(6 * MB);
      try {
        client.putObject(bucketName, objectName, is, 9 * MB, nullContentType);
      } catch (EOFException e) {
        ignore();
      }
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      is = new ContentInputStream(3 * MB);
      byte[] inBytes = readAllBytes(is);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      is = new ContentInputStream(3 * MB);
      byte[] inBytes = readAllBytes(is);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      is = new ContentInputStream(3 * MB);
      byte[] inBytes = readAllBytes(is);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      multipartBuilder.addFormDataPart("file", objectName, RequestBody.create(null, readAllBytes(is)));
      is.close();

      Request.Builder requestBuilder = new Request.Builder();
      String urlString = client.getObjectUrl(bucketName, objectName);
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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);
      client.copyObject(bucketName, objectName, destBucketName);
      is = client.getObject(destBucketName, objectName);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      ObjectStat stat = client.statObject(bucketName, objectName);
      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setMatchETag(stat.etag());

      // File should be copied as ETag set in copyConditions matches object's ETag.
      client.copyObject(bucketName, objectName, destBucketName, copyConditions);
      is = client.getObject(destBucketName, objectName);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      CopyConditions copyConditions = new CopyConditions();
      copyConditions.setMatchETagNone("TestETag");

      // File should be copied as ETag set in copyConditions doesn't match object's ETag.
      client.copyObject(bucketName, objectName, destBucketName, copyConditions);
      is = client.getObject(destBucketName, objectName);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

      String destBucketName = getRandomName();
      client.makeBucket(destBucketName);

      CopyConditions modifiedDateCondition = new CopyConditions();
      DateTime dateRepresentation = new DateTime(2015, Calendar.MAY, 3, 10, 10);

      modifiedDateCondition.setModified(dateRepresentation);

      // File should be copied as object was modified after the set date.
      client.copyObject(bucketName, objectName, destBucketName, modifiedDateCondition);
      is = client.getObject(destBucketName, objectName);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, nullContentType);
      is.close();

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
      InputStream is = new ContentInputStream(3 * MB);
      client.putObject(bucketName, objectName, is, 3 * MB, "application/octet-stream");
      is.close();

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
   * Test get bucket policy for given policy type.
   */
  public static void testGetBucketPolicy(String objectPrefix, PolicyType policyType) throws Exception {
    if (!mintEnv) {
      System.out.println("Test: " + policyType + ": getBucketPolicy(String bucketName, String objectPrefix)");
    }

    long startTime = System.currentTimeMillis();
    try {
      client.setBucketPolicy(bucketName, objectPrefix, policyType);
      PolicyType type = client.getBucketPolicy(bucketName, objectPrefix);
      if (type != policyType) {
        throw new Exception("[FAILED] Expected: " + policyType + ", Got: " + type);
      }
      mintSuccessLog("getBucketPolicy(String bucketName, String objectPrefix)", null, startTime);
    } catch (Exception e) {
      mintFailedLog("getBucketPolicy(String bucketName, String objectPrefix)", null, startTime, null,
                    e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: NONE type: getBucketPolicy(String bucketName, String objectPrefix).
   */
  public static void getBucketPolicy_test1() throws Exception {
    testGetBucketPolicy("get-bucket-policy-none", PolicyType.NONE);
  }

  /**
   * Test: READ_ONLY type: getBucketPolicy(String bucketName, String objectPrefix).
   */
  public static void getBucketPolicy_test2() throws Exception {
    testGetBucketPolicy("get-bucket-policy-read-only", PolicyType.READ_ONLY);
  }

  /**
   * Test: WRITE_ONLY type: getBucketPolicy(String bucketName, String objectPrefix).
   */
  public static void getBucketPolicy_test3() throws Exception {
    testGetBucketPolicy("get-bucket-policy-write-only", PolicyType.WRITE_ONLY);
  }

  /**
   * Test: READ_WRITE type: getBucketPolicy(String bucketName, String objectPrefix).
   */
  public static void getBucketPolicy_test4() throws Exception {
    testGetBucketPolicy("get-bucket-policy-read-write", PolicyType.READ_WRITE);
  }

  /**
   * Test: NONE type: setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType).
   */
  public static void setBucketPolicy_test1() throws Exception {
    if (!mintEnv) {
      System.out.println("Test: NONE: setBucketPolicy(String bucketName, String objectPrefix, "
                         + "PolicyType policyType)");
    }

    long startTime = System.currentTimeMillis();
    try {
      String objectPrefix = "set-bucket-policy-none";
      client.setBucketPolicy(bucketName, objectPrefix, PolicyType.NONE);

      String objectName = objectPrefix + "/" + getRandomName();
      InputStream is = new ContentInputStream(16);
      client.putObject(bucketName, objectName, is, 16, "application/octet-stream");
      is.close();

      String urlString = client.getObjectUrl(bucketName, objectName);
      Request.Builder requestBuilder = new Request.Builder();
      Request request = requestBuilder
          .url(HttpUrl.parse(urlString))
          .method("GET", null)
          .build();
      OkHttpClient transport = new OkHttpClient();
      Response response = transport.newCall(request).execute();

      if (response == null) {
        throw new Exception("[FAILED] empty response");
      }

      if (response.isSuccessful()) {
        throw new Exception("[FAILED] Anonmymous has access for None policy type.  Response: " + response);
      }

      client.removeObject(bucketName, objectName);
      mintSuccessLog("setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType)",
                     null, startTime);
    } catch (Exception e) {
      mintFailedLog("setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType)", null, startTime,
                    null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test set bucket policy.
   */
  public static void testSetBucketPolicy(String objectPrefix, PolicyType policyType) throws Exception {
    if (!mintEnv) {
      System.out.println("Test: " + policyType + ": setBucketPolicy(String bucketName, "
                         + " String objectPrefix, PolicyType policyType)");
    }

    long startTime = System.currentTimeMillis();
    try {
      client.setBucketPolicy(bucketName, objectPrefix, policyType);

      String objectName = objectPrefix + "/" + getRandomName();
      String urlString = client.getObjectUrl(bucketName, objectName);
      byte[] data = "hello, world".getBytes(StandardCharsets.UTF_8);

      if (policyType == PolicyType.READ_ONLY) {
        client.putObject(bucketName, objectName, new ByteArrayInputStream(data), data.length, customContentType);
      }

      if ((policyType == PolicyType.READ_WRITE) || (policyType == PolicyType.WRITE_ONLY)) {
        writeObject(urlString, data);

        InputStream is = client.getObject(bucketName, objectName);
        byte[] readBytes = readAllBytes(is);
        is.close();

        if (!Arrays.equals(data, readBytes)) {
          throw new Exception("content differs");
        }
      }

      if ((policyType == PolicyType.READ_WRITE) || (policyType == PolicyType.READ_ONLY)) {
        readObject(urlString);
      }

      client.removeObject(bucketName, objectName);

      mintSuccessLog("setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType)", 
                     null, startTime);
    } catch (Exception e) {
      mintFailedLog("setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType)", null, startTime,
                    null, e.toString() + " >>> " + Arrays.toString(e.getStackTrace()));
      throw e;
    }
  }

  /**
   * Test: READ_ONLY type: setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType).
   */
  public static void setBucketPolicy_test2() throws Exception {
    testSetBucketPolicy("set-bucket-policy-read-only", PolicyType.READ_ONLY);
  }

  /**
   * Test: WRITE_ONLY type: setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType).
   */
  public static void setBucketPolicy_test3() throws Exception {
    testSetBucketPolicy("set-bucket-policy-write-only", PolicyType.WRITE_ONLY);
  }

  /**
   * Test: READ_WRITE type: setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType).
   */
  public static void setBucketPolicy_test4() throws Exception {
    testSetBucketPolicy("set-bucket-policy-read-write", PolicyType.READ_WRITE);
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
    if (endpoint.toLowerCase().contains("s3")) {
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

    statObject_test();

    getObject_test1();
    getObject_test2();
    getObject_test3();
    getObject_test4();
    getObject_test5();
    getObject_test6();
    getObject_test7();

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

    getBucketPolicy_test1();
    getBucketPolicy_test2();
    getBucketPolicy_test3();
    getBucketPolicy_test4();

    setBucketPolicy_test1();
    setBucketPolicy_test2();
    setBucketPolicy_test3();
    setBucketPolicy_test4();

    threadedPutObject();

    teardown();

    // notification tests requires 'MINIO_JAVA_TEST_TOPIC' and 'MINIO_JAVA_TEST_REGION' environment variables
    // to be set appropriately.
    setBucketNotification_test1();
    getBucketNotification_test1();
    removeAllBucketNotification_test1();
  }

  /**
   * runFastTests: runs a fast set of tests.
   */
  public static void runFastTests() throws Exception {
    makeBucket_test1();
    listBuckets_test();
    bucketExists_test();
    removeBucket_test();

    setup();

    putObject_test1();
    statObject_test();
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
    getBucketPolicy_test2();
    getBucketPolicy_test3();
    getBucketPolicy_test4();
    setBucketPolicy_test4();

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

    endpoint = args[0];
    accessKey = args[1];
    secretKey = args[2];
    region = args[3];

    try {
      client = new MinioClient(endpoint, accessKey, secretKey);
      // Enable trace for debugging.
      // client.traceOn(System.out);
      FunctionalTest.runTests();

      // Run fast test with region parameter passed to the constructor
      client = new MinioClient(endpoint, accessKey, secretKey, region);
      FunctionalTest.runFastTests();

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
