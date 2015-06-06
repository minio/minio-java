# Minimal object storage library in Java [![Build Status](https://travis-ci.org/minio/minio-java.svg)](https://travis-ci.org/minio/minio-java)

## Install from maven

--- TODO ---

## Install from source

```sh
$ git clone https://github.com/minio/minio-java
$ ./gradlew jar
$ ls build/libs/
[2015-06-01 00:26:39 PDT] 1.6MiB minio-java-1.0.jar
```

## Example
```java
import io.minio.client.Client;
import io.minio.client.errors.ClientException;
import io.minio.client.messages.ListAllMyBucketsResult;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class HelloListBuckets {
    public static void main(String[] args) throws IOException, XmlPullParserException, ClientException {
        System.out.println("Hello app");

        // Set s3 endpoint, region is calculated automatically
        Client s3client = Client.getClient("https://s3.amazonaws.com");

        // Set access and secret keys
        s3client.setKeys("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

        // Set a user agent for your app
        s3client.addUserAgent("Example app", "0.1", "amd64");

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

* [ExampleGetObject.Java](./src/test/java/io/minio/examples/ExampleGetObject.java)
* [ExamplePutObject.java](./src/test/java/io/minio/examples/ExamplePutObject.java)
* [ExampleListBuckets.java](./src/test/java/io/minio/examples/ExampleListBuckets.java)
* [ExampleListObjects.java](./src/test/java/io/minio/examples/ExampleListObjects.java)
* [ExampleMakeBucket.java](./src/test/java/io/minio/examples/ExampleMakeBucket.java)
* [ExampleRemoveBucket.java](./src/test/java/io/minio/examples/ExampleRemoveBucket.java)

## Join The Community
* Community hangout on Gitter    [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Minio/minio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
* Ask questions on Quora  [![Quora](http://upload.wikimedia.org/wikipedia/commons/thumb/5/57/Quora_logo.svg/55px-Quora_logo.svg.png)](http://www.quora.com/Minio)

## Contribute

[Contributors Guide](./CONTRIBUTING.md)
