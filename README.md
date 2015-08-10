# Minio Java Library for Amazon S3 Compatible Cloud Storage [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Minio/minio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Install from maven [![Build Status](https://travis-ci.org/minio/minio-java.svg)](https://travis-ci.org/minio/minio-java)

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>0.2.2</version>
</dependency>
```

## Install from gradle

```gradle
compileDependencies {
    compile 'io.minio:minio:0.2.2'
}
```

## Install from jars

You can download the latest jars directly from maven - [![Maven](https://img.shields.io/maven-central/v/io.minio/minio.svg)](http://repo1.maven.org/maven2/io/minio/minio/0.2.2/)

## Example
```java

package hello.listbuckets;

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

* [ListBuckets.java](./src/main/java/io/minio/examples/ListBuckets.java)
* [ListObjects.java](./src/main/java/io/minio/examples/ListObjects.java)
* [BucketExists.java](./src/main/java/io/minio/examples/BucketExists.java)
* [MakeBucket.java](./src/main/java/io/minio/examples/MakeBucket.java)
* [SetBucketAcl.java](./src/main/java/io/minio/examples/SetBucketAcl.java)
* [GetBucketAcl.java](./src/main/java/io/minio/examples/GetBucketAcl.java)
* [RemoveBucket.java](./src/main/java/io/minio/examples/RemoveBucket.java)

#### Object Operations

* [PutObject.java](./src/main/java/io/minio/examples/PutObject.java)
* [GetObject.Java](./src/main/java/io/minio/examples/GetObject.java)
* [GetPartialObject.java](./src/main/java/io/minio/examples/GetPartialObject.java)
* [RemoveObject.java](./src/main/java/io/minio/examples/RemoveObject.java)
* [StatObject.java](./src/main/java/io/minio/examples/StatObject.java)

### How to run these examples?

#### Step 1: clone the project

```bash
$ git clone https://github.com/minio/minio-java
```

#### Edit examples with your favorite editor

Edit any examples you want to test and fill in access key placeholders

```bash
$ editor src/main/io/minio/examples/ListBuckets.java
```

#### Run the example

```bash
$ ./gradlew copyLibraries
$ java -cp 'lib/*' io.minio.examples.ListBuckets
bucket1
bucket2
....
...
bucketN

```

## Contribute

[Contributors Guide](./CONTRIBUTING.md)
