# Java Client API Reference [![Slack](https://slack.min.io/slack?type=svg)](https://slack.min.io)

## Initialize MinIO Client object.

## MinIO

```java
MinioClient minioClient = new MinioClient("https://play.min.io",
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
```

## AWS S3

```java
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com",
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");
```

| Bucket operations                                             | Object operations                                       |
|---------------------------------------------------------------|---------------------------------------------------------|
| [`bucketExists`](#bucketExists)                               | [`composeObject`](#composeObject)                       |
| [`deleteBucketLifeCycle`](#deleteBucketLifeCycle)             | [`copyObject`](#copyObject)                             |
| [`disableVersioning`](#disableVersioning)                     | [`disableObjectLegalHold`](#disableObjectLegalHold)     |
| [`enableVersioning`](#enableVersioning)                       | [`enableObjectLegalHold`](#enableObjectLegalHold)       |
| [`getBucketLifeCycle`](#getBucketLifeCycle)                   | [`getObject`](#getObject)                               |
| [`getBucketNotification`](#getBucketNotification)             | [`getObjectRetention`](#getObjectRetention)             |
| [`getBucketPolicy`](#getBucketPolicy)                         | [`getObjectUrl`](#getObjectUrl)                         |
| [`getDefaultRetention`](#getDefaultRetention)                 | [`getPresignedObjectUrl`](#getPresignedObjectUrl)       |
| [`listBuckets`](#listBuckets)                                 | [`isObjectLegalHoldEnabled`](#isObjectLegalHoldEnabled) |
| [`listenBucketNotification`](#listenBucketNotification)       | [`listObjects`](#listObjects)                           |
| [`listIncompleteUploads`](#listIncompleteUploads)             | [`presignedGetObject`](#presignedGetObject)             |
| [`makeBucket`](#makeBucket)                                   | [`presignedPostPolicy`](#presignedPostPolicy)           |
| [`removeAllBucketNotification`](#removeAllBucketNotification) | [`presignedPutObject`](#presignedPutObject)             |
| [`removeBucket`](#removeBucket)                               | [`putObject`](#putObject)                               |
| [`removeIncompleteUpload`](#removeIncompleteUpload)           | [`removeObject`](#removeObject)                         |
| [`setBucketLifeCycle`](#setBucketLifeCycle)                   | [`removeObjects`](#removeObjects)                       |
| [`setBucketNotification`](#setBucketNotification)             | [`selectObjectContent`](#selectObjectContent)           |
| [`setBucketPolicy`](#setBucketPolicy)                         | [`setObjectRetention`](#setObjectRetention)             |
| [`setDefaultRetention`](#setDefaultRetention)                 | [`statObject`](#statObject)                             |

## 1. Constructors
|                                                                                                                          |
|--------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-1]_ |
| Creates MinIO client object with given endpoint using anonymous access.                                                  |

|                                                                                                                  |
|------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(URL url) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-2]_ |
| Creates MinIO client object with given url using anonymous access.                                               |

|                                                                                                                               |
|-------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(okhttp3.HttpUrl url) throws  InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-3]_ |
| Creates MinIO client object with given HttpUrl object using anonymous access.                                                 |

|                                                                                                                                                              |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-4]_ |
| Creates MinIO client object with given endpoint, access key and secret key.                                                                                  |

|                                                                                                                                                                         |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, int port,  String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-5]_ |
| Creates MinIO client object with given endpoint, port, access key and secret key using secure (HTTPS) connection.                                                       |

|                                                                                                                                                                              |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, String accessKey, String secretKey, boolean secure) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-6]_ |
| Creates MinIO client object with given endpoint, access key and secret key using secure (HTTPS) connection.                                                                  |

|                                                                                                                                                                                        |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-7]_ |
| Creates MinIO client object using given endpoint, port, access key, secret key and secure option.                                                                                      |

|                                                                                                                                                                  |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(okhttp3.HttpUrl url, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-8]_ |
| Creates MinIO client object with given URL object, access key and secret key.                                                                                    |

|                                                                                                                                                      |
|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(URL url, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-9]_ |
| Creates MinIO client object with given URL object, access key and secret key.                                                                        |

|                                                                                                                                                                              |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, String accessKey, String secretKey, String region) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-10]_ |
| Creates MinIO client object with given URL object, access key and secret key.                                                                                                |

|                                                                                                                                                                                                        |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-11]_ |
| Creates MinIO client object using given endpoint, port, access key, secret key, region and secure option.                                                                                              |

|                                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure, okhttp3.OkHttpClient httpClient) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-12]_ |
| Creates MinIO client object using given endpoint, port, access key, secret key, region and secure option.                                                                                                                               |

__Parameters__

| Parameter    | Type                   | Description                                                                        |
|--------------|------------------------|------------------------------------------------------------------------------------|
| `endpoint`   | _String_               | Endpoint is an URL, domain name, IPv4 address or IPv6 address of S3 service.       |
|              |                        | Examples:                                                                          |
|              |                        | https://s3.amazonaws.com                                                           |
|              |                        | https://play.min.io                                                                |
|              |                        | https://play.min.io:9000                                                           |
|              |                        | localhost                                                                          |
|              |                        | play.min.io                                                                        |
| `url`        | _URL_                  | Endpoint as URL object.                                                            |
| `url`        | _okhttp3.HttpUrl_      | Endpoint as okhttp3.HttpUrl object.                                                |
| `port`       | _int_                  | (Optional) TCP/IP port number. 80 and 443 are used as defaults for HTTP and HTTPS. |
| `accessKey`  | _String_               | (Optional) Access key (aka user ID) of your account in S3 service.                 |
| `secretKey`  | _String_               | (Optional) Secret Key (aka password) of your account in S3 service.                |
| `secure`     | _boolean_              | (Optional) Flag to indicate to use secure connection to S3 service or not.         |
| `region`     | _String_               | (Optional) Region name of buckets in S3 service.                                   |
| `httpClient` | _okhttp3.OkHttpClient_ | (Optional) Custom HTTP client object.                                              |

__Example__

### MinIO

```java
// 1. Create client to S3 service 'play.min.io' at port 443 with TLS security
// for anonymous access.
MinioClient minioClient = new MinioClient("https://play.min.io");

// 2. Create client to S3 service 'play.min.io' at port 443 with TLS security
// using URL object for anonymous access.
MinioClient minioClient = new MinioClient(new URL("https://play.min.io"));

// 3. Create client to S3 service 'play.min.io' at port 9000 with TLS security
// using okhttp3.HttpUrl object for anonymous access.
MinioClient minioClient = new MinioClient(new HttpUrl.parse("https://play.min.io:9000"));

// 4. Create client to S3 service 'play.min.io' at port 443 with TLS security
// for authenticated access.
MinioClient minioClient = new MinioClient("https://play.min.io",
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 5. Create client to S3 service 'play.min.io' at port 9000 with non-TLS security
// for authenticated access.
MinioClient minioClient = new MinioClient("play.min.io", 9000,
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 6. Create client to S3 service 'play.min.io' at port 9000 with TLS security
// for authenticated access.
MinioClient minioClient = new MinioClient("play.min.io", 9000,
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);

// 7. Create client to S3 service 'play.min.io' at port 443 with TLS security
// using URL object for authenticated access.
MinioClient minioClient = new MinioClient(new URL("https://play.min.io"),
     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 8. Create client to S3 service 'play.min.io' at port 443 with TLS security
// using okhttp3.HttpUrl object for authenticated access.
MinioClient minioClient = new MinioClient(HttpUrl.parse("https://play.min.io"),
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 9. Create client to S3 service 'play.min.io' at port 443 with TLS security
// and region 'us-west-1' for authenticated access.
MinioClient minioClient = new MinioClient("https://play.min.io",
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "us-west-1");

// 10. Create client to S3 service 'play.min.io' at port 9000 with TLS security
// and region 'eu-east-1' for authenticated access.
MinioClient minioClient = new MinioClient("play.min.io", 9000,
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "eu-east-1", true);

// 11. Create client to S3 service 'play.min.io' at port 9000 with TLS security,
// region 'eu-east-1' and custom HTTP client for authenticated access.
MinioClient minioClient = new MinioClient("play.min.io", 9000,
    "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "eu-east-1",
    true, customHttpClient);
```

### AWS S3

```java
// 1. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security
// for anonymous access.
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com");

// 2. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security
// using URL object for anonymous access.
MinioClient minioClient = new MinioClient(new URL("https://s3.amazonaws.com"));

// 3. Create client to S3 service 's3.amazonaws.com' at port 9000 with TLS security
// using okhttp3.HttpUrl object for anonymous access.
MinioClient s3Client = new MinioClient(new HttpUrl.parse("https://s3.amazonaws.com"));

// 4. Create client to S3 service 's3.amazonaws.com' at port 80 with TLS security
// for authenticated access.
MinioClient s3Client = new MinioClient("s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 5. Create client to S3 service 's3.amazonaws.com' at port 443 with non-TLS security
// for authenticated access.
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 433, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 6. Create client to S3 service 's3.amazonaws.com' at port 80 with non-TLS security
// for authenticated access.
MinioClient s3Client = new MinioClient("s3.amazonaws.com",
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", false);

// 7. Create client to S3 service 's3.amazonaws.com' at port 80 with TLS security
// for authenticated access.
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 80,
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", true);

// 8. Create client to S3 service 's3.amazonaws.com' at port 80 with non-TLS security
// using URL object for authenticated access.
MinioClient s3Client = new MinioClient(new URL("s3.amazonaws.com"),
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 9. Create client to S3 service 's3.amazonaws.com' at port 80 with non-TLS security
// using okhttp3.HttpUrl object for authenticated access.
MinioClient s3Client = new MinioClient(HttpUrl.parse("s3.amazonaws.com"),
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 10. Create client to S3 service 's3.amazonaws.com' at port 80 with non-TLS security
// and region 'us-west-1' for authenticated access.
MinioClient s3Client = new MinioClient("s3.amazonaws.com",
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "us-west-1");

// 11. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security
// and region 'eu-west-2' for authenticated access.
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 443,
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "eu-west-2", true);

// 12. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security,
// region 'eu-central-1' and custom HTTP client for authenticated access.
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 443,
    "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "eu-central-1", true, customHttpClient);
```

## Common Exceptions
All APIs throw below exceptions in addition to specific to API.

| Exception                  | Cause                                                                |
|:---------------------------|:---------------------------------------------------------------------|
| ErrorResponseException     | Thrown to indicate S3 service returned an error response.            |
| IllegalArgumentException   | Throws to indicate invalid argument is passed.                       |
| InsufficientDataException  | Thrown to indicate not enough data available in InputStream.         |
| InternalException          | Thrown to indicate internal library error.                           |
| InvalidBucketNameException | Thrown to indicate invalid bucket name is passed.                    |
| InvalidKeyException        | Thrown to indicate missing of HMAC SHA-256 library.                  |
| InvalidResponseException   | Thrown to indicate S3 service returned invalid or no error response. |
| IOException                | Thrown to indicate I/O error on S3 operation.                        |
| NoSuchAlgorithmException   | Thrown to indicate missing of MD5 or SHA-256 digest library.         |
| XmlParserException         | Thrown to indicate XML parsing error.                                |

## 2. Bucket operations

<a name="bucketExists"></a>
### bucketExists(String bucketName)
`public boolean bucketExists(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#bucketExists-java.lang.String-)_

Checks if a bucket exists.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                               |
|:--------------------------------------|
| _boolean_: true if the bucket exists. |

__Example__
```java
// Check whether 'my-bucketname' exists or not.
boolean found = minioClient.bucketExists("mybucket");
if (found) {
  System.out.println("mybucket exists");
} else {
  System.out.println("mybucket does not exist");
}
```

<a name="deleteBucketLifeCycle"></a>
### deleteBucketLifeCycle(String bucketName)
`private void deleteBucketLifeCycle(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketLifeCycle-java.lang.String-)_

Delete the lifecycle of the bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

__Example__
```java
minioClient.deleteBucketLifeCycle("my-bucketname");
```

<a name="disableVersioning"></a>
### disableVersioning(String bucketName)
`public void disableVersioning(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#disableVersioning-java.lang.String-)_

Object versioning is disabled in bucketName.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

__Example__
```java
minioClient.disableVersioning("my-bucketname");
```

<a name="enableVersioning"></a>
### enableVersioning(String bucketName)
`public void enableVersioning(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#enableVersioning-java.lang.String-)_

Object versioning is enabled in bucketName.

__Parameters__

| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

__Example__
```java
minioClient.enableVersioning("my-bucketname");
```

<a name="getBucketLifeCycle"></a>
### getBucketLifeCycle(String bucketName)
`public String getBucketLifeCycle(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketLifeCycle-java.lang.String-)_

Get the lifecycle of the bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                     |
|:--------------------------------------------|
| _String_: contains lifecycle configuration. |

__Example__
```java
String lifecycle = minioClient.getBucketLifecycle("my-bucketname");
System.out.println("Life cycle settings: " + lifecycle);
```

<a name="getBucketNotification"></a>
### getBucketNotification(String bucketName)
`public NotificationConfiguration getBucketNotification(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketNotification-java.lang.String-)_

Get bucket notification configuration.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                                          |
|:-----------------------------------------------------------------|
| _[NotificationConfiguration]_: NotificationConfiguration object. |

__Example__
```java
NotificationConfiguration config = minioClient.getBucketNotification("my-bucketname");
System.out.println(config);
```

<a name="getBucketPolicy"></a>
### getBucketPolicy(String bucketName)
`public String getBucketPolicy(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketPolicy-java.lang.String-)_

Get bucket policy for a bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |


| Returns                              |
|:-------------------------------------|
| _String_: Bucket policy JSON string. |

__Example__
```java
String config = minioClient.getBucketPolicy("myBucket");
System.out.println("Bucket policy: " + config);
```

<a name="getDefaultRetention"></a>
### getDefaultRetention(String bucketName)
`public ObjectLockConfiguration getDefaultRetention(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getDefaultRetention-java.lang.String-)_

Get default retention of bucket.

__Parameters__

| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                                      |
|:-------------------------------------------------------------|
| _[ObjectLockConfiguration]_: ObjectLockConfiguration object. |

__Example__
```java
// bucket must be created with object lock enabled.
s3Client.makeBucket("my-bucketname", null, true);
ObjectLockConfiguration config = s3Client.getDefaultRetention("my-bucketname");
System.out.println("Mode: " + config.mode());
System.out.println("Duration: " + config.duration().duration() + " " + config.duration().unit());
```

<a name="listBuckets"></a>
### listBuckets()
`public List<Bucket> listBuckets()` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listBuckets--)_

Lists all buckets.

| Returns                                   |
|:------------------------------------------|
| _List<[Bucket]>_ : List of Bucket object. |

__Example__
```java
List<Bucket> bucketList = minioClient.listBuckets();
for (Bucket bucket : bucketList) {
  System.out.println(bucket.creationDate() + ", " + bucket.name());
}
```

<a name="listenBucketNotification"></a>
### listenBucketNotification(String bucketName, String prefix, String suffix, String[] events)
`public CloseableIterator<Result<NotificationRecords>> listenBucketNotification(String bucketName, String prefix, String suffix, String[] events)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listenBucketNotification-java.lang.String-java.lang.String-java.lang.String-java.lang.String:A-)_

Listen events of object prefix and suffix in bucket.

__Parameters__
| Parameter      | Type       | Description                                 |
|:---------------|:-----------|:--------------------------------------------|
| ``bucketName`` | _String_   | Name of the bucket.                         |
| ``prefix``     | _String_   | Listen events of object starts with prefix. |
| ``suffix``     | _String_   | Listen events of object ends with suffix.   |
| ``events``     | _String[]_ | Events to listen.                           |

| Returns                                                                                                  |
|:---------------------------------------------------------------------------------------------------------|
| _[CloseableIterator]<[Result]<[NotificationRecords]>>_: closable iterator of Result NotificationRecords. |

__Example__
```java
String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
try (CloseableIterator<Result<NotificationInfo>> ci =
    minioClient.listenBucketNotification("bcketName", "", "", events)) {
  while (ci.hasNext()) {
    NotificationRecords records = ci.next().get();
    for (Event event : records.events()) {
      System.out.println("Event " + event.eventType() + " occurred at " + event.eventTime()
          + " for " + event.bucketName() + "/" + event.objectName());
    }
  }
}
```

<a name="listIncompleteUploads"></a>
### listIncompleteUploads(String bucketName)
`public Iterable<Result<Upload>> listIncompleteUploads(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-)_

Lists partially uploaded objects in a bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                                |
|:-------------------------------------------------------|
| _Iterable<[Result]<[Upload]>>_: an iterator of Upload. |

__Example__
```java
Iterable<Result<Upload>> results = minioClient.listIncompleteUploads("my-bucketname");
for (Result<Upload> result : results) {
  Upload upload = result.get();
  System.out.println(upload.uploadId() + ", " + upload.objectName());
}
```

<a name="listIncompleteUploads"></a>
### listIncompleteUploads(String bucketName, String prefix)
`public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-java.lang.String-)_

Lists incomplete uploads of objects in given bucket and prefix.

__Parameters__
| Parameter      | Type     | Description                      |
|:---------------|:---------|:---------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.              |
| ``prefix``     | _String_ | List objects starts with prefix. |

| Returns                                                |
|:-------------------------------------------------------|
| _Iterable<[Result]<[Upload]>>_: an iterator of Upload. |

__Example__
```java
Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("my-bucketname", "my-obj");
for (Result<Upload> result : results) {
  Upload upload = result.get();
  System.out.println(upload.uploadId() + ", " + upload.objectName());
}
```

<a name="listIncompleteUploads"></a>
### listIncompleteUploads(String bucketName, String prefix, boolean recursive)
`public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix, boolean recursive)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-java.lang.String-boolean-)_

Lists partially uploaded objects in a bucket.

__Parameters__
| Param          | Type      | Description                                                    |
|:---------------|:----------|:---------------------------------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                                            |
| ``prefix``     | _String_  | List objects starts with prefix.                               |
| ``recursive``  | _boolean_ | List objects recursively; else emulates a directory structure. |

| Returns                                                |
|:-------------------------------------------------------|
| _Iterable<[Result]<[Upload]>>_: an iterator of Upload. |

__Example__
```java
Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("my-bucketname", "my-obj", true);
for (Result<Upload> result : results) {
  Upload upload = result.get();
  System.out.println(upload.uploadId() + ", " + upload.objectName());
}
```

<a name="listObjects"></a>
### listObjects(String bucketName)
`public Iterable<Result<Item>> listObjects(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-)_

Lists object information in given bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                                    |
|:-----------------------------------------------------------|
| _Iterable<[Result]<[Item]>>_: an iterator of Result Items. |

__Example__
```java
Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname");
for (Result<Item> result : results) {
  Item item = result.get();
  System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
}
```

<a name="listObjects"></a>
### listObjects(String bucketName, String prefix)
`public Iterable<Result<Item>> listObjects(String bucketName, String prefix))` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-)_

Lists object information in given bucket and prefix.

__Parameters__
| Parameter      | Type     | Description                      |
|:---------------|:---------|:---------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.              |
| ``prefix``     | _String_ | List objects starts with prefix. |

| Returns                                                    |
|:-----------------------------------------------------------|
| _Iterable<[Result]<[Item]>>_: an iterator of Result Items. |

__Example__
```java
Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj");
for (Result<Item> result : results) {
  Item item = result.get();
  System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
}
```

<a name="listObjects"></a>
### listObjects(String bucketName, String prefix, boolean recursive)
`public Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-boolean-)_

Lists object information as Iterable<Result><Item> in given bucket, prefix and recursive flag.

__Parameters__
| Parameter      | Type      | Description                                                    |
|:---------------|:----------|:---------------------------------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                                            |
| ``prefix``     | _String_  | List objects starts with prefix.                               |
| ``recursive``  | _boolean_ | List objects recursively; else emulates a directory structure. |

| Returns                                                    |
|:-----------------------------------------------------------|
| _Iterable<[Result]<[Item]>>_: an iterator of Result Items. |

__Example__
```java
Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj", true);
for (Result<Item> result : results) {
  Item item = result.get();
  System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
}
```

<a name="listObjects"></a>
### listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)
`public Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-boolean-boolean-)_

Lists all objects in a bucket.

__Parameters__
| Parameter       | Type      | Description                                                    |
|:----------------|:----------|:---------------------------------------------------------------|
| ``bucketName``  | _String_  | Name of the bucket.                                            |
| ``prefix``      | _String_  | List objects starts with prefix.                               |
| ``recursive``   | _boolean_ | List objects recursively; else emulates a directory structure. |
| ``useVersion1`` | _boolean_ | when true, version 1 of REST API is used.                      |

| Returns                                                    |
|:-----------------------------------------------------------|
| _Iterable<[Result]<[Item]>>_: an iterator of Result Items. |

__Example__
```java
Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj", true, true);
for (Result<Item> result : results) {
  Item item = result.get();
  System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
}
```

<a name="makeBucket"></a>
### makeBucket(String bucketName)
`public void makeBucket(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-java.lang.String-)_

Creates a new bucket with default region.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

__Example__
```java
minioClient.makeBucket("my-bucketname");
```

<a name="makeBucket"></a>
### makeBucket(String bucketName, String region)
`public void makeBucket(String bucketName, String region)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-java.lang.String-java.lang.String-)_

Creates a new bucket with given region.

__Parameters__
| Parameter      | Type     | Description                                 |
|:---------------|:---------|:--------------------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.                         |
| ``region``     | _String_ | Region in which the bucket will be created. |

__Example__
```java
minioClient.makeBucket("my-bucketname", "eu-west-1");
```

<a name="makeBucket"></a>
### makeBucket(String bucketName, String region, boolean objectLock)
`public void makeBucket(String bucketName, String region, boolean objectLock)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-java.lang.String-java.lang.String-boolean-)_

Creates a new bucket with object lock functionality enabled.

__Parameters__
| Parameter      | Type      | Description                                    |
|:---------------|:----------|:-----------------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                            |
| ``region``     | _String_  | Region in which the bucket will be created.    |
| ``objectLock`` | _boolean_ | Create bucket with object lock feature or not. |

__Example__
```java
minioClient.makeBucket("my-bucketname", "us-west-2", true);
```

<a name="removeAllBucketNotification"></a>
### removeAllBucketNotification(String bucketName)
`public void removeAllBucketNotification(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeAllBucketNotification-java.lang.String-)_

Remove all notification configuration from a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeAllBucketNotification-java.lang.String)

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

__Example__
```java
minioClient.removeAllBucketNotification("my-bucketname");
```

<a name="removeBucket"></a>
### removeBucket(String bucketName)
`public void removeBucket(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeBucket-java.lang.String-)_

Removes an empty bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

__Example__
```java
minioClient.removeBucket("my-bucketname");
```

<a name="removeIncompleteUpload"></a>
### removeIncompleteUpload(String bucketName, String objectName)
`public void removeIncompleteUpload(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeIncompleteUpload-java.lang.String-java.lang.String-)_

Removes a partially uploaded object.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

__Example__
```java
minioClient.removeIncompleteUpload("my-bucketname", "my-objectname");
```

<a name="setBucketLifeCycle"></a>
### setBucketLifeCycle(String bucketName, String lifeCycle)
`public void setBucketLifeCycle(String bucketName, String lifeCycle)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketLifeCycle-java.lang.String-java.lang.String-)_

Set a life cycle on bucket.

__Parameters__
| Parameter      | Type     | Description                            |
|:---------------|:---------|:---------------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.                    |
| ``lifeCycle``  | _String_ | Life cycle configuraion as XML string. |

__Example__
```java
// Lets consider variable 'lifeCycleXml' contains below XML String;
// <LifecycleConfiguration>
//   <Rule>
//     <ID>expire-bucket</ID>
//     <Prefix></Prefix>
//     <Status>Enabled</Status>
//     <Expiration>
//       <Days>365</Days>
//     </Expiration>
//   </Rule>
// </LifecycleConfiguration>
//
minioClient.setBucketLifecycle("my-bucketname", lifeCycleXml);
```

<a name="setBucketNotification"></a>
### setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)
`public void setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketNotification-java.lang.String-io.minio.messages.NotificationConfiguration-)_

Set bucket notification configuration.

__Parameters__

| Parameter                     | Type                          | Description                           |
|:------------------------------|:------------------------------|:--------------------------------------|
| ``bucketName``                | _String_                      | Name of the bucket.                   |
| ``notificationConfiguration`` | _[NotificationConfiguration]_ | Notification configuration to be set. |

__Example__
```java
List<EventType> eventList = new LinkedList<>();
eventList.add(EventType.OBJECT_CREATED_PUT);
eventList.add(EventType.OBJECT_CREATED_COPY);

QueueConfiguration queueConfiguration = new QueueConfiguration();
queueConfiguration.setQueue("arn:minio:sqs::1:webhook");
queueConfiguration.setEvents(eventList);
queueConfiguration.setPrefixRule("images");
queueConfiguration.setSuffixRule("pg");

List<QueueConfiguration> queueConfigurationList = new LinkedList<>();
queueConfigurationList.add(queueConfiguration);

NotificationConfiguration config = new NotificationConfiguration();
config.setQueueConfigurationList(queueConfigurationList);

minioClient.setBucketNotification("my-bucketname", config);
```

<a name="setBucketPolicy"></a>
### setBucketPolicy(String bucketName, String policy)
`public void setBucketPolicy(String bucketName, String policy)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketPolicy-java.lang.String-java.lang.String-)_

Set a policy on bucket.

__Parameters__

| Parameter      | Type     | Description                   |
|:---------------|:---------|:------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.           |
| ``policy``     | _String_ | Bucket policy as JSON string. |

__Example__
```java
// Assume policyJson contains below JSON string;
// {
//     "Statement": [
//         {
//             "Action": [
//                 "s3:GetBucketLocation",
//                 "s3:ListBucket"
//             ],
//             "Effect": "Allow",
//             "Principal": "*",
//             "Resource": "arn:aws:s3:::my-bucketname"
//         },
//         {
//             "Action": "s3:GetObject",
//             "Effect": "Allow",
//             "Principal": "*",
//             "Resource": "arn:aws:s3:::my-bucketname/myobject*"
//         }
//     ],
//     "Version": "2012-10-17"
// }
//
minioClient.setBucketPolicy("my-bucketname", policyJson);
```

<a name="setDefaultRetention"></a>
### setDefaultRetention(String bucketName, ObjectLockConfiguration config)
`public void setDefaultRetention(String bucketName, ObjectLockConfiguration config)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setDefaultRetention-java.lang.String-io.minio.messages.ObjectLockConfiguration-)_

Set default retention on bucket.

__Parameters__
| Parameter      | Type                        | Description                |
|:---------------|:----------------------------|:---------------------------|
| ``bucketName`` | _String_                    | Name of the bucket.        |
| ``config``     | _[ObjectLockConfiguration]_ | Object lock Configuration. |

__Example__
```java
ObjectLockConfiguration config =
    new ObjectLockConfiguration(RetentionMode.COMPLIANCE, new RetentionDurationDays(100));
minioClient.setDefaultRetention("my-bucketname", config);
```

## 3. Object operations

 <a name="composeObject"></a>
### composeObject(String bucketName, String objectName, List<ComposeSource> sources, Map<String,String> headerMap, ServerSideEncryption sse)
`public void composeObject(String bucketName, String objectName, List<ComposeSource> sources, Map<String,String> headerMap, ServerSideEncryption sse)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#composeObject-java.lang.String-java.lang.String-java.util.List-java.util.Map-io.minio.ServerSideEncryption-)_

 Creates a new Object by combining different source objects.

 __Parameters__
| Param          | Type                     | Description                        |
|:---------------|:-------------------------|:-----------------------------------|
| ``bucketName`` | _String_                 | Name of the bucket.                |
| ``objectName`` | _String_                 | Object name to be created.         |
| ``sources``    | _List<[ComposeSource]>_  | List of compose sources.           |
| ``headerMap``  | _Map<String,String>_     | (Optional) User metadata.          |
| ``sse``        | _[ServerSideEncryption]_ | (Optional) Server-side encryption. |

__Example__
 ```java
List<ComposeSource> sourceObjectList = new ArrayList<ComposeSource>();
sourceObjectList.add(new ComposeSource("my-job-bucket", "my-objectname-part-one"));
sourceObjectList.add(new ComposeSource("my-job-bucket", "my-objectname-part-two"));
sourceObjectList.add(new ComposeSource("my-job-bucket", "my-objectname-part-three"));
minioClient.composeObject("my-bucketname", "my-objectname", sourceObjectList, null, null);
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, Map<String,String> headerMap, ServerSideEncryption sse, String srcBucketName, String srcObjectName, ServerSideEncryption srcSse, CopyConditions copyConditions)
`public void copyObject(String bucketName, String objectName, Map<String,String> headerMap, ServerSideEncryption sse, String srcBucketName, String srcObjectName, ServerSideEncryption srcSse, CopyConditions copyConditions)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.util.Map-io.minio.ServerSideEncryption-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-io.minio.CopyConditions-)_

Create object by copying from source object in source bucket.

__Parameters__
| Parameter          | Type                     | Description                                |
|:-------------------|:-------------------------|:-------------------------------------------|
| ``bucketName``     | _String_                 | Bucket name.                               |
| ``objectName``     | _String_                 | Object name to be created.                 |
| ``headerMap``      | _Map<String,String>_     | (Optional) User metadata.                  |
| ``sse``            | _[ServerSideEncryption]_ | (Optional) Server-side encryption.         |
| ``srcBucketName``  | _String_                 | Source bucket name.                        |
| ``srcObjectName``  | _String_                 | (Optional) Source object name.             |
| ``srcSse``         | _[ServerSideEncryption]_ | (Optional) Source Server-side encryption.  |
| ``copyConditions`` | _[CopyConditions]_       | (Optional) Conditiions to be used to copy. |

__Example__

```java
minioClient.copyObject("my-bucketname", "my-objectname", null, null, "my-source-bucketname", null, null, null);
```

<a name="disableObjectLegalHold"></a>
### disableObjectLegalHold(String bucketName, String objectName, String versionId)
`public void disableObjectLegalHold(String bucketName, String objectName, String versionId)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#disableObjectLegalHold-java.lang.String-java.lang.String-java.lang.String-)_

Disables legal hold on an object.

 __Parameters__
| Parameter      | Type     | Description               |
|:---------------|:---------|:--------------------------|
| ``bucketName`` | _String_ | Bucket name.              |
| ``objectName`` | _String_ | Object name.              |
| ``versionId``  | _String_ | Version ID of the object. |

 __Example__
```java
minioClient.disableObjectLegalHold("my-bucketname", "my-objectName", null);
```

<a name="enableObjectLegalHold"></a>
### enableObjectLegalHold(String bucketName, String objectName, String versionId)
`public void enableObjectLegalHold(String bucketName, String objectName, String versionId)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#enableObjectLegalHold-java.lang.String-java.lang.String-java.lang.String-)_

Enables legal hold on an object.

 __Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name to be created. |
| ``versionId``  | _String_ | Version ID of the object.  |

 __Example__
 ```java
minioClient.enableObjectLegalHold("my-bucketname", "my-objectname", null);
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName)
`public InputStream getObject(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-)_

Downloads an object as a stream.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns                                            |
|:---------------------------------------------------|
| _InputStream_: InputStream containing object data. |

__Example__
```java
InputStream stream = minioClient.getObject("my-bucketname", "my-objectname");
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, long offset)
`public InputStream getObject(String bucketName, String objectName, long offset)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-)_

Gets object's data starting from given offset as InputStream in the given bucket. The InputStream must be closed after use else the connection will remain open.

__Parameters__
| Parameter      | Type     | Description                         |
|:---------------|:---------|:------------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.                 |
| ``objectName`` | _String_ | Object name in the bucket.          |
| ``offset``     | _long_   | Start byte position of object data. |

| Returns                                            |
|:---------------------------------------------------|
| _InputStream_: InputStream containing object data. |

__Example__
```java
InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L);
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, long offset, Long length)
`public InputStream getObject(String bucketName,  String objectName, long offset, Long length)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-java.lang.Long-)_

Downloads the specified range bytes of an object as a stream.

__Parameters__
| Parameter      | Type     | Description                                            |
|:---------------|:---------|:-------------------------------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.                                    |
| ``objectName`` | _String_ | Object name in the bucket.                             |
| ``offset``     | _long_   | Start byte position of object data.                    |
| ``length``     | _Long_   | (Optional) Number of bytes of object data from offset. |

| Returns                                            |
|:---------------------------------------------------|
| _InputStream_: InputStream containing object data. |

__Example__
```java
InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L);
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, ServerSideEncryption sse)
`public InputStream getObject(String bucketName, String objectName, ServerSideEncryption sse)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-)_

Gets entire server-side encrypted object's data as InputStream in given bucket. The InputStream must be closed after use else the connection will remain open.

__Parameters__
| Parameter      | Type                     | Description                |
|:---------------|:-------------------------|:---------------------------|
| ``bucketName`` | _String_                 | Name of the bucket.        |
| ``objectName`` | _String_                 | Object name in the bucket. |
| ``sse``        | _[ServerSideEncryption]_ | Server-side encryption.    |

| Returns                                            |
|:---------------------------------------------------|
| _InputStream_: InputStream containing object data. |

__Example__
```java
InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", sse);
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, Long offset, Long length, ServerSideEncryption sse)
`public InputStream getObject(String bucketName, String objectName, Long offset, Long length, ServerSideEncryption sse)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-java.lang.Long-java.lang.Long-io.minio.ServerSideEncryption-)_

Gets specified range bytes of server-side encrypted object's data as InputStream in given bucket. The InputStream must be closed after use else the connection will remain open.

__Parameters__
| Parameter      | Type                     | Description                                            |
|:---------------|:-------------------------|:-------------------------------------------------------|
| ``bucketName`` | _String_                 | Name of the bucket.                                    |
| ``objectName`` | _String_                 | Object name in the bucket.                             |
| ``offset``     | _long_                   | Start byte position of object data.                    |
| ``length``     | _Long_                   | (Optional) Number of bytes of object data from offset. |
| ``sse``        | _[ServerSideEncryption]_ | (Optional) Server-side encryption.                     |

| Returns                                            |
|:---------------------------------------------------|
| _InputStream_: InputStream containing object data. |

__Example__
```java
InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L, sse);
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, String fileName)
`public void getObject(String bucketName, String objectName, String fileName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-java.lang.String-)_

Downloads object in bucket to given file name.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |
| ``fileName``   | _String_ | File name.                 |

__Example__
```java
minioClient.getObject("my-bucketname", "my-objectname", "my-object-file");
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, ServerSideEncryption sse, String fileName)
`public void getObject(String bucketName, String objectName, ServerSideEncryption sse, String fileName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-java.lang.String-)_

Downloads server-side encrypted object in bucket to given file name.

__Parameters__
| Parameter      | Type                     | Description                |
|:---------------|:-------------------------|:---------------------------|
| ``bucketName`` | _String_                 | Name of the bucket.        |
| ``objectName`` | _String_                 | Object name in the bucket. |
| ``sse``        | _[ServerSideEncryption]_ | Server-side encryption.    |
| ``fileName``   | _String_                 | File name.                 |

__Example__
```java
minioClient.getObject("my-bucketname", "my-objectname", sse, "my-object-file");
```

 <a name="getObjectRetention"></a>
### getObjectRetention(String bucketName, String objectName, String versionId)
`public Retention getObjectRetention(String bucketName, String objectName, String versionId)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObjectRetention-java.lang.String-java.lang.String-java.lang.String-)_

Returns retention set on a given object.

 __Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |
| ``versionId``  | _String_ | Object Version.            |

| Returns                         |
|:--------------------------------|
| _[Retention]_: Retention object |

 __Example__
 ```java
Retention retention = s3Client.getObjectRetention("my-bucketname", "my-objectname", null);
System.out.println("mode: " + retention.mode() + "until: " + retention.retainUntilDate());
```

 <a name="getObjectUrl"></a>
### getObjectUrl(String bucketName, String objectName)
`public String getObjectUrl(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObjectUrl-java.lang.String-java.lang.String-)_

Gets object's URL in given bucket.

 __Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns               |
|:----------------------|
| _String_: URL string. |

 __Example__
 ```java
String url = client.getObjectUrl("my-bucketname", "my-objectname");
System.out.println("my-bucketname/my-objectname can be downloaded by " + url);
```

 <a name="getPresignedObjectUrl"></a>
### getPresignedObjectUrl(Method method, String bucketName, String objectName, Integer expires, Map<String,String> reqParams)
`public String getPresignedObjectUrl(Method method, String bucketName, String objectName, Integer expires, Map<String,String> reqParams)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getPresignedObjectUrl-io.minio.http.Method-java.lang.String-java.lang.String-java.lang.Integer-java.util.Map-)_

Returns a presigned URL string with given HTTP method, expiry time and custom request params for a specific object in the bucket.

 __Parameters__
| Parameter      | Type                 | Description                            |
|:---------------|:---------------------|:---------------------------------------|
| ``method``     | _Method_             | HTTP method to generate presigned URL. |
| ``bucketName`` | _String_             | Name of the bucket.                    |
| ``objectName`` | _String_             | Object name in the bucket.             |
| ``expiry``     | _Integer_            | Expiry in seconds; defaults to 7 days. |
| ``reqParams``  | _Map<String,String>_ | Request parameters to override.        |

| Returns               |
|:----------------------|
| _String_: URL string. |

 __Example__
 ```java
String url = getPresignedObjectUrl(Method.PUT, "my-bucketname", null, null, null);
System.out.println("my-bucketname can be created by " + url);
```
 <a name="isObjectLegalHoldEnabled"></a>
### isObjectLegalHoldEnabled(String bucketName, String objectName, String versionId)
`public boolean isObjectLegalHoldEnabled(String bucketName, String objectName, String versionId)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#isObjectLegalHoldEnabled-java.lang.String-java.lang.String-java.lang.String-)_

Returns true if object legal hold is enabled.

 __Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |
| ``versionId``  | _String_ | Object Version.            |

| Returns                                   |
|:------------------------------------------|
| _boolean_: true if legal hold is enabled. |

 __Example__
```java
boolean status = s3Client.isObjectLegalHoldEnabled("my-bucketname", "my-objectname", null);
if (status) {
  System.out.println("Legal hold is on");
else {
  System.out.println("Legal hold is off");
}
```

<a name="presignedGetObject"></a>
### presignedGetObject(String bucketName, String objectName)
`public String presignedGetObject(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-)_

Returns an presigned URL to download the object in the bucket with default expiry time. Default expiry time is 7 days in seconds.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns                                      |
|:---------------------------------------------|
| _String_: URL string to download the object. |

__Example__
```java
String url = minioClient.presignedGetObject("my-bucketname", "my-objectname");
System.out.println("my-bucketname/my-objectname can be downloaded by " + url);
```


<a name="presignedGetObject"></a>
### presignedGetObject(String bucketName, String objectName, Integer expires)
`public String presignedGetObject(String bucketName, String objectName, Integer expires)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-)_

Generates a presigned URL for HTTP GET operations. Browsers/Mobile clients may point to this URL to directly download objects even if the bucket is private. This presigned URL can have an associated expiration time in seconds after which it is no longer operational. The default expiry is set to 7 days.

__Parameters__
| Parameter      | Type      | Description                                         |
|:---------------|:----------|:----------------------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                                 |
| ``objectName`` | _String_  | Object name in the bucket.                          |
| ``expiry``     | _Integer_ | Expiry in seconds. Default expiry is set to 7 days. |

| Returns                                      |
|:---------------------------------------------|
| _String_: URL string to download the object. |

__Example__
```java
String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24);
System.out.println("my-bucketname/my-objectname can be downloaded by " + url);
```

<a name="presignedGetObject"></a>
### presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,String> reqParams)
`public String presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,String> reqParams)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-java.util.Map-)_

Returns an presigned URL to download the object in the bucket with given expiry time with custom request params.

__Parameters__
| Parameter      | Type                 | Description                            |
|:---------------|:---------------------|:---------------------------------------|
| ``bucketName`` | _String_             | Name of the bucket.                    |
| ``objectName`` | _String_             | Object name in the bucket.             |
| ``expiry``     | _Integer_            | Expiry in seconds; defaults to 7 days. |
| ``reqParams``  | _Map<String,String>_ | Request parameters to override.        |

| Returns                                      |
|:---------------------------------------------|
| _String_: URL string to download the object. |

__Example__
```java
String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24, reqParams);
System.out.println("my-bucketname/my-objectname can be downloaded by " + url);
```

<a name="presignedPostPolicy"></a>
### presignedPostPolicy(PostPolicy policy)
`public Map<String,String> presignedPostPolicy(PostPolicy policy)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPostPolicy-io.minio.PostPolicy-)_

Allows setting policy conditions to a presigned URL for POST operations. Policies such as bucket name to receive object uploads, key name prefixes, expiry policy may be set. The client receives HTTP status code under key success_action_status, when the file is uploaded successfully. If its value is set to 201, the client notifies with a XML document containing the key where the file was uploaded to.

__Parameters__
| Parameter  | Type           | Description               |
|:-----------|:---------------|:--------------------------|
| ``policy`` | _[PostPolicy]_ | Post policy of an object. |


| Returns                                                         |
|:----------------------------------------------------------------|
| _Map<String, String>_: key/value pairs to construct form-data. |

__Example__
```java
PostPolicy policy = new PostPolicy("my-bucketname", "my-objectname", ZonedDateTime.now().plusDays(7));

// 'my-objectname' should be 'image/png' content type
policy.setContentType("image/png");

// set success action status to 201 to receive XML document
policy.setSuccessActionStatus(201);

Map<String,String> formData = minioClient.presignedPostPolicy(policy);

// Print curl command to be executed by anonymous user to upload /tmp/userpic.png.
System.out.print("curl -X POST ");
for (Map.Entry<String,String> entry : formData.entrySet()) {
  System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
}
System.out.println(" -F file=@/tmp/userpic.png https://play.min.io/my-bucketname");
```

<a name="presignedPutObject"></a>
### presignedPutObject(String bucketName, String objectName)
`public String presignedPutObject(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPutObject-java.lang.String-java.lang.String-)_

Returns a presigned URL to upload an object in the bucket with default expiry time. Default expiry time is 7 days in seconds.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns                                   |
|:------------------------------------------|
| _String_: URL string to upload an object. |

__Example__
```java
String url = minioClient.presignedPutObject("my-bucketname", "my-objectname");
System.out.println("my-bucketname/my-objectname can be uploaded by " + url);
```

<a name="presignedPutObject"></a>
### presignedPutObject(String bucketName, String objectName, Integer expires)
`public String presignedPutObject(String bucketName, String objectName, Integer expires)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPutObject-java.lang.String-java.lang.String-java.lang.Integer-)_

Generates a presigned URL for HTTP PUT operations. Browsers/Mobile clients may point to this URL to upload objects directly to a bucket even if it is private. This presigned URL can have an associated expiration time in seconds after which it is no longer operational. The default expiry is set to 7 days.

__Parameters__
| Parameter      | Type      | Description                            |
|:---------------|:----------|:---------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                    |
| ``objectName`` | _String_  | Object name in the bucket.             |
| ``expiry``     | _Integer_ | Expiry in seconds; defaults to 7 days. |

| Returns                                   |
|:------------------------------------------|
| _String_: URL string to upload an object. |

__Example__
```java
String url = minioClient.presignedPutObject("my-bucketname", "my-objectname", 60 * 60 * 24);
System.out.println("my-bucketname/my-objectname can be uploaded by " + url);
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)
`public void putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-io.minio.PutObjectOptions-)_

Uploads given stream as object in bucket by using given options.

__Parameters__
| Parameter      | Type                 | Description                    |
|:---------------|:---------------------|:-------------------------------|
| ``bucketName`` | _String_             | Name of the bucket.            |
| ``objectName`` | _String_             | Object name in the bucket.     |
| ``stream``     | _InputStream_        | Stream contains object data.   |
| ``options``    | _[PutObjectOptions]_ | Options to be used for upload. |

__Example__
```java
minioClient.putObject("my-bucketname", "my-objectname", stream, new PutObjectOptions(-1, 5 * MB));
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, String filename, PutObjectOptions options)
`public void putObject(String bucketName, String objectName, String filename, PutObjectOptions options)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.lang.String-io.minio.PutObjectOptions-)_

Uploads contents from a file as object in bucket using options.

__Parameters__
| Parameter      | Type                 | Description                    |
|:---------------|:---------------------|:-------------------------------|
| ``bucketName`` | _String_             | Name of the bucket.            |
| ``objectName`` | _String_             | Object name in the bucket.     |
| ``fileName``   | _String_             | File name.                     |
| ``options``    | _[PutObjectOptions]_ | Options to be used for upload. |

__Example__
```java
minioClient.putObject("my-bucketname", "my-objectname", "/mnt/photos/island,jpg", new PutObjectOptions(11 * MB, -1));
```

<a name="removeObject"></a>
### removeObject(String bucketName, String objectName)
`public void removeObject(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObject-java.lang.String-java.lang.String-)_

Removes an object.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

__Example__
```java
minioClient.removeObject("my-bucketname", "my-objectname");
```

<a name="removeObjects"></a>
### removeObjects(String bucketName, Iterable<String> objectNames)
`public Iterable<Result<DeleteError>> removeObjects(String bucketName, Iterable<String> objectNames)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObjects-java.lang.String-java.lang.Iterable-)_

Removes multiple objects.

__Parameters__
| Parameter       | Type               | Description         |
|:----------------|:-------------------|:--------------------|
| ``bucketName``  | _String_           | Name of the bucket. |
| ``objectNames`` | _Iterable<String>_ | List of objects.    |

| Returns                                                                           |
|:----------------------------------------------------------------------------------|
| _Iterable<[Result]<[DeleteError]>>_: an iterator contains errors on object removal. |

__Example__
```java
List<String> myObjectNames = new LinkedList<String>();
objectNames.add("my-objectname1");
objectNames.add("my-objectname2");
objectNames.add("my-objectname3");
Iterable<Result<DeleteError>> results = minioClient.removeObjects("my-bucketname", myObjectNames);
for (Result<DeleteError> result : results) {
  DeleteError error = errorResult.get();
  System.out.println("Error in deleting object " + error.objectName() + "; " + error.message());
}
```

 <a name="selectObjectContent"></a>
### selectObjectContent(String bucketName, String objectName, String sqlExpression, InputSerialization is, OutputSerialization os, boolean requestProgress, Long scanStartRange, Long scanEndRange, ServerSideEncryption sse)
`public SelectResponseStream selectObjectContent(String bucketName, String objectName, String sqlExpression, InputSerialization is, OutputSerialization os, boolean requestProgress, Long scanStartRange, Long scanEndRange, ServerSideEncryption sse)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#selectObjectContent-java.lang.String-java.lang.String-java.lang.String-io.minio.messages.InputSerialization-io.minio.messages.OutputSerialization-boolean-java.lang.Long-java.lang.Long-io.minio.ServerSideEncryption-)_

Select object content using SQL expression.

__Parameters__

| Parameter           | Type                     | Description                           |
|:--------------------|:-------------------------|:--------------------------------------|
| ``bucketName``      | _String_                 | Name of the bucket.                   |
| ``objectName``      | _String_                 | Object name in the bucket.            |
| ``sqlExpression``   | _String_                 | SQL expression.                       |
| ``is``              | _[InputSerialization]_   | Object's input specification.         |
| ``os``              | _[OutputSerialization]_  | Object's output specification.        |
| ``requestProgress`` | _boolean_                | Flag to request progress information. |
| ``scanStartRange``  | _Long_                   | scan start range of the object.       |
| ``scanEndRange``    | _Long_                   | scan end range of the object.         |
| ``sse``             | _[ServerSideEncryption]_ | Server-side encryptio.n               |

| Returns                                                           |
|:------------------------------------------------------------------|
| _[SelectResponseStream]_: contains filtered records and progress. |

__Example__
```java
String sqlExpression = "select * from S3Object";
InputSerialization is = new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null);
OutputSerialization os = new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);
SelectResponseStream stream = minioClient.selectObjectContent("my-bucketname", "my-objectName", sqlExpression, is, os, true, null, null, null);

byte[] buf = new byte[512];
int bytesRead = stream.read(buf, 0, buf.length);
System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));

Stats stats = stream.stats();
System.out.println("bytes scanned: " + stats.bytesScanned());
System.out.println("bytes processed: " + stats.bytesProcessed());
System.out.println("bytes returned: " + stats.bytesReturned());

stream.close();
```

<a name="setObjectRetention"></a>
### setObjectRetention(String bucketName, String objectName, String versionId, boolean bypassGovernanceRetention, Retention retention)
`public void setObjectLockRetention(String bucketName, String objectName, String versionId, boolean bypassGovernanceRetention, Retention retention)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setObjectRetention-java.lang.String-java.lang.String-java.lang.String-io.minio.messages.Retention-boolean-)_

Applies object retention lock onto an object.

 __Parameters__
| Parameter                     | Type          | Description                     |
|:------------------------------|:--------------|:--------------------------------|
| ``bucketName``                | _String_      | Name of the bucket.             |
| ``objectName``                | _String_      | Object name in the bucket.      |
| ``versionId``                 | _String_      | Object version.                 |
| ``bypassGovernanceRetention`` | _bool_        | By pass Governance Retention.   |
| ``config``                    | _[Retention]_ | Object retention configuration. |

 __Example__
```java
Retention retention = new Retention(RetentionMode.COMPLIANCE, ZonedDateTime.now().plusYears(1));
minioClient.setObjectRetention("my-bucketname", "my-objectname", null, true, retention);
```

<a name="statObject"></a>
### statObject(String bucketName, String objectName)
`public ObjectStat statObject(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#statObject-java.lang.String-java.lang.String-)_

Gets metadata of an object.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns                          |
|:---------------------------------|
| _[ObjectStat]_: object metadata. |

__Example__
```java
ObjectStat objectStat = minioClient.statObject("my-bucketname", "my-objectname");
```

<a name="statObject"></a>
### statObject(String bucketName, String objectName, ServerSideEncryption sse)
`public ObjectStat statObject(String bucketName, String objectName, ServerSideEncryption sse)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#statObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-)_

Returns meta data information of given object in given bucket.

__Parameters__
| Parameter      | Type                     | Description                        |
|:---------------|:-------------------------|:-----------------------------------|
| ``bucketName`` | _String_                 | Name of the bucket.                |
| ``objectName`` | _String_                 | Object name in the bucket.         |
| ``sse``        | _[ServerSideEncryption]_ | SSE-C type Server-side encryption. |

| Returns                          |
|:---------------------------------|
| _[ObjectStat]_: object metadata. |

__Example__
```java
ObjectStat objectStat = minioClient.statObject("my-bucketname", "my-objectname", sse);
```

## 5. Explore Further
- [Build your own Photo API Service - Full Application Example ](https://github.com/minio/minio-java-rest-example)
- [Complete JavaDoc](http://minio.github.io/minio-java/)

[constructor-1]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-
[constructor-2]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-
[constructor-3]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-okhttp3.HttpUrl-
[constructor-4]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-
[constructor-5]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-
[constructor-6]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-boolean-
[constructor-7]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-java.lang.String-boolean-
[constructor-8]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-okhttp3.HttpUrl-java.lang.String-java.lang.String-
[constructor-9]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-java.lang.String-java.lang.String-
[constructor-10]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-java.lang.String-
[constructor-11]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-java.lang.String-boolean-
[constructor-12]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-java.lang.String-boolean-okhttp3.OkHttpClient-
[NotificationConfiguration]: http://minio.github.io/minio-java/io/minio/messages/NotificationConfiguration.html
[ObjectLockConfiguration]: http://minio.github.io/minio-java/io/minio/messages/ObjectLockConfiguration.html
[Bucket]: http://minio.github.io/minio-java/io/minio/messages/Bucket.html
[CloseableIterator]: http://minio.github.io/minio-java/io/minio/CloseableIterator.html
[Result]: http://minio.github.io/minio-java/io/minio/Result.html
[NotificationRecords]: http://minio.github.io/minio-java/io/minio/messages/NotificationRecords.html
[Upload]: http://minio.github.io/minio-java/io/minio/messages/Upload.html
[Item]: http://minio.github.io/minio-java/io/minio/messages/Item.html
[ComposeSource]: http://minio.github.io/minio-java/io/minio/ComposeSource.html
[ServerSideEncryption]: http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html
[CopyConditions]: http://minio.github.io/minio-java/io/minio/CopyConditions.html
[PostPolicy]: http://minio.github.io/minio-java/io/minio/PostPolicy.html
[PutObjectOptions]: http://minio.github.io/minio-java/io/minio/PutObjectOptions.html
[InputSerialization]: http://minio.github.io/minio-java/io/minio/messages/InputSerialization.html
[OutputSerialization]: http://minio.github.io/minio-java/io/minio/messages/OutputSerialization.html
[Retention]: http://minio.github.io/minio-java/io/minio/messages/Retention.html
[ObjectStat]: http://minio.github.io/minio-java/io/minio/ObjectStat.html
[DeleteError]: http://minio.github.io/minio-java/io/minio/messages/DeleteError.html
[SelectResponseStream]: http://minio.github.io/minio-java/io/minio/SelectResponseStream.html
