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
    String filename = getRandomName();
    byte[] data = new byte[size];
    random.nextBytes(data);

    OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(filename), CREATE, APPEND));
    out.write(data, 0, data.length);
    out.close();

    return filename;
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
   * Test: putObject(String bucketName, String objectName, String filename).
   */
  public static void putObject_test1() throws Exception {
    System.out.println("Test: putObject(String bucketName, String objectName, String filename)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: multipart: putObject(String bucketName, String objectName, String filename).
   */
  public static void putObject_test2() throws Exception {
    System.out.println("Test: multipart: putObject(String bucketName, String objectName, String filename)");
    String filename = createFile(13 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: multipart resume: putObject(String bucketName, String objectName, String filename).
   */
  public static void putObject_test3() throws Exception {
    System.out.println("Test: multipart resume: putObject(String bucketName, String objectName, String filename)");
    String filename = createFile(13 * MB);
    InputStream is = Files.newInputStream(Paths.get(filename));
    try {
      client.putObject(bucketName, filename, is, 20 * 1024 * 1024, null);
    } catch (InsufficientDataException e) {
      ignore();
    }
    is.close();

    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: putObject(String bucketName, String objectName, String contentType, long size, InputStream body).
   */
  public static void putObject_test4() throws Exception {
    System.out.println("Test: putObject(String bucketName, String objectName, String contentType, long size, "
                       + "InputStream body)");
    String filename = createFile(3 * MB);
    InputStream is = Files.newInputStream(Paths.get(filename));
    client.putObject(bucketName, filename, is, 1024 * 1024, customContentType);
    is.close();
    Files.delete(Paths.get(filename));
    ObjectStat objectStat = client.statObject(bucketName, filename);
    if (!customContentType.equals(objectStat.contentType())) {
      throw new Exception("[FAILED] Test: putObject(String bucketName, String objectName, String contentType, "
                          + "long size, InputStream body)");
    }
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: With content-type: putObject(String bucketName, String objectName, String filename, String contentType).
   */
  public static void putObject_test5() throws Exception {
    System.out.println("Test: putObject(String bucketName, String objectName, String filename,"
                       + " String contentType)");
    String filename = createFile(13 * MB);
    client.putObject(bucketName, filename, filename, customContentType);
    Files.delete(Paths.get(filename));
    ObjectStat objectStat = client.statObject(bucketName, filename);
    if (!customContentType.equals(objectStat.contentType())) {
      throw new Exception("[FAILED] Test: putObject(String bucketName, String objectName, String filename,"
                          + " String contentType)");
    }
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: putObject(String bucketName, String objectName, String filename).
   * where objectName has multiple path segments.
   */
  public static void putObject_test6() throws Exception {
    System.out.println("Test: objectName with path segments: "
                       + "putObject(String bucketName, String objectName, String filename)");
    String filename = createFile(3 * MB);
    String objectName = "path/to/" + filename;
    client.putObject(bucketName, objectName, filename);
    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, objectName);
  }

  /**
   * Test: statObject(String bucketName, String objectName).
   */
  public static void statObject_test() throws Exception {
    System.out.println("Test: statObject(String bucketName, String objectName)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    client.statObject(bucketName, filename);
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: getObject(String bucketName, String objectName).
   */
  public static void getObject_test1() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    InputStream is = client.getObject(bucketName, filename);
    is.close();
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset).
   */
  public static void getObject_test2() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName, long offset)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    InputStream is = client.getObject(bucketName, filename, 1000L);
    is.close();
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: getObject(String bucketName, String objectName, long offset, Long length).
   */
  public static void getObject_test3() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName, long offset, Long length)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    InputStream is = client.getObject(bucketName, filename, 1000L, 1024 * 1024L);
    is.close();
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: getObject(String bucketName, String objectName, String filename).
   */
  public static void getObject_test4() throws Exception {
    System.out.println("Test: getObject(String bucketName, String objectName, String filename)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    client.getObject(bucketName, filename, filename + ".downloaded");
    Files.delete(Paths.get(filename + ".downloaded"));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: getObject(String bucketName, String objectName, String filename).
   * where objectName has multiple path segments.
   */
  public static void getObject_test5() throws Exception {
    System.out.println("Test: objectName with multiple path segments: "
                       + "getObject(String bucketName, String objectName, String filename)");
    String filename = createFile(3 * MB);
    String objectName = "path/to/" + filename;
    client.putObject(bucketName, objectName, filename);
    Files.delete(Paths.get(filename));
    client.getObject(bucketName, objectName, filename + ".downloaded");
    Files.delete(Paths.get(filename + ".downloaded"));
    client.removeObject(bucketName, objectName);
  }

  /**
   * Test: listObjects(final String bucketName).
   */
  public static void listObject_test1() throws Exception {
    int i;
    System.out.println("Test: listObjects(final String bucketName)");
    String[] filenames = new String[3];
    for (i = 0; i < 3; i++) {
      String filename = createFile(1 * MB);
      client.putObject(bucketName, filename, filename);
      Files.delete(Paths.get(filename));
      filenames[i] = filename;
    }

    i = 0;
    for (Result<?> r : client.listObjects(bucketName)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    for (i = 0; i < 3; i++) {
      client.removeObject(bucketName, filenames[i]);
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix).
   */
  public static void listObject_test2() throws Exception {
    int i;
    System.out.println("Test: listObjects(final String bucketName, final String prefix)");
    String[] filenames = new String[3];
    for (i = 0; i < 3; i++) {
      String filename = createFile(1 * MB);
      client.putObject(bucketName, filename, filename);
      Files.delete(Paths.get(filename));
      filenames[i] = filename;
    }

    i = 0;
    for (Result<?> r : client.listObjects(bucketName, "minio")) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    for (i = 0; i < 3; i++) {
      client.removeObject(bucketName, filenames[i]);
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix, final boolean recursive).
   */
  public static void listObject_test3() throws Exception {
    int i;
    System.out.println("Test: listObjects(final String bucketName, final String prefix, final boolean recursive)");
    String[] filenames = new String[3];
    for (i = 0; i < 3; i++) {
      String filename = createFile(1 * MB);
      client.putObject(bucketName, filename, filename);
      Files.delete(Paths.get(filename));
      filenames[i] = filename;
    }

    i = 0;
    for (Result<?> r : client.listObjects(bucketName, "minio", true)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    for (i = 0; i < 3; i++) {
      client.removeObject(bucketName, filenames[i]);
    }
  }

  /**
   * Test: listObjects(final string bucketName).
   */
  public static void listObject_test4() throws Exception {
    int i;
    System.out.println("Test: empty bucket: listObjects(final String bucketName)");

    i = 0;
    for (Result<?> r : client.listObjects(bucketName, "minio", true)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }
  }

  /**
   * Test: listObjects(bucketName, final String prefix, final boolean recursive, final boolean useVersion1).
   */
  public static void listObject_test5() throws Exception {
    int i;
    System.out.println("Test: listObjects(final String bucketName, final String prefix, final boolean recursive,"
                       + " final boolean useVersion1)");
    String[] filenames = new String[3];
    for (i = 0; i < 3; i++) {
      String filename = createFile(1 * MB);
      client.putObject(bucketName, filename, filename);
      Files.delete(Paths.get(filename));
      filenames[i] = filename;
    }

    i = 0;
    for (Result<?> r : client.listObjects(bucketName, "minio", true, true)) {
      ignore(i++, r.get());
      if (i == 10) {
        break;
      }
    }

    for (i = 0; i < 3; i++) {
      client.removeObject(bucketName, filenames[i]);
    }
  }

  /**
   * Test: removeObject(String bucketName, String objectName).
   */
  public static void removeObject_test1() throws Exception {
    System.out.println("Test: removeObject(String bucketName, String objectName)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: removeObject(final String bucketName, final Iterable&lt;String&gt; objectNames).
   */
  public static void removeObject_test2() throws Exception {
    System.out.println("Test: removeObject(final String bucketName, final Iterable<String> objectNames)");

    String[] filenames = new String[4];
    for (int i = 0; i < 3; i++) {
      String filename = createFile(1 * MB);
      client.putObject(bucketName, filename, filename);
      Files.delete(Paths.get(filename));
      filenames[i] = filename;
    }
    filenames[3] = "nonexistent-object";

    for (Result<?> r : client.removeObject(bucketName, Arrays.asList(filenames))) {
      ignore(r.get());
    }
  }

  /**
   * Test: listIncompleteUploads(String bucketName).
   */
  public static void listIncompleteUploads_test1() throws Exception {
    System.out.println("Test: listIncompleteUploads(String bucketName)");
    String filename = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(filename));
    try {
      client.putObject(bucketName, filename, is, 9 * 1024 * 1024, null);
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

    Files.delete(Paths.get(filename));
    client.removeIncompleteUpload(bucketName, filename);
  }

  /**
   * Test: listIncompleteUploads(String bucketName, String prefix).
   */
  public static void listIncompleteUploads_test2() throws Exception {
    System.out.println("Test: listIncompleteUploads(String bucketName, String prefix)");
    String filename = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(filename));
    try {
      client.putObject(bucketName, filename, is, 9 * 1024 * 1024, null);
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

    Files.delete(Paths.get(filename));
    client.removeIncompleteUpload(bucketName, filename);
  }

  /**
   * Test: listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive).
   */
  public static void listIncompleteUploads_test3() throws Exception {
    System.out.println("Test: listIncompleteUploads(final String bucketName, final String prefix, "
                       + "final boolean recursive)");
    String filename = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(filename));
    try {
      client.putObject(bucketName, filename, is, 9 * 1024 * 1024, null);
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

    Files.delete(Paths.get(filename));
    client.removeIncompleteUpload(bucketName, filename);
  }

  /**
   * Test: removeIncompleteUpload(String bucketName, String objectName).
   */
  public static void removeIncompleteUploads_test() throws Exception {
    System.out.println("Test: removeIncompleteUpload(String bucketName, String objectName)");
    String filename = createFile(6 * MB);
    InputStream is = Files.newInputStream(Paths.get(filename));
    try {
      client.putObject(bucketName, filename, is, 9 * 1024 * 1024, null);
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

    Files.delete(Paths.get(filename));
    client.removeIncompleteUpload(bucketName, filename);
  }

  /**
   * public String presignedGetObject(String bucketName, String objectName).
   */
  public static void presignedGetObject_test1() throws Exception {
    System.out.println("Test: presignedGetObject(String bucketName, String objectName)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);

    String urlString = client.presignedGetObject(bucketName, filename);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("GET", null)
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (response.isSuccessful()) {
        OutputStream os = Files.newOutputStream(Paths.get(filename + ".downloaded"), StandardOpenOption.CREATE);
        ByteStreams.copy(response.body().byteStream(), os);
        response.body().close();
        os.close();
      } else {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new Scanner(response.body().charStream());
        scanner.useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }
        scanner.close();

        throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName)"
                          + ", Error: <No response from server>");
    }

    if (!Arrays.equals(Files.readAllBytes(Paths.get(filename)),
                       Files.readAllBytes(Paths.get(filename + ".downloaded")))) {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName)"
                          + ", Error: <Content differs>");
    }

    Files.delete(Paths.get(filename));
    Files.delete(Paths.get(filename + ".downloaded"));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: presignedGetObject(String bucketName, String objectName, Integer expires).
   */
  public static void presignedGetObject_test2() throws Exception {
    System.out.println("Test: presignedGetObject(String bucketName, String objectName, Integer expires)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);

    String urlString = client.presignedGetObject(bucketName, filename, 3600);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("GET", null)
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (response.isSuccessful()) {
        OutputStream os = Files.newOutputStream(Paths.get(filename + ".downloaded"), StandardOpenOption.CREATE);
        ByteStreams.copy(response.body().byteStream(), os);
        response.body().close();
        os.close();
      } else {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new Scanner(response.body().charStream());
        scanner.useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }
        scanner.close();

        throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName, Integer expires)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName, Integer expires)"
                          + ", Error: <No response from server>");
    }

    if (!Arrays.equals(Files.readAllBytes(Paths.get(filename)),
                       Files.readAllBytes(Paths.get(filename + ".downloaded")))) {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName, Integer expires)"
                          + ", Error: <Content differs>");
    }

    Files.delete(Paths.get(filename));
    Files.delete(Paths.get(filename + ".downloaded"));
    client.removeObject(bucketName, filename);
  }

  /**
   * public String presignedGetObject(String bucketName, String objectName, Integer expires, Map reqParams).
   */
  public static void presignedGetObject_test3() throws Exception {
    System.out.println("Test: presignedGetObject(String bucketName, String objectName, Integer expires, "
                       + "Map<String, String> reqParams)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);

    Map<String, String> reqParams = new HashMap<>();
    reqParams.put("response-content-type", "application/json");

    String urlString = client.presignedGetObject(bucketName, filename, 3600, reqParams);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("GET", null)
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (response.isSuccessful()) {
        OutputStream os = Files.newOutputStream(Paths.get(filename + ".downloaded"), StandardOpenOption.CREATE);
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
        Scanner scanner = new Scanner(response.body().charStream());
        scanner.useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }
        scanner.close();

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

    if (!Arrays.equals(Files.readAllBytes(Paths.get(filename)),
                       Files.readAllBytes(Paths.get(filename + ".downloaded")))) {
      throw new Exception("[FAILED] Test: presignedGetObject(String bucketName, String objectName,"
                          + " Integer expires, Map<String, String> reqParams)"
                          + ", Error: <Content differs>");
    }

    Files.delete(Paths.get(filename));
    Files.delete(Paths.get(filename + ".downloaded"));
    client.removeObject(bucketName, filename);
  }

  /**
   * public String presignedPutObject(String bucketName, String objectName).
   */
  public static void presignedPutObject_test1() throws Exception {
    System.out.println("Test: presignedPutObject(String bucketName, String objectName)");
    String filename = createFile(3 * MB);
    String urlString = client.presignedPutObject(bucketName, filename);

    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("PUT", RequestBody.create(null, Files.readAllBytes(Paths.get(filename))))
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (!response.isSuccessful()) {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new Scanner(response.body().charStream());
        scanner.useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }
        scanner.close();

        throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName)"
                          + ", Error: <No response from server>");
    }

    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: presignedPutObject(String bucketName, String objectName, Integer expires).
   */
  public static void presignedPutObject_test2() throws Exception {
    System.out.println("Test: presignedPutObject(String bucketName, String objectName, Integer expires)");
    String filename = createFile(3 * MB);
    String urlString = client.presignedPutObject(bucketName, filename, 3600);

    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder
        .url(HttpUrl.parse(urlString))
        .method("PUT", RequestBody.create(null, Files.readAllBytes(Paths.get(filename))))
        .build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (!response.isSuccessful()) {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new Scanner(response.body().charStream());
        scanner.useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }
        scanner.close();

        throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName, Integer expires)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedPutObject(String bucketName, String objectName, Integer expires)"
                          + ", Error: <No response from server>");
    }

    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, filename);
  }

  /**
   * Test: presignedPostPolicy(PostPolicy policy).
   */
  public static void presignedPostPolicy_test() throws Exception {
    System.out.println("Test: presignedPostPolicy(PostPolicy policy)");
    String filename = createFile(3 * MB);
    PostPolicy policy = new PostPolicy(bucketName, filename, DateTime.now().plusDays(7));
    policy.setContentRange(1 * MB, 4 * MB);
    Map<String, String> formData = client.presignedPostPolicy(policy);

    MultipartBuilder multipartBuilder = new MultipartBuilder();
    multipartBuilder.type(MultipartBuilder.FORM);
    for (Map.Entry<String, String> entry : formData.entrySet()) {
      multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
    }
    multipartBuilder.addFormDataPart("file", filename, RequestBody.create(null, new File(filename)));

    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder.url(endpoint + "/" + bucketName).post(multipartBuilder.build()).build();
    OkHttpClient transport = new OkHttpClient();
    Response response = transport.newCall(request).execute();

    if (response != null) {
      if (!response.isSuccessful()) {
        String errorXml = "";

        // read entire body stream to string.
        Scanner scanner = new Scanner(response.body().charStream());
        scanner.useDelimiter("\\A");
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }
        scanner.close();

        throw new Exception("[FAILED] Test: presignedPostPolicy(PostPolicy policy)"
                            + ", Response: " + response
                            + ", Error: " + errorXml);
      }
    } else {
      throw new Exception("[FAILED] Test: presignedPostPolicy(PostPolicy policy)"
                          + ", Error: <No response from server>");
    }

    Files.delete(Paths.get(filename));
    client.removeObject(bucketName, filename);
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
   * Test: copyObject(String bucketName, String objectName, String destBucketName).
   */
  public static void copyObject_test1() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));

    String destBucketName = getRandomName();
    client.makeBucket(destBucketName);
    client.copyObject(bucketName, filename, destBucketName);
    client.getObject(destBucketName, filename, filename + ".downloaded");
    Files.delete(Paths.get(filename + ".downloaded"));

    client.removeObject(bucketName, filename);
    client.removeObject(destBucketName, filename);
    client.removeBucket(destBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to match.
   */
  public static void copyObject_test2() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + "CopyConditions copyConditions) with Matching ETag (Negative Case)");

    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));

    String destBucketName = getRandomName();
    client.makeBucket(destBucketName);

    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETag("TestETag");

    try {
      client.copyObject(bucketName, filename, destBucketName, copyConditions);
    } catch (ErrorResponseException e) {
      ignore();
    }

    client.removeObject(bucketName, filename);
    client.removeBucket(destBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to match.
   */
  public static void copyObject_test3() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + "CopyConditions copyConditions) with Matching ETag (Positive Case)");

    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));

    String destBucketName = getRandomName();
    client.makeBucket(destBucketName);

    ObjectStat stat = client.statObject(bucketName, filename);
    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETag(stat.etag());

    // File should be copied as ETag set in copyConditions matches object's ETag.
    client.copyObject(bucketName, filename, destBucketName, copyConditions);
    client.getObject(destBucketName, filename, filename + ".downloaded");
    Files.delete(Paths.get(filename + ".downloaded"));

    client.removeObject(bucketName, filename);
    client.removeObject(destBucketName, filename);
    client.removeBucket(destBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to not match.
   */
  public static void copyObject_test4() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + "CopyConditions copyConditions) with not matching ETag"
                       + " (Positive Case)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));
    
    String destBucketName = getRandomName();
    client.makeBucket(destBucketName);

    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETagNone("TestETag");

    // File should be copied as ETag set in copyConditions doesn't match object's ETag.
    client.copyObject(bucketName, filename, destBucketName, copyConditions);
    client.getObject(destBucketName, filename, filename + ".downloaded");
    Files.delete(Paths.get(filename + ".downloaded"));

    client.removeObject(bucketName, filename);
    client.removeObject(destBucketName, filename);
    client.removeBucket(destBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with ETag to not match.
   */
  public static void copyObject_test5() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + "CopyConditions copyConditions) with not matching ETag"
                       + " (Negative Case)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));

    String destBucketName = getRandomName();
    client.makeBucket(destBucketName);

    ObjectStat stat = client.statObject(bucketName, filename);
    CopyConditions copyConditions = new CopyConditions();
    copyConditions.setMatchETagNone(stat.etag());

    try {
      client.copyObject(bucketName, filename, destBucketName, copyConditions);
    } catch (ErrorResponseException e) {
      // File should not be copied as ETag set in copyConditions matches object's ETag.
      ignore();
    }

    client.removeObject(bucketName, filename);
    client.removeBucket(destBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with object modified after condition.
   */
  public static void copyObject_test6() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + "CopyConditions copyConditions) with modified after "
                       + "condition (Positive Case)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));

    String destBucketName = getRandomName();
    client.makeBucket(destBucketName);

    CopyConditions copyConditions = new CopyConditions();
    DateTime dateRepresentation = new DateTime(2015, Calendar.MAY, 3, 10, 10);

    copyConditions.setModified(dateRepresentation);

    // File should be copied as object was modified after the set date.
    client.copyObject(bucketName, filename, destBucketName, copyConditions);
    client.getObject(destBucketName, filename, filename + ".downloaded");
    Files.delete(Paths.get(filename + ".downloaded"));

    client.removeObject(bucketName, filename);
    client.removeObject(destBucketName, filename);
    client.removeBucket(destBucketName);
  }

  /**
   * Test: copyObject(String bucketName, String objectName, String destBucketName,
   * CopyConditions copyConditions) with object modified after condition.
   */
  public static void copyObject_test7() throws Exception {
    System.out.println("Test: copyObject(String bucketName, String objectName, String destBucketName,"
                       + "CopyConditions copyConditions) with modified after"
                       + " condition (Negative Case)");
    String filename = createFile(3 * MB);
    client.putObject(bucketName, filename, filename);
    Files.delete(Paths.get(filename));

    String destBucketName = getRandomName();
    client.makeBucket(destBucketName);

    CopyConditions copyConditions = new CopyConditions();
    DateTime dateRepresentation = new DateTime(2015, Calendar.MAY, 3, 10, 10);

    copyConditions.setUnmodified(dateRepresentation);

    try {
      client.copyObject(bucketName, filename, destBucketName, copyConditions);
    } catch (ErrorResponseException e) {
      // File should not be copied as object was modified after date set in copyConditions.
      if (!e.errorResponse().code().equals("PreconditionFailed")) {
        throw e;
      }
    }

    client.removeObject(bucketName, filename);
    // Destination bucket is expected to be empty, otherwise it will trigger an exception.
    client.removeBucket(destBucketName);
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

    System.out.println("Test: setBucketNotification(String bucketName, "
                       + "NotificationConfiguration notificationConfiguration)");

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

    System.out.println("Test: getBucketNotification(String bucketName)");

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

    System.out.println("Test: removeAllBucketNotification(String bucketName)");

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
      System.out.println("FAILED. expected: " + expectedResult + ", got: " + result);
    }

    client.removeBucket(destBucketName);
  }

  /**
   * runTests: runs as much as possible of test combinations.
   */
  public static void runTests() throws Exception {
    makeBucket_test1();
    if (endpoint.toLowerCase().contains("s3")) {
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
    listObject_test5();

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
    }
  }
}
