# Java Client API Reference [![Slack](https://slack.min.io/slack?type=svg)](https://slack.min.io)

## Initialize MinIO Client object.

## MinIO

```java
MinioClient minioClient = new MinioClient("https://play.min.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
```

## AWS S3


```java
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");
```

| Bucket operations |  Object operations | Presigned operations  | Bucket Policy/LifeCycle Operations
|:--- |:--- |:--- |:--- |
| [`makeBucket`](#makeBucket)  |[`getObject`](#getObject)   |[`presignedGetObject`](#presignedGetObject)   | [`getBucketPolicy`](#getBucketPolicy)   |
| [`listBuckets`](#listBuckets)  | [`putObject`](#putObject)  | [`presignedPutObject`](#presignedPutObject)  | [`setBucketPolicy`](#setBucketPolicy)   |
| [`bucketExists`](#bucketExists)  | [`copyObject`](#copyObject)  | [`presignedPostPolicy`](#presignedPostPolicy)  | [`setBucketLifeCycle`](#setBucketLifeCycle) |
| [`removeBucket`](#removeBucket)  | [`statObject`](#statObject) |   |  [`getBucketLifeCycle`](#getBucketLifeCycle) |
| [`listObjects`](#listObjects)  | [`removeObject`](#removeObject) |   |  [`deleteBucketLifeCycle`](#deleteBucketLifeCycle) |
| [`listIncompleteUploads`](#listIncompleteUploads)  | [`removeIncompleteUpload`](#removeIncompleteUpload) |   |   |
| [`listenBucketNotification`](#listenBucketNotification) |  |   |   |
| [`setBucketNotification`](#setBucketNotification) |  |   |   |
| [`getBucketNotification`](#getBucketNotification) |  |   |   |

## 1. Constructors


|  |
|---|
|`public MinioClient(String endpoint) throws InvalidEndpointException, InvalidPortException`   |
| Creates MinIO client object with given endpoint using anonymous access.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-)  |


|   |
|---|
|`public MinioClient(URL url) throws InvalidEndpointException, InvalidPortException`   |
| Creates MinIO client object with given url using anonymous access.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-)  |


|  |
|---|
| `public MinioClient(okhttp3.HttpUrl url) throws  InvalidEndpointException, InvalidPortException`  |
|Creates MinIO client object with given HttpUrl object using anonymous access.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-okhttp3.HttpUrl-)  |

|   |
|---|
| `public MinioClient(String endpoint, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException`  |
|  Creates MinIO client object with given endpoint, access key and secret key. |
|   [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-)|

|   |
|---|
| `public MinioClient(String endpoint, int port,  String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException`  |
| Creates MinIO client object with given endpoint, port, access key and secret key using secure (HTTPS) connection.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-)  |


|   |
|---|
| `public MinioClient(String endpoint, String accessKey, String secretKey, boolean secure) throws InvalidEndpointException, InvalidPortException`  |
| Creates MinIO client object with given endpoint, access key and secret key using secure (HTTPS) connection.  |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-boolean-) |


|   |
|---|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure) throws InvalidEndpointException, InvalidPortException`  |
| Creates MinIO client object using given endpoint, port, access key, secret key and secure option.  |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-boolean-) |

|   |
|---|
| `public MinioClient(okhttp3.HttpUrl url, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException`  |
| Creates MinIO client object with given URL object, access key and secret key.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-okhttp3.HttpUrl-java.lang.String-java.lang.String-)  |


|   |
|---|
| `public MinioClient(URL url, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException`  |
|  Creates MinIO client object with given URL object, access key and secret key. |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-java.lang.String-java.lang.String-) |



|   |
|---|
| `public MinioClient(String endpoint, String accessKey, String secretKey, String region) throws InvalidEndpointException, InvalidPortException`  |
|  Creates MinIO client object with given URL object, access key and secret key. |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-java.lang.String-) |



|   |
|---|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure) throws InvalidEndpointException, InvalidPortException`  |
|  Creates MinIO client object using given endpoint, port, access key, secret key, region and secure option. |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-java.lang.String-boolean-) |



|   |
|---|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure, okhttp3.OkHttpClient httpClient) throws InvalidEndpointException, InvalidPortException`  |
|  Creates MinIO client object using given endpoint, port, access key, secret key, region and secure option. |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-java.lang.String-boolean-) |


__Parameters__

| Param  | Type  | Description  |
|---|---|---|
| `endpoint`  |  _string_ | endPoint is an URL, domain name, IPv4 address or IPv6 address.Valid endpoints are listed below: |
| | |https://s3.amazonaws.com |
| | |https://play.min.io:9000 |
| | |localhost |
| | |play.min.io|
| `port` | _int_  | TCP/IP port number. This input is optional. Default value set to 80 for HTTP and 443 for HTTPs. |
| `accessKey`   | _string_   |accessKey is like user-id that uniquely identifies your account. |
|`secretKey`  |  _string_   | secretKey is the password to your account.|
|`secure`    | _boolean_    |If set to true, https is used instead of http. Default is https if not set. |
|`url`    | _URL_    |Endpoint URL object. |
|`url`    | _HttpURL_    |Endpoint HttpUrl object. |
|`region`    | _string_    |Region name to access service in endpoint. |


__Example__


### MinIO


```java
// 1. public MinioClient(String endpoint)
MinioClient minioClient = new MinioClient("https://play.min.io:9000");

// 2. public MinioClient(URL url)
MinioClient minioClient = new MinioClient(new URL("https://play.min.io:9000"));

// 3. public MinioClient(okhttp3.HttpUrl url)
 MinioClient minioClient = new MinioClient(new HttpUrl.parse("https://play.min.io:9000"));

// 4. public MinioClient(String endpoint, String accessKey, String secretKey)
MinioClient minioClient = new MinioClient("https://play.min.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 5. public MinioClient(String endpoint, int port,  String accessKey, String secretKey)
MinioClient minioClient = new MinioClient("https://play.min.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 6. public MinioClient(String endpoint, String accessKey, String secretKey, boolean insecure)
MinioClient minioClient = new MinioClient("https://play.min.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);

// 7. public MinioClient(String endpoint, int port,  String accessKey, String secretKey, boolean insecure)
MinioClient minioClient = new MinioClient("https://play.min.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);

// 8. public MinioClient(okhttp3.HttpUrl url, String accessKey, String secretKey)
 MinioClient minioClient = new MinioClient(new URL("https://play.min.io:9000"), "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 9. public MinioClient(URL url, String accessKey, String secretKey)
MinioClient minioClient = new MinioClient(HttpUrl.parse("https://play.min.io:9000"), "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 10. public MinioClient(String endpoint, String accessKey, String secretKey, String region)
MinioClient minioClient = new MinioClient("https://play.min.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "us-east-1");

// 11. public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
MinioClient minioClient = new MinioClient("play.min.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "us-east-1", true);

// 12. public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure, okhttp3.OkHttpClient httpClient)
MinioClient minioClient = new MinioClient("play.min.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "us-east-1", true, customHttpClient);
```


### AWS S3


```java
// 1. public MinioClient(String endpoint)
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com");

// 2. public MinioClient(URL url)
MinioClient minioClient = new MinioClient(new URL("https://s3.amazonaws.com"));

// 3. public MinioClient(okhttp3.HttpUrl url)
 MinioClient s3Client = new MinioClient(new HttpUrl.parse("https://s3.amazonaws.com"));

// 4. public MinioClient(String endpoint, String accessKey, String secretKey)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 5. public MinioClient(String endpoint, int port,  String accessKey, String secretKey)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 80, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 6. public MinioClient(String endpoint, String accessKey, String secretKey, boolean insecure)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", false);

// 7. public MinioClient(String endpoint, int port,  String accessKey, String secretKey, boolean insecure)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 80, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY",false);

// 8. public MinioClient(okhttp3.HttpUrl url, String accessKey, String secretKey)
 MinioClient s3Client = new MinioClient(new URL("s3.amazonaws.com"), "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 9. public MinioClient(URL url, String accessKey, String secretKey)
MinioClient s3Client = new MinioClient(HttpUrl.parse("s3.amazonaws.com"), "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 10. public MinioClient(String endpoint, String accessKey, String secretKey, String region)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "YOUR-BUCKETREGION");

// 11. public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 80, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "YOUR-BUCKETREGION", false);

// 12. public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure, okhttp3.OkHttpClient httpClient)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 80, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "YOUR-BUCKETREGION", false, customHttpClient);
```

## 2. Bucket operations

<a name="makeBucket"></a>
### makeBucket(String bucketName)
`public void makeBucket(String bucketName)`

Creates a new bucket with default region.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | Name of the bucket.  |


| Return Type	  | Exceptions	  |
|:--- |:--- |
| ``None``  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        |  ``RegionConflictException`` : upon  passed region conflicts with the one previously specified. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |



__Example__


```java
try {
  // Create bucket if it doesn't exist.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    System.out.println("mybucket already exists");
  } else {
    // Create bucket 'my-bucketname'.
    minioClient.makeBucket("mybucket");
    System.out.println("mybucket is created successfully");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="makeBucket"></a>
### makeBucket(String bucketName, String region)
`public void makeBucket(String bucketName, String region)`

Creates a new bucket with given region.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | Name of the bucket.  |
| ``region``  | _String_ | Region in which the bucket will be created.  |


| Return Type	  | Exceptions	  |
|:--- |:--- |
| ``None``  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        |  ``RegionConflictException`` : upon  passed region conflicts with the one previously specified. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |



__Example__


```java
try {
  // Create bucket if it doesn't exist.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    System.out.println("mybucket already exists");
  } else {
    // Create bucket 'my-bucketname'.
    minioClient.makeBucket("mybucket","us-east-1");
    System.out.println("mybucket is created successfully");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="listBuckets"></a>
### listBuckets()

`public List<Bucket> listBuckets()`

Lists all buckets.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listBuckets--)

|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``List Bucket`` : List of bucket type.  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


```java
try {
  // List buckets that have read access.
  List<Bucket> bucketList = minioClient.listBuckets();
  for (Bucket bucket : bucketList) {
    System.out.println(bucket.creationDate() + ", " + bucket.name());
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="bucketExists"></a>
### bucketExists(String bucketName)

`public boolean bucketExists(String bucketName)`

Checks if a bucket exists.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#bucketExists-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``boolean``: true if the bucket exists           | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__


```java
try {
  // Check whether 'my-bucketname' exists or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    System.out.println("mybucket exists");
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="removeBucket"></a>
### removeBucket(String bucketName)

`public void removeBucket(String bucketName)`

Removes a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeBucket-java.lang.String-)

NOTE: -  removeBucket does not delete the objects inside the bucket. The objects need to be deleted using the removeObject API.


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


```java
try {
  // Check if my-bucket exists before removing it.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // Remove bucket my-bucketname. This operation will succeed only if the bucket is empty.
    minioClient.removeBucket("mybucket");
    System.out.println("mybucket is removed successfully");
  } else {
    System.out.println("mybucket does not exist");
  }
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="listObjects"></a>
### listObjects(String bucketName)

`public Iterable<Result<Item>> listObjects(String bucketName)`

Lists object information in given bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |

|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<Item>>``:an iterator of Result Items.  | _None_  |


__Example__


```java
try {
  // Check whether 'mybucket' exists or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // List objects from 'my-bucketname'
    Iterable<Result<Item>> myObjects = minioClient.listObjects("mybucket");
    for (Result<Item> result : myObjects) {
      Item item = result.get();
      System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
    }
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="listObjects"></a>
### listObjects(String bucketName, String prefix)

`public Iterable<Result<Item>> listObjects(String bucketName, String prefix))`

Lists object information in given bucket and prefix.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``prefix``  | _String_  | Prefix string. List objects whose name starts with ``prefix``. |

|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<Item>>``:an iterator of Result Items.  | _None_  |


__Example__


```java
try {
  // Check whether 'mybucket' exists or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // List objects from 'my-bucketname'
    Iterable<Result<Item>> myObjects = minioClient.listObjects("mybucket","minio");
    for (Result<Item> result : myObjects) {
      Item item = result.get();
      System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
    }
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="listObjects"></a>
### listObjects(String bucketName, String prefix, boolean recursive)

`public Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive)`

Lists object information as Iterable<Result><Item> in given bucket, prefix and recursive flag.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-boolean-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``prefix``  | _String_  | Prefix string. List objects whose name starts with ``prefix``. |
| ``recursive``  | _boolean_  | when false, emulates a directory structure where each listing returned is either a full object or part of the object's key up to the first '/'. All objects with the same prefix up to the first '/' will be merged into one entry. |


|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<Item>>``:an iterator of Result Items.  | _None_  |


__Example__


```java
try {
  // Check whether 'mybucket' exists or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // List objects from 'my-bucketname'
    Iterable<Result<Item>> myObjects = minioClient.listObjects("mybucket","minio",true);
    for (Result<Item> result : myObjects) {
      Item item = result.get();
      System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
    }
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```



<a name="listObjects"></a>
### listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)

`public Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)`

Lists all objects in a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-boolean-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``prefix``  | _String_  | Prefix string. List objects whose name starts with ``prefix``. |
| ``recursive``  | _boolean_  | when false, emulates a directory structure where each listing returned is either a full object or part of the object's key up to the first '/'. All objects with the same prefix up to the first '/' will be merged into one entry. |
| ``useVersion1``  | _boolean_  | when true, version 1 of REST API is used. |


|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<Item>>``:an iterator of Result Items.  | _None_  |


__Example__


```java
try {
  // Check whether 'mybucket' exists or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // List objects from 'my-bucketname'
    Iterable<Result<Item>> myObjects = minioClient.listObjects("mybucket");
    for (Result<Item> result : myObjects) {
      Item item = result.get();
      System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
    }
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="setBucketLifeCycle"></a>
### setBucketLifeCycle(String bucketName, String lifeCycle)
`public void setBucketLifeCycle(String bucketName, String lifeCycle)`

Set a life cycle on bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketLifeCycle-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``lifeCycle`` | _String_ | Life cycle XML for the bucket. |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |

__Example__


```java
try {
    /* Amazon S3: */
  MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
                                          "YOUR-SECRETACCESSKEY");
  String lifeCycle = "<LifecycleConfiguration><Rule><ID>expire-bucket</ID><Prefix></Prefix>"
                + "<Status>Enabled</Status><Expiration><Days>365</Days></Expiration>"
                + "</Rule></LifecycleConfiguration>";


  minioClient.setBucketLifecycle("lifecycleminiotest", lifeCycle);
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getBucketLifeCycle"></a>
### getBucketLifeCycle(String bucketName)
`public String getBucketLifeCycle(String bucketName)`

Get the lifecycle of the bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketLifeCycle-java.lang.String-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__


```java

try {
   /* Amazon S3: */
   MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
            "YOUR-SECRETACCESSKEY");
   String lifecycle = minioClient.getBucketLifecycle("my-bucketName" );
   System.out.println(" Life Cycle is : " + lifecycle );
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="deleteBucketLifeCycle"></a>
### deleteBucketLifeCycle(String bucketName)
`private void deleteBucketLifeCycle(String bucketName)`

Delete the lifecycle of the bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#deleteBucketLifeCycle-java.lang.String-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__


```java

try {
   /* Amazon S3: */
   MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
            "YOUR-SECRETACCESSKEY");
   minioClient.deleteBucketLifeCycle("my-bucketName" );
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="listenBucketNotification"></a>
### listenBucketNotification(String bucketName, String prefix, String suffix, String[] events, BucketEventListener listener)
`public void listenBucketNotification(String bucketName, String prefix, String suffix, String[] events, BucketEventListener listener)`

Listen to events related to objects under the specified bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listenBucketNotification-java.lang.String-java.lang.String-java.lang.String-java.lang.String:A-io.minio.BucketEventListener-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``prefix`` | _String_ | Only listen for objects with the given prefix. |
| ``suffix`` | _String_ | Only listen for objects with the given suffix. |
| ``events`` | _String[]_ | Only listen for the specified events, such as s3:ObjectCreated:*, s3:ObjectAccessed:*, s3:ObjectRemoved:*, ..  |
| ``listener`` | _BucketEventListener_ | Interface with updateEvent method |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading the InputStream gets end of file exception before reading the complete length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


```java
  try {
    class TestBucketListener implements BucketEventListener {
      @Override
      public void updateEvent(NotificationInfo info) {
        System.out.println(info.records[0].s3.bucket.name + "/"
           + info.records[0].s3.object.key + " has been created");
      }
    }

    minioClient.listenBucketNotification("testbucket", "", "",
        new String[]{"s3:ObjectCreated:*", "s3:ObjectAccessed:*"}, new TestBucketListener());
  } catch (Exception e) {
    System.out.println("Error occurred: " + e);
  }
  ```

  <a name="setBucketNotification"></a>
### setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)
`public void setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)`

Set bucket notification configuration.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketNotification-java.lang.String-io.minio.messages.NotificationConfiguration-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``notificationConfiguration`` | _NotificationConfiguration_ | Notification configuration to be set. |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``InvalidObjectPrefixException`` : upon invalid object prefix. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` :  Thrown to indicate that reading the InputStream gets end of file exception before reading the complete length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


```java
 try {
      NotificationConfiguration notificationConfiguration = minioClient.getBucketNotification("my-bucketname");

      // Add a new SQS configuration.
      List<QueueConfiguration> queueConfigurationList = notificationConfiguration.queueConfigurationList();
      QueueConfiguration queueConfiguration = new QueueConfiguration();
      queueConfiguration.setQueue("arn:minio:sqs::1:webhook");

      List<EventType> eventList = new LinkedList<>();
      eventList.add(EventType.OBJECT_CREATED_PUT);
      eventList.add(EventType.OBJECT_CREATED_COPY);
      queueConfiguration.setEvents(eventList);

      Filter filter = new Filter();
      filter.setPrefixRule("images");
      filter.setSuffixRule("pg");
      queueConfiguration.setFilter(filter);

      queueConfigurationList.add(queueConfiguration);
      notificationConfiguration.setQueueConfigurationList(queueConfigurationList);

      // Set updated notification configuration.
      minioClient.setBucketNotification("my-bucketname", notificationConfiguration);
      System.out.println("Bucket notification is set successfully");
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
```

  <a name="getBucketNotification"></a>
### getBucketNotification(String bucketName)
`public NotificationConfiguration getBucketNotification(String bucketName)`

Get bucket notification configuration.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketNotification-java.lang.String-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``NotificationConfiguration``:  NotificationConfiguration    | Listed Exceptions: |
|        | ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``InvalidObjectPrefixException`` : upon invalid object prefix. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` :  Thrown to indicate that reading the InputStream gets end of file exception before reading the complete length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


```java
 try {
      /* play.min.io for test and development. */
      MinioClient minioClient = new MinioClient("https://play.min.io:9000", "Q3AM3UQ867SPQQA43P2F",
                                                "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
      NotificationConfiguration notificationConfiguration = minioClient.getBucketNotification("my-bucketname");
      System.out.println(notificationConfiguration);
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
```



<a name="listIncompleteUploads"></a>
###  listIncompleteUploads(String bucketName)

`public Iterable<Result<Upload>>  listIncompleteUploads(String bucketName)`

Lists partially uploaded objects in a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |


|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<Upload>>``: an iterator of Upload.  | _None_  |


__Example__


```java
try {
  // Check whether 'mybucket' exist or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // List all incomplete multipart upload of objects in 'my-bucketname
    Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("mybucket");
    for (Result<Upload> result : myObjects) {
      Upload upload = result.get();
      System.out.println(upload.uploadId() + ", " + upload.objectName());
    }
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="listIncompleteUploads"></a>
###  listIncompleteUploads(String bucketName, String prefix)

`public Iterable<Result<Upload>>  listIncompleteUploads(String bucketName, String prefix)`

Lists incomplete uploads of objects in given bucket and prefix.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``prefix``  | _String_  | Prefix string. List objects whose name starts with ``prefix``. |


|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<Upload>>``: an iterator of Upload.  | _None_  |


__Example__


```java
try {
  // Check whether 'mybucket' exist or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // List all incomplete multipart upload of objects in 'my-bucketname
    Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("mybucket", "minio");
    for (Result<Upload> result : myObjects) {
      Upload upload = result.get();
      System.out.println(upload.uploadId() + ", " + upload.objectName());
    }
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="listIncompleteUploads"></a>
### listIncompleteUploads(String bucketName, String prefix, boolean recursive)

`public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix, boolean recursive)`

Lists partially uploaded objects in a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-java.lang.String-boolean-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``prefix``  | _String_  | Prefix string. List objects whose name starts with ``prefix``. |
| ``recursive``  | _boolean_  | when false, emulates a directory structure where each listing returned is either a full object or part of the object's key up to the first '/'. All objects with the same prefix up to the first '/' will be merged into one entry. |


|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<Upload>>``: an iterator of Upload.  | _None_  |


__Example__


```java
try {
  // Check whether 'mybucket' exist or not.
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // List all incomplete multipart upload of objects in 'my-bucketname
    Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("mybucket", "minio", true);
    for (Result<Upload> result : myObjects) {
      Upload upload = result.get();
      System.out.println(upload.uploadId() + ", " + upload.objectName());
    }
  } else {
    System.out.println("mybucket does not exist");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getBucketPolicy"></a>
### getBucketPolicy(String bucketName)
`public PolicyType getBucketPolicy(String bucketName)`

Get bucket policy for a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketPolicy-java.lang.String-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  _String_: Bucket policy JSON string. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``InvalidObjectPrefixException`` : upon invalid object prefix.        |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidBucketNameException `` : upon invalid bucket name.       |
|        | ``BucketPolicyTooLargeException `` : upon bucket policy too large in size       |

__Example__


```java
try {
  System.out.println("Current policy: " + minioClient.getBucketPolicy("myBucket"));
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="setBucketPolicy"></a>
### setBucketPolicy(String bucketName, String policy)
`public void setBucketPolicy(String bucketName, String policy)`

Set a policy on bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketPolicy-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type   | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``policy`` | _String_ | Policy JSON for the bucket. |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``InvalidObjectPrefixException`` : upon invalid object prefix.        |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__


```java
try {
  StringBuilder builder = new StringBuilder();
  builder.append("{\n");
  builder.append("    \"Statement\": [\n");
  builder.append("        {\n");
  builder.append("            \"Action\": [\n");
  builder.append("                \"s3:GetBucketLocation\",\n");
  builder.append("                \"s3:ListBucket\"\n");
  builder.append("            ],\n");
  builder.append("            \"Effect\": \"Allow\",\n");
  builder.append("            \"Principal\": \"*\",\n");
  builder.append("            \"Resource\": \"arn:aws:s3:::my-bucketname\"\n");
  builder.append("        },\n");
  builder.append("        {\n");
  builder.append("            \"Action\": \"s3:GetObject\",\n");
  builder.append("            \"Effect\": \"Allow\",\n");
  builder.append("            \"Principal\": \"*\",\n");
  builder.append("            \"Resource\": \"arn:aws:s3:::my-bucketname/myobject*\"\n");
  builder.append("        }\n");
  builder.append("    ],\n");
  builder.append("    \"Version\": \"2012-10-17\"\n");
  builder.append("}\n");
  minioClient.setBucketPolicy("my-bucketname", builder.toString());
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

## 3. Object operations

<a name="getObject"></a>
### getObject(String bucketName, String objectName)

`public InputStream getObject(String bucketName, String objectName)`

Downloads an object as a stream.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``InputStream``: InputStream containing the object data.  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__


```java
try {
  // Check whether the object exists using statObject().
  // If the object is not found, statObject() throws an exception,
  // else it means that the object exists.
  // Execution is successful.
  minioClient.statObject("mybucket", "myobject");

  // Get input stream to have content of 'my-objectname' from 'my-bucketname'
  InputStream stream = minioClient.getObject("mybucket", "myobject");

  // Read the input stream and print to the console till EOF.
  byte[] buf = new byte[16384];
  int bytesRead;
  while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead));
  }

  // Close the input stream.
  stream.close();
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="getObject"></a>
### getObject(String bucketName, String objectName, long offset)

`public InputStream getObject(String bucketName, String objectName, long offset)`

Gets object's data starting from given offset as InputStream in the given bucket. The InputStream must be closed after use else the connection will remain open.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``offset``  | _Long_  | ``offset`` of the object from where the stream will start. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``InputStream``: InputStream containing the object data.  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__


```java
try {

  // Check whether the object exists using statObject().
  // If the object is not found, statObject() throws an exception,
  // else it means that the object exists.
  // Execution is successful.
  minioClient.statObject("mybucket", "myobject");

  // Get input stream to have content of 'my-objectname' from 'my-bucketname'
  InputStream stream = minioClient.getObject("mybucket", "myobject", 1024L);

  // Read the input stream and print to the console till EOF.
  byte[] buf = new byte[16384];
  int bytesRead;
  while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead));
  }

  // Close the input stream.
  stream.close();
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, long offset, Long length)

`public InputStream getObject(String bucketName,  String objectName, long offset, Long length)`

Downloads the specified range bytes of an object as a stream.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-java.lang.Long-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``offset``  | _Long_  | ``offset`` of the object from where the stream will start. |
| ``length``  | _Long_  | ``length`` of the object that will be read in the stream (optional, if not specified we read the rest of the file from the offset). |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``InputStream`` : InputStream containing the object's data. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__


```java
try {

  // Check whether the object exists using statObject().
  // If the object is not found, statObject() throws an exception,
  // else it means that the object exists.
  // Execution is successful.
  minioClient.statObject("mybucket", "myobject");

  // Get input stream to have content of 'my-objectname' from 'my-bucketname'
  InputStream stream = minioClient.getObject("mybucket", "myobject", 1024L, 4096L);

  // Read the input stream and print to the console till EOF.
  byte[] buf = new byte[16384];
  int bytesRead;
  while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead));
  }

  // Close the input stream.
  stream.close();
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, String fileName)

`public void getObject(String bucketName, String objectName, String fileName)`

Downloads and saves the object as a file in the local filesystem.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``fileName``  | _String_  | File name. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |

__Example__

```java
try {
  // Check whether the object exists using statObject().
  // If the object is not found, statObject() throws an exception,
  // else it means that the object exists.
  // Execution is successful.
  minioClient.statObject("mybucket", "myobject");

  // Gets the object's data and stores it in photo.jpg
  minioClient.getObject("mybucket", "myobject", "photo.jpg");

} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, ServerSideEncryption sse)

`public InputStream getObject(String bucketName, String objectName, ServerSideEncryption sse)`

Gets entire object's data as InputStream in given bucket. The InputStream must be closed after use else the connection will remain open.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``sse``  | _ServerSideEncryption_  | Form of server-side encryption [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.  |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |

__Example__

```java
try {
 // Check whether the object exists using statObject().
  // If the object is not found, statObject() throws an exception,
  // else it means that the object exists.
  // Execution is successful.
  minioClient.statObject("mybucket", "myobject");

   InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", sse);
   byte[] buf = new byte[16384];
   int bytesRead;
   while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
   System.out.println(new String(buf, 0, bytesRead));
   }
   stream.close();
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```
<a name="putObject"></a>
### putObject(String bucketName, String objectName, String fileName, String contentType)

`public void putObject(String bucketName, String objectName, String fileName, String contentType)`

Uploads given file as object in given bucket.
If the object is larger than 5MB, the client will automatically use a multipart session.

If the multipart session fails, the uploaded parts are aborted automatically.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.lang.String-java.lang.String-)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``fileName``  | _String_  | File name to upload. |
| ``contentType``  | _String_ | File content type of the object, user supplied. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


```java
try {
  minioClient.putObject("mybucket",  "island.jpg", "/mnt/photos/island.jpg" ,"application/octet-stream")
  System.out.println("island.jpg is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```
<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, String contentType)

`public void putObject(String bucketName, String objectName, InputStream stream, String contentType)`

Uploads data from given stream as object to given bucket where the stream size is unknown.
If the stream has more than 525MiB data, the client uses a multipart session automatically.

If the multipart session fails, the uploaded parts are aborted automatically.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-java.lang.String-)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  |  stream to upload. |
| ``contentType``  | _String_ | Content type of the stream. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__


```java
 StringBuilder builder = new StringBuilder();
 for (int i = 0; i < 1000; i++) {
   builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
   builder.append("(29 letters)\n");
   builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
   builder.append("(31 letters)\n");
   builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
   builder.append("NASAs Space Shuttle. (32 letters)\n");
   builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
   builder.append("(39 letters)\n");
   builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
   builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
   builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
   builder.append("computers after System 7.\n");
   builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
   builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
   builder.append("---\n");
 }
 ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
 // create object
 minioClient.putObject("my-bucketname", "my-objectname", bais, "application/octet-stream");
 bais.close();
 System.out.println("my-objectname is uploaded successfully");
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)

`public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)`

Uploads an object from an InputStream.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-java.lang.String-)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``size``  | _long_  | Size of the data to read from ``stream`` that will be uploaded. |
| ``contentType``  | _String_ | Content type of the stream. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


The maximum size of a single object is limited to 5TB. putObject transparently uploads objects larger than 5MiB in multiple parts.


```java
try {
  StringBuilder builder = new StringBuilder();
  for (int i = 0; i < 1000; i++) {
    builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
    builder.append("(29 letters)\n");
    builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
    builder.append("(31 letters)\n");
    builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
    builder.append("NASAs Space Shuttle. (32 letters)\n");
    builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
    builder.append("(39 letters)\n");
    builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
    builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
    builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
    builder.append("computers after System 7.\n");
    builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
    builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
    builder.append("- --\n");
  }
  ByteArrayInputStream bais = new
  ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
  // Create an object
  minioClient.putObject("mybucket", "myobject", bais, bais.available(), "application/octet-stream");
  bais.close();
  System.out.println("myobject is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, String fileName)

`public void putObject(String bucketName, String objectName, String fileName)`

Uploads contents from a file to objectName.
[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``fileName``  | _String_  | File name. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__


The maximum size of a single object is limited to 5TB. putObject transparently uploads objects larger than 5MB in multiple parts. Uploaded data is carefully verified using MD5SUM signatures.


```java
try {
  minioClient.putObject("mybucket",  "island.jpg", "/mnt/photos/island.jpg")
  System.out.println("island.jpg is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, long size, ServerSideEncryption sse)

`public void putObject(String bucketName, String objectName, InputStream stream, long size, ServerSideEncryption sse)`

Uploads data from given stream as object to given bucket where the stream size is unknown.
If the stream has more than 525MiB data, the client uses a multipart session automatically.

If the multipart session fails, the uploaded parts are aborted automatically.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-io.minio.ServerSideEncryption-)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``size``  | _long_  | Size of the data to read from ``stream`` that will be uploaded. |
| ``sse``  | _ServerSideEncryption_  | Form of server-side encryption [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__

```java
StringBuilder builder = new StringBuilder();
 for (int i = 0; i < 1000; i++) {
   builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
   builder.append("(29 letters)\n");
   builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
   builder.append("(31 letters)\n");
   builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
   builder.append("NASAs Space Shuttle. (32 letters)\n");
   builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
   builder.append("(39 letters)\n");
   builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
   builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
   builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
   builder.append("computers after System 7.\n");
   builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
   builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
   builder.append("---\n");
 }
 ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
 // create object
 minioClient.putObject("my-bucketname", "my-objectname", bais, "application/octet-stream");
 bais.close();
 System.out.println("my-objectname is uploaded successfully");
```


<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, Map<String,String> headerMap)

`public void putObject(String bucketName, String objectName, InputStream stream, long size, Map<String,String> headerMap)`
Uploads data from given stream as object to given bucket with specified meta data.
If the object is larger than 5MB, the client will automatically use a multipart session.

If the session fails, the user may attempt to re-upload the object by attempting to create the exact same object again. The client will examine all parts of any current upload session and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail before uploading any more data. Otherwise, it will resume uploading where the session left off.

If the multipart session fails, the user is responsible for resuming or removing the session.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.io.InputStream-long-java.util.Map-)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``headerMap``  | _Map<String,String>_  | Custom/additional meta data of the object. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__

```java
 StringBuilder builder = new StringBuilder();
 for (int i = 0; i < 1000; i++) {
   builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
   builder.append("(29 letters)\n");
   builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
   builder.append("(31 letters)\n");
   builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
   builder.append("NASAs Space Shuttle. (32 letters)\n");
   builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
   builder.append("(39 letters)\n");
   builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
   builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
   builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
   builder.append("computers after System 7.\n");
   builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
   builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
   builder.append("---\n");
 }
 ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
 // create object
 Map<String, String> headerMap = new HashMap<>();
 headerMap.put("Content-Type", "application/octet-stream");
 minioClient.putObject("my-bucketname", "my-objectname", bais, headerMap);
 bais.close();
 System.out.println("my-objectname is uploaded successfully");
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, long size, Map<String,String> headerMap)

`public void putObject(String bucketName, String objectName, InputStream stream, long size, Map<String,String> headerMap)`
Uploads data from given stream as object to given bucket with specified meta data.
If the object is larger than 5MB, the client will automatically use a multipart session.

If the multipart session fails, the uploaded parts are aborted automatically.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-java.util.Map-)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``size``  | _long_  | Size of the data to read from ``stream`` that will be uploaded. |
| ``headerMap``  | _Map<String,String>_  | Custom/additional meta data of the object. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__

```java
 StringBuilder builder = new StringBuilder();
 for (int i = 0; i < 1000; i++) {
   builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
   builder.append("(29 letters)\n");
   builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
   builder.append("(31 letters)\n");
   builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
   builder.append("NASAs Space Shuttle. (32 letters)\n");
   builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
   builder.append("(39 letters)\n");
   builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
   builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
   builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
   builder.append("computers after System 7.\n");
   builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
   builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
   builder.append("---\n");
 }
 ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
 // create object
 Map<String, String> headerMap = new HashMap<>();
 headerMap.put("Content-Type", "application/octet-stream");
 minioClient.putObject("my-bucketname", "my-objectname", bais, bais.available(), headerMap);
 bais.close();
 System.out.println("my-objectname is uploaded successfully");
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, long size, Map<String, String> headerMap, SecretKey key)
 `public void putObject(String bucketName, String objectName, InputStream stream, long size, Map<String, String> headerMap,
		  SecretKey key)`
 Takes data from given stream, encrypts it using a random content key and uploads it as object to given bucket. Also
uploads the encrypted content key and iv as header of the encrypted object. The content key is encrypted using the
master key passed to this function.
 Any custom or additional meta data can also be provided through `headerMap`.
 If the object is larger than 5MB, the client will automatically perform multi part upload.
 [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-java.lang.String-javax.crypto.SecretKey-)
 __Parameters__
 |Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``size``  | _long_  | Size of the data to read from ``stream`` that will be uploaded. |
| ``headerMap``  | Map<String, String> | Custom/additional meta data of the object. |
| ``key``  | _SecretKey_ | An object of type initialized with AES [SecretKey](https://docs.oracle.com/javase/7/docs/api/javax/crypto/SecretKey.html).  |
 | Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
| 		 | ``InvalidAlgorithmParameterException`` : upon wrong encryption algorithm used. |
|		 | ``BadPaddingException`` : upon incorrect padding in a block. |
|		 | ``IllegalBlockSizeException`` : upon incorrect block. |
|		 | ``NoSuchPaddingException`` : upon wrong padding type specified. |
 __Example__
 Object is encrypted using a randomly generated data encryption key. The data encryption key is then encrypted using a master key known only to the client (wrapped in encryptionMaterials object). The encrypted data encryption key is uploaded as the object header along with the IV used and the encrypted object to the remote server.
 ```java
try {
  StringBuilder builder = new StringBuilder();
  for (int i = 0; i < 1000; i++) {
    builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
    builder.append("(29 letters)\n");
    builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
    builder.append("(31 letters)\n");
    builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
    builder.append("NASAs Space Shuttle. (32 letters)\n");
    builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
    builder.append("(39 letters)\n");
    builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
    builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
    builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
    builder.append("computers after System 7.\n");
    builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
    builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
    builder.append("- --\n");
  }
  ByteArrayInputStream bais = new
  ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
  
  // create object
  Map<String, String> headerMap = new HashMap<>();
  headerMap.put("Content-Type", "application/octet-stream");
  headerMap.put("X-Amz-Meta-Key", "meta-data");
  
  //Generate symmetric 256 bit AES key.
  KeyGenerator symKeyGenerator = KeyGenerator.getInstance("AES");
  symKeyGenerator.init(256);
  SecretKey symKey = symKeyGenerator.generateKey();
  
  // Create an object
  minioClient.putObject("mybucket", "myobject", bais, bais.available(), headerMap, symKey);
  bais.close();
  System.out.println("myobject is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="getObject"></a>
### getObject(String bucketName, String objectName, ServerSideEncryption sse, String fileName)

`public void getObject(String bucketName, String objectName, ServerSideEncryption sse, String fileName)`

Download the contents from an encrypted objectName to a given file.
[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``sse``  | _ServerSideEncryption_  | Form of server-side encryption [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |
| ``fileName``  | _String_  | File name to download into. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon passing of an invalid value to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__


```java
try {
  KeyGenerator keyGen = KeyGenerator.getInstance("AES");
  keyGen.init(256);
  ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
  minioClient.putObject("mybucket",  "island.jpg", sse, "/mnt/photos/island.jpg")
  System.out.println("island.jpg is uploaded successfully");
  minioClient.getObject("mybucket",  "island.jpg", sse, "/mnt/photos/islandCopy.jpg")
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="putObject"></a>
### putObject(String bucketName, String objectName, ServerSideEncryption sse, String fileName)

`public void putObject(String bucketName, String objectName, ServerSideEncryption sse, String fileName)`

Uploads contents from a file to objectName and encrypt with a sse key.
[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``sse``  | _ServerSideEncryption_  | Form of server-side encryption [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |
| ``fileName``  | _String_  | File name. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__


```java
try {
  KeyGenerator keyGen = KeyGenerator.getInstance("AES");
  keyGen.init(256);
  ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
  minioClient.putObject("mybucket",  "island.jpg", sse, "/mnt/photos/island.jpg")
  System.out.println("island.jpg is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, Map<String,String> headerMap, ServerSideEncryption sse)

`public void putObject(String bucketName, String objectName, InputStream stream, Map<String,String> headerMap, ServerSideEncryption sse)`
Uploads data from given stream as object to given bucket with specified meta data and encrypt with a sse key.
If the object is larger than 5MB, the client will automatically use a multipart session.

If the session fails, the user may attempt to re-upload the object by attempting to create the exact same object again. The client will examine all parts of any current upload session and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail before uploading any more data. Otherwise, it will resume uploading where the session left off.

If the multipart session fails, the user is responsible for resuming or removing the session.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.util.Map-io.minio.ServerSideEncryption-)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``headerMap``  | _Map<String,String>_  | Custom/additional meta data of the object. |
| ``sse``  | _ServerSideEncryption_  | Form of server-side encryption [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |



| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__

```java
 StringBuilder builder = new StringBuilder();
 for (int i = 0; i < 1000; i++) {
   builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
   builder.append("(29 letters)\n");
   builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
   builder.append("(31 letters)\n");
   builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
   builder.append("NASAs Space Shuttle. (32 letters)\n");
   builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
   builder.append("(39 letters)\n");
   builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
   builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
   builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
   builder.append("computers after System 7.\n");
   builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
   builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
   builder.append("---\n");
 }
 ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
 KeyGenerator keyGen = KeyGenerator.getInstance("AES");
 keyGen.init(256);
 ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
 // create object
 Map<String, String> headerMap = new HashMap<>();
 headerMap.put("Content-Type", "application/octet-stream");
 minioClient.putObject("my-bucketname", "my-objectname", bais, headerMap, sse);
 bais.close();
 System.out.println("my-objectname is uploaded successfully");
```


<a name="putObject"></a>
### putObject(String bucketName, String objectName, String fileName, Long size, Map<String, String> headerMap, ServerSideEncryption sse, String contentType)

`public void putObject(String bucketName, String objectName, String fileName, Long size, Map<String, String> headerMap, ServerSideEncryption sse, String contentType)`
Uploads contents from a file as object to given bucket with specified meta data and encrypt with a sse key.
If the object is larger than 5MB, the client will automatically use a multipart session.

If the session fails, the user may attempt to re-upload the object by attempting to create the exact same object again. The client will examine all parts of any current upload session and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail before uploading any more data. Otherwise, it will resume uploading where the session left off.

If the multipart session fails, the user is responsible for resuming or removing the session.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.lang.String-java.lang.Long-java.util.Map-io.minio.ServerSideEncryption-java.lang.String)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``fineName``  | _String_  | File name to upload. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``size``  | _long_  | Size of the file. |
| ``headerMap``  | _Map<String,String>_  | Custom/additional meta data of the object. |
| ``sse``  | _ServerSideEncryption_  | Form of server-side encryption [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |
| ``contentType``  | _String_ | File content type of the object, user supplied. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__

```java
try {
  KeyGenerator keyGen = KeyGenerator.getInstance("AES");
  keyGen.init(256);
  ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
  Map<String, String> headerMap = new HashMap<>();
  headerMap.put("my-custom-data", "foo");
  minioClient.putObject("mybucket",  "island.jpg", "/mnt/photos/island.jpg",headerMap,sse, "application/octet-stream" );
  System.out.println("island.jpg is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}


<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, Long size, Map<String, String> headerMap, ServerSideEncryption sse, String contentType)

`public void putObject(String bucketName, String objectName, InputStream stream, Long size, Map<String, String> headerMap, ServerSideEncryption sse, String contentType)`
Uploads data from given stream as object to given bucket with specified meta data and encrypt with a sse key.
If the object is larger than 5MB, the client will automatically use a multipart session.

If the session fails, the user may attempt to re-upload the object by attempting to create the exact same object again. The client will examine all parts of any current upload session and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail before uploading any more data. Otherwise, it will resume uploading where the session left off.

If the multipart session fails, the user is responsible for resuming or removing the session.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-java.util.Map-io.minio.ServerSideEncryption-java.lang.String)


__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``stream``  | _InputStream_  | stream to upload. |
| ``size``  | _long_  | Size of the data to read from ``stream`` that will be uploaded. |
| ``headerMap``  | _Map<String,String>_  | Custom/additional meta data of the object. |
| ``sse``  | _ServerSideEncryption_  | Form of server-side encryption [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |
| ``contentType``  | _String_ | File content type of the object, user supplied. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |

__Example__

```java
 StringBuilder builder = new StringBuilder();
 for (int i = 0; i < 1000; i++) {
   builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
   builder.append("(29 letters)\n");
   builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
   builder.append("(31 letters)\n");
   builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
   builder.append("NASAs Space Shuttle. (32 letters)\n");
   builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
   builder.append("(39 letters)\n");
   builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
   builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
   builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
   builder.append("computers after System 7.\n");
   builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
   builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
   builder.append("---\n");
 }
 ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
 KeyGenerator keyGen = KeyGenerator.getInstance("AES");
 keyGen.init(256);
 ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
 // create object
 Map<String, String> headerMap = new HashMap<>();
 headerMap.put("my-custom-data", "foo");
 minioClient.putObject("my-bucketname", "my-objectname", bais, bias.available(), headerMap, sse, contentType);
 bais.close();
 System.out.println("my-objectname is uploaded successfully");
```


<a name="statObject"></a>
### statObject(String bucketName, String objectName)

*`public ObjectStat statObject(String bucketName, String objectName)`*

Gets metadata of an object.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#statObject-java.lang.String-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``ObjectStat``: Populated object meta data. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__


```java
try {
  // Get the metadata of the object.
  ObjectStat objectStat = minioClient.statObject("mybucket", "myobject");
  System.out.println(objectStat);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="statObject"></a>
### statObject(String bucketName, String objectName, ServerSideEncryption sse)

*`public ObjectStat statObject(String bucketName, String objectName, ServerSideEncryption sse)`*

Returns meta data information of given object in given bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#statObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``sse``  | _ServerSideEncryption_  | Encryption metadata only required for SSE-C. [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``ObjectStat``: Populated object meta data. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |

__Example__


```java
try {
  // Get the metadata of the object.
  ObjectStat objectStat = minioClient.statObject("my-bucketname", "my-objectname", sse);
  System.out.println(objectStat);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, String destBucketName)

*`public void copyObject(String bucketName, String objectName, String destBucketName)`*

Copy a source object into a new destination object with same object name.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the source bucket.  |
| ``objectName``  | _String_  | Object name in the source bucket to be copied. |
| ``destBucketName``  | _String_  | Destination bucket name. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__


```java
minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname");
```


<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, String destBucketName, CopyConditions copyConditions)

*`public void copyObject(String bucketName, String objectName, String destBucketName, CopyConditions copyConditions)`*

Copy a source object into a new object with the provided name in the provided bucket. optionally can take a key value CopyConditions as well for conditionally attempting copyObject.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.lang.String-io.minio.CopyConditions-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the source bucket.  |
| ``objectName``  | _String_  | Object name in the source bucket to be copied. |
| ``destBucketName``  | _String_  | Destination bucket name. |
| ``copyConditions`` | _CopyConditions_ | Map of conditions useful for applying restrictions on copy operation.|


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__

```java
minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname", copyConditions);
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, String destBucketName, String destObjectName)

*`public void copyObject(String bucketName, String objectName, String destBucketName, String destObjectName)`*

Copy a source object into a new destination object.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the source bucket.  |
| ``objectName``  | _String_  | Object name in the source bucket to be copied. |
| ``destBucketName``  | _String_  | Destination bucket name. |
| ``destObjectName`` | _String_ | Destination object name to be created, if not provided defaults to source object name.|


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``No| ``copyConditions`` | _CopyConditions_ | Map of conditions useful for applying restrictions on copy operation.|ResponseException`` : upon no response from server.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__

```java
minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname", "my-destobjname");
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, String destBucketName, String destObjectName, CopyConditions copyConditions)

*`public void copyObject(String bucketName, String objectName, String destBucketName, String destObjectName, CopyConditions copyConditions)`*

Copy a source object into a new object with the provided name in the provided bucket. optionally can take a key value CopyConditions as well for conditionally attempting copyObject.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.lang.String-java.lang.String-io.minio.CopyConditions-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the source bucket.  |
| ``objectName``  | _String_  | Object name in the source bucket to be copied. |
| ``destBucketName``  | _String_  | Destination bucket name. |
| ``destObjectName`` | _String_ | Destination object name to be created, if not provided defaults to source object name.|
| ``copyConditions`` | _CopyConditions_ | Map of conditions useful for applying restrictions on copy operation.|


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__

```java
minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname", "my-destobjname", copyConditions);
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String destBucketName, String destObjectName, CopyConditions copyConditions, ServerSideEncryption sseTarget)

*`public void copyObject(String bucketName, String objectName, ServerSideEncryption sseSource, String destBucketName, String destObjectName, CopyConditions copyConditions, ServerSideEncryption sseTarget)`*

Copy a source object into a new object with the provided name in the provided bucket. optionally can take a key value CopyConditions as well for conditionally attempting copyObject.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-io.minio.ServerSideEncryption-java.lang.String-java.lang.String-io.minio.CopyConditions-io.minio.ServerSideEncryption-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the source bucket.  |
| ``objectName``  | _String_  | Object name in the source bucket to be copied. |
| ``sseSource``  | _ServerSideEncryption_  | Source Encryption metadata [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |
| ``destBucketName``  | _String_  | Destination bucket name. |
| ``destObjectName`` | _String_ | Destination object name to be created, if not provided defaults to source object name.|
| ``copyConditions`` | _CopyConditions_ | Map of conditions useful for applying restrictions on copy operation.|
| ``sseTarget``  | _ServerSideEncryption_  | Target Encryption metadata [ServerSideEncryption](http://minio.github.io/minio-java/io/minio/ServerSideEncryption.html). |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |


__Example__

```java
 minioClient.copyObject("my-bucketname", "my-objectname", sseSource, "my-destbucketname", "my-destobjname", copyConditions, sseTarget);
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, String destBucketName, String destObjectName, CopyConditions copyConditions, Map<String, String> metadata)

*`public void copyObject(String bucketName, String objectName, String destBucketName, String destObjectName, CopyConditions copyConditions, Map<String, String> metadata)`*

Copies content from objectName to destObjectName.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.lang.String-java.lang.String-io.minio.CopyConditions-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the source bucket.  |
| ``objectName``  | _String_  | Object name in the source bucket to be copied. |
| ``destBucketName``  | _String_  | Destination bucket name. |
| ``destObjectName`` | _String_ | Destination object name to be created, if not provided defaults to source object name.|
| ``copyConditions`` | _CopyConditions_ | Map of conditions useful for applying restrictions on copy operation.|
| ``metadata``  | _Map_ | Map of object metadata for destination object.|


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__


This API performs a server side copy operation from a given source object to destination object.

```java
try {
  CopyConditions copyConditions = new CopyConditions();
  copyConditions.setMatchETagNone("TestETag");

  minioClient.copyObject("mybucket",  "island.jpg", "mydestbucket", "processed.png", copyConditions);
  System.out.println("island.jpg is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="removeObject"></a>
### removeObject(String bucketName, String objectName)

`public void removeObject(String bucketName, String objectName)`

Removes an object.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObject-java.lang.String-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``IOException`` : upon connection error.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |



__Example__


```java
try {
      // Remove my-objectname from the bucket my-bucketname.
      minioClient.removeObject("mybucket", "myobject");
      System.out.println("successfully removed mybucket/myobject");
} catch (MinioException e) {
      System.out.println("Error: " + e);
}
```

<a name="removeObjects"></a>
### removeObjects(String bucketName, Iterable<String> objectNames)

`public Iterable<Result<DeleteError>> removeObjects(String bucketName, Iterable<String> objectNames)`

Removes multiple objects.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObjects-java.lang.String-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectNames`` | _Iterable<String>_  | Iterable object contains object names for removal. |

|Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Iterable<Result<DeleteError>>``:an iterator of Result DeleteError.  | _None_  |



__Example__


```java
List<String> objectNames = new LinkedList<String>();
objectNames.add("my-objectname1");
objectNames.add("my-objectname2");
objectNames.add("my-objectname3");
try {
      // Remove object all objects in objectNames list from the bucket my-bucketname.
      for (Result<DeleteError> errorResult: minioClient.removeObjects("my-bucketname", objectNames)) {
        DeleteError error = errorResult.get();
        System.out.println("Failed to remove '" + error.objectName() + "'. Error:" + error.message());
      }
} catch (MinioException e) {
      System.out.println("Error: " + e);
}
```

<a name="removeIncompleteUpload"></a>
### removeIncompleteUpload(String bucketName, String objectName)

`public void removeIncompleteUpload(String bucketName, String objectName)`

Removes a partially uploaded object.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeIncompleteUpload-java.lang.String-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  None  | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |


__Example__


```java
try {
  // Removes partially uploaded objects from buckets.
	minioClient.removeIncompleteUpload("mybucket", "myobject");
	System.out.println("successfully removed all incomplete upload session of my-bucketname/my-objectname");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

## 4. Presigned operations
<a name="presignedGetObject"></a>

### presignedGetObject(String bucketName, String objectName)
`public String presignedGetObject(String bucketName, String objectName)`

Returns an presigned URL to download the object in the bucket with default expiry time. Default expiry time is 7 days in seconds.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``String`` : string contains URL to download the object. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidExpiresRangeException`` : upon input expires is out of range.            |

__Example__


```java
try {
	 String url = minioClient.presignedGetObject("my-bucketname", "my-objectname");
     System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```


<a name="presignedGetObject"></a>

### presignedGetObject(String bucketName, String objectName, Integer expires)
`public String presignedGetObject(String bucketName, String objectName, Integer expires)`

Generates a presigned URL for HTTP GET operations. Browsers/Mobile clients may point to this URL to directly download objects even if the bucket is private. This presigned URL can have an associated expiration time in seconds after which it is no longer operational. The default expiry is set to 7 days.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``expiry``  | _Integer_  | Expiry in seconds. Default expiry is set to 7 days. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``String`` : string contains URL to download the object. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidExpiresRangeException`` : upon input expires is out of range.            |

__Example__


```java
try {
	String url = minioClient.presignedGetObject("mybucket", "myobject", 60 * 60 * 24);
	System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="presignedGetObject"></a>

### presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,String> reqParams)
`public String presignedGetObject(String bucketName, String objectName, Integer expires, Map<String,String> reqParams)`

Returns an presigned URL to download the object in the bucket with given expiry time with custom request params.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-java.util.Map-)


__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``expiry``  | _Integer_  | Expiry in seconds. Default expiry is set to 7 days. |
| ``reqParams``  | _Map<String,String>_  | Override values for set of response headers. Currently supported request parameters are [response-expires, response-content-type, response-cache-control, response-content-disposition]. |


| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``String`` : string contains URL to download the object. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidExpiresRangeException`` : upon input expires is out of range.            |

__Example__


```java
try {
	String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24, reqParams);
	System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="presignedPutObject"></a>
### presignedPutObject(String bucketName, String objectName)

`public String presignedPutObject(String bucketName, String objectName)`

Returns a presigned URL to upload an object in the bucket with default expiry time. Default expiry time is 7 days in seconds.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPutObject-java.lang.String-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``String`` : string contains URL to download the object. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InvalidExpiresRangeException`` : upon input expires is out of range.            |
|        | ``InternalException`` : upon internal library error.        |

__Example__

```java
try {
	String url = minioClient.presignedPutObject("my-bucketname", "my-objectname");
    System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="presignedPutObject"></a>
### presignedPutObject(String bucketName, String objectName, Integer expires)

`public String presignedPutObject(String bucketName, String objectName, Integer expires)`

Generates a presigned URL for HTTP PUT operations. Browsers/Mobile clients may point to this URL to upload objects directly to a bucket even if it is private. This presigned URL can have an associated expiration time in seconds after which it is no longer operational. The default expiry is set to 7 days.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPutObject-java.lang.String-java.lang.String-java.lang.Integer-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | Name of the bucket.  |
| ``objectName``  | _String_  | Object name in the bucket. |
| ``expiry``  | _Integer_  | Expiry in seconds. Default expiry is set to 7 days. |

| Return Type	  | Exceptions	  |
|:--- |:--- |
|  ``String`` : string contains URL to download the object. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidExpiresRangeException`` : upon input expires is out of range.            |


__Example__

```java
try {
	String url = minioClient.presignedPutObject("mybucket", "myobject", 60 * 60 * 24);
	System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="presignedPostPolicy"></a>
### presignedPostPolicy(PostPolicy policy)

`public Map<String,String> presignedPostPolicy(PostPolicy policy)`

Allows setting policy conditions to a presigned URL for POST operations. Policies such as bucket name to receive object uploads, key name prefixes, expiry policy may be set.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPostPolicy-io.minio.PostPolicy-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``policy``  | _PostPolicy_  | Post policy of an object.  |


| Return Type	  | Exceptions	  |
|:--- |:--- |
| ``Map``: Map of strings to construct form-data. | Listed Exceptions: |
|        |  ``InvalidBucketNameException`` : upon invalid bucket name. |
|        | ``NoSuchAlgorithmException`` : upon requested algorithm was not found during signature calculation.           |
|        | ``InsufficientDataException`` : Thrown to indicate that reading given InputStream gets EOFException before reading given length. |
|        | ``IOException`` : upon connection error.            |
|        | ``InvalidKeyException`` : upon an invalid access key or secret key.           |
|        | ``NoResponseException`` : upon no response from server.            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : upon parsing response XML.            |
|        | ``ErrorResponseException`` : upon unsuccessful execution.            |
|        | ``InternalException`` : upon internal library error.        |
|        | ``InvalidArgumentException`` : upon invalid value is passed to a method.        |

__Example__

```java
try {
	PostPolicy policy = new PostPolicy("mybucket", "myobject",
  DateTime.now().plusDays(7));
	policy.setContentType("image/png");
	Map<String,String> formData = minioClient.presignedPostPolicy(policy);
	System.out.print("curl -X POST ");
	for (Map.Entry<String,String> entry : formData.entrySet()) {
    System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
	}
	System.out.println(" -F file=@/tmp/userpic.png  https://play.min.io:9000/mybucket");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
```

## 5. Explore Further

- [Build your own Photo API Service - Full Application Example ](https://github.com/minio/minio-java-rest-example)
- [Complete JavaDoc](http://minio.github.io/minio-java/)
