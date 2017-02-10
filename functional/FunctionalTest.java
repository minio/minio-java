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
import java.io.*;
import java.lang.*;

import static java.nio.file.StandardOpenOption.*;
import java.nio.file.*;

import org.joda.time.DateTime;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Response;
import com.google.common.io.ByteStreams;

import io.minio.*;
import io.minio.messages.*;
import io.minio.errors.*;


public class FunctionalTest {
  private static final int MB = 1024 * 1024;
  private static final SecureRandom random = new SecureRandom();
  private static final String bucketName = getRandomName();
  private static final String customContentType = "application/javascript";
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
    String fileName = getRandomName();
    byte[] data = new byte[size];
    random.nextBytes(data);

    OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(fileName), CREATE, APPEND));
    out.write(data, 0, data.length);
    out.close();

    return fileName;
  }

  /**
   * Generate random name.
   */
  public static String getRandomName() {
    return "minio-java-test-" + new BigInteger(32, random).toString(32);
  }

  /**
   * Test: makeBucket(String bucketName).
   */
  public static void makeBucket_test1() throws Exception {
    System.out.println("Test: makeBucket(String bucketName)");
    String name = getRandomName();
    client.makeBucket(name);
    client.removeBucket(name);
  }

  /**
   * Test: makeBucket(String bucketName, String region).
   */
  public static void makeBucket_test2() throws Exception {
    System.out.println("Test: makeBucket(String bucketName, String region)");
    String name = getRandomName();
    client.makeBucket(name, "eu-west-1");
    client.removeBucket(name);
  }

  /**
   * Test: makeBucket(String bucketName, String region) where bucketName has
   * periods in its name.
   */
  public static void makeBucket_test3() throws Exception {
    System.out.println("Test: makeBucket(String bucketName, String region)");
    String name = getRandomName() + ".withperiod";
    client.makeBucket(name, "eu-central-1");
    client.removeBucket(name);
  }

  /**
   * Test: listBuckets().
   */
  public static void listBuckets_test() throws Exception {
    System.out.println("Test: listBuckets()");
    for (Bucket bucket : client.listBuckets()) {
      ignore(bucket);
    }
  }

  /**
   * Test: bucketExists(String bucketName).
   */
  public static void bucketExists_test() throws Exception {
    System.out.println("Test: bucketExists(String bucketName)");
    String name = getRandomName();
    client.makeBucket(name);
    if (!client.bucketExists(name)) {
      throw new Exception("[FAILED] Test: bucketExists(String bucketName)");
    }
    client.removeBucket(name);
  }

  /**
   * Test: removeBucket(String bucketName).
   */
  public static void removeBucket_test() throws Exception {
    System.out.println("Test: removeBucket(String bucketName)");
    String name = getRandomName();
    client.makeBucket(name);
    client.removeBucket(name);
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
   * Test: putObject(String bucketName, String objectName, String fileName).
   */
  public static void putObject_test1() throws Exception {
    System.out.println("Test: putObject(String bucketName, String objectName, String fileName)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: multipart: putObject(String bucketName, String objectName, String fileName).
   */
  public static void putObject_test2() throws Exception {
    System.out.println("Test: multipart: putObject(String bucketName, String objectName, String fileName)");
    String fileName = createFile(13 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: multipart resume: putObject(String bucketName, String objectName, String fileName).
   */
  public static void putObject_test3() throws Exception {
    System.out.println("Test: multipart resume: putObject(String bucketName, String objectName, String fileName)");
    String fileName = createFile(13 * MB);
    InputStream is = Files.newInputStream(Paths.get(fileName));
    try {
      client.putObject(bucketName, fileName, is, 20 * 1024 * 1024, null);
    } catch (InsufficientDataException e) {
      ignore();
    }
    is.close();

    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: putObject(String bucketName, String objectName, String contentType, long size, InputStream body).
   */
  public static void putObject_test4() throws Exception {
    System.out.println("Test: putObject(String bucketName, String objectName, String contentType, long size, "
                       + "InputStream body)");
    String fileName = createFile(3 * MB);
    InputStream is = Files.newInputStream(Paths.get(fileName));
    client.putObject(bucketName, fileName, is, 1024 * 1024, customContentType);
    is.close();
    Files.delete(Paths.get(fileName));
    ObjectStat objectStat = client.statObject(bucketName, fileName);
    if (!customContentType.equals(objectStat.contentType())) {
      throw new Exception("[FAILED] Test: putObject(String bucketName, String objectName, String contentType, "
                          + "long size, InputStream body)");
    }
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: With content-type: putObject(String bucketName, String objectName, String fileName, String contentType).
   */
  public static void putObject_test5() throws Exception {
    System.out.println("Test: putObject(String bucketName, String objectName, String fileName,"
                       + " String contentType)");
    String fileName = createFile(13 * MB);
    client.putObject(bucketName, fileName, fileName, customContentType);
    Files.delete(Paths.get(fileName));
    ObjectStat objectStat = client.statObject(bucketName, fileName);
    if (!customContentType.equals(objectStat.contentType())) {
      throw new Exception("[FAILED] Test: putObject(String bucketName, String objectName, String fileName,"
                          + " String contentType)");
    }
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: putObject(String bucketName, String objectName, String fileName).
   * where objectName has multiple path segments.
   */
  public static void putObject_test6() throws Exception {
    System.out.println("Test: objectName with path segments: "
                       + "putObject(String bucketName, String objectName, String fileName)");
    String fileName = createFile(3 * MB);
    String objectName = "path/to/" + fileName;
    client.putObject(bucketName, objectName, fileName);
    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, objectName);
  }

  /**
   * Test: statObject(String bucketName, String objectName).
   */
  public static void statObject_test() throws Exception {
    System.out.println("Test: statObject(String bucketName, String objectName)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    client.statObject(bucketName, fileName);
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: getObject(String bucketName, String objectName).
   */
  public static void getObject_test1() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    InputStream is = client.getObject(bucketName, fileName);
    is.close();
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset).
   */
  public static void getObject_test2() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName, long offset)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    InputStream is = client.getObject(bucketName, fileName, 1000L);
    is.close();
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset, Long length).
   */
  public static void getObject_test3() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName, long offset, Long length)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    InputStream is = client.getObject(bucketName, fileName, 1000L, 1024 * 1024L);
    is.close();
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: getObject(String bucketName, String objectName, String fileName).
   */
  public static void getObject_test4() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName, String fileName)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    client.getObject(bucketName, fileName, fileName + ".downloaded");
    Files.delete(Paths.get(fileName + ".downloaded"));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: getObject(String bucketName, String objectName, String fileName).
   * where objectName has multiple path segments.
   */
  public static void getObject_test5() throws Exception {
    System.out.println("Test: objectName with multiple path segments: "
                       + "getObject(String bucketName, String objectName, String fileName)");
    String fileName = createFile(3 * MB);
    String objectName = "path/to/" + fileName;
    client.putObject(bucketName, objectName, fileName);
    Files.delete(Paths.get(fileName));
    client.getObject(bucketName, objectName, fileName + ".downloaded");
    Files.delete(Paths.get(fileName + ".downloaded"));
    client.removeObject(bucketName, objectName);
  }

  /**
   * Test: listObjects(final String bucketName).
   */
  public static void listObject_test1() throws Exception {
    int i;
    System.out.println("Test: listObjects(final String bucketName)");
    String[] fileNames = new String[3];
    for (i = 0; i < 3; i++) {
      String fileName = createFile(1 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      fileNames[i] = fileName;
    }

    i = 0;
    for (Result r : client.listObjects(bucketName)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    for (i = 0; i < 3; i++) {
      client.removeObject(bucketName, fileNames[i]);
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix).
   */
  public static void listObject_test2() throws Exception {
    int i;
    System.out.println("Test: listObjects(final String bucketName, final String prefix)");
    String[] fileNames = new String[3];
    for (i = 0; i < 3; i++) {
      String fileName = createFile(1 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      fileNames[i] = fileName;
    }

    i = 0;
    for (Result r : client.listObjects(bucketName, "minio")) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    for (i = 0; i < 3; i++) {
      client.removeObject(bucketName, fileNames[i]);
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix, final boolean recursive).
   */
  public static void listObject_test3() throws Exception {
    int i;
    System.out.println("Test: listObjects(final String bucketName, final String prefix, final boolean recursive)");
    String[] fileNames = new String[3];
    for (i = 0; i < 3; i++) {
      String fileName = createFile(1 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      fileNames[i] = fileName;
    }

    i = 0;
    for (Result r : client.listObjects(bucketName, "minio", true)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    for (i = 0; i < 3; i++) {
      client.removeObject(bucketName, fileNames[i]);
    }
  }

  /**
   * Test: listObjects(final string bucketName).
   */
  public static void listObject_test4() throws Exception {
    int i;
    System.out.println("Test: empty bucket: listObjects(final String bucketName)");

    i = 0;
    for (Result r : client.listObjects(bucketName, "minio", true)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }
  }

  /**
   * Test: removeObject(String bucketName, String objectName).
   */
  public static void removeObject_test() throws Exception {
    System.out.println("Test: removeObject(String bucketName, String objectName)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);
    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: listIncompleteUploads(String bucketName).
   */
  public static void listIncompleteUploads_test1() throws Exception {
    System.out.println("Test: listIncompleteUploads(String bucketName)");
    String fileName = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(fileName));
    try {
      client.putObject(bucketName, fileName, is, 9 * 1024 * 1024, null);
    } catch (InsufficientDataException e) {
      ignore("Exception occurred as excepted");
    }
    is.close();

    int i = 0;
    for (Result<Upload> r : client.listIncompleteUploads(bucketName)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    Files.delete(Paths.get(fileName));
    client.removeIncompleteUpload(bucketName, fileName);
  }

  /**
   * Test: listIncompleteUploads(String bucketName, String prefix).
   */
  public static void listIncompleteUploads_test2() throws Exception {
    System.out.println("Test: listIncompleteUploads(String bucketName, String prefix)");
    String fileName = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(fileName));
    try {
      client.putObject(bucketName, fileName, is, 9 * 1024 * 1024, null);
    } catch (InsufficientDataException e) {
      ignore("Exception occurred as excepted");
    }
    is.close();

    int i = 0;
    for (Result<Upload> r : client.listIncompleteUploads(bucketName, "minio")) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    Files.delete(Paths.get(fileName));
    client.removeIncompleteUpload(bucketName, fileName);
  }

  /**
   * Test: listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive).
   */
  public static void listIncompleteUploads_test3() throws Exception {
    System.out.println("Test: listIncompleteUploads(final String bucketName, final String prefix, "
                       + "final boolean recursive)");
    String fileName = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(fileName));
    try {
      client.putObject(bucketName, fileName, is, 9 * 1024 * 1024, null);
    } catch (InsufficientDataException e) {
      ignore("Exception occurred as excepted");
    }
    is.close();

    int i = 0;
    for (Result<Upload> r : client.listIncompleteUploads(bucketName, "minio", true)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    Files.delete(Paths.get(fileName));
    client.removeIncompleteUpload(bucketName, fileName);
  }

  /**
   * Test: removeIncompleteUpload(String bucketName, String objectName).
   */
  public static void removeIncompleteUploads_test() throws Exception {
    System.out.println("Test: removeIncompleteUpload(String bucketName, String objectName)");
    String fileName = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(fileName));
    try {
      client.putObject(bucketName, fileName, is, 9 * 1024 * 1024, null);
    } catch (InsufficientDataException e) {
      ignore("Exception occurred as excepted");
    }
    is.close();

    int i = 0;
    for (Result<Upload> r : client.listIncompleteUploads(bucketName)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    Files.delete(Paths.get(fileName));
    client.removeIncompleteUpload(bucketName, fileName);
  }

  /**
   * public String presignedGetObject(String bucketName, String objectName).
   */
  public static void presignedGetObject_test1() throws Exception {
    System.out.println("Test: presignedGetObject(String bucketName, String objectName)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);

    String urlString = client.presignedGetObject(bucketName, fileName);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("GET", null)
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (response.isSuccessful()) {
        OutputStream os = Files.newOutputStream(Paths.get(fileName + ".downloaded"), StandardOpenOption.CREATE);
        ByteStreams.copy(response.body().byteStream(), os);
        response.body().close();
        os.close();
      } else {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new java.util.Scanner(response.body().charStream()).useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }

        throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName)"
                          + ", Error: <No response from server>");
    }

    if (!Arrays.equals(Files.readAllBytes(Paths.get(fileName)),
                       Files.readAllBytes(Paths.get(fileName + ".downloaded")))) {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName)"
                          + ", Error: <Content differs>");
    }

    Files.delete(Paths.get(fileName));
    Files.delete(Paths.get(fileName + ".downloaded"));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: presignedGetObject(String bucketName, String objectName, Integer expires).
   */
  public static void presignedGetObject_test2() throws Exception {
    System.out.println("Test: presignedGetObject(String bucketName, String objectName, Integer expires)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);

    String urlString = client.presignedGetObject(bucketName, fileName, 3600);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("GET", null)
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (response.isSuccessful()) {
        OutputStream os = Files.newOutputStream(Paths.get(fileName + ".downloaded"), StandardOpenOption.CREATE);
        ByteStreams.copy(response.body().byteStream(), os);
        response.body().close();
        os.close();
      } else {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new java.util.Scanner(response.body().charStream()).useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }

        throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName, Integer expires)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName, Integer expires)"
                          + ", Error: <No response from server>");
    }

    if (!Arrays.equals(Files.readAllBytes(Paths.get(fileName)),
                       Files.readAllBytes(Paths.get(fileName + ".downloaded")))) {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName, Integer expires)"
                          + ", Error: <Content differs>");
    }

    Files.delete(Paths.get(fileName));
    Files.delete(Paths.get(fileName + ".downloaded"));
    client.removeObject(bucketName, fileName);
  }

  /**
   * public String presignedGetObject(String bucketName, String objectName, Integer expires, Map reqParams).
   */
  public static void presignedGetObject_test3() throws Exception {
    System.out.println("Test: presignedGetObject(String bucketName, String objectName, Integer expires, "
                       + "Map<String, String> reqParams)");
    String fileName = createFile(3 * MB);
    client.putObject(bucketName, fileName, fileName);

    Map<String, String> reqParams = new HashMap<>();
    reqParams.put("response-content-type", "application/json");

    String urlString = client.presignedGetObject(bucketName, fileName, 3600, reqParams);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("GET", null)
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (response.isSuccessful()) {
        OutputStream os = Files.newOutputStream(Paths.get(fileName + ".downloaded"), StandardOpenOption.CREATE);
        ByteStreams.copy(response.body().byteStream(), os);
        if (!response.header("Content-Type").equals("application/json")) {
          throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName,"
                              + " Integer expires, Map<String, String> reqParams)"
                              + ", Response: " + response);
        }
        response.body().close();
        os.close();
      } else {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new java.util.Scanner(response.body().charStream()).useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }

        throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName,"
                            + " Integer expires, Map<String, String> reqParams)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName,"
                          + " Integer expires, Map<String, String> reqParams)"
                          + ", Error: <No response from server>");
    }

    if (!Arrays.equals(Files.readAllBytes(Paths.get(fileName)),
                       Files.readAllBytes(Paths.get(fileName + ".downloaded")))) {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName,"
                          + " Integer expires, Map<String, String> reqParams)"
                          + ", Error: <Content differs>");
    }

    Files.delete(Paths.get(fileName));
    Files.delete(Paths.get(fileName + ".downloaded"));
    client.removeObject(bucketName, fileName);
  }

  /**
   * public String presignedPutObject(String bucketName, String objectName).
   */
  public static void presignedPutObject_test1() throws Exception {
    System.out.println("Test: presignedPutObject(String bucketName, String objectName)");
    String fileName = createFile(3 * MB);
    String urlString = client.presignedPutObject(bucketName, fileName);

    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("PUT", RequestBody.create(null, Files.readAllBytes(Paths.get(fileName))))
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (!response.isSuccessful()) {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new java.util.Scanner(response.body().charStream()).useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }

        throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName)"
                          + ", Error: <No response from server>");
    }

    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: presignedPutObject(String bucketName, String objectName, Integer expires).
   */
  public static void presignedPutObject_test2() throws Exception {
    System.out.println("Test: presignedPutObject(String bucketName, String objectName, Integer expires)");
    String fileName = createFile(3 * MB);
    String urlString = client.presignedPutObject(bucketName, fileName, 3600);

    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("PUT", RequestBody.create(null, Files.readAllBytes(Paths.get(fileName))))
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (!response.isSuccessful()) {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new java.util.Scanner(response.body().charStream()).useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }

        throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName, Integer expires)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName, Integer expires)"
                          + ", Error: <No response from server>");
    }

    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: presignedPostPolicy(PostPolicy policy).
   */
  public static void presignedPostPolicy_test() throws Exception {
    System.out.println("Test: presignedPostPolicy(PostPolicy policy)");
    String fileName = createFile(3 * MB);
    PostPolicy policy = new PostPolicy(bucketName, fileName, DateTime.now().plusDays(7));
    policy.setContentRange(1 * MB, 4 * MB);
    Map<String, String> formData = client.presignedPostPolicy(policy);

    MultipartBuilder multipartBuilder = new MultipartBuilder();
    multipartBuilder.type(MultipartBuilder.FORM);
    for (Map.Entry<String, String> entry : formData.entrySet()) {
      multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
    }
    multipartBuilder.addFormDataPart("file", fileName, RequestBody.create(null, new File(fileName)));

    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder.url(endpoint + "/" + bucketName).post(multipartBuilder.build()).build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (!response.isSuccessful()) {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new java.util.Scanner(response.body().charStream()).useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }

        throw new Exception("[FAILED] Test: presignedPostPolicy(PostPolicy policy)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedPostPolicy(PostPolicy policy)"
                          + ", Error: <No response from server>");
    }

    Files.delete(Paths.get(fileName));
    client.removeObject(bucketName, fileName);
  }

  /**
   * Test: PutObject(): do put object using multi-threaded way in parallel.
   */
  public static void threadedPutObject() throws Exception {
    System.out.println("Test: threadedPutObject");
    Thread[] threads = new Thread[7];

    for (int i = 0; i < 7; i++) {
      threads[i] = new Thread(new PutObjectRunnable(client, bucketName, createFile(17 * MB)));
    }

    for (int i = 0; i < 7; i++) {
      threads[i].start();
    }

    // Waiting for threads to complete.
    for (int i = 0; i < 7; i++) {
      threads[i].join();
    }

    // All threads are completed.
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, String, destObjectName).
   */
  public static void copyObject_test1() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + "String destObjectName)");
    String bucketName = getRandomName();
    String secondBucketName = getRandomName();

    String fileName = createFile(3 * MB);

    client.makeBucket(bucketName);
    client.makeBucket(secondBucketName);
    client.putObject(bucketName, fileName, fileName);

    // File should be copied as there are no conditions set.
    client.copyObject(bucketName, fileName, secondBucketName);
    InputStream is = client.getObject(secondBucketName, fileName);
    is.close();

    client.removeObject(bucketName, fileName);
    client.removeObject(secondBucketName, fileName);
    client.removeBucket(bucketName);
    client.removeBucket(secondBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, String, destObjectName,
   * CopyConditions copyConditions) with ETag to match.
   */
  public static void copyObject_test2() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + " String destObjectName, CopyConditions copyConditions) with Matching ETag (Negative Case)");
    String bucketName = getRandomName();
    String secondBucketName = getRandomName();

    String fileName = createFile(3 * MB);

    client.makeBucket(bucketName);
    client.makeBucket(secondBucketName);
    client.putObject(bucketName, fileName, fileName);

    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETag("TestETag");

    try {
      client.copyObject(bucketName, fileName, secondBucketName, copyConditions);
    } catch (ErrorResponseException e) {
      ignore();
    }

    client.removeObject(bucketName, fileName);
    client.removeBucket(bucketName);
    client.removeBucket(secondBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, String, destObjectName,
   * CopyConditions copyConditions) with ETag to match.
   */
  public static void copyObject_test3() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + " String destObjectName, CopyConditions copyConditions) with Matching ETag (Positive Case)");
    String bucketName = getRandomName();
    String secondBucketName = getRandomName();

    String fileName = createFile(3 * MB);

    client.makeBucket(bucketName);
    client.makeBucket(secondBucketName);
    client.putObject(bucketName, fileName, fileName);

    ObjectStat stat = client.statObject(bucketName, fileName);

    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETag(stat.etag());

    // File should be copied as ETag set in copyConditions matches object's ETag.
    client.copyObject(bucketName, fileName, secondBucketName, copyConditions);
    InputStream is = client.getObject(secondBucketName, fileName);
    is.close();

    client.removeObject(bucketName, fileName);
    client.removeObject(secondBucketName, fileName);
    client.removeBucket(bucketName);
    client.removeBucket(secondBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, String, destObjectName,
   * CopyConditions copyConditions) with ETag to not match.
   */
  public static void copyObject_test4() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + " String destObjectName, CopyConditions copyConditions) with not matching ETag"
                       + " (Positive Case)");
    String bucketName = getRandomName();
    String secondBucketName = getRandomName();

    String fileName = createFile(3 * MB);

    client.makeBucket(bucketName);
    client.makeBucket(secondBucketName);
    client.putObject(bucketName, fileName, fileName);

    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETagNone("TestETag");

    // File should be copied as ETag set in copyConditions doesn't match object's ETag.
    client.copyObject(bucketName, fileName, secondBucketName, copyConditions);
    InputStream is = client.getObject(secondBucketName, fileName);
    is.close();

    client.removeObject(bucketName, fileName);
    client.removeObject(secondBucketName, fileName);
    client.removeBucket(bucketName);
    client.removeBucket(secondBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, String, destObjectName,
   * CopyConditions copyConditions) with ETag to not match.
   */
  public static void copyObject_test5() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + " String destObjectName, CopyConditions copyConditions) with not matching ETag"
                       + " (Negative Case)");
    String bucketName = getRandomName();
    String secondBucketName = getRandomName();

    String fileName = createFile(3 * MB);

    client.makeBucket(bucketName);
    client.makeBucket(secondBucketName);
    client.putObject(bucketName, fileName, fileName);

    ObjectStat stat = client.statObject(bucketName, fileName);

    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETagNone(stat.etag());

    try {
      client.copyObject(bucketName, fileName, secondBucketName, copyConditions);
    } catch (ErrorResponseException e) {
      // File should not be copied as ETag set in copyConditions matches object's ETag.
      ignore();
    }
    client.removeObject(bucketName, fileName);
    client.removeObject(secondBucketName, fileName);
    client.removeBucket(bucketName);
    client.removeBucket(secondBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, String, destObjectName,
   * CopyConditions copyConditions) with object modified after condition.
   */
  public static void copyObject_test6() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + " String destObjectName, CopyConditions copyConditions) with modified after "
                       + "condition (Positive Case)");
    String bucketName = getRandomName();
    String secondBucketName = getRandomName();

    String fileName = createFile(3 * MB);

    client.makeBucket(bucketName);
    client.makeBucket(secondBucketName);
    client.putObject(bucketName, fileName, fileName);

    CopyConditions copyConditions = new CopyConditions();
    DateTime dateRepresentation = new DateTime(2015, Calendar.MAY, 3, 10, 10);

    copyConditions.setModified(dateRepresentation);

    // File should be copied as object was modified after the set date.
    client.copyObject(bucketName, fileName, secondBucketName, copyConditions);
    InputStream is = client.getObject(secondBucketName, fileName);
    is.close();

    client.removeObject(bucketName, fileName);
    client.removeObject(secondBucketName, fileName);
    client.removeBucket(bucketName);
    client.removeBucket(secondBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName, String, destObjectName,
   * CopyConditions copyConditions) with object modified after condition.
   */
  public static void copyObject_test7() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + " String destObjectName, CopyConditions copyConditions) with modified after"
                       + " condition (Negative Case)");
    String bucketName = getRandomName();
    String secondBucketName = getRandomName();

    String fileName = createFile(3 * MB);

    client.makeBucket(bucketName);
    client.makeBucket(secondBucketName);
    client.putObject(bucketName, fileName, fileName);

    CopyConditions copyConditions = new CopyConditions();
    DateTime dateRepresentation = new DateTime(2015, Calendar.MAY, 3, 10, 10);

    copyConditions.setUnmodified(dateRepresentation);

    try {
      client.copyObject(bucketName, fileName, secondBucketName, copyConditions);
    } catch (ErrorResponseException e) {
      // File should not be copied as object was modified after date set in copyConditions.
      ignore();
    }

    client.removeObject(bucketName, fileName);
    client.removeObject(secondBucketName, fileName);
    client.removeBucket(bucketName);
    client.removeBucket(secondBucketName);
  }

  /**
   * runTests: runs as much as possible of test combinations.
   */
  public static void runTests() throws Exception {
    makeBucket_test1();
    if (!endpoint.toLowerCase().contains("minio")) {
      makeBucket_test2();
      makeBucket_test3();
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

    statObject_test();
    getObject_test1();
    getObject_test2();
    getObject_test3();
    getObject_test4();
    getObject_test5();

    listObject_test1();
    listObject_test2();
    listObject_test3();
    listObject_test4();

    removeObject_test();

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

    threadedPutObject();

    teardown();
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
    removeObject_test();
    listIncompleteUploads_test1();
    removeIncompleteUploads_test();
    presignedGetObject_test1();
    presignedPutObject_test1();
    presignedPostPolicy_test();
    copyObject_test1();
    teardown();
  }


  /**
   * main().
   */
  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY> <REGION>");
      return;
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
      return;
    }
  }
}
