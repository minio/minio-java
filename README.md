# MinIO Java SDK for Amazon S3 Compatible Cloud Storage [![Slack](https://slack.min.io/slack?type=svg)](https://slack.min.io)

The MinIO Java Client SDK provides simple APIs to access any Amazon S3 compatible object storage server.

This quickstart guide will show you how to install the client SDK and execute an example java program. For a complete list of APIs and examples, please take a look at the [Java Client API Reference](https://docs.min.io/docs/java-client-api-reference) documentation.

## Minimum Requirements
Java 1.8 or above, with one of the following environments:

* [OracleJDK 8.0](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [OpenJDK8.0](https://openjdk.java.net/install/)

## Download from maven
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>7.0.2</version>
</dependency>
```

## Download from gradle
```xml
dependencies {
    compile 'io.minio:minio:7.0.2'
}
```

## Download from JAR
You can download the latest [JAR](https://repo1.maven.org/maven2/io/minio/minio/7.0.2/) directly from maven.

## Quick Start Example - File Uploader
This example program connects to an object storage server, makes a bucket on the server and then uploads a file to the bucket.

You need three items in order to connect to an object storage server.

| Params     | Description |
| :------- | :------------ |
| Endpoint | URL to object storage service. |
| Access Key    | Access key is like user ID that uniquely identifies your account.   |
| Secret Key     | Secret key is the password to your account.    |

For the following example, we will use a freely hosted MinIO server running at [https://play.min.io](https://play.min.io). Feel free to use this service for test and development. Access credentials shown in this example are open to the public.

#### FileUploader.java
```java
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import org.xmlpull.v1.XmlPullParserException;

import io.minio.MinioClient;
import io.minio.errors.MinioException;

public class FileUploader {
  public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeyException, XmlPullParserException {
    try {
      // Create a minioClient with the MinIO Server name, Port, Access key and Secret key.
      MinioClient minioClient = new MinioClient("https://play.min.io", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      // Check if the bucket already exists.
      boolean isExist = minioClient.bucketExists("asiatrip");
      if(isExist) {
        System.out.println("Bucket already exists.");
      } else {
        // Make a new bucket called asiatrip to hold a zip file of photos.
        minioClient.makeBucket("asiatrip");
      }

      // Upload the zip file to the bucket with putObject
      minioClient.putObject("asiatrip","asiaphotos.zip", "/home/user/Photos/asiaphotos.zip", null);
      System.out.println("/home/user/Photos/asiaphotos.zip is successfully uploaded as asiaphotos.zip to `asiatrip` bucket.");
    } catch(MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
```

#### Compile FileUploader
```sh
javac -cp "minio-7.0.2-all.jar"  FileUploader.java
```

#### Run FileUploader
```sh
java -cp "minio-7.0.2-all.jar:." FileUploader
/home/user/Photos/asiaphotos.zip is successfully uploaded as asiaphotos.zip to `asiatrip` bucket.

mc ls play/asiatrip/
[2016-06-02 18:10:29 PDT]  82KiB asiaphotos.zip
```

## API Reference
The full API Reference is available here.

* [Complete API Reference](https://docs.min.io/docs/java-client-api-reference)

### API Reference: Bucket Operations
* [`makeBucket`](https://docs.min.io/docs/java-client-api-reference#makeBucket)
* [`listBuckets`](https://docs.min.io/docs/java-client-api-reference#listBuckets)
* [`bucketExists`](https://docs.min.io/docs/java-client-api-reference#bucketExists)
* [`removeBucket`](https://docs.min.io/docs/java-client-api-reference#removeBucket)
* [`listObjects`](https://docs.min.io/docs/java-client-api-reference#listObjects)
* [`listIncompleteUploads`](https://docs.min.io/docs/java-client-api-reference#listIncompleteUploads)

### API Reference: Object Operations
* [`getObject`](https://docs.min.io/docs/java-client-api-reference#getObject)
* [`putObject`](https://docs.min.io/docs/java-client-api-reference#putObject)
* [`copyObject`](https://docs.min.io/docs/java-client-api-reference#copyObject)
* [`statObject`](https://docs.min.io/docs/java-client-api-reference#statObject)
* [`removeObject`](https://docs.min.io/docs/java-client-api-reference#removeObject)
* [`removeIncompleteUpload`](https://docs.min.io/docs/java-client-api-reference#removeIncompleteUpload)

### API Reference: Presigned Operations
* [`presignedGetObject`](https://docs.min.io/docs/java-client-api-reference#presignedGetObject)
* [`presignedPutObject`](https://docs.min.io/docs/java-client-api-reference#presignedPutObject)
* [`presignedPostPolicy`](https://docs.min.io/docs/java-client-api-reference#presignedPostPolicy)

### API Reference: Bucket Policy Operations
* [`getBucketPolicy`](https://docs.min.io/docs/java-client-api-reference#getBucketPolicy)
* [`setBucketPolicy`](https://docs.min.io/docs/java-client-api-reference#setBucketPolicy)

## Full Examples

#### Full Examples: Bucket Operations
* [ListBuckets.java](https://github.com/minio/minio-java/tree/master/examples/ListBuckets.java)
* [ListObjects.java](https://github.com/minio/minio-java/tree/master/examples/ListObjects.java)
* [BucketExists.java](https://github.com/minio/minio-java/tree/master/examples/BucketExists.java)
* [MakeBucket.java](https://github.com/minio/minio-java/tree/master/examples/MakeBucket.java)
* [RemoveBucket.java](https://github.com/minio/minio-java/tree/master/examples/RemoveBucket.java)
* [ListIncompleteUploads.java](https://github.com/minio/minio-java/tree/master/examples/ListIncompleteUploads.java)

#### Full Examples: Object Operations
* [PutObject.java](https://github.com/minio/minio-java/tree/master/examples/PutObject.java)
* [PutObjectEncrypted.java](https://github.com/minio/minio-java/tree/master/examples/PutObjectEncrypted.java)
* [GetObject.Java](https://github.com/minio/minio-java/tree/master/examples/GetObject.java)
* [GetObjectEncrypted.Java](https://github.com/minio/minio-java/tree/master/examples/GetObjectEncrypted.java)
* [GetPartialObject.java](https://github.com/minio/minio-java/tree/master/examples/GetPartialObject.java)
* [RemoveObject.java](https://github.com/minio/minio-java/tree/master/examples/RemoveObject.java)
* [RemoveObjects.java](https://github.com/minio/minio-java/tree/master/examples/RemoveObjects.java)
* [StatObject.java](https://github.com/minio/minio-java/tree/master/examples/StatObject.java)

#### Full Examples: Presigned Operations
* [PresignedGetObject.java](https://github.com/minio/minio-java/tree/master/examples/PresignedGetObject.java)
* [PresignedPutObject.java](https://github.com/minio/minio-java/tree/master/examples/PresignedPutObject.java)
* [PresignedPostPolicy.java](https://github.com/minio/minio-java/tree/master/examples/PresignedPostPolicy.java)

#### Full Examples: Bucket Policy Operations
* [SetBucketPolicy.java](https://github.com/minio/minio-java/tree/master/examples/SetBucketPolicy.java)
* [GetBucketPolicy.Java](https://github.com/minio/minio-java/tree/master/examples/GetBucketPolicy.java)

#### Full Examples: Server Side Encryption
* [CopyObjectEncrypted.java](https://github.com/minio/minio-java/tree/master/examples/CopyObjectEncrypted.java)
* [CopyObjectEncryptedKms.java](https://github.com/minio/minio-java/tree/master/examples/CopyObjectEncryptedKms.java)
* [CopyObjectEncryptedS3.java](https://github.com/minio/minio-java/tree/master/examples/CopyObjectEncryptedS3.java)
* [PutGetObjectEncrypted.java](https://github.com/minio/minio-java/tree/master/examples/PutGetObjectEncrypted.java)
* [PutObjectEncryptedKms.java](https://github.com/minio/minio-java/tree/master/examples/PutObjectEncryptedKms.java)
* [PutObjectEncryptedS3.java](https://github.com/minio/minio-java/tree/master/examples/PutObjectEncryptedS3.java)

## Explore Further
* [Complete Documentation](https://docs.min.io)
* [MinIO Java Client SDK API Reference](https://docs.min.io/docs/java-client-api-reference)
* [Build your own Photo API Service - Full Application Example ](https://github.com/minio/minio-java-rest-example)

## Contribute
[Contributors Guide](https://github.com/minio/minio-java/blob/master/CONTRIBUTING.md)

[![Build Status](https://travis-ci.org/minio/minio-java.svg)](https://travis-ci.org/minio/minio-java)
[![Build status](https://ci.appveyor.com/api/projects/status/1d05e6nvxcelmrak?svg=true)](https://ci.appveyor.com/project/harshavardhana/minio-java)
