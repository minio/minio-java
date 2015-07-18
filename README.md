# Minimal object storage library in Java [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Minio/minio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Install from maven [![Build Status](https://travis-ci.org/minio/minio-java.svg)](https://travis-ci.org/minio/minio-java)

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>0.2.1</version>
</dependency>
```
[![Maven](https://img.shields.io/maven-central/v/io.minio/minio.svg)](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22minio%22)

## Example
```java
import io.minio.client.Client;
import io.minio.client.errors.ClientException;
import io.minio.client.messages.ListAllMyBucketsResult;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class HelloListBuckets {
    public static void main(String[] args) throws IOException, XmlPullParserException, ClientException {
        // Set s3 endpoint, region is calculated automatically
        Client s3client = Client.getClient("https://s3.amazonaws.com");

        // Set access and secret keys
        s3client.setKeys("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

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

* [ExamplePutObject.java](./src/test/java/io/minio/examples/ExamplePutObject.java)
* [ExampleGetObject.Java](./src/test/java/io/minio/examples/ExampleGetObject.java)
* [ExampleGetPartialObject.java](./src/test/java/io/minio/examples/ExampleGetPartialObject.java)
* [ExampleListBuckets.java](./src/test/java/io/minio/examples/ExampleListBuckets.java)
* [ExampleListObjects.java](./src/test/java/io/minio/examples/ExampleListObjects.java)
* [ExampleMakeBucket.java](./src/test/java/io/minio/examples/ExampleMakeBucket.java)
* [ExampleRemoveBucket.java](./src/test/java/io/minio/examples/ExampleRemoveBucket.java)

## Contribute

[Contributors Guide](./CONTRIBUTING.md)
