# Minio Java Library for Amazon S3 Compatible Cloud Storage [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Minio/minio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Download from maven

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>0.2.6</version>
</dependency>
```

## Download from gradle

```gradle
dependencies {
    compile 'io.minio:minio:0.2.6'
}
```

## Download from JAR

You can download the latest [JAR](http://repo1.maven.org/maven2/io/minio/minio/0.2.6/) directly from maven.

## Example
```java

import io.minio.MinioClient;
import io.minio.errors.ClientException;
import io.minio.messages.ListAllMyBucketsResult;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class HelloListBuckets {
    public static void main(String[] args) throws IOException, XmlPullParserException, ClientException {
        // Set s3 endpoint, region is calculated automatically
        Client s3client = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

        // list buckets
        Iterator<Bucket> bucketList = s3Client.listBuckets();
        while (bucketList.hasNext()) {
            Bucket bucket = bucketList.next();
            System.out.println(bucket.getName());
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
* [SetBucketAcl.java](./examples/SetBucketAcl.java)
* [GetBucketAcl.java](./examples/GetBucketAcl.java)
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

NOTE: `minio-0.2.6-all.jar` includes all the necessary dependencies to run these examples.

```bash
$ git clone https://github.com/minio/minio-java
$ cd minio-java
[edit examples/ListBuckets.java]
$ cd minio-java/examples; wget http://repo1.maven.org/maven2/io/minio/minio/0.2.6/minio-0.2.6-all.jar;
$ javac -cp 'minio-0.2.6-all.jar' ListBuckets.java
$ java -cp '.:minio-0.2.6-all.jar' ListBuckets
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
