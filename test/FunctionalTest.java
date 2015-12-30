/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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
import java.io.*;

import org.xmlpull.v1.XmlPullParserException;
import org.joda.time.DateTime;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Response;
import com.google.common.io.ByteStreams;

import io.minio.*;
import io.minio.messages.*;
import io.minio.errors.*;


public class FunctionalTest {
  public static final int MB = 1024 * 1024;
  public static final SecureRandom random = new SecureRandom();
  public static final String bucketName = getRandomName();
  public static String endpoint;
  public static String accessKey;
  public static String secretKey;
  public static MinioClient client = null;


  public static void println(Object ...args) {
    boolean space = false;

    if (args.length > 0) {
      for (Object arg : args) {
        if (space) {
          System.out.print(" ");
        }
        System.out.print(arg.toString());
        space = true;
      }
    }

    System.out.println();
  }


  public static String createFile(int size) throws IOException {
    String fileName = getRandomName();
    byte[] data = new byte[size];
    random.nextBytes(data);

    OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(fileName), CREATE, APPEND));
    out.write(data, 0, data.length);
    out.close();

    return fileName;
  }


  public static String getRandomName() {
    return "minio-java-test-" + new BigInteger(32, random).toString(32);
  }


  // Test: makeBucket(String bucketName)
  public static void makeBucket_test1() {
    try {
      println("Test: makeBucket(String bucketName)");
      String name = getRandomName();
      client.makeBucket(name);
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: makeBucket(String bucketName, String region)
  public static void makeBucket_test2() {
    try {
      println("Test: makeBucket(String bucketName, String region)");
      String name = getRandomName();
      client.makeBucket(name, "eu-west-1");
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: makeBucket(String bucketName, Acl acl)
  public static void makeBucket_test3() {
    try {
      println("Test: makeBucket(String bucketName, Acl acl)");
      String name = getRandomName();
      client.makeBucket(name, Acl.PUBLIC_READ_WRITE);
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: makeBucket(String bucketName, String region, Acl acl)
  public static void makeBucket_test4() {
    try {
      println("Test: makeBucket(String bucketName, String region, Acl acl)");
      String name = getRandomName();
      client.makeBucket(name, "eu-west-1", Acl.PUBLIC_READ_WRITE);
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: listBuckets()
  public static void listBuckets_test() {
    try {
      println("Test: listBuckets()");
      Iterator<Bucket> bucketIter = client.listBuckets();
      while (bucketIter.hasNext()) {
        bucketIter.next();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: bucketExists(String bucketName)
  public static void bucketExists_test() {
    try {
      println("Test: bucketExists(String bucketName)");
      String name = getRandomName();
      client.makeBucket(name);
      if (!client.bucketExists(name)) {
        println("FAILED");
      }
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: removeBucket(String bucketName)
  public static void removeBucket_test() {
    try {
      println("Test: removeBucket(String bucketName)");
      String name = getRandomName();
      client.makeBucket(name);
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: getBucketAcl(String bucketName)
  public static void getBucketAcl_test() {
    try {
      println("Test: getBucketAcl(String bucketName)");
      String name = getRandomName();
      client.makeBucket(name, Acl.PRIVATE);
      Acl acl = client.getBucketAcl(name);
      if (acl != Acl.PRIVATE) {
        println("FAILED.", "expected =", Acl.PRIVATE, "received =", acl);
      }
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: setBucketAcl(String bucketName, Acl acl)
  public static void setBucketAcl_test() {
    try {
      println("Test: setBucketAcl(String bucketName, Acl acl)");
      String name = getRandomName();
      client.makeBucket(name);
      client.setBucketAcl(name, Acl.PUBLIC_READ_WRITE);
      client.removeBucket(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static void setup() {
    try {
      client.makeBucket(bucketName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static void teardown() {
    try {
      client.removeBucket(bucketName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: putObject(String bucketName, String objectName, String fileName)
  public static void putObject_test1() {
    try {
      println("Test: putObject(String bucketName, String objectName, String fileName)");
      String fileName = createFile(3 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: multipart: putObject(String bucketName, String objectName, String fileName)
  public static void putObject_test2() {
    try {
      println("Test: multipart: putObject(String bucketName, String objectName, String fileName)");
      String fileName = createFile(13 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: multipart resume: putObject(String bucketName, String objectName, String fileName)
  public static void putObject_test3() {
    try {
      println("Test: multipart resume: putObject(String bucketName, String objectName, String fileName)");
      String fileName = createFile(13 * MB);
      InputStream is = Files.newInputStream(Paths.get(fileName));
      try {
        client.putObject(bucketName, fileName, null, 20 * 1024 * 1024, is);
      } catch (InsufficientDataException e) {
      }
      is.close();

      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: putObject(String bucketName, String objectName, String contentType, long size, InputStream body)
  public static void putObject_test4() {
    try {
      println("Test: putObject(String bucketName, String objectName, String contentType, long size, InputStream body)");
      String fileName = createFile(3 * MB);
      InputStream is = Files.newInputStream(Paths.get(fileName));
      client.putObject(bucketName, fileName, null, 1024 * 1024, is);
      is.close();
      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: statObject(String bucketName, String objectName)
  public static void statObject_test() {
    try {
      println("Test: statObject(String bucketName, String objectName)");
      String fileName = createFile(3 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      client.statObject(bucketName, fileName);
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: getObject(String bucketName, String objectName)
  public static void getObject_test1() {
    try {
      println("Test: getObject(String bucketName, String objectName)");
      String fileName = createFile(3 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      InputStream is = client.getObject(bucketName, fileName);
      is.close();
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: getObject(String bucketName, String objectName, long offset)
  public static void getObject_test2() {
    try {
      println("Test: getObject(String bucketName, String objectName, long offset)");
      String fileName = createFile(3 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      InputStream is = client.getObject(bucketName, fileName, 1000L);
      is.close();
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: getObject(String bucketName, String objectName, long offset, Long length)
  public static void getObject_test3() {
    try {
      println("Test: getObject(String bucketName, String objectName, long offset, Long length)");
      String fileName = createFile(3 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      InputStream is = client.getObject(bucketName, fileName, 1000L, 1024 * 1024L);
      is.close();
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: getObject(String bucketName, String objectName, String fileName)
  public static void getObject_test4() {
    try {
      println("Test: getObject(String bucketName, String objectName, String fileName)");
      String fileName = createFile(3 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      client.getObject(bucketName, fileName, fileName + ".downloaded");
      Files.delete(Paths.get(fileName + ".downloaded"));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: listObjects(final String bucketName)
  public static void listObject_test1() {
    try {
      println("Test: listObjects(final String bucketName)");
      String[] fileNames = new String[3];
      for (int i = 0; i < 3; i++) {
        String fileName = createFile(1 * MB);
        client.putObject(bucketName, fileName, fileName);
        Files.delete(Paths.get(fileName));
        fileNames[i] = fileName;
      }

      Iterator<Result<Item>> objectIter = client.listObjects(bucketName);
      for (int i = 0; i < 10; i++) {
        if (objectIter.hasNext()) {
          Result r = objectIter.next();
          println(i, r.getResult());
        } else {
          break;
        }
      }

      for (int i = 0; i < 3; i++) {
        client.removeObject(bucketName, fileNames[i]);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: listObjects(bucketName, final String prefix)
  public static void listObject_test2() {
    try {
      println("Test: listObjects(final String bucketName, final String prefix)");
      String[] fileNames = new String[3];
      for (int i = 0; i < 3; i++) {
        String fileName = createFile(1 * MB);
        client.putObject(bucketName, fileName, fileName);
        Files.delete(Paths.get(fileName));
        fileNames[i] = fileName;
      }

      Iterator<Result<Item>> objectIter = client.listObjects(bucketName, "minio");
      for (int i = 0; i < 10; i++) {
        if (objectIter.hasNext()) {
          Result r = objectIter.next();
          println(i, r.getResult());
        } else {
          break;
        }
      }

      for (int i = 0; i < 3; i++) {
        client.removeObject(bucketName, fileNames[i]);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: listObjects(bucketName, final String prefix, final boolean recursive)
  public static void listObject_test3() {
    try {
      println("Test: listObjects(final String bucketName, final String prefix, final boolean recursive)");
      String[] fileNames = new String[3];
      for (int i = 0; i < 3; i++) {
        String fileName = createFile(1 * MB);
        client.putObject(bucketName, fileName, fileName);
        Files.delete(Paths.get(fileName));
        fileNames[i] = fileName;
      }

      Iterator<Result<Item>> objectIter = client.listObjects(bucketName, "minio", true);
      for (int i = 0; i < 10; i++) {
        if (objectIter.hasNext()) {
          Result r = objectIter.next();
          println(i, r.getResult());
        } else {
          break;
        }
      }

      for (int i = 0; i < 3; i++) {
        client.removeObject(bucketName, fileNames[i]);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: removeObject(String bucketName, String objectName)
  public static void removeObject_test() {
    try {
      println("Test: removeObject(String bucketName, String objectName)");
      String fileName = createFile(3 * MB);
      client.putObject(bucketName, fileName, fileName);
      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: listIncompleteUploads(String bucketName)
  public static void listIncompleteUploads_test1() {
    try {
      println("Test: listIncompleteUploads(String bucketName)");
      String fileName = createFile(6 * MB);
      InputStream is = Files.newInputStream(Paths.get(fileName));
      try {
        client.putObject(bucketName, fileName, null, 9 * 1024 * 1024, is);
      } catch (InsufficientDataException e) {
      }
      is.close();

      Iterator<Result<Upload>> uploadIter = client.listIncompleteUploads(bucketName);
      for (int i = 0; i < 10; i++) {
        if (uploadIter.hasNext()) {
          Result r = uploadIter.next();
          println(i, r.getResult());
        } else {
          break;
        }
      }

      Files.delete(Paths.get(fileName));
      client.removeIncompleteUpload(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: listIncompleteUploads(String bucketName, String prefix)
  public static void listIncompleteUploads_test2() {
    try {
      println("Test: listIncompleteUploads(String bucketName, String prefix)");
      String fileName = createFile(6 * MB);
      InputStream is = Files.newInputStream(Paths.get(fileName));
      try {
        client.putObject(bucketName, fileName, null, 9 * 1024 * 1024, is);
      } catch (InsufficientDataException e) {
      }
      is.close();

      Iterator<Result<Upload>> uploadIter = client.listIncompleteUploads(bucketName, "minio");
      for (int i = 0; i < 10; i++) {
        if (uploadIter.hasNext()) {
          Result r = uploadIter.next();
          println(i, r.getResult());
        } else {
          break;
        }
      }

      Files.delete(Paths.get(fileName));
      client.removeIncompleteUpload(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive)
  public static void listIncompleteUploads_test3() {
    try {
      println("Test: listIncompleteUploads(final String bucketName, final String prefix, final boolean recursive)");
      String fileName = createFile(6 * MB);
      InputStream is = Files.newInputStream(Paths.get(fileName));
      try {
        client.putObject(bucketName, fileName, null, 9 * 1024 * 1024, is);
      } catch (InsufficientDataException e) {
      }
      is.close();

      Iterator<Result<Upload>> uploadIter = client.listIncompleteUploads(bucketName, "minio", true);
      for (int i = 0; i < 10; i++) {
        if (uploadIter.hasNext()) {
          Result r = uploadIter.next();
          println(i, r.getResult());
        } else {
          break;
        }
      }

      Files.delete(Paths.get(fileName));
      client.removeIncompleteUpload(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: removeIncompleteUpload(String bucketName, String objectName)
  public static void removeIncompleteUploads_test() {
    try {
      println("Test: removeIncompleteUpload(String bucketName, String objectName)");
      String fileName = createFile(6 * MB);
      InputStream is = Files.newInputStream(Paths.get(fileName));
      try {
        client.putObject(bucketName, fileName, null, 9 * 1024 * 1024, is);
      } catch (InsufficientDataException e) {
      }
      is.close();

      Iterator<Result<Upload>> uploadIter = client.listIncompleteUploads(bucketName);
      for (int i = 0; i < 10; i++) {
        if (uploadIter.hasNext()) {
          Result r = uploadIter.next();
          println(i, r.getResult());
        } else {
          break;
        }
      }

      Files.delete(Paths.get(fileName));
      client.removeIncompleteUpload(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // public String presignedGetObject(String bucketName, String objectName)
  public static void presignedGetObject_test1() {
    try {
      println("Test: presignedGetObject(String bucketName, String objectName)");
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
          println("FAILED");
        }
      } else {
        println("NO RESPONSE");
      }

      if (!Arrays.equals(Files.readAllBytes(Paths.get(fileName)),
                         Files.readAllBytes(Paths.get(fileName + ".downloaded")))) {
        println("CONTENT DIFFERS");
      }

      Files.delete(Paths.get(fileName));
      Files.delete(Paths.get(fileName + ".downloaded"));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Test: presignedGetObject(String bucketName, String objectName, Integer expires)
  public static void presignedGetObject_test2() {
    try {
      println("Test: presignedGetObject(String bucketName, String objectName, Integer expires)");
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
          println("FAILED");
        }
      } else {
        println("NO RESPONSE");
      }

      if (!Arrays.equals(Files.readAllBytes(Paths.get(fileName)),
                         Files.readAllBytes(Paths.get(fileName + ".downloaded")))) {
        println("CONTENT DIFFERS");
      }

      Files.delete(Paths.get(fileName));
      Files.delete(Paths.get(fileName + ".downloaded"));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // public String presignedPutObject(String bucketName, String objectName)
  public static void presignedPutObject_test1() {
    try {
      println("Test: presignedPutObject(String bucketName, String objectName)");
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
          println("FAILED");
        }
      } else {
        println("NO RESPONSE");
      }

      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: presignedPutObject(String bucketName, String objectName, Integer expires)
  public static void presignedPutObject_test2() {
    try {
      println("Test: presignedPutObject(String bucketName, String objectName, Integer expires)");
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
          println("FAILED");
        }
      } else {
        println("NO RESPONSE");
      }

      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Test: presignedPostPolicy(PostPolicy policy)
  public static void presignedPostPolicy_test() {
    try {
      println("Test: presignedPostPolicy(PostPolicy policy)");
      String fileName = createFile(3 * MB);
      PostPolicy policy = new PostPolicy(bucketName, fileName, DateTime.now().plusDays(7));
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
          println("FAILED");
        }
      } else {
        println("NO RESPONSE");
      }

      Files.delete(Paths.get(fileName));
      client.removeObject(bucketName, fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static void main(String[] args) {
    if (args.length != 3) {
      println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY>");
      return;
    }

    endpoint = args[0];
    accessKey = args[1];
    secretKey = args[2];

    try {
      client = new MinioClient(endpoint, accessKey, secretKey);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    makeBucket_test1();
    // makeBucket_test2(); - throws exception due to Amazon S3 region issue
    makeBucket_test3();
    // makeBucket_test4(); - throws exception due to Amazon S3 region issue

    listBuckets_test();

    bucketExists_test();

    removeBucket_test();

    getBucketAcl_test();

    setBucketAcl_test();

    setup();

    putObject_test1();
    putObject_test2();
    putObject_test3();
    putObject_test4();

    statObject_test();

    getObject_test1();
    getObject_test2();
    getObject_test3();
    getObject_test4();

    listObject_test1();
    listObject_test2();
    listObject_test3();

    removeObject_test();

    listIncompleteUploads_test1();
    listIncompleteUploads_test2();
    listIncompleteUploads_test3();

    removeIncompleteUploads_test();

    presignedGetObject_test1();
    presignedGetObject_test2();

    presignedPutObject_test1();
    presignedPutObject_test2();

    presignedPostPolicy_test();

    teardown();
  }
}
