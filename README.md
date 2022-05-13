# MinIO Java SDK for Amazon S3 Compatible Cloud Storage [![Slack](https://slack.min.io/slack?type=svg)](https://slack.min.io)

MinIO Java SDK is Simple Storage Service (aka S3) client to perform bucket and object operations to any Amazon S3 compatible object storage service.

For a complete list of APIs and examples, please take a look at the [Java Client API Reference](https://docs.min.io/docs/java-client-api-reference) documentation.

## Minimum Requirements
Java 1.8 or above.

## Maven usage
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.4.1</version>
</dependency>
```

## Gradle usage
```
dependencies {
    implementation("io.minio:minio:8.4.1")
}
```

## JAR download
The latest JAR can be downloaded from [here](https://repo1.maven.org/maven2/io/minio/minio/8.4.1/)

## Quick Start Example - File Uploader
This example program connects to an object storage server, makes a bucket on the server and then uploads a file to the bucket.

You need three items in order to connect to an object storage server.

| Parameters | Description                                                |
|------------|------------------------------------------------------------|
| Endpoint   | URL to S3 service.                                         |
| Access Key | Access key (aka user ID) of an account in the S3 service.  |
| Secret Key | Secret key (aka password) of an account in the S3 service. |

This example uses MinIO server playground [https://play.min.io](https://play.min.io). Feel free to use this service for test and development.

### FileUploader.java
```java
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FileUploader {
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException {
    try {
      // Create a minioClient with the MinIO server playground, its access key and secret key.
      MinioClient minioClient =
          MinioClient.builder()
              .endpoint("https://play.min.io")
              .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
              .build();

      // Make 'asiatrip' bucket if not exist.
      boolean found =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket("asiatrip").build());
      if (!found) {
        // Make a new bucket called 'asiatrip'.
        minioClient.makeBucket(MakeBucketArgs.builder().bucket("asiatrip").build());
      } else {
        System.out.println("Bucket 'asiatrip' already exists.");
      }

      // Upload '/home/user/Photos/asiaphotos.zip' as object name 'asiaphotos-2015.zip' to bucket
      // 'asiatrip'.
      minioClient.uploadObject(
          UploadObjectArgs.builder()
              .bucket("asiatrip")
              .object("asiaphotos-2015.zip")
              .filename("/home/user/Photos/asiaphotos.zip")
              .build());
      System.out.println(
          "'/home/user/Photos/asiaphotos.zip' is successfully uploaded as "
              + "object 'asiaphotos-2015.zip' to bucket 'asiatrip'.");
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
      System.out.println("HTTP trace: " + e.httpTrace());
    }
  }
}
```

#### Compile FileUploader
```sh
$ javac -cp minio-8.4.1-all.jar FileUploader.java
```

#### Run FileUploader
```sh
$ java -cp minio-8.4.1-all.jar:. FileUploader
'/home/user/Photos/asiaphotos.zip' is successfully uploaded as object 'asiaphotos-2015.zip' to bucket 'asiatrip'.

$ mc ls play/asiatrip/
[2016-06-02 18:10:29 PDT]  82KiB asiaphotos-2015.zip
```

## More References
* [Java Client API Reference](https://docs.min.io/docs/java-client-api-reference)
* [Javadoc](https://minio-java.min.io/)
* [Examples](https://github.com/minio/minio-java/tree/release/examples)

## Explore Further
* [Complete Documentation](https://docs.min.io)
* [Build your own Photo API Service - Full Application Example ](https://github.com/minio/minio-java-rest-example)

## Contribute
Please refer [Contributors Guide](https://github.com/minio/minio-java/blob/release/CONTRIBUTING.md)
