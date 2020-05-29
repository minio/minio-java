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
| [`deleteBucketEncryption`](#deleteBucketEncryption)           | [`copyObject`](#copyObject)                             |
| [`deleteBucketLifeCycle`](#deleteBucketLifeCycle)             | [`deleteObjectTags`](#deleteObjectTags)                 |
| [`deleteBucketPolicy`](#deleteBucketPolicy)                   | [`disableObjectLegalHold`](#disableObjectLegalHold)     |
| [`deleteBucketTags`](#deleteBucketTags)                       | [`downloadObject`](#downloadObject)                     |
| [`disableVersioning`](#disableVersioning)                     | [`enableObjectLegalHold`](#enableObjectLegalHold)       |
| [`enableVersioning`](#enableVersioning)                       | [`getObject`](#getObject)                               |
| [`getBucketEncryption`](#getBucketEncryption)                 | [`getObjectRetention`](#getObjectRetention)             |
| [`getBucketLifeCycle`](#getBucketLifeCycle)                   | [`getObjectTags`](#getObjectTags)                       |
| [`getBucketNotification`](#getBucketNotification)             | [`getObjectUrl`](#getObjectUrl)                         |
| [`getBucketPolicy`](#getBucketPolicy)                         | [`getPresignedObjectUrl`](#getPresignedObjectUrl)       |
| [`getBucketTags`](#getBucketTags)                             | [`isObjectLegalHoldEnabled`](#isObjectLegalHoldEnabled) |
| [`getDefaultRetention`](#getDefaultRetention)                 | [`listObjects`](#listObjects)                           |
| [`isVersioningEnabled`](#isVersioningEnabled)                 | [`presignedGetObject`](#presignedGetObject)             |
| [`listBuckets`](#listBuckets)                                 | [`presignedPostPolicy`](#presignedPostPolicy)           |
| [`listenBucketNotification`](#listenBucketNotification)       | [`presignedPutObject`](#presignedPutObject)             |
| [`listIncompleteUploads`](#listIncompleteUploads)             | [`putObject`](#putObject)                               |
| [`makeBucket`](#makeBucket)                                   | [`removeObject`](#removeObject)                         |
| [`removeAllBucketNotification`](#removeAllBucketNotification) | [`removeObjects`](#removeObjects)                       |
| [`removeBucket`](#removeBucket)                               | [`selectObjectContent`](#selectObjectContent)           |
| [`removeIncompleteUpload`](#removeIncompleteUpload)           | [`setObjectRetention`](#setObjectRetention)             |
| [`setBucketEncryption`](#setBucketEncryption)                 | [`setObjectTags`](#setObjectTags)                       |
| [`setBucketLifeCycle`](#setBucketLifeCycle)                   | [`statObject`](#statObject)                             |
| [`setBucketNotification`](#setBucketNotification)             |                                                         |
| [`setBucketPolicy`](#setBucketPolicy)                         |                                                         |
| [`setBucketTags`](#setBucketTags)                             |                                                         |
| [`setDefaultRetention`](#setDefaultRetention)                 |                                                         |

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

|                                                                                                                                                                                                                                             |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `public MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String region, Boolean secure, okhttp3.OkHttpClient httpClient) throws InvalidEndpointException, InvalidPortException` _[[Javadoc]][constructor-12]_ |
| Creates MinIO client object using given endpoint, port, access key, secret key, region and secure option.                                                                                                                                   |

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
| IllegalArgumentException   | Throws to indicate invalid argument passed.                          |
| InsufficientDataException  | Thrown to indicate not enough data available in InputStream.         |
| InternalException          | Thrown to indicate internal library error.                           |
| InvalidBucketNameException | Thrown to indicate invalid bucket name passed.                       |
| InvalidKeyException        | Thrown to indicate missing of HMAC SHA-256 library.                  |
| InvalidResponseException   | Thrown to indicate S3 service returned invalid or no error response. |
| IOException                | Thrown to indicate I/O error on S3 operation.                        |
| NoSuchAlgorithmException   | Thrown to indicate missing of MD5 or SHA-256 digest library.         |
| XmlParserException         | Thrown to indicate XML parsing error.                                |

## 2. Bucket operations

<a name="bucketExists"></a>
### bucketExists(BucketExistsArgs args)
`public boolean bucketExists(BucketExistsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#bucketExists-io.minio.BucketExistsArgs-)_

Checks if a bucket exists.

__Parameters__
| Parameter      | Type                 | Description    |
|:---------------|:---------------------|:---------------|
| ``bucketName`` | _[BucketExistsArgs]_ | Arguments.     |

| Returns                                |
|:---------------------------------------|
| _boolean_ - True if the bucket exists. |

__Example__
```java
// Check whether 'my-bucketname' exists or not.
boolean found = 
  minioClient.bucketExists(BucketExistsArgs.builder().bucket("my-bucketname").build());
if (found) {
  System.out.println("my-bucketname exists");
} else {
  System.out.println("my-bucketname does not exist");
}
```

<a name="deleteBucketEncryption"></a>
### deleteBucketEncryption(DeleteBucketEncryptionArgs args)
`private void deleteBucketEncryption(DeleteBucketEncryptionArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketEncryption-io.minio.DeleteBucketEncryptionArgs-)_

Deletes encryption configuration of a bucket.

__Parameters__
| Parameter | Type                           | Description |
|:----------|:-------------------------------|:------------|
| ``args``  | _[DeleteBucketEncryptionArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteBucketEncryption(
    DeleteBucketEncryptionArgs.builder().bucket("my-bucketname").build());
```

<a name="deleteBucketLifeCycle"></a>
### deleteBucketLifeCycle(DeleteBucketLifeCycleArgs args)
`private void deleteBucketLifeCycle(DeleteBucketLifeCycleArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketLifeCycle-io.minio.DeleteBucketLifeCycleArgs-)_

Deletes life-cycle configuration of a bucket.

__Parameters__
| Parameter | Type                          | Description |
|:----------|:------------------------------|:------------|
| ``args``  | _[DeleteBucketLifeCycleArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteBucketLifeCycle(
    DeleteBucketLifeCycleArgs.builder().bucket("my-bucketname").build());
```

<a name="deleteBucketTags"></a>
### deleteBucketTags(DeleteBucketTagsArgs args)
`private void deleteBucketTags(DeleteBucketTagsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketTags-io.minio.DeleteBucketTagsArgs-)_

Deletes tags of a bucket.

__Parameters__
| Parameter | Type                     | Description |
|:----------|:-------------------------|:------------|
| ``args``  | _[DeleteBucketTagsArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteBucketTags(DeleteBucketTagsArgs.builder().bucket("my-bucketname").build());
```

<a name="deleteBucketPolicy"></a>
### deleteBucketPolicy(DeleteBucketPolicyArgs args)
`private void deleteBucketPolicy(DeleteBucketPolicyArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketPolicy-io.minio.DeleteBucketPolicyArgs-)_

Deletes bucket policy configuration of a bucket.

__Parameters__
| Parameter | Type                       | Description |
|:----------|:---------------------------|:------------|
| ``args``  | _[DeleteBucketPolicyArgs]_ | Argumnets.  |

__Example__
```java
minioClient.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket("my-bucketname").build());
```

<a name="disableVersioning"></a>
### disableVersioning(DisableVersioningArgs args)
`public void disableVersioning(DisableVersioningArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#disableVersioning-io.minio.DisableVersioningArgs-)_

Disables object versioning feature in a bucket.

__Parameters__

| Parameter  | Type                       | Description      |
|:-----------|:---------------------------|:-----------------|
| ``args``   | _[DisableVersioningArgs]_  | Arguments.       |

__Example__
```java
minioClient.disableVersioning(DisableVersioningArgs.builder().bucket("my-bucket").build());
```

<a name="enableVersioning"></a>
### enableVersioning(EnableVersioningArgs args)
`public void enableVersioning(EnableVersioningArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#enableVersioning-io.minio.EnableVersioningArgs-)_

Enables object versioning feature in a bucket.

__Parameters__

| Parameter  | Type                      | Description      |
|:-----------|:--------------------------|:-----------------|
| ``args``   | _[EnableVersioningArgs]_  | Arguments.       |

__Example__
```java
minioClient.enableVersioning(EnableVersioningArgs.builder().bucket("my-bucket").build());
```

<a name="isVersioningEnabled"></a>
### isVersioningEnabled(IsVersioningEnabledArgs args)
`public boolean isVersioningEnabled(IsVersioningEnabledArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#isVersioningEnabled-io.minio.IsVersioningEnabledArgs-)_

Get bucket version status.

__Parameters__

| Parameter  | Type                         | Description      |
|:-----------|:-----------------------------|:-----------------|
| ``args``   | _[IsVersioningEnabledArgs]_  | Arguments.       |

| Returns                                           |
|:--------------------------------------------------|
| _boolean_ - True if bucket versioning is enabled. |

__Example__
```java
boolean isVersioningEnabled =
  minioClient.isVersioningEnabled(
      IsVersioningEnabledArgs.builder().bucket("my-bucketname").build());
if (isVersioningEnabled) {
  System.out.println("Bucket versioning is enabled");
} else {
  System.out.println("Bucket versioning is disabled");
}
```

<a name="getBucketEncryption"></a>
### getBucketEncryption(GetBucketEncryptionArgs args)
`public SseConfiguration getBucketEncryption(GetBucketEncryptionArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketEncryption-io.minio.GetBucketEncryptionArgs-)_

Gets encryption configuration of a bucket.

__Parameters__
| Parameter | Type                        | Description |
|:----------|:----------------------------|:------------|
| ``args``  | _[GetBucketEncryptionArgs]_ | Arguments.  |

| Returns                                                      |
|:-------------------------------------------------------------|
| _[SseConfiguration]_ - Server-side encryption configuration. |

__Example__
```java
SseConfiguration config =
    minioClient.getBucketEncryption(
        GetBucketEncryptionArgs.builder().bucket("my-bucketname").build());
```

<a name="getBucketLifeCycle"></a>
### getBucketLifeCycle(GetBucketLifeCycleArgs args)
`public String getBucketLifeCycle(GetBucketLifeCycleArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketLifeCycle-io.minio.GetBucketLifeCycleArgs-)_

Gets life-cycle configuration of a bucket.

__Parameters__
| Parameter | Type                       | Description |
|:----------|:---------------------------|:------------|
| ``args``  | _[GetBucketLifeCycleArgs]_ | Arguments.  |

| Returns                                            |
|:---------------------------------------------------|
| _String_ - Life-cycle configuration as XML string. |

__Example__
```java
String lifecycle =
    minioClient.getBucketLifecycle(
	    GetBucketLifeCycleArgs.builder().bucket("my-bucketname").build());
System.out.println("Life cycle settings: " + lifecycle);
```

<a name="getBucketNotification"></a>
### getBucketNotification(String bucketName)
`public NotificationConfiguration getBucketNotification(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketNotification-java.lang.String-)_

Gets notification configuration of a bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                                     |
|:------------------------------------------------------------|
| _[NotificationConfiguration]_ - Notification configuration. |

__Example__
```java
NotificationConfiguration config = minioClient.getBucketNotification("my-bucketname");
```

<a name="getBucketPolicy"></a>
### getBucketPolicy(GetBucketPolicyArgs args)
`public String getBucketPolicy(GetBucketPolicyArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketPolicy-io.minio.GetBucketPolicyArgs-)_

Gets bucket policy configuration of a bucket.

__Parameters__
| Parameter | Type                    | Description |
|:----------|:------------------------|:------------|
| ``args``  | _[GetBucketPolicyArgs]_ | Arguments.  |


| Returns                                                |
|:-------------------------------------------------------|
| _String_ - Bucket policy configuration as JSON string. |

__Example__
```java
String config =
    minioClient.getBucketPolicy(GetBucketPolicyArgs.builder().bucket("my-bucketname").build());
```

<a name="getBucketTags"></a>
### getBucketTags(GetBucketTagsArgs args)
`public Tags getBucketTags(GetBucketTagsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.htmlgetBucketTags-io.minio.GetBucketTagsArgs-)_

Gets tags of a bucket.

__Parameters__
| Parameter | Type                  | Description |
|:----------|:----------------------|:------------|
| ``args``  | _[GetBucketTagsArgs]_ | Arguments.  |


| Returns          |
|:-----------------|
| _[Tags]_ - tags. |

__Example__
```java
Tags tags = minioClient.getBucketTags(GetBucketTagsArgs.builder().bucket("my-bucketname").build());
```

<a name="getDefaultRetention"></a>
### getDefaultRetention(String bucketName)
`public ObjectLockConfiguration getDefaultRetention(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getDefaultRetention-java.lang.String-)_

Gets default object retention in a bucket.

__Parameters__

| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                                        |
|:---------------------------------------------------------------|
| _[ObjectLockConfiguration]_ - Default retention configuration. |

__Example__
```java
// bucket must be created with object lock enabled.
minioClient.makeBucket("my-bucketname", null, true);
ObjectLockConfiguration config = minioClient.getDefaultRetention("my-bucketname");
System.out.println("Mode: " + config.mode());
System.out.println("Duration: " + config.duration().duration() + " " + config.duration().unit());
```

<a name="listBuckets"></a>
### listBuckets()
`public List<Bucket> listBuckets()` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listBuckets--)_

Lists bucket information of all buckets.

| Returns                                        |
|:-----------------------------------------------|
| _List<[Bucket]>_ - List of bucket information. |

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

Listens events of object prefix and suffix of a bucket. The returned closable iterator is lazily evaluated hence its required to iterate to get new records and must be used with try-with-resource to release underneath network resources.

__Parameters__
| Parameter      | Type       | Description                                 |
|:---------------|:-----------|:--------------------------------------------|
| ``bucketName`` | _String_   | Name of the bucket.                         |
| ``prefix``     | _String_   | Listen events of object starts with prefix. |
| ``suffix``     | _String_   | Listen events of object ends with suffix.   |
| ``events``     | _String[]_ | Events to listen.                           |

| Returns                                                                                                 |
|:--------------------------------------------------------------------------------------------------------|
| _[CloseableIterator]<[Result]<[NotificationRecords]>>_ - Lazy closable iterator contains event records. |

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

Lists incomplete object upload information of a bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

| Returns                                                                            |
|:-----------------------------------------------------------------------------------|
| _Iterable<[Result]<[Upload]>>_ - Lazy iterator contains object upload information. |

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

Lists incomplete object upload information of a bucket for prefix.

__Parameters__
| Parameter      | Type     | Description                     |
|:---------------|:---------|:--------------------------------|
| ``bucketName`` | _String_ | Name of the bucket.             |
| ``prefix``     | _String_ | Object name starts with prefix. |

| Returns                                                                            |
|:-----------------------------------------------------------------------------------|
| _Iterable<[Result]<[Upload]>>_ - Lazy iterator contains object upload information. |

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

Lists incomplete object upload information of a bucket for prefix recursively.

__Parameters__
| Param          | Type      | Description                                          |
|:---------------|:----------|:-----------------------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                                  |
| ``prefix``     | _String_  | Object name starts with prefix.                      |
| ``recursive``  | _boolean_ | List recursively than directory structure emulation. |

| Returns                                                                            |
|:-----------------------------------------------------------------------------------|
| _Iterable<[Result]<[Upload]>>_ - Lazy iterator contains object upload information. |

__Example__
```java
Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("my-bucketname", "my-obj", true);
for (Result<Upload> result : results) {
  Upload upload = result.get();
  System.out.println(upload.uploadId() + ", " + upload.objectName());
}
```

<a name="listObjects"></a>
### listObjects(ListObjectsArgs args)
`public Iterable<Result<Item>> listObjects(ListObjectsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-io.minio.ListObjectsArgs-)_

Lists object information of a bucket.

__Parameters__
| Parameter        | Type                | Description               |
|:-----------------|:--------------------|:--------------------------|
| ``args``         | _[ListObjectsArgs]_ | Arguments to list objects |

| Returns                                                                   |
|:--------------------------------------------------------------------------|
| _Iterable<[Result]<[Item]>>_ - Lazy iterator contains object information. |

__Example__
```java
Iterable<Result<Item>> results = minioClient.listObjects(
  ListObjectsArgs.builder()
    .bucket("my-bucketname")
    .includeUserMetadata(true)
    .startAfter("start-after-entry")
    .prefix("my-obj")
    .maxKeys(100)
    .fetchOwner(true)
);
for (Result<Item> result : results) {
  Item item = result.get();
  System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
}
```

### makeBucket(MakeBucketArgs args)
`public void makeBucket(MakeBucketArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-io.minio.MakeBucketArgs-)_

Creates a bucket with given region and object lock feature enabled.

__Parameters__

| Parameter      | Type               | Description                |
|:---------------|:-------------------|:---------------------------|
| ``args``       | _[MakeBucketArgs]_ | Arguments to create bucket |

__Example__

```java
// Create bucket with default region.
minioClient.makeBucket(
    MakeBucketArgs.builder()
        .bucket("my-bucketname")
        .build());

// Create bucket with specific region.
minioClient.makeBucket(
    MakeBucketArgs.builder()
        .bucket("my-bucketname")
        .region("us-west-1")
        .build());

// Create object-lock enabled bucket with specific region.
minioClient.makeBucket(
    MakeBucketArgs.builder()
        .bucket("my-bucketname")
        .region("us-west-1")
        .objectLock(true)
        .build());
```

<a name="removeAllBucketNotification"></a>
### removeAllBucketNotification(String bucketName)
`public void removeAllBucketNotification(String bucketName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeAllBucketNotification-java.lang.String-)_

Removes notification configuration of a bucket.

__Parameters__
| Parameter      | Type     | Description         |
|:---------------|:---------|:--------------------|
| ``bucketName`` | _String_ | Name of the bucket. |

__Example__
```java
minioClient.removeAllBucketNotification("my-bucketname");
```

<a name="removeBucket"></a>
### removeBucket(RemoveBucketArgs args)
`public void removeBucket(RemoveBucketArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeBucket-io.minio.RemoveBucketArgs-)_

Removes an empty bucket.

__Parameters__

| Parameter    | Type                 | Description     |
|:-------------|:---------------------|:----------------|
| ``args``     | _[RemoveBucketArgs]_ | Arguments.      |

__Example__
```java
minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
```

<a name="removeIncompleteUpload"></a>
### removeIncompleteUpload(String bucketName, String objectName)
`public void removeIncompleteUpload(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeIncompleteUpload-java.lang.String-java.lang.String-)_

Removes incomplete uploads of an object.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

__Example__
```java
minioClient.removeIncompleteUpload("my-bucketname", "my-objectname");
```

<a name="setBucketEncryption"></a>
### setBucketEncryption(SetBucketEncryptionArgs args)
`public void setBucketEncryption(SetBucketEncryptionArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketEncryption-io.minio.SetBucketEncryptionArgs-)_

Sets encryption configuration of a bucket.

__Parameters__
| Parameter | Type                        | Description |
|:----------|:----------------------------|:------------|
| ``args``  | _[SetBucketEncryptionArgs]_ | Arguments.  |

__Example__
```java
minioClient.setBucketEncryption(
    SetBucketEncryptionArgs.builder().bucket("my-bucketname").config(config).build());
 ```

<a name="setBucketLifeCycle"></a>
### setBucketLifeCycle(SetBucketLifeCycleArgs args)
`public void setBucketLifeCycle(SetBucketLifeCycleArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketLifeCycle-io.minio.SetBucketLifeCycleArgs-)_

Sets life-cycle configuration to a bucket.

__Parameters__
| Parameter | Type                       | Description |
|:----------|:---------------------------|:------------|
| ``args``  | _[SetBucketLifeCycleArgs]_ | Arguments.  |

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
minioClient.setBucketLifecycle(
    SetBucketLifecycleArgs.builder().bucket("my-bucketname").config(lifeCycleXml).build());
```

<a name="setBucketNotification"></a>
### setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)
`public void setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketNotification-java.lang.String-io.minio.messages.NotificationConfiguration-)_

Sets notification configuration to a bucket.

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
### setBucketPolicy(SetBucketPolicyArgs args)
`public void setBucketPolicy(SetBucketPolicyArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketPolicy-io.minio.SetBucketPolicyArgs-)_

Sets bucket policy configuration to a bucket.

__Parameters__

| Parameter | Type                    | Description |
|:----------|:------------------------|:------------|
| ``args``  | _[SetBucketPolicyArgs]_ | Arguments.  |

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
minioClient.setBucketPolicy(
    SetBucketPolicyArgs.builder().bucket("my-bucketname").config(policyJson).build());
```

<a name="setBucketTags"></a>
### setBucketTags(SetBucketTagsArgs args)
`public void setBucketTags(SetBucketTagsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketTags-io.minio.SetBucketTagsArgs-)_

Sets tags to a bucket.

__Parameters__

| Parameter | Type                  | Description |
|:----------|:----------------------|:------------|
| ``args``  | _[SetBucketTagsArgs]_ | Arguments.  |

__Example__
```java
Map<String, String> map = new HashMap<>();
map.put("Project", "Project One");
map.put("User", "jsmith");
minioClient.setBucketTags(SetBucketTagsArgs.builder().bucket("my-bucketname").tags(map).build());
```

<a name="setDefaultRetention"></a>
### setDefaultRetention(String bucketName, ObjectLockConfiguration config)
`public void setDefaultRetention(String bucketName, ObjectLockConfiguration config)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setDefaultRetention-java.lang.String-io.minio.messages.ObjectLockConfiguration-)_

Sets default object retention in a bucket.

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

Creates an object by combining data from different source objects using server-side copy.

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

// Create my-bucketname/my-objectname by combining source object list.
minioClient.composeObject("my-bucketname", "my-objectname", sourceObjectList, null, null);

// Create my-bucketname/my-objectname with user metadata by combining source object list.
minioClient.composeObject("my-bucketname", "my-objectname", sourceObjectList, userMetadata, null);

// Create my-bucketname/my-objectname with user metadata and server-side encryption by combining
// source object list.
minioClient.composeObject("my-bucketname", "my-objectname", sourceObjectList, userMetadata, sse);
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, Map<String,String> headerMap, ServerSideEncryption sse, String srcBucketName, String srcObjectName, ServerSideEncryption srcSse, CopyConditions copyConditions)
`public void copyObject(String bucketName, String objectName, Map<String,String> headerMap, ServerSideEncryption sse, String srcBucketName, String srcObjectName, ServerSideEncryption srcSse, CopyConditions copyConditions)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.util.Map-io.minio.ServerSideEncryption-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-io.minio.CopyConditions-)_

Creates an object by server-side copying data from another object.

__Parameters__
| Parameter          | Type                     | Description                                                    |
|:-------------------|:-------------------------|:---------------------------------------------------------------|
| ``bucketName``     | _String_                 | Name of the bucket.                                            |
| ``objectName``     | _String_                 | Object name to be created.                                     |
| ``headerMap``      | _Map<String,String>_     | (Optional) User metadata.                                      |
| ``sse``            | _[ServerSideEncryption]_ | (Optional) Server-side encryption.                             |
| ``srcBucketName``  | _String_                 | Source bucket name.                                            |
| ``srcObjectName``  | _String_                 | (Optional) Source object name.                                 |
| ``srcSse``         | _[ServerSideEncryption]_ | (Optional) SSE-C type server-side encryption of source object. |
| ``copyConditions`` | _[CopyConditions]_       | (Optional) Conditiions to be used in copy operation.           |

__Example__

```java
// Copy data from my-source-bucketname/my-objectname to my-bucketname/my-objectname.
minioClient.copyObject("my-bucketname", "my-objectname", null, null, "my-source-bucketname", null, null,
    null);

// Copy data from my-source-bucketname/my-source-objectname to my-bucketname/my-objectname.
minioClient.copyObject("my-bucketname", "my-objectname", null, null, "my-source-bucketname",
    "my-source-objectname", null, null);

// Copy data from my-source-bucketname/my-objectname to my-bucketname/my-objectname by server-side encryption.
minioClient.copyObject("my-bucketname", "my-objectname", null, sse, "my-source-bucketname", null, null, null);

// Copy data from SSE-C encrypted my-source-bucketname/my-objectname to my-bucketname/my-objectname.
minioClient.copyObject("my-bucketname", "my-objectname", null, null, "my-source-bucketname", null, srcSsec,
    null);

// Copy data from my-source-bucketname/my-objectname to my-bucketname/my-objectname with user metadata and
// copy conditions.
minioClient.copyObject("my-bucketname", "my-objectname", headers, null, "my-source-bucketname", null, null,
    conditions);
```

<a name="deleteObjectTags"></a>
### deleteObjectTags(DeleteObjectTagsArgs args)
`private void deleteObjectTags(DeleteObjectTagsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteObjectTags-io.minio.DeleteObjectTagsArgs-)_

Deletes tags of an object.

__Parameters__
| Parameter | Type                     | Description |
|:----------|:-------------------------|:------------|
| ``args``  | _[DeleteObjectTagsArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteObjectTags(
    DeleteObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
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
| ``objectName`` | _String_ | Object name in the bucket. |
| ``versionId``  | _String_ | Version ID of the object.  |

 __Example__
 ```java
minioClient.enableObjectLegalHold("my-bucketname", "my-objectname", null);
```

<a name="getObject"></a>
### getObject(GetObjectArgs args)
`public InputStream getObject(GetObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-io.minio.GetObjectArgs-)_

Gets data of an object. Returned `InputStream` must be closed after use to release network resources.

__Parameters__
| Parameter      | Type            | Description                |
|:---------------|:----------------|:---------------------------|
| ``args``       | _GetObjectArgs_ | Arguments.                 |

| Returns                               |
|:--------------------------------------|
| _InputStream_ - Contains object data. |

__Example__
```java
// get object given the bucket and object name
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .build()) {
  // Read data from stream
}

// get object data from offset
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .offset(1024L)
  .build()) {
  // Read data from stream
}

// get object data from offset to length
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .offset(1024L)
  .length(4096L)
  .build()) {
  // Read data from stream
}

// get data of an SSE-C encrypted object
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .ssec(ssec)
  .build()) {
  // Read data from stream
}

// get object data from offset to length of an SSE-C encrypted object
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .offset(1024L)
  .length(4096L)
  .ssec(ssec)
  .build()) {
  // Read data from stream
}
```

<a name="downloadObject"></a>
### downloadObject(DownloadObjectArgs args)
`public void downloadObject(DownloadObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-io.minio.DownloadObjectArgs-)_

Downloads data of an object to file.

__Parameters__
| Parameter        | Type                 | Description                  |
|:-----------------|:---------------------|:-----------------------------|
| ``args``         | _DownloadObjectArgs_ | Arguments.                   |

__Example__
```java
// Download object given the bucket, object name and output file name
minioClient.downloadObject(
  DownloadObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .fileName("my-object-file")
  .build());

// Download server-side encrypted object in bucket to given file name
minioClient.downloadObject(
  DownloadObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .ssec(ssec)
  .fileName("my-object-file")
  .build());
```

 <a name="getObjectRetention"></a>
### getObjectRetention(GetObjectRetentionArgs args)
`public Retention getObjectRetention(GetObjectRetentionArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObjectRetention-io.minio.GetObjectRetentionArgs-)_

Gets retention configuration of an object.

 __Parameters__
 
| Parameter      | Type                       | Description   |
|:---------------|:---------------------------|:--------------|
| ``args``       | _[GetObjectRetentionArgs]_ | Arguments.    |

| Returns                                         |
|:------------------------------------------------|
| _[Retention]_ - Object retention configuration. |

 __Example__
 ```java
// Object with version id.
Retention retention =
    minioClient.getObjectRetention(
        GetObjectRetentionArgs.builder()
            .bucket("my-bucketname")
            .object("my-objectname")
            .versionId("object-version-id")
            .build());
System.out.println("mode: " + retention.mode() + "until: " + retention.retainUntilDate());
```

<a name="getObjectTags"></a>
### getObjectTags(GetObjectTagsArgs args)
`public Tags getObjectTags(GetObjectTagsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObjectTags-io.minio.GetObjectTagsArgs-)_

Gets tags of an object.

__Parameters__
| Parameter | Type                  | Description |
|:----------|:----------------------|:------------|
| ``args``  | _[GetObjectTagsArgs]_ | Arguments.  |


| Returns          |
|:-----------------|
| _[Tags]_ - tags. |

__Example__
```java
Tags tags = minioClient.getObjectTags(
    GetObjectTagsArgs.builder().bucket("my-bucketname").object("my-objectname").build());
```

 <a name="getObjectUrl"></a>
### getObjectUrl(String bucketName, String objectName)
`public String getObjectUrl(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObjectUrl-java.lang.String-java.lang.String-)_

Gets URL of an object useful when this object has public read access.

 __Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns                |
|:-----------------------|
| _String_ - URL string. |

 __Example__
 ```java
String url = minioClient.getObjectUrl("my-bucketname", "my-objectname");
System.out.println("my-bucketname/my-objectname can be downloaded by " + url);
```

 <a name="getPresignedObjectUrl"></a>
### getPresignedObjectUrl(Method method, String bucketName, String objectName, Integer expires, Map<String,String> reqParams)
`public String getPresignedObjectUrl(Method method, String bucketName, String objectName, Integer expires, Map<String,String> reqParams)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getPresignedObjectUrl-io.minio.http.Method-java.lang.String-java.lang.String-java.lang.Integer-java.util.Map-)_

Gets presigned URL of an object for HTTP method, expiry time and custom request parameters.

 __Parameters__
| Parameter      | Type                 | Description                            |
|:---------------|:---------------------|:---------------------------------------|
| ``method``     | _[Method]_           | HTTP method to generate presigned URL. |
| ``bucketName`` | _String_             | Name of the bucket.                    |
| ``objectName`` | _String_             | Object name in the bucket.             |
| ``expires``    | _Integer_            | Expiry in seconds; defaults to 7 days. |
| ``reqParams``  | _Map<String,String>_ | Request parameters to override.        |

| Returns                |
|:-----------------------|
| _String_ - URL string. |

 __Example__
 ```java
String url = minioClient.getPresignedObjectUrl(Method.DELETE, "my-bucketname", "my-objectname", 24 * 60 * 60,
    reqParams);
```

 <a name="isObjectLegalHoldEnabled"></a>
### isObjectLegalHoldEnabled(String bucketName, String objectName, String versionId)
`public boolean isObjectLegalHoldEnabled(String bucketName, String objectName, String versionId)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#isObjectLegalHoldEnabled-java.lang.String-java.lang.String-java.lang.String-)_

Returns true if legal hold is enabled on an object.

 __Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |
| ``versionId``  | _String_ | Version ID of the object.  |

| Returns                                    |
|:-------------------------------------------|
| _boolean_ - True if legal hold is enabled. |

 __Example__
```java
boolean status = minioClient.isObjectLegalHoldEnabled("my-bucketname", "my-objectname", null);
if (status) {
  System.out.println("Legal hold is on");
else {
  System.out.println("Legal hold is off");
}
```

<a name="presignedGetObject"></a>
### presignedGetObject(String bucketName, String objectName)
`public String presignedGetObject(String bucketName, String objectName)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-)_

Gets presigned URL of an object to download its data for 7 days.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns                                       |
|:----------------------------------------------|
| _String_ - URL string to download the object. |

__Example__
```java
String url = minioClient.presignedGetObject("my-bucketname", "my-objectname");
```


<a name="presignedGetObject"></a>
### presignedGetObject(String bucketName, String objectName, Integer expires)
`public String presignedGetObject(String bucketName, String objectName, Integer expires)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-)_

Gets presigned URL of an object to download its data for expiry time.

__Parameters__
| Parameter      | Type      | Description                                         |
|:---------------|:----------|:----------------------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                                 |
| ``objectName`` | _String_  | Object name in the bucket.                          |
| ``expires``    | _Integer_ | Expiry in seconds. Default expiry is set to 7 days. |

| Returns                                       |
|:----------------------------------------------|
| _String_ - URL string to download the object. |

__Example__
```java
// Get presigned URL to download my-objectname data with one day expiry.
String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 24 * 60 * 60);
```

<a name="presignedGetObject"></a>
### presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,String> reqParams)
`public String presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,String> reqParams)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-java.util.Map-)_

Gets presigned URL of an object to download its data for expiry time and request parameters.

__Parameters__
| Parameter      | Type                 | Description                            |
|:---------------|:---------------------|:---------------------------------------|
| ``bucketName`` | _String_             | Name of the bucket.                    |
| ``objectName`` | _String_             | Object name in the bucket.             |
| ``expires``    | _Integer_            | Expiry in seconds; defaults to 7 days. |
| ``reqParams``  | _Map<String,String>_ | Request parameters to override.        |

| Returns                                       |
|:----------------------------------------------|
| _String_ - URL string to download the object. |

__Example__
```java
// Get presigned URL to download my-objectname data with one day expiry and request parameters.
String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 24 * 60 * 60, reqParams);
```

<a name="presignedPostPolicy"></a>
### presignedPostPolicy(PostPolicy policy)
`public Map<String,String> presignedPostPolicy(PostPolicy policy)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPostPolicy-io.minio.PostPolicy-)_

Gets form-data of [PostPolicy] of an object to upload its data using POST method.

__Parameters__
| Parameter  | Type           | Description               |
|:-----------|:---------------|:--------------------------|
| ``policy`` | _[PostPolicy]_ | Post policy of an object. |


| Returns                                                                           |
|:----------------------------------------------------------------------------------|
| _Map<String, String>_ - Contains form-data to upload an object using POST method. |

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

Gets presigned URL of an object to upload data for 7 days.

__Parameters__
| Parameter      | Type     | Description                |
|:---------------|:---------|:---------------------------|
| ``bucketName`` | _String_ | Name of the bucket.        |
| ``objectName`` | _String_ | Object name in the bucket. |

| Returns                                    |
|:-------------------------------------------|
| _String_ - URL string to upload an object. |

__Example__
```java
String url = minioClient.presignedPutObject("my-bucketname", "my-objectname");
```

<a name="presignedPutObject"></a>
### presignedPutObject(String bucketName, String objectName, Integer expires)
`public String presignedPutObject(String bucketName, String objectName, Integer expires)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPutObject-java.lang.String-java.lang.String-java.lang.Integer-)_

Gets presigned URL of an object to upload data for expiry time.

__Parameters__
| Parameter      | Type      | Description                            |
|:---------------|:----------|:---------------------------------------|
| ``bucketName`` | _String_  | Name of the bucket.                    |
| ``objectName`` | _String_  | Object name in the bucket.             |
| ``expires``    | _Integer_ | Expiry in seconds; defaults to 7 days. |

| Returns                                    |
|:-------------------------------------------|
| _String_ - URL string to upload an object. |

__Example__
```java
// Get presigned URL to upload data to my-objectname with one day expiry.
String url = minioClient.presignedPutObject("my-bucketname", "my-objectname", 24 * 60 * 60);
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)
`public void putObject(String bucketName, String objectName, InputStream stream, PutObjectOptions options)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-io.minio.PutObjectOptions-)_

Uploads given stream as object in bucket by using given options.

__Parameters__
| Parameter      | Type                 | Description                       |
|:---------------|:---------------------|:----------------------------------|
| ``bucketName`` | _String_             | Name of the bucket.               |
| ``objectName`` | _String_             | Object name in the bucket.        |
| ``stream``     | _InputStream_        | Stream contains object data.      |
| ``options``    | _[PutObjectOptions]_ | Options to be used during upload. |

__Example__
```java
PutObjectOptions options = new PutObjectOptions(7003256, -1);
minioClient.putObject("my-bucketname", "my-objectname", stream, options);
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, String filename, PutObjectOptions options)
`public void putObject(String bucketName, String objectName, String filename, PutObjectOptions options)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.lang.String-io.minio.PutObjectOptions-)_

Uploads contents from a file as object in bucket using options.

__Parameters__
| Parameter      | Type                 | Description                                  |
|:---------------|:---------------------|:---------------------------------------------|
| ``bucketName`` | _String_             | Name of the bucket.                          |
| ``objectName`` | _String_             | Object name in the bucket.                   |
| ``fileName``   | _String_             | Name of file to upload.                      |
| ``options``    | _[PutObjectOptions]_ | (Optional) Options to be used during upload. |

__Example__
```java
minioClient.putObject("my-bucketname", "my-objectname", "my-filename", null);
```

<a name="removeObject"></a>
### removeObject(RemoveObjectArgs args)
`public void removeObject(RemoveObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObject-io.minio.RemoveObjectArgs-)_

Removes an object.

__Parameters__
| Parameter | Type                 | Description |
|:----------|:---------------------|:------------|
| ``args``  | _[RemoveObjectArgs]_ | Arguments.  |

__Example__
```java
// Remove object.
minioClient.removeObject(
    RemoveObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());

// Remove versioned object.
minioClient.removeObject(
    RemoveObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-versioned-objectname")
        .versionId("my-versionid")
        .build());

// Remove versioned object bypassing Governance mode.
minioClient.removeObject(
    RemoveObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-versioned-objectname")
        .versionId("my-versionid")
        .bypassRetentionMode(true)
        .build());
```

<a name="removeObjects"></a>
### removeObjects(String bucketName, Iterable<String> objectNames)
`public Iterable<Result<DeleteError>> removeObjects(String bucketName, Iterable<String> objectNames)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObjects-java.lang.String-java.lang.Iterable-)_

Removes multiple objects lazily. Its required to iterate the returned Iterable to perform removal.

__Parameters__
| Parameter       | Type               | Description                    |
|:----------------|:-------------------|:-------------------------------|
| ``bucketName``  | _String_           | Name of the bucket.            |
| ``objectNames`` | _Iterable<String>_ | List of objects in the bucket. |

| Returns                                                                             |
|:------------------------------------------------------------------------------------|
| _Iterable<[Result]<[DeleteError]>>_ - Lazy iterator contains object removal status. |

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
### selectObjectContent(String bucketName, String objectName, String sqlExpression, InputSerialization is, OutputSerialization os, boolean requestProgress, Long scanStartRange, Long scanEndRange, ServerSideEncryptionCustomerKey ssec)
`public SelectResponseStream selectObjectContent(String bucketName, String objectName, String sqlExpression, InputSerialization is, OutputSerialization os, boolean requestProgress, Long scanStartRange, Long scanEndRange, ServerSideEncryptionCustomerKey ssec)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#selectObjectContent-java.lang.String-java.lang.String-java.lang.String-io.minio.messages.InputSerialization-io.minio.messages.OutputSerialization-boolean-java.lang.Long-java.lang.Long-io.minio.ServerSideEncryptionCustomerKey-)_

Selects content of a object by SQL expression.

__Parameters__

| Parameter           | Type                                | Description                           |
|:--------------------|:------------------------------------|:--------------------------------------|
| ``bucketName``      | _String_                            | Name of the bucket.                   |
| ``objectName``      | _String_                            | Object name in the bucket.            |
| ``sqlExpression``   | _String_                            | SQL expression.                       |
| ``is``              | _[InputSerialization]_              | Input specification of object data.   |
| ``os``              | _[OutputSerialization]_             | Output specification of result.       |
| ``requestProgress`` | _boolean_                           | Flag to request progress information. |
| ``scanStartRange``  | _Long_                              | scan start range of the object.       |
| ``scanEndRange``    | _Long_                              | scan end range of the object.         |
| ``sse``             | _[ServerSideEncryptionCustomerKey]_ | SSE-C type server-side encryption.    |

| Returns                                                            |
|:-------------------------------------------------------------------|
| _[SelectResponseStream]_ - Contains filtered records and progress. |

__Example__
```java
String sqlExpression = "select * from S3Object";
InputSerialization is = new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null);
OutputSerialization os = new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);
SelectResponseStream stream = minioClient.selectObjectContent("my-bucketname", "my-objectName", sqlExpression,
    is, os, true, null, null, null);

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
### setObjectRetention(SetObjectRetentionArgs args)
`public void setObjectLockRetention(SetObjectRetentionArgs)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setObjectRetention-io.minio.SetObjectRetentionArgs-)_

Sets retention configuration to an object.

 __Parameters__
 
| Parameter        | Type                       | Description  |
|:-----------------|:---------------------------|:-------------|
| ``args``         | _[SetObjectRetentionArgs]_ | Arguments.   |

 __Example__
```java
Retention retention = new Retention(RetentionMode.COMPLIANCE, ZonedDateTime.now().plusYears(1));
minioClient.setObjectRetention(
    SetObjectRetentionArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .config(retention)
        .bypassGovernanceMode(true)
        .build());
```

<a name="setObjectTags"></a>
### setObjectTags(SetObjectTagsArgs args)
`public void setObjectTags(SetObjectTagsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setObjectTags-io.minio.SetObjectTagsArgs-)_

Sets tags to an object.

__Parameters__

| Parameter | Type                  | Description |
|:----------|:----------------------|:------------|
| ``args``  | _[SetObjectTagsArgs]_ | Arguments.  |

__Example__
```java
Map<String, String> map = new HashMap<>();
map.put("Project", "Project One");
map.put("User", "jsmith");
minioClient.setObjectTags(
    SetObjectTagsArgs.builder().bucket("my-bucketname").object("my-objectname").tags(map).build());
```

<a name="statObject"></a>
### statObject(StatObjectArgs args)
`public ObjectStat statObject(StatObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#statObject-io.minio.StatObjectArgs-)_

Gets object information and metadata of an object.

__Parameters__
| Parameter | Type               | Description |
|:----------|:-------------------|:------------|
| ``args``  | _[StatObjectArgs]_ | Arguments.  |

| Returns                                                     |
|:------------------------------------------------------------|
| _[ObjectStat]_ - Populated object information and metadata. |

__Example__
```java
// Get information of an object.
ObjectStat objectStat =
    minioClient.statObject(
        StatObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());

// Get information of SSE-C encrypted object.
ObjectStat objectStat =
    minioClient.statObject(
        StatObjectArgs.builder()
            .bucket("my-bucketname")
            .object("my-objectname")
            .ssec(ssec)
            .build());

// Get information of a versioned object.
ObjectStat objectStat =
    minioClient.statObject(
        StatObjectArgs.builder()
            .bucket("my-bucketname")
            .object("my-objectname")
            .versionId("version-id")
            .build());

// Get information of a SSE-C encrypted versioned object.
ObjectStat objectStat =
    minioClient.statObject(
        StatObjectArgs.builder()
            .bucket("my-bucketname")
            .object("my-objectname")
            .versionId("version-id")
            .ssec(ssec)
            .build());
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
[constructor-12]: http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.Integer-java.lang.String-java.lang.String-java.lang.String-java.lang.Boolean-okhttp3.OkHttpClient-
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
[ServerSideEncryptionCustomerKey]: http://minio.github.io/minio-java/io/minio/ServerSideEncryptionCustomerKey.html
[CopyConditions]: http://minio.github.io/minio-java/io/minio/CopyConditions.html
[PostPolicy]: http://minio.github.io/minio-java/io/minio/PostPolicy.html
[PutObjectOptions]: http://minio.github.io/minio-java/io/minio/PutObjectOptions.html
[InputSerialization]: http://minio.github.io/minio-java/io/minio/messages/InputSerialization.html
[OutputSerialization]: http://minio.github.io/minio-java/io/minio/messages/OutputSerialization.html
[Retention]: http://minio.github.io/minio-java/io/minio/messages/Retention.html
[ObjectStat]: http://minio.github.io/minio-java/io/minio/ObjectStat.html
[DeleteError]: http://minio.github.io/minio-java/io/minio/messages/DeleteError.html
[SelectResponseStream]: http://minio.github.io/minio-java/io/minio/SelectResponseStream.html
[MakeBucketArgs]: http://minio.github.io/minio-java/io/minio/MakeBucketArgs.html
[ListObjectsArgs]: http://minio.github.io/minio-java/io/minio/ListObjectsArgs.html
[RemoveBucketArgs]: http://minio.github.io/minio-java/io/minio/RemoveBucketArgs.html
[EnableVersioningArgs]: http://minio.github.io/minio-java/io/minio/EnableVersioningArgs.html
[DisableVersioningArgs]: http://minio.github.io/minio-java/io/minio/DisableVersioningArgs.html
[SetObjectRetentionArgs]: http://minio.github.io/minio-java/io/minio/SetObjectRetentionArgs.html
[GetObjectRetentionArgs]: http://minio.github.io/minio-java/io/minio/GetObjectRetentionArgs.html
[Method]: http://minio.github.io/minio-java/io/minio/http/Method.html
[StatObjectArgs]: http://minio.github.io/minio-java/io/minio/StatObjectArgs.html
[RemoveObjectArgs]: http://minio.github.io/minio-java/io/minio/RemoveObjectArgs.html
[SseConfiguration]: http://minio.github.io/minio-java/io/minio/messages/SseConfiguration.html
[DeleteBucketEncryptionArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketEncryptionArgs.html
[GetBucketEncryptionArgs]: http://minio.github.io/minio-java/io/minio/GetBucketEncryptionArgs.html
[SetBucketEncryptionArgs]: http://minio.github.io/minio-java/io/minio/SetBucketEncryptionArgs.html
[Tags]: http://minio.github.io/minio-java/io/minio/messages/Tags.html
[DeleteBucketTagsArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketTagsArgs.html
[GetBucketTagsArgs]: http://minio.github.io/minio-java/io/minio/GetBucketTagsArgs.html
[SetBucketTagsArgs]: http://minio.github.io/minio-java/io/minio/SetBucketTagsArgs.html
[DeleteObjectTagsArgs]: http://minio.github.io/minio-java/io/minio/DeleteObjectTagsArgs.html
[GetObjectTagsArgs]: http://minio.github.io/minio-java/io/minio/GetObjectTagsArgs.html
[SetObjectTagsArgs]: http://minio.github.io/minio-java/io/minio/SetObjectTagsArgs.html
[DeleteBucketLifeCycleArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketLifeCycleArgs.html
[GetBucketLifeCycleArgs]: http://minio.github.io/minio-java/io/minio/GetBucketLifeCycleArgs.html
[SetBucketLifeCycleArgs]: http://minio.github.io/minio-java/io/minio/SetBucketLifeCycleArgs.html
[GetBucketPolicyArgs]: http://minio.github.io/minio-java/io/minio/GetBucketPolicyArgs.html
[SetBucketPolicyArgs]: http://minio.github.io/minio-java/io/minio/SetBucketPolicyArgs.html
[DeleteBucketPolicyArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketPolicyArgs.html
[GetObjectArgs]: http://minio.github.io/minio-java/io/minio/GetObjectArgs.html
[DownloadObjectArgs]: http://minio.github.io/minio-java/io/minio/DownloadObjectArgs.html
[IsVersioningEnabledArgs]: http://minio.github.io/minio-java/io/minio/IsVersioningEnabledArgs.html
[BucketExistsArgs]: http://minio.github.io/minio-java/io/minio/BucketExistsArgs.html
