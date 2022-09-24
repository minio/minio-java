# Java Client API Reference [![Slack](https://slack.min.io/slack?type=svg)](https://slack.min.io)

## Create MinIO Client.

## MinIO

```java
MinioClient minioClient =
    MinioClient.builder()
        .endpoint("https://play.min.io")
        .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
        .build();
```

## AWS S3

```java
MinioClient minioClient =
    MinioClient.builder()
        .endpoint("https://s3.amazonaws.com")
        .credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY")
        .build();
```

| Bucket operations                                                 | Object operations                                       |
|-------------------------------------------------------------------|---------------------------------------------------------|
| [`bucketExists`](#bucketExists)                                   | [`composeObject`](#composeObject)                       |
| [`deleteBucketEncryption`](#deleteBucketEncryption)               | [`copyObject`](#copyObject)                             |
| [`deleteBucketLifecycle`](#deleteBucketLifecycle)                 | [`deleteObjectTags`](#deleteObjectTags)                 |
| [`deleteBucketNotification`](#deleteBucketNotification)           | [`disableObjectLegalHold`](#disableObjectLegalHold)     |
| [`deleteBucketPolicy`](#deleteBucketPolicy)                       | [`downloadObject`](#downloadObject)                     |
| [`deleteBucketReplication`](#deleteBucketReplication)             | [`enableObjectLegalHold`](#enableObjectLegalHold)       |
| [`deleteBucketTags`](#deleteBucketTags)                           | [`getObject`](#getObject)                               |
| [`deleteObjectLockConfiguration`](#deleteObjectLockConfiguration) | [`getObjectRetention`](#getObjectRetention)             |
| [`getBucketEncryption`](#getBucketEncryption)                     | [`getObjectTags`](#getObjectTags)                       |
| [`getBucketLifecycle`](#getBucketLifecycle)                       | [`getPresignedObjectUrl`](#getPresignedObjectUrl)       |
| [`getBucketNotification`](#getBucketNotification)                 | [`getPresignedPostFormData`](#getPresignedPostFormData) |
| [`getBucketPolicy`](#getBucketPolicy)                             | [`isObjectLegalHoldEnabled`](#isObjectLegalHoldEnabled) |
| [`getBucketReplication`](#getBucketReplication)                   | [`listObjects`](#listObjects)                           |
| [`getBucketTags`](#getBucketTags)                                 | [`putObject`](#putObject)                               |
| [`getBucketVersioning`](#getBucketVersioning)                     | [`removeObject`](#removeObject)                         |
| [`getObjectLockConfiguration`](#getObjectLockConfiguration)       | [`removeObjects`](#removeObjects)                       |
| [`listBuckets`](#listBuckets)                                     | [`restoreObject`](#restoreObject)                       |
| [`listenBucketNotification`](#listenBucketNotification)           | [`selectObjectContent`](#selectObjectContent)           |
| [`makeBucket`](#makeBucket)                                       | [`setObjectRetention`](#setObjectRetention)             |
| [`removeBucket`](#removeBucket)                                   | [`setObjectTags`](#setObjectTags)                       |
| [`setBucketEncryption`](#setBucketEncryption)                     | [`statObject`](#statObject)                             |
| [`setBucketLifecycle`](#setBucketLifecycle)                       | [`uploadObject`](#uploadObject)                         |
| [`setBucketNotification`](#setBucketNotification)                 | [`uploadSnowballObjects`](#uploadSnowballObjects)       |
| [`setBucketPolicy`](#setBucketPolicy)                             |                                                         |
| [`setBucketReplication`](#setBucketReplication)                   |                                                         |
| [`setBucketTags`](#setBucketTags)                                 |                                                         |
| [`setBucketVersioning`](#setBucketVersioning)                     |                                                         |
| [`setObjectLockConfiguration`](#setObjectLockConfiguration)       |                                                         |

## 1. MinIO Client Builder

MinIO Client Builder is used to create MinIO client. Builder has below methods to accept arguments.
| Method          | Description                                                                                                                                |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `endpoint()`    | Accepts endpoint as a String, URL or okhttp3.HttpUrl object and optionally accepts port number and flag to enable secure (TLS) connection. |
|                 | Endpoint as a string can be formatted like below:                                                                                          |
|                 | `https://s3.amazonaws.com`                                                                                                                 |
|                 | `https://play.min.io`                                                                                                                      |
|                 | `https://play.min.io:9000`                                                                                                                 |
|                 | `localhost`                                                                                                                                |
|                 | `play.min.io`                                                                                                                              |
| `credentials()` | Accepts access key (aka user ID) and secret key (aka password) of an account in S3 service.                                                |
| `region()`      | Accepts region name of S3 service. If specified, all operations use this region otherwise region is probed per bucket.                     |
| `httpClient()`  | Custom HTTP client to override default.                                                                                                    |

__Examples__

### MinIO

```java
// 1. Create client to S3 service 'play.min.io' at port 443 with TLS security
// for anonymous access.
MinioClient minioClient = MinioClient.builder().endpoint("https://play.min.io").build();

// 2. Create client to S3 service 'play.min.io' at port 443 with TLS security
// using URL object for anonymous access.
MinioClient minioClient = MinioClient.builder().endpoint(new URL("https://play.min.io")).build();

// 3. Create client to S3 service 'play.min.io' at port 9000 with TLS security
// using okhttp3.HttpUrl object for anonymous access.
MinioClient minioClient =
    MinioClient.builder().endpoint(HttpUrl.parse("https://play.min.io:9000")).build();

// 4. Create client to S3 service 'play.min.io' at port 443 with TLS security
// for authenticated access.
MinioClient minioClient =
    MinioClient.builder()
	    .endpoint("https://play.min.io")
		.credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
		.build();

// 5. Create client to S3 service 'play.min.io' at port 9000 with non-TLS security
// for authenticated access.
MinioClient minioClient =
    MinioClient.builder()
	    .endpoint("play.min.io", 9000, false)
	    .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
		.build();

// 6. Create client to S3 service 'play.min.io' at port 9000 with TLS security
// for authenticated access.
MinioClient minioClient =
    MinioClient.builder()
	    .endpoint("play.min.io", 9000, true)
		.credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
		.build();

// 7. Create client to S3 service 'play.min.io' at port 443 with TLS security
// and region 'us-west-1' for authenticated access.
MinioClient minioClient =
    MinioClient.builder()
	    .endpoint(new URL("https://play.min.io"))
		.credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
		.region("us-west-1")
		.build();

// 8. Create client to S3 service 'play.min.io' at port 9000 with TLS security,
// region 'eu-east-1' and custom HTTP client for authenticated access.
MinioClient minioClient =
    MinioClient.builder()
	    .endpoint("https://play.min.io:9000")
		.credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
		.region("eu-east-1")
		.httpClient(customHttpClient)
		.build();
```

### AWS S3

```java
// 1. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security
// for anonymous access.
MinioClient s3Client = MinioClient.builder().endpoint("https://s3.amazonaws.com").build();

// 2. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security
// using URL object for anonymous access.
MinioClient s3Client = MinioClient.builder().endpoint(new URL("https://s3.amazonaws.com")).build();

// 3. Create client to S3 service 's3.amazonaws.com' at port 9000 with TLS security
// using okhttp3.HttpUrl object for anonymous access.
MinioClient s3Client =
    MinioClient.builder().endpoint(HttpUrl.parse("https://s3.amazonaws.com")).build();

// 4. Create client to S3 service 's3.amazonaws.com' at port 80 with TLS security
// for authenticated access.
MinioClient s3Client =
    MinioClient.builder()
	    .endpoint("s3.amazonaws.com")
		.credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY")
		.build();

// 5. Create client to S3 service 's3.amazonaws.com' at port 443 with non-TLS security
// for authenticated access.
MinioClient s3Client =
    MinioClient.builder()
        .endpoint("s3.amazonaws.com", 433, false)
		.credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY")
		.build();

// 6. Create client to S3 service 's3.amazonaws.com' at port 80 with non-TLS security
// for authenticated access.
MinioClient s3Client =
    MinioClient.builder()
	    .endpoint("s3.amazonaws.com", 80, false)
        .credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY")
		.build();

// 7. Create client to S3 service 's3.amazonaws.com' at port 80 with TLS security
// for authenticated access.
MinioClient s3Client =
    MinioClient.builder()
	    .endpoint("s3.amazonaws.com", 80, true)
        .credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY")
		.build();

// 8. Create client to S3 service 's3.amazonaws.com' at port 80 with non-TLS security
// and region 'us-west-1' for authenticated access.
MinioClient s3Client =
    MinioClient.builder()
	    .endpoint("s3.amazonaws.com", 80, false)
        .credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY")
		.region("us-west-1")
		.build();

// 9. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security
// and region 'eu-west-2' for authenticated access.
MinioClient s3Client =
    MinioClient.builder()
	    .endpoint("s3.amazonaws.com", 443, true)
		.credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY").
		.region("eu-west-2")
		.build();

// 10. Create client to S3 service 's3.amazonaws.com' at port 443 with TLS security,
// region 'eu-central-1' and custom HTTP client for authenticated access.
MinioClient s3Client =
    MinioClient.builder()
	    .endpoint("s3.amazonaws.com", 443, true)
        .credentials("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY")
		.region("eu-central-1")
		.httpClient(customHttpClient)
		.build();
```

## Common Exceptions
All APIs throw below exceptions in addition to specific to API.

| Exception                 | Cause                                                                |
|:--------------------------|:---------------------------------------------------------------------|
| ErrorResponseException    | Thrown to indicate S3 service returned an error response.            |
| IllegalArgumentException  | Throws to indicate invalid argument passed.                          |
| InsufficientDataException | Thrown to indicate not enough data available in InputStream.         |
| InternalException         | Thrown to indicate internal library error.                           |
| InvalidKeyException       | Thrown to indicate missing of HMAC SHA-256 library.                  |
| InvalidResponseException  | Thrown to indicate S3 service returned invalid or no error response. |
| IOException               | Thrown to indicate I/O error on S3 operation.                        |
| NoSuchAlgorithmException  | Thrown to indicate missing of MD5 or SHA-256 digest library.         |
| ServerException           | Thrown to indicate HTTP server error.                                |
| XmlParserException        | Thrown to indicate XML parsing error.                                |

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

<a name="deleteBucketLifecycle"></a>
### deleteBucketLifecycle(DeleteBucketLifecycleArgs args)
`private void deleteBucketLifecycle(DeleteBucketLifecycleArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketLifecycle-io.minio.DeleteBucketLifecycleArgs-)_

Deletes lifecycle configuration of a bucket.

__Parameters__
| Parameter | Type                          | Description |
|:----------|:------------------------------|:------------|
| ``args``  | _[DeleteBucketLifecycleArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteBucketLifecycle(
    DeleteBucketLifecycleArgs.builder().bucket("my-bucketname").build());
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
| ``args``  | _[DeleteBucketPolicyArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket("my-bucketname").build());
```

<a name="deleteBucketReplication"></a>
### deleteBucketReplication(DeleteBucketReplicationArgs args)
`private void deleteBucketReplication(DeleteBucketReplicationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketReplication-io.minio.DeleteBucketReplicationArgs-)_

Deletes bucket replication configuration of a bucket.

__Parameters__
| Parameter | Type                            | Description |
|:----------|:--------------------------------|:------------|
| ``args``  | _[DeleteBucketReplicationArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteBucketReplication(
    DeleteBucketReplicationArgs.builder().bucket("my-bucketname").build());
```

<a name="deleteBucketNotification"></a>
### deleteBucketNotification(DeleteBucketNotificationArgs args)
`public void deleteBucketNotification(DeleteBucketNotificationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketNotification-io.minio.DeleteBucketNotificationArgs-)_

Deletes notification configuration of a bucket.

__Parameters__
| Parameter | Type                             | Description |
|:----------|:---------------------------------|:------------|
| ``args``  | _[DeleteBucketNotificationArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteBucketNotification(
    DeleteBucketNotificationArgs.builder().bucket("my-bucketname").build());
```

<a name="deleteObjectLockConfiguration"></a>
### deleteObjectLockConfiguration(DeleteObjectLockConfigurationArgs args)
`public void deleteObjectLockConfiguration(DeleteObjectLockConfigurationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteObjectLockConfiguration-io.minio.DeleteObjectLockConfigurationArgs-)_

Deletes object-lock configuration in a bucket.

__Parameters__
| Parameter | Type                                  | Description |
|:----------|:--------------------------------------|:------------|
| ``args``  | _[DeleteObjectLockConfigurationArgs]_ | Arguments.  |

__Example__
```java
minioClient.deleteObjectLockConfiguration(
    DeleteObjectLockConfigurationArgs.builder().bucket("my-bucketname").build());
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

<a name="getBucketLifecycle"></a>
### getBucketLifecycle(GetBucketLifecycleArgs args)
`public LifecycleConfiguration getBucketLifecycle(GetBucketLifecycleArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketLifecycle-io.minio.GetBucketLifecycleArgs-)_

Gets lifecycle configuration of a bucket.

__Parameters__
| Parameter | Type                       | Description |
|:----------|:---------------------------|:------------|
| ``args``  | _[GetBucketLifecycleArgs]_ | Arguments.  |

| Returns                                               |
|:------------------------------------------------------|
| _[LifecycleConfiguration]_ - lifecycle configuration. |

__Example__
```java
LifecycleConfiguration config =
    minioClient.getBucketLifecycle(
	    GetBucketLifecycleArgs.builder().bucket("my-bucketname").build());
System.out.println("Lifecycle configuration: " + config);
```

<a name="getBucketNotification"></a>
### getBucketNotification(GetBucketNotificationArgs args)
`public NotificationConfiguration getBucketNotification(GetBucketNotificationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketNotification-io.minio.GetBucketNotificationArgs-)_

Gets notification configuration of a bucket.

__Parameters__
| Parameter | Type                          | Description |
|:----------|:------------------------------|:------------|
| ``args``  | _[GetBucketNotificationArgs]_ | Arguments.  |

| Returns                                                     |
|:------------------------------------------------------------|
| _[NotificationConfiguration]_ - Notification configuration. |

__Example__
```java
NotificationConfiguration config =
    minioClient.getBucketNotification(
	    GetBucketNotificationArgs.builder().bucket("my-bucketname").build());
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

<a name="getBucketReplication"></a>
### getBucketReplication(GetBucketReplicationArgs args)
`public ReplicationConfiguration getBucketReplication(GetBucketReplicationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketReplication-io.minio.GetBucketReplicationArgs-)_

Gets bucket replication configuration of a bucket.

__Parameters__
| Parameter | Type                         | Description |
|:----------|:-----------------------------|:------------|
| ``args``  | _[GetBucketReplicationArgs]_ | Arguments.  |


| Returns                                                          |
|:-----------------------------------------------------------------|
| _[ReplicationConfiguration]_ - Bucket replication configuration. |

__Example__
```java
ReplicationConfiguration config =
    minioClient.getBucketReplication(
	    GetBucketReplicationArgs.builder().bucket("my-bucketname").build());
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

<a name="getBucketVersioning"></a>
### getBucketVersioning(GetBucketVersioningArgs args)
`public VersioningConfiguration getBucketVersioning(GetBucketVersioningArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketVersioning-io.minio.GetBucketVersioningArgs-)_

Gets versioning configuration of a bucket.

__Parameters__
| Parameter | Type                        | Description |
|:----------|:----------------------------|:------------|
| ``args``  | _[GetBucketVersioningArgs]_ | Arguments.  |

| Returns                                                 |
|:--------------------------------------------------------|
| _[VersioningConfiguration]_ - Versioning configuration. |

__Example__
```java
VersioningConfiguration config =
    minioClient.getBucketVersioning(
        GetBucketVersioningArgs.builder().bucket("my-bucketname").build());
```

<a name="getObjectLockConfiguration"></a>
### getObjectLockConfiguration(GetObjectLockConfigurationArgs args)
`public ObjectLockConfiguration getObjectLockConfiguration(GetObjectLockConfigurationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObjectLockConfiguration-io.minio.GetObjectLockConfigurationArgs-)_

Gets object-lock configuration in a bucket.

__Parameters__

| Parameter | Type                               | Description |
|:----------|:-----------------------------------|:------------|
| ``args``  | _[GetObjectLockConfigurationArgs]_ | Arguments.  |

| Returns                                                        |
|:---------------------------------------------------------------|
| _[ObjectLockConfiguration]_ - Default retention configuration. |

__Example__
```java
ObjectLockConfiguration config =
    minioClient.getObjectLockConfiguration(
	    GetObjectLockConfigurationArgs.builder().bucket("my-bucketname").build());
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

<a name="listBuckets"></a>
### listBuckets(ListBucketsArgs args)
`public List<Bucket> listBuckets(ListBucketsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listBuckets-io.minio.ListBucketsArgs-)_

Lists bucket information of all buckets.

__Parameters__
| Parameter | Type                | Description |
|:----------|:--------------------|:------------|
| ``args``  | _[ListBucketsArgs]_ | Arguments.  |

| Returns                                        |
|:-----------------------------------------------|
| _List<[Bucket]>_ - List of bucket information. |

__Example__
```java
List<Bucket> bucketList =
    minioClient.listBuckets(ListBuckets.builder().extraHeaders(headers).build());
for (Bucket bucket : bucketList) {
  System.out.println(bucket.creationDate() + ", " + bucket.name());
}
```

<a name="listenBucketNotification"></a>
### listenBucketNotification(ListenBucketNotificationArgs args)
`public CloseableIterator<Result<NotificationRecords>> listenBucketNotification(ListenBucketNotificationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#listenBucketNotification-io.minio.ListenBucketNotificationArgs-)_

Listens events of object prefix and suffix of a bucket. The returned closable iterator is lazily evaluated hence its required to iterate to get new records and must be used with try-with-resource to release underneath network resources.

__Parameters__
| Parameter | Type                             | Description |
|:----------|:---------------------------------|:------------|
| ``args``  | _[ListenBucketNotificationArgs]_ | Arguments.  |

| Returns                                                                                                 |
|:--------------------------------------------------------------------------------------------------------|
| _[CloseableIterator]<[Result]<[NotificationRecords]>>_ - Lazy closable iterator contains event records. |

__Example__
```java
String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
try (CloseableIterator<Result<NotificationRecords>> ci =
    minioClient.listenBucketNotification(
        ListenBucketNotificationArgs.builder()
            .bucket("bucketName")
            .prefix("")
            .suffix("")
            .events(events)
            .build())) {
  while (ci.hasNext()) {
    NotificationRecords records = ci.next().get();
    for (Event event : records.events()) {
      System.out.println("Event " + event.eventType() + " occurred at " + event.eventTime()
          + " for " + event.bucketName() + "/" + event.objectName());
    }
  }
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
// Lists objects information.
Iterable<Result<Item>> results = minioClient.listObjects(
    ListObjectsArgs.builder().bucket("my-bucketname").build());

// Lists objects information recursively.
Iterable<Result<Item>> results = minioClient.listObjects(
    ListObjectsArgs.builder().bucket("my-bucketname").recursive(true).build());

// Lists maximum 100 objects information whose names starts with 'E' and after 'ExampleGuide.pdf'.
Iterable<Result<Item>> results = minioClient.listObjects(
    ListObjectsArgs.builder()
        .bucket("my-bucketname")
        .startAfter("ExampleGuide.pdf")
        .prefix("E")
        .maxKeys(100)
        .build());

// Lists maximum 100 objects information with version whose names starts with 'E' and after
// 'ExampleGuide.pdf'.
Iterable<Result<Item>> results = minioClient.listObjects(
    ListObjectsArgs.builder()
        .bucket("my-bucketname")
        .startAfter("ExampleGuide.pdf")
        .prefix("E")
        .maxKeys(100)
        .includeVersions(true)
        .build());
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

<a name="setBucketLifecycle"></a>
### setBucketLifecycle(SetBucketLifecycleArgs args)
`public void setBucketLifecycle(SetBucketLifecycleArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketLifecycle-io.minio.SetBucketLifecycleArgs-)_

Sets lifecycle configuration to a bucket.

__Parameters__
| Parameter | Type                       | Description |
|:----------|:---------------------------|:------------|
| ``args``  | _[SetBucketLifecycleArgs]_ | Arguments.  |

__Example__
```java
List<LifecycleRule> rules = new LinkedList<>();
rules.add(
    new LifecycleRule(
        Status.ENABLED,
        null,
        null,
        new RuleFilter("documents/"),
        "rule1",
        null,
        null,
        new Transition((ZonedDateTime) null, 30, "GLACIER")));
rules.add(
    new LifecycleRule(
        Status.ENABLED,
        null,
        new Expiration((ZonedDateTime) null, 365, null),
        new RuleFilter("logs/"),
        "rule2",
        null,
        null,
        null));
LifecycleConfiguration config = new LifecycleConfiguration(rules);
minioClient.setBucketLifecycle(
    SetBucketLifecycleArgs.builder().bucket("my-bucketname").config(config).build());
```

<a name="setBucketNotification"></a>
### setBucketNotification(SetBucketNotificationArgs args)
`public void setBucketNotification(SetBucketNotificationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketNotification-io.minio.SetBucketNotificationArgs-)_

Sets notification configuration to a bucket.

__Parameters__

| Parameter | Type                          | Description |
|:----------|:------------------------------|:------------|
| ``args``  | _[SetBucketNotificationArgs]_ | Arguments.  |

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

minioClient.setBucketNotification(
    SetBucketNotificationArgs.builder().bucket("my-bucketname").config(config).build());
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

<a name="setBucketReplication"></a>
### setBucketReplication(SetBucketReplicationArgs args)
`public void setBucketReplication(SetBucketReplicationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketReplication-io.minio.SetBucketReplicationArgs-)_

Sets bucket replication configuration to a bucket.

__Parameters__

| Parameter | Type                         | Description |
|:----------|:-----------------------------|:------------|
| ``args``  | _[SetBucketReplicationArgs]_ | Arguments.  |

__Example__
```java
Map<String, String> tags = new HashMap<>();
tags.put("key1", "value1");
tags.put("key2", "value2");

ReplicationRule rule =
    new ReplicationRule(
        new DeleteMarkerReplication(Status.DISABLED),
        new ReplicationDestination(
            null, null, "REPLACE-WITH-ACTUAL-DESTINATION-BUCKET-ARN", null, null, null, null),
        null,
        new RuleFilter(new AndOperator("TaxDocs", tags)),
        "rule1",
        null,
        1,
        null,
        Status.ENABLED);

List<ReplicationRule> rules = new LinkedList<>();
rules.add(rule);

ReplicationConfiguration config =
    new ReplicationConfiguration("REPLACE-WITH-ACTUAL-ROLE", rules);

minioClient.setBucketReplication(
    SetBucketReplicationArgs.builder().bucket("my-bucketname").config(config).build());
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

<a name="setBucketVersioning"></a>
### setBucketVersioning(SetBucketVersioningArgs args)
`public void setBucketVersioning(SetBucketVersioningArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketVersioning-io.minio.SetBucketVersioningArgs-)_

Sets versioning configuration of a bucket.

__Parameters__
| Parameter | Type                        | Description |
|:----------|:----------------------------|:------------|
| ``args``  | _[SetBucketVersioningArgs]_ | Arguments.  |

__Example__
```java
minioClient.setBucketVersioning(
    SetBucketVersioningArgs.builder().bucket("my-bucketname").config(config).build());
 ```

<a name="setObjectLockConfiguration"></a>
### setObjectLockConfiguration(SetObjectLockConfigurationArgs args)
`public void setObjectLockConfiguration(SetObjectLockConfigurationArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#setObjectLockConfiguration-io.minio.SetObjectLockConfigurationArgs-)_

Sets object-lock configuration in a bucket.

__Parameters__
| Parameter | Type                               | Description |
|:----------|:-----------------------------------|:------------|
| ``args``  | _[SetObjectLockConfigurationArgs]_ | Arguments.  |

__Example__
```java
ObjectLockConfiguration config =
    new ObjectLockConfiguration(RetentionMode.COMPLIANCE, new RetentionDurationDays(100));
minioClient.setObjectLockConfiguration(
    SetObjectLockConfigurationArgs.builder().bucket("my-bucketname").config(config).build());
```

## 3. Object operations

<a name="composeObject"></a>
### composeObject(ComposeObjectArgs args)
`public ObjectWriteResponse composeObject(ComposeObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#composeObject-io.minio.ComposeObjectArgs--)_

Creates an object by combining data from different source objects using server-side copy.

 __Parameters__
| Param          | Type                     | Description   |
|:---------------|:-------------------------|:--------------|
| ``args``       | _[ComposeObjectArgs]_    | Arguments.    |

| Returns                                                          |
|:-----------------------------------------------------------------|
| _[ObjectWriteResponse]_ - Contains information of created object.|

__Example__
 ```java
List<ComposeSource> sourceObjectList = new ArrayList<ComposeSource>();
sourceObjectList.add(
  ComposeSource.builder().bucket("my-job-bucket").object("my-objectname-part-one").build());
sourceObjectList.add(
  ComposeSource.builder().bucket("my-job-bucket").object("my-objectname-part-two").build());
sourceObjectList.add(
  ComposeSource.builder().bucket("my-job-bucket").object("my-objectname-part-three").build());

// Create my-bucketname/my-objectname by combining source object list.
minioClient.composeObject(
  ComposeObjectArgs.builder()
      .bucket("my-bucketname")
      .object("my-objectname")
      .sources(sourceObjectList)
      .build());

// Create my-bucketname/my-objectname with user metadata by combining source object
// list.
Map<String, String> userMetadata = new HashMap<>();
userMetadata.put("My-Project", "Project One");
minioClient.composeObject(
    ComposeObjectArgs.builder()
      .bucket("my-bucketname")
      .object("my-objectname")
      .sources(sourceObjectList)
      .userMetadata(userMetadata)
      .build());

// Create my-bucketname/my-objectname with user metadata and server-side encryption
// by combining source object list.
minioClient.composeObject(
  ComposeObjectArgs.builder()
      .bucket("my-bucketname")
      .object("my-objectname")
      .sources(sourceObjectList)
      .userMetadata(userMetadata)
      .ssec(sse)
      .build());
```

<a name="copyObject"></a>
### copyObject(CopyObjectArgs args)
`public ObjectWriteResponse copyObject(CopyObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-io.minio.CopyObjectArgs-)_

Creates an object by server-side copying data from another object.

__Parameters__
| Parameter | Type               | Description |
|:----------|:-------------------|:------------|
| ``args``  | _[CopyObjectArgs]_ | Arguments.  |

| Returns                                                          |
|:-----------------------------------------------------------------|
| _[ObjectWriteResponse]_ - Contains information of created object.|

__Example__

```java
// Create object "my-objectname" in bucket "my-bucketname" by copying from object
// "my-objectname" in bucket "my-source-bucketname".
minioClient.copyObject(
    CopyObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .source(
            CopySource.builder()
                .bucket("my-source-bucketname")
                .object("my-objectname")
                .build())
        .build());

// Create object "my-objectname" in bucket "my-bucketname" by copying from object
// "my-source-objectname" in bucket "my-source-bucketname".
minioClient.copyObject(
    CopyObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .source(
            CopySource.builder()
                .bucket("my-source-bucketname")
                .object("my-source-objectname")
                .build())
        .build());

// Create object "my-objectname" in bucket "my-bucketname" with SSE-KMS server-side
// encryption by copying from object "my-objectname" in bucket "my-source-bucketname".
minioClient.copyObject(
    CopyObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .source(
            CopySource.builder()
                .bucket("my-source-bucketname")
                .object("my-objectname")
                .build())
        .sse(sseKms) // Replace with actual key.
        .build());

// Create object "my-objectname" in bucket "my-bucketname" with SSE-S3 server-side
// encryption by copying from object "my-objectname" in bucket "my-source-bucketname".
minioClient.copyObject(
    CopyObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .source(
            CopySource.builder()
                .bucket("my-source-bucketname")
                .object("my-objectname")
                .build())
        .sse(sseS3) // Replace with actual key.
        .build());

// Create object "my-objectname" in bucket "my-bucketname" with SSE-C server-side encryption
// by copying from object "my-objectname" in bucket "my-source-bucketname".
minioClient.copyObject(
    CopyObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .source(
            CopySource.builder()
                .bucket("my-source-bucketname")
                .object("my-objectname")
                .build())
        .sse(ssec) // Replace with actual key.
        .build());

// Create object "my-objectname" in bucket "my-bucketname" by copying from SSE-C encrypted
// object "my-source-objectname" in bucket "my-source-bucketname".
minioClient.copyObject(
    CopyObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .source(
            CopySource.builder()
                .bucket("my-source-bucketname")
                .object("my-source-objectname")
                .ssec(ssec) // Replace with actual key.
                .build())
        .build());

// Create object "my-objectname" in bucket "my-bucketname" with custom headers conditionally
// by copying from object "my-objectname" in bucket "my-source-bucketname".
minioClient.copyObject(
    CopyObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .source(
            CopySource.builder()
                .bucket("my-source-bucketname")
                .object("my-objectname")
                .matchETag(etag) // Replace with actual etag.
                .build())
        .headers(headers) // Replace with actual headers.
        .build());
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
### disableObjectLegalHold(DisableObjectLegalHoldArgs args)
`public void disableObjectLegalHold(DisableObjectLegalHoldArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#disableObjectLegalHold-io.minio.DisableObjectLegalHoldArgs-)_

Disables legal hold on an object.

 __Parameters__

| Parameter      | Type                           | Description  |
|:---------------|:-------------------------------|:-------------|
| ``args``       | _[DisableObjectLegalHoldArgs]_ | Arguments.   |

 __Example__

```java
// Disables legal hold on an object.
minioClient.disableObjectLegalHold(
    DisableObjectLegalHoldArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .build());
```

<a name="enableObjectLegalHold"></a>
### enableObjectLegalHold(EnableObjectLegalHoldArgs args)
`public void enableObjectLegalHold(EnableObjectLegalHoldArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#enableObjectLegalHold-io.minio.EnableObjectLegalHoldArgs-)_

Enables legal hold on an object.

 __Parameters__

| Parameter      | Type                          | Description  |
|:---------------|:------------------------------|:-------------|
| ``args``       | _[EnableObjectLegalHoldArgs]_ | Argumments.  |

 __Example__
 ```java

 // Disables legal hold on an object.
minioClient.enableObjectLegalHold(
    EnableObjectLegalHoldArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .build());
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
  .build())) {
  // Read data from stream
}

// get object data from offset
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .offset(1024L)
  .build())) {
  // Read data from stream
}

// get object data from offset to length
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .offset(1024L)
  .length(4096L)
  .build())) {
  // Read data from stream
}

// get data of an SSE-C encrypted object
try (InputStream stream = minioClient.getObject(
  GetObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .ssec(ssec)
  .build())) {
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
  .build())) {
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
  .filename("my-object-file")
  .build());

// Download server-side encrypted object in bucket to given file name
minioClient.downloadObject(
  DownloadObjectArgs.builder()
  .bucket("my-bucketname")
  .object("my-objectname")
  .ssec(ssec)
  .filename("my-object-file")
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

 <a name="getPresignedObjectUrl"></a>
### getPresignedObjectUrl(GetPresignedObjectUrlArgs args)
`public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getPresignedObjectUrl-io.minio.GetPresignedObjectUrlArgs-)_

Gets presigned URL of an object for HTTP method, expiry time and custom request parameters.

 __Parameters__
| Parameter   | Type                           | Description  |
|:------------|:-------------------------------|:-------------|
| ``args``    | _[GetPresignedObjectUrlArgs]_  | Arguments.   |

| Returns                |
|:-----------------------|
| _String_ - URL string. |

 __Example__
 ```java
// Get presigned URL string to download 'my-objectname' in 'my-bucketname'
// with an expiration of 2 hours.
//
// Additionally also add 'response-content-type' to dynamically set content-type
// for the server response.
Map<String, String> reqParams = new HashMap<String, String>();
reqParams.put("response-content-type", "application/json");

String url =
    minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket("my-bucketname")
            .object("my-objectname")
            .expiry(2, TimeUnit.HOURS)
            .extraQueryParams(reqParams)
            .build());
System.out.println(url);

// Get presigned URL string to upload 'my-objectname' in 'my-bucketname'
// with an expiration of 1 day.
String url =
    minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.PUT)
            .bucket("my-bucketname")
            .object("my-objectname")
            .expiry(1, TimeUnit.DAYS)
            .build());
System.out.println(url);

// Get presigned URL string to lookup metadata for 'my-objectname' in 'my-bucketname'
// with an expiration of 2 hours.
//
// Additionally also add 'response-content-type' to dynamically set content-type
// for the server metadata response.
Map<String, String> reqParams = new HashMap<String, String>();
reqParams.put("response-content-type", "application/json");

String url =
    minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.HEAD)
            .bucket("my-bucketname")
            .object("my-objectname")
            .expiry(2, TimeUnit.HOURS)
            .extraQueryParams(reqParams)
            .build());
System.out.println(url);
```

 <a name="isObjectLegalHoldEnabled"></a>
### isObjectLegalHoldEnabled(IsObjectLegalHoldEnabledArgs args)
`public boolean isObjectLegalHoldEnabled(IsObjectLegalHoldEnabledArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#isObjectLegalHoldEnabled-io.minio.IsObjectLegalHoldEnabledArgs-)_

Returns true if legal hold is enabled on an object.

 __Parameters__

| Parameter | Type                             | Description  |
|:----------|:---------------------------------|:-------------|
| ``args``  | _[IsObjectLegalHoldEnabledArgs]_ | Arguments.   |


| Returns                                    |
|:-------------------------------------------|
| _boolean_ - True if legal hold is enabled. |

 __Example__

```java
boolean status =
    s3Client.isObjectLegalHoldEnabled(
       IsObjectLegalHoldEnabledArgs.builder()
            .bucket("my-bucketname")
            .object("my-objectname")
            .versionId("object-versionId")
            .build());
if (status) {
  System.out.println("Legal hold is on");
else {
  System.out.println("Legal hold is off");
}
```

<a name="getPresignedPostFormData"></a>
### getPresignedPostFormData(PostPolicy policy)
`public Map<String,String> getPresignedPostFormData(PostPolicy policy)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#getPresignedPostFormData-io.minio.PostPolicy-)_

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
// Create new post policy for 'my-bucketname' with 7 days expiry from now.
PostPolicy policy = new PostPolicy("my-bucketname", ZonedDateTime.now().plusDays(7));

// Add condition that 'key' (object name) equals to 'my-objectname'.
policy.addEqualsCondition("key", "my-objectname");

// Add condition that 'Content-Type' starts with 'image/'.
policy.addStartsWithCondition("Content-Type", "image/");

// Add condition that 'content-length-range' is between 64kiB to 10MiB.
policy.addContentLengthRangeCondition(64 * 1024, 10 * 1024 * 1024);

Map<String, String> formData = minioClient.getPresignedPostFormData(policy);

// Upload an image using POST object with form-data.
MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
multipartBuilder.setType(MultipartBody.FORM);
for (Map.Entry<String, String> entry : formData.entrySet()) {
  multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
}
multipartBuilder.addFormDataPart("key", "my-objectname");
multipartBuilder.addFormDataPart("Content-Type", "image/png");

// "file" must be added at last.
multipartBuilder.addFormDataPart(
    "file", "my-objectname", RequestBody.create(new File("Pictures/avatar.png"), null));

Request request =
    new Request.Builder()
        .url("https://play.min.io/my-bucketname")
        .post(multipartBuilder.build())
        .build();
OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
Response response = httpClient.newCall(request).execute();
if (response.isSuccessful()) {
  System.out.println("Pictures/avatar.png is uploaded successfully using POST object");
} else {
  System.out.println("Failed to upload Pictures/avatar.png");
}
```

<a name="putObject"></a>
### putObject(PutObjectArgs args)
`public ObjectWriteResponse putObject(PutObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-io.minio.PutObjectArgs-)_

Uploads given stream as object in bucket.

__Parameters__
| Parameter | Type              | Description |
|:----------|:------------------|:------------|
| ``args``  | _[PutObjectArgs]_ | Arguments.  |

| Returns                                                          |
|:-----------------------------------------------------------------|
| _[ObjectWriteResponse]_ - Contains information of created object.|

__Example__
```java
// Upload known sized input stream.
minioClient.putObject(
    PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
            inputStream, size, -1)
        .contentType("video/mp4")
        .build());

// Upload unknown sized input stream.
minioClient.putObject(
    PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
            inputStream, -1, 10485760)
        .contentType("video/mp4")
        .build());

// Create object ends with '/' (also called as folder or directory).
minioClient.putObject(
    PutObjectArgs.builder().bucket("my-bucketname").object("path/to/").stream(
            new ByteArrayInputStream(new byte[] {}), 0, -1)
        .build());

// Upload input stream with headers and user metadata.
Map<String, String> headers = new HashMap<>();
headers.put("X-Amz-Storage-Class", "REDUCED_REDUNDANCY");
Map<String, String> userMetadata = new HashMap<>();
userMetadata.put("My-Project", "Project One");
minioClient.putObject(
    PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
            inputStream, size, -1)
        .headers(headers)
        .userMetadata(userMetadata)
        .build());

// Upload input stream with server-side encryption.
minioClient.putObject(
    PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
            inputStream, size, -1)
        .sse(sse)
        .build());
```

<a name="uploadObject"></a>
### uploadObject(UploadObjectArgs args)
`public void uploadObject(UploadObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#uploadObject-io.minio.UploadObjectArgs-)_

Uploads contents from a file as object in bucket.

__Parameters__
| Parameter | Type                 | Description |
|:----------|:---------------------|:------------|
| ``args``  | _[UploadObjectArgs]_ | Arguments.  |

__Example__
```java
// Upload an JSON file.
minioClient.uploadObject(
    UploadObjectArgs.builder()
        .bucket("my-bucketname").object("my-objectname").filename("person.json").build());

// Upload a video file.
minioClient.uploadObject(
    UploadObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .filename("my-video.avi")
        .contentType("video/mp4")
        .build());
```

<a name="uploadSnowballObjects"></a>
### uploadSnowballObjects(UploadSnowballObjectsArgs args)
`public void uploadSnowballObjects(UploadSnowballObjectsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#uploadSnowballObjects-io.minio.UploadSnowballObjectsArgs-)_

Uploads multiple objects in a single put call. It is done by creating intermediate TAR file optionally compressed which is uploaded to S3 service.

__Parameters__
| Parameter | Type                          | Description |
|:----------|:------------------------------|:------------|
| ``args``  | _[UploadSnowballObjectsArgs]_ | Arguments.  |

__Example__
```java
List<SnowballObject> objects = new ArrayList<SnowballObject>();
objects.add(
    new SnowballObject(
        "my-object-one",
        new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
        5,
        null));
objects.add(
    new SnowballObject(
        "my-object-two",
        new ByteArrayInputStream("java".getBytes(StandardCharsets.UTF_8)),
        4,
        null));
minioClient.uploadSnowballObjects(
    UploadSnowballObjectsArgs.builder().bucket("my-bucketname").objects(objects).build());
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
### removeObjects(RemoveObjectsArgs args)
`public Iterable<Result<DeleteError>> removeObjects(RemoveObjectsArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObjects-io.minio.RemoveObjectsArgs-)_

Removes multiple objects lazily. Its required to iterate the returned Iterable to perform removal.

__Parameters__
| Parameter | Type                  | Description |
|:----------|:----------------------|:------------|
| ``args``  | _[RemoveObjectsArgs]_ | Arguments.  |

| Returns                                                                             |
|:------------------------------------------------------------------------------------|
| _Iterable<[Result]<[DeleteError]>>_ - Lazy iterator contains object removal status. |

__Example__
```java
List<DeleteObject> objects = new LinkedList<>();
objects.add(new DeleteObject("my-objectname1"));
objects.add(new DeleteObject("my-objectname2"));
objects.add(new DeleteObject("my-objectname3"));
Iterable<Result<DeleteError>> results =
    minioClient.removeObjects(
        RemoveObjectsArgs.builder().bucket("my-bucketname").objects(objects).build());
for (Result<DeleteError> result : results) {
  DeleteError error = result.get();
  System.out.println(
      "Error in deleting object " + error.objectName() + "; " + error.message());
}
```

<a name="restoreObject"></a>
### restoreObject(RestoreObjectArgs args)
`public void restoreObject(RestoreObjectArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#restoreObject-io.minio.RestoreObjectArgs-)_

Restores an object.

__Parameters__
| Parameter | Type                  | Description |
|:----------|:----------------------|:------------|
| ``args``  | _[RestoreObjectArgs]_ | Arguments.  |

__Example__
```java
// Restore object.
minioClient.restoreObject(
    RestoreObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-objectname")
        .request(new RestoreRequest(null, null, null, null, null, null))
        .build());

// Restore versioned object.
minioClient.restoreObject(
    RestoreObjectArgs.builder()
        .bucket("my-bucketname")
        .object("my-versioned-objectname")
        .versionId("my-versionid")
        .request(new RestoreRequest(null, null, null, null, null, null))
        .build());
```

 <a name="selectObjectContent"></a>
### selectObjectContent(SelectObjectContentArgs args)
`public SelectResponseStream selectObjectContent(SelectObjectContentArgs args)` _[[Javadoc]](http://minio.github.io/minio-java/io/minio/MinioClient.html#selectObjectContent-io.minio.SelectObjectContentArgs-)_

Selects content of a object by SQL expression.

__Parameters__

| Parameter           | Type                                | Description                           |
|:--------------------|:------------------------------------|:--------------------------------------|
| ``args``            | _[SelectObjectContentArgs]_           | Arguments.                            |

| Returns                                                            |
|:-------------------------------------------------------------------|
| _[SelectResponseStream]_ - Contains filtered records and progress. |

__Example__
```java
String sqlExpression = "select * from S3Object";
InputSerialization is = new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null);
OutputSerialization os = new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);
SelectResponseStream stream =
    minioClient.selectObjectContent(
        SelectObjectContentArgs.builder()
            .bucket("my-bucketname")
            .object("my-objectName")
            .sqlExpression(sqlExpression)
            .inputSerialization(is)
            .outputSerialization(os)
            .requestProgress(true)
            .build());

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
[LifecycleConfiguration]: http://minio.github.io/minio-java/io/minio/messages/LifecycleConfiguration.html
[DeleteBucketLifecycleArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketLifecycleArgs.html
[GetBucketLifecycleArgs]: http://minio.github.io/minio-java/io/minio/GetBucketLifecycleArgs.html
[SetBucketLifecycleArgs]: http://minio.github.io/minio-java/io/minio/SetBucketLifecycleArgs.html
[GetBucketPolicyArgs]: http://minio.github.io/minio-java/io/minio/GetBucketPolicyArgs.html
[SetBucketPolicyArgs]: http://minio.github.io/minio-java/io/minio/SetBucketPolicyArgs.html
[DeleteBucketPolicyArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketPolicyArgs.html
[GetObjectArgs]: http://minio.github.io/minio-java/io/minio/GetObjectArgs.html
[DownloadObjectArgs]: http://minio.github.io/minio-java/io/minio/DownloadObjectArgs.html
[BucketExistsArgs]: http://minio.github.io/minio-java/io/minio/BucketExistsArgs.html
[EnableObjectLegalHoldArgs]: http://minio.github.io/minio-java/io/minio/EnableObjectLegalHoldArgs.html
[DisableObjectLegalHoldArgs]: http://minio.github.io/minio-java/io/minio/DisableObjectLegalHoldArgs.html
[IsObjectLegalHoldEnabledArgs]: http://minio.github.io/minio-java/io/minio/IsObjectLegalHoldEnabledArgs.html
[DeleteBucketNotificationArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketNotificationArgs.html
[GetBucketNotificationArgs]: http://minio.github.io/minio-java/io/minio/GetBucketNotificationArgs.html
[SetBucketNotificationArgs]: http://minio.github.io/minio-java/io/minio/SetBucketNotificationArgs.html
[ListenBucketNotificationArgs]: http://minio.github.io/minio-java/io/minio/ListenBucketNotificationArgs.html
[SelectObjectContentArgs]: http://minio.github.io/minio-java/io/minio/SelectObjectContentArgs.html
[GetObjectLockConfigurationArgs]: http://minio.github.io/minio-java/io/minio/GetObjectLockConfigurationArgs.html
[SetObjectLockConfigurationArgs]: http://minio.github.io/minio-java/io/minio/SetObjectLockConfigurationArgs.html
[DeleteObjectLockConfigurationArgs]: http://minio.github.io/minio-java/io/minio/DeleteObjectLockConfigurationArgs.html
[GetPresignedObjectUrlArgs]: http://minio.github.io/minio-java/io/minio/GetPresignedObjectUrlArgs.html
[RemoveObjectsArgs]: http://minio.github.io/minio-java/io/minio/RemoveObjectsArgs.html
[CopyObjectArgs]: http://minio.github.io/minio-java/io/minio/CopyObjectArgs.html
[PutObjectArgs]: http://minio.github.io/minio-java/io/minio/PutObjectArgs.html
[UploadObjectArgs]: http://minio.github.io/minio-java/io/minio/UploadObjectArgs.html
[UploadSnowballObjectsArgs]: http://minio.github.io/minio-java/io/minio/UploadSnowballObjectsArgs.html
[ComposeObjectArgs]: http://minio.github.io/minio-java/io/minio/ComposeObjectArgs.html
[ObjectWriteResponse]: http://minio.github.io/minio-java/io/minio/ObjectWriteResponse.html
[ListBucketsArgs]: http://minio.github.io/minio-java/io/minio/ListBucketsArgs.html
[DeleteBucketReplicationArgs]: http://minio.github.io/minio-java/io/minio/DeleteBucketReplicationArgs.html
[GetBucketReplicationArgs]: http://minio.github.io/minio-java/io/minio/GetBucketReplicationArgs.html
[SetBucketReplicationArgs]: http://minio.github.io/minio-java/io/minio/SetBucketReplicationArgs.html
[ReplicationConfiguration]: http://minio.github.io/minio-java/io/minio/messages/ReplicationConfiguration.html
[VersioningConfiguration]: http://minio.github.io/minio-java/io/minio/messages/VersioningConfiguration.html
[GetBucketVersioningArgs]: http://minio.github.io/minio-java/io/minio/GetBucketVersioningArgs.html
[SetBucketVersioningArgs]: http://minio.github.io/minio-java/io/minio/SetBucketVersioningArgs.html
[RestoreObjectArgs]: http://minio.github.io/minio-java/io/minio/RestoreObjectArgs.html
