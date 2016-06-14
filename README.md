# Minio Java Library for Amazon S3 Compatible Cloud Storage [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Minio/minio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

The Minio Java Client SDK provides simple APIs to access any Amazon S3 compatible object storage server.

This quickstart guide will show you how to install the client SDK and execute an example java program. For a complete list of APIs and examples, please take a look at the [Java Client API Reference](http://docs.minio.io/docs/java-client-api-reference) documentation.

This document assumes that you have one of the following Java Environments setup in place.
* [OracleJDK 7.0](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html) or[ OracleJDK 8.0](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
* [OpenJDK7.0](http://openjdk.java.net/install/) or [OpenJDK8.0](http://openjdk.java.net/install/) 

## Download from maven

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Download from gradle

```gradle
dependencies {
    compile 'io.minio:minio:2.0.0'
}
```

## Download from JAR

You can download the latest [JAR](http://repo1.maven.org/maven2/io/minio/minio/2.0.0/) directly from maven.

## Example
```java

import io.minio.MinioClient;
import io.minio.messages.Bucket;
import io.minio.errors.MinioException;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import org.xmlpull.v1.XmlPullParserException;

public class HelloListBuckets {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeyException,     XmlPullParserException, MinioException {
        // Create a s3Client.
        MinioClient s3Client = new MinioClient("s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

        // list buckets
        List<Bucket> bucketList = s3Client.listBuckets();

        for (Bucket bucket : bucketList) {
            System.out.println(bucket.name());
        }
    }
}
```

### Additional Examples

#### Bucket Operations

* [ListBuckets.java](./examples/ListBuckets.java)
* [ListObjects.java](./examples/ListObjects.java)
* [BucketExists.java](./examples/BucketExists.java)
* [MakeBucket.java](./examples/MakeBucket.java)
* [RemoveBucket.java](./examples/RemoveBucket.java)
* [ListIncompleteUploads.java](./examples/ListIncompleteUploads.java)

#### Object Operations

* [PutObject.java](./examples/PutObject.java)
* [GetObject.Java](./examples/GetObject.java)
* [GetPartialObject.java](./examples/GetPartialObject.java)
* [RemoveObject.java](./examples/RemoveObject.java)
* [StatObject.java](./examples/StatObject.java)

#### Presigned Operations
* [PresignedGetObject.java](./examples/PresignedGetObject.java)
* [PresignedPutObject.java](./examples/PresignedPutObject.java)
* [PresignedPostPolicy.java](./examples/PresignedPostPolicy.java)

### How to run these examples?

Simply edit the example java program to include your access credentials and follow the steps below.

NOTE: `minio-2.0.0-all.jar` includes all the necessary dependencies to run these examples.

```bash
$ sudo apt-get install openjdk-7-jdk
$ git clone https://github.com/minio/minio-java
$ cd minio-java
[edit examples/ListBuckets.java]
$ cd minio-java/examples; wget http://repo1.maven.org/maven2/io/minio/minio/2.0.0/minio-2.0.0-all.jar;
$ javac -cp 'minio-2.0.0-all.jar' ListBuckets.java
$ java -cp '.:minio-2.0.0-all.jar' ListBuckets
bucket1
bucket2
....
...
bucketN
```

## Contribute

[Contributors Guide](./CONTRIBUTING.md)

[![Build Status](https://travis-ci.org/minio/minio-java.svg)](https://travis-ci.org/minio/minio-java)
[![Build status](https://ci.appveyor.com/api/projects/status/1d05e6nvxcelmrak?svg=true)](https://ci.appveyor.com/project/harshavardhana/minio-java)
