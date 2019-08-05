# Java Client API参考文档 [![Slack](https://slack.min.io/slack?type=svg)](https://slack.min.io)

## 初始化Minio Client object。

## MinIO

```java
MinioClient minioClient = new MinioClient("https://play.min.io", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
```

## AWS S3


```java
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");
```

| 存储桶操作 |  文件对象操作 | Presigned操作  | 存储桶策略
|:--- |:--- |:--- |:--- |
| [`makeBucket`](#makeBucket)  |[`getObject`](#getObject)   |[`presignedGetObject`](#presignedGetObject)   | [`getBucketPolicy`](#getBucketPolicy)   |
| [`listBuckets`](#listBuckets)  | [`putObject`](#putObject)  | [`presignedPutObject`](#presignedPutObject)  | [`setBucketPolicy`](#setBucketPolicy)   |
| [`bucketExists`](#bucketExists)  | [`copyObject`](#copyObject)  | [`presignedPostPolicy`](#presignedPostPolicy)  |  |
| [`removeBucket`](#removeBucket)  | [`statObject`](#statObject) |   |   |
| [`listObjects`](#listObjects)  | [`removeObject`](#removeObject) |   |   |
| [`listIncompleteUploads`](#listIncompleteUploads)  | [`removeIncompleteUpload`](#removeIncompleteUpload) |   |   |


## 1. 构造函数

<a name="constructors"></a>

|  |
|---|
|`public MinioClient(String endpoint) throws NullPointerException, InvalidEndpointException, InvalidPortException`   |
| 使用给定的endpoint以及匿名方式创建一个Minio client对象。|
| [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-)  |


|   |
|---|
|`public MinioClient(URL url) throws NullPointerException, InvalidEndpointException, InvalidPortException`   |
| 使用给定的url以及匿名方式创建一个Minio client对象。 |
| [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-)  |


|  |
|---|
| `public MinioClient(com.squareup.okhttp.HttpUrl url) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
|使用给定的HttpUrl以及匿名方式创建一个Minio client对象。 |
| [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-com.squareup.okhttp.HttpUrl-)  |

|   |
|---|
| `public MinioClient(String endpoint, String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
|  使用给定的endpoint、access key和secret key创建一个Minio client对象。 |
|   [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-)|

|   |
|---|
| `public MinioClient(String endpoint, int port,  String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
|  使用给定的endpoint、port、access key和secret key创建一个Minio client对象。 |
| [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-)  |


|   |
|---|
| `public MinioClient(String endpoint, String accessKey, String secretKey, boolean secure) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
| 使用给定的endpoint、access key、secret key和一个secure选项（是否使用https）创建一个Minio client对象。 |
|  [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-boolean-) |


|   |
|---|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
| 使用给定的endpoint、port、access key、secret key和一个secure选项（是否使用https）创建一个Minio client对象。  |
|  [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-boolean-) |

|   |
|---|
| `public MinioClient(com.squareup.okhttp.HttpUrl url, String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
| 使用给定的HttpUrl对象、access key、secret key创建一个Minio client对象。 |
| [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-com.squareup.okhttp.HttpUrl-java.lang.String-java.lang.String-)  |


|   |
|---|
| `public MinioClient(URL url, String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
|  使用给定的URL对象、access key、secret key创建一个Minio client对象。 |
|  [查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-java.lang.String-java.lang.String-) |


__参数__

| 参数  | 类型  | 描述  |
|---|---|---|
| `endpoint`  |  _string_ | endPoint是一个URL，域名，IPv4或者IPv6地址。以下是合法的endpoints: |
| | |https://s3.amazonaws.com |
| | |https://play.min.io |
| | |localhost |
| | |play.min.io|
| `port` | _int_  | TCP/IP端口号。可选，默认值是，如果是http,则默认80端口，如果是https,则默认是443端口。|
| `accessKey`   | _string_   |accessKey类似于用户ID，用于唯一标识你的账户。 |
|`secretKey`  |  _string_   | secretKey是你账户的密码。|
|`secure`    | _boolean_    |如果是true，则用的是https而不是http,默认值是true。 |
|`url`    | _URL_    |Endpoint URL对象。|
|`url`    | _HttpURL_    |Endpoint HttpUrl对象。 |


__示例__


### MinIO


```java
// 1. public MinioClient(String endpoint)
MinioClient minioClient = new MinioClient("https://play.min.io");

// 2. public MinioClient(URL url)
MinioClient minioClient = new MinioClient(new URL("https://play.min.io"));

// 3. public MinioClient(com.squareup.okhttp.HttpUrl url)
 MinioClient minioClient = new MinioClient(new HttpUrl.parse("https://play.min.io"));

// 4. public MinioClient(String endpoint, String accessKey, String secretKey)
MinioClient minioClient = new MinioClient("https://play.min.io", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 5. public MinioClient(String endpoint, int port,  String accessKey, String secretKey)
MinioClient minioClient = new MinioClient("https://play.min.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 6. public MinioClient(String endpoint, String accessKey, String secretKey, boolean insecure)
MinioClient minioClient = new MinioClient("https://play.min.io", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);

// 7. public MinioClient(String endpoint, int port,  String accessKey, String secretKey, boolean insecure)
MinioClient minioClient = new MinioClient("https://play.min.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);

// 8. public MinioClient(com.squareup.okhttp.HttpUrl url, String accessKey, String secretKey)
 MinioClient minioClient = new MinioClient(new URL("https://play.min.io"), "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 9. public MinioClient(URL url, String accessKey, String secretKey)
MinioClient minioClient = new MinioClient(HttpUrl.parse("https://play.min.io"), "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
```


### AWS S3


```java
// 1. public MinioClient(String endpoint)
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com");

// 2. public MinioClient(URL url)
MinioClient minioClient = new MinioClient(new URL("https://s3.amazonaws.com"));

// 3. public MinioClient(com.squareup.okhttp.HttpUrl url)
 MinioClient s3Client = new MinioClient(new HttpUrl.parse("https://s3.amazonaws.com"));

// 4. public MinioClient(String endpoint, String accessKey, String secretKey)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 5. public MinioClient(String endpoint, int port,  String accessKey, String secretKey)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 80, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 6. public MinioClient(String endpoint, String accessKey, String secretKey, boolean insecure)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", false);

// 7. public MinioClient(String endpoint, int port,  String accessKey, String secretKey, boolean insecure)
MinioClient s3Client = new MinioClient("s3.amazonaws.com", 80, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY",false);

// 8. public MinioClient(com.squareup.okhttp.HttpUrl url, String accessKey, String secretKey)
 MinioClient s3Client = new MinioClient(new URL("s3.amazonaws.com"), "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

// 9. public MinioClient(URL url, String accessKey, String secretKey)
MinioClient s3Client = new MinioClient(HttpUrl.parse("s3.amazonaws.com"), "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");
```

## 2. 存储桶操作

<a name="makeBucket"></a>
### makeBucket(String bucketName)
`public void makeBucket(String bucketName)`

创建一个新的存储桶

[查看Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-java.lang.String-)

__参数__

|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ |存储桶名称  |


| 返回值类型	  | 异常	  |
|:--- |:--- |
| ``None``  | 异常列表: |
|        |  ``InvalidBucketNameException`` : 非法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常           |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常            |
|        | ``ErrorResponseException`` : 执行失败            |
|        | ``InternalException`` : 内部异常       |


__示例__


```java
try {
  // 如存储桶不存在，创建之。
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    System.out.println("mybucket already exists");
  } else {
    // 创建名为'my-bucketname'的存储桶。
    minioClient.makeBucket("mybucket");
    System.out.println("mybucket is created successfully");
  }
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="listBuckets"></a>
### listBuckets()

`public List<Bucket> listBuckets()`

列出所有存储桶。

[查看Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listBuckets--)

|返回值类型	  | 异常	  |
|:--- |:--- |
| ``List Bucket`` : List of bucket type.  | 异常列表： |
|        |  ``NoResponseException`` : 服务端无响应 |
|        | ``IOException`` : 连接异常 |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常 |
|        | ``ErrorResponseException`` :执行失败异溃|
|        | ``InternalException`` : 内部错误|


__示例__


```java
try {
  // 列出所有存储桶
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

检查存储桶是否存在。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#bucketExists-java.lang.String-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称  |


| 返回值值类型	  | 异常   |
|:--- |:--- |
|  ``boolean``: true if the bucket exists  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |



__示例__


```java
try {
  // 检查'my-bucketname'是否存在。
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

删除一个存储桶。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeBucket-java.lang.String-)

注意: -  removeBucket不会删除存储桶里的对象，你需要通过removeObject API来删除它们。


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |


__示例__


```java
try {
  // 删除之前先检查`my-bucket`是否存在。
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // 删除`my-bucketname`存储桶，注意，只有存储桶为空时才能删除成功。
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
### listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)

`public Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)`

列出某个存储桶中的所有对象。

[查看Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-boolean-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``prefix``  | _String_  | 对象名称的前缀 |
| ``recursive``  | _boolean_  | 是否递归查找，如果是false,就模拟文件夹结构查找。 |
| ``useVersion1``  | _boolean_  | 如果是true, 使用版本1 REST API |


|返回值类型	  | 异常   |
|:--- |:--- |
| ``Iterable<Result<Item>>``:an iterator of Result Items.  | _None_  |


__示例__


```java
try {
  // 检查'mybucket'是否存在。
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // 列出'my-bucketname'里的对象
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


<a name="listIncompleteUploads"></a>
### listIncompleteUploads(String bucketName, String prefix, boolean recursive)

`public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix, boolean recursive)`

列出存储桶中被部分上传的对象。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-java.lang.String-boolean-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``prefix``  | _String_  | 对象名称的前缀，列出有该前缀的对象 |
| ``recursive``  | _boolean_  | 是否递归查找，如果是false,就模拟文件夹结构查找。 |


|返回值类型	  | 异常   |
|:--- |:--- |
| ``Iterable<Result<Upload>>``: an iterator of Upload.  | _None_  |


__示例__


```java
try {
  // 检查'mybucket'是否存在。
  boolean found = minioClient.bucketExists("mybucket");
  if (found) {
    // 列出'mybucket'中所有未完成的multipart上传的的对象。 
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

<a name="getBucketPolicy"></a>
### getBucketPolicy(String bucketName, String objectPrefix)
`public PolicyType getBucketPolicy(String bucketName, String objectPrefix)`

获得指定对象前缀的存储桶策略。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getBucketPolicy-java.lang.String-java.lang.String-)

__参数__

|参数   | 类型   | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectPrefix``  | _String_  | 策略适用的对象的前缀 |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  ``PolicyType``: The current bucket policy type for a given bucket and objectPrefix.  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |
|        | ``InvalidBucketNameException `` : 不合法的存储桶名称。       |
|        | ``InvalidObjectPrefixException`` : 不合法的对象前缀       |
|        | ``NoSuchAlgorithmException`` : 找不到相应的签名算法。  |
|        | ``InsufficientDataException`` : 在读到相应length之前就得到一个EOFException。 |


__示例__


```java
try {
  System.out.println("Current policy: " + minioClient.getBucketPolicy("myBucket", "downloads"));
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="setBucketPolicy"></a>
### setBucketPolicy(String bucketName, String objectPrefix, PolicyType policy)
`public void setBucketPolicy(String bucketName, String objectPrefix, PolicyType policy)`

给一个存储桶+对象前缀设置策略。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#setBucketPolicy-java.lang.String-java.lang.String-io.minio.BucketPolicy-)

__参数__

|参数   | 类型   | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectPrefix``  | _String_  | 对象前缀。 |
| ``policy``  | _PolicyType_  | 要赋予的策略，可选值有[PolicyType.NONE, PolicyType.READ_ONLY, PolicyType.READ_WRITE, PolicyType.WRITE_ONLY]. |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |
|        | ``InvalidBucketNameException `` : 不合法的存储桶名称。       |
|        | ``InvalidObjectPrefixException`` : 不合法的对象前缀       |
|        | ``NoSuchAlgorithmException`` : 找不到相应的签名算法。  |
|        | ``InsufficientDataException`` : 在读到相应length之前就得到一个EOFException。 |



__示例__


```java
try {
  minioClient.setBucketPolicy("myBucket", "uploads", PolicyType.READ_ONLY);
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

## 3. Object operations

<a name="getObject"></a>
### getObject(String bucketName, String objectName)

`public InputStream getObject(String bucketName, String objectName, long offset)`

以流的形式下载一个对象。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  ``InputStream``: InputStream containing the object data.  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |


__示例__


```java
try {
  // 调用statObject()来判断对象是否存在。
  // 如果不存在, statObject()抛出异常,
  // 否则则代表对象存在。
  minioClient.statObject("mybucket", "myobject");

  // 获取"myobject"的输入流。
  InputStream stream = minioClient.getObject("mybucket", "myobject");

  // 读取输入流直到EOF并打印到控制台。
  byte[] buf = new byte[16384];
  int bytesRead;
  while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead));
  }

  // 关闭流，此处为示例，流关闭最好放在finally块。
  stream.close();
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, long offset, Long length)

`public InputStream getObject(String bucketName,  String objectName, long offset, Long length)`

下载对象指定区域的字节数组做为流。（断点下载）

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-java.lang.Long-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``offset``  | _Long_  | ``offset`` 是起始字节的位置 |
| ``length``  | _Long_  | ``length``是要读取的长度 (可选，如果无值则代表读到文件结尾)。 |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  ``InputStream`` : InputStream containing the object's data. | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |


__示例__


```java
try {

  // 调用statObject()来判断对象是否存在。
  // 如果不存在, statObject()抛出异常,
  // 否则则代表对象存在。
  minioClient.statObject("mybucket", "myobject");

  // 获取指定offset和length的"myobject"的输入流。
  InputStream stream = minioClient.getObject("mybucket", "myobject", 1024L, 4096L);

  // 读取输入流直到EOF并打印到控制台。
  byte[] buf = new byte[16384];
  int bytesRead;
  while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead));
  }

  // 关闭流。
  stream.close();
} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, String fileName)

`public void getObject(String bucketName, String objectName, String fileName)`

下载并将文件保存到本地。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-java.lang.String-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``fileName``  | _String_  | File name. |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |

__示例__

```java
try {
  // 调用statObject()来判断对象是否存在。
  // 如果不存在, statObject()抛出异常,
  // 否则则代表对象存在。
  minioClient.statObject("mybucket", "myobject");

  // 获取myobject的流并保存到photo.jpg文件中。
  minioClient.getObject("mybucket", "myobject", "photo.jpg");

} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, SecretKey key)

`public CipherInputStream getObject(String bucketName, String objectName, SecretKey key)`

在给定的存储桶中获取整个加密对象的数据作为InputStream，然后用传入的master key解密和加密对象关联的content key。然后创建一个含有InputStream和Cipher的CipherInputStream。这个Cipher被初始为用于使用content key进行解密，所以CipherInputStream会在返回数据前，尝试读取数据并进行解密。所以read()方法返回的是处理过的原始对象数据。

CipherInputStream必须用完关闭，否则连接不会被释放。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-javax.crypto.SecretKey-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``key``  | _SecretKey_  | [SecretKey](https://docs.oracle.com/javase/7/docs/api/javax/crypto/SecretKey.html)类型的数据。|

| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |
|        | ``InvalidEncryptionMetadataException`` : 加密秘钥错误。  |
|        | ``BadPaddingException`` : 错误的padding   |
|        | ``IllegalBlockSizeException`` : 不正确的block size |
|        | ``NoSuchPaddingException`` : 错误的pading类型 |
|        | ``InvalidAlgorithmParameterException`` : 该算法不存在 |

__示例__

```java
try {
  // 调用statObject()来判断对象是否存在。
  // 如果不存在, statObject()抛出异常,
  // 否则则代表对象存在。
  minioClient.statObject("mybucket", "myobject");

  //生成256位AES key。
  KeyGenerator symKeyGenerator = KeyGenerator.getInstance("AES");
  symKeyGenerator.init(256);
  SecretKey symKey = symKeyGenerator.generateKey();

  // 获取对象数据并保存到photo.jpg
  InputStream stream = minioClient.getObject("testbucket", "my-objectname", symKey);

  // 读流到EOF，并输出到控制台。
  byte[] buf = new byte[16384];
  int bytesRead;
  while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
  }

  // 关闭流。
  stream.close();

} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="getObject"></a>
### getObject(String bucketName, String objectName, KeyPair key)

`public InputStream getObject(String bucketName, String objectName, KeyPair key)`

在给定的存储桶中获取整个加密对象的数据作为InputStream，然后用传入的master keyPair解密和加密对象关联的content key。然后创建一个含有InputStream和Cipher的CipherInputStream。这个Cipher被初始为用于使用content key进行解密，所以CipherInputStream会在返回数据前，尝试读取数据并进行解密。所以read()方法返回的是处理过的原始对象数据。

CipherInputStream必须用完关闭，否则连接不会被释放。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-java.security.KeyPair-)

__参数__

|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``key``  | _KeyPair_  | RSA [KeyPair](https://docs.oracle.com/javase/7/docs/api/java/security/KeyPair.html)类型的对象。 |

| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |
|        | ``InvalidEncryptionMetadataException`` : 加密秘钥错误。  |
|        | ``BadPaddingException`` : 错误的padding   |
|        | ``IllegalBlockSizeException`` : 不正确的block size |
|        | ``NoSuchPaddingException`` : 错误的pading类型 |
|        | ``InvalidAlgorithmParameterException`` : 该算法不存在 |

__示例__

```java
try {
  // 调用statObject()来判断对象是否存在。
  // 如果不存在, statObject()抛出异常,
  // 否则则代表对象存在。
  minioClient.statObject("mybucket", "myobject");

  KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
  keyGenerator.initialize(1024, new SecureRandom());
  KeyPair keypair = keyGenerator.generateKeyPair();

  // 获取对象数据并保存到photo.jpg
  InputStream stream = minioClient.getObject("testbucket", "my-objectname", keypair);

  // 读流到EOF，并输出到控制台。
  byte[] buf = new byte[16384];
  int bytesRead;
  while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
  }

  // 关闭流。
  stream.close();

} catch (MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)

`public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)`

通过InputStream上传对象。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-java.lang.String-)


__参数__

|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``stream``  | _InputStream_  | 要上传的流。 |
| ``size``  | _long_  | 要上传的`stream`的size  |
| ``contentType``  | _String_ | Content type。 |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |


__示例__


单个对象的最大大小限制在5TB。putObject在对象大于5MiB时，自动使用multiple parts方式上传。这样，当上传失败时，客户端只需要上传未成功的部分即可（类似断点上传）。上传的对象使用MD5SUM签名进行完整性验证。

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
  // 创建对象
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

通过文件上传到对象中。
[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.lang.String-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``fileName``  | _String_  | File name. |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |

__示例__


```java
try {
  minioClient.putObject("mybucket",  "island.jpg", "/mnt/photos/island.jpg")
  System.out.println("island.jpg is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```
<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, long size, String contentType, SecretKey key)

`public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType,
		  SecretKey key)`

拿到流的数据，使用随机生成的content key进行加密，并上传到指定存储桶中。同时将加密后的content key和iv做为加密对象有header也上传到存储桶中。content key使用传入到该方法的master key进行加密。

如果对象大于5MB,客户端会自动进行multi part上传。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-java.lang.String-javax.crypto.SecretKey-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``stream``  | _InputStream_  | 要上传的流。 |
| ``size``  | _long_  | 要上传的流的大小。|
| ``contentType``  | _String_ | Content type。|
| ``key``  | _SecretKey_ | 用AES初使化的对象[SecretKey](https://docs.oracle.com/javase/7/docs/api/javax/crypto/SecretKey.html)。  |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |
| 		 | ``InvalidAlgorithmParameterException`` : 错误的加密算法。 |
|		 | ``BadPaddingException`` : 不正确的padding. |
|		 | ``IllegalBlockSizeException`` : 不正确的block。 |
|		 | ``NoSuchPaddingException`` : 错误的padding类型。 |

__示例__

对象使用随机生成的key进行加密，然后这个用于加密数据的key又被由仅被client知道的master key(封装在encryptionMaterials对象里)进行加密。这个被加密后的key和IV做为对象的header和加密后的对象一起被上传到存储服务上。

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
  
  //生成256位AES key.
  KeyGenerator symKeyGenerator = KeyGenerator.getInstance("AES");
  symKeyGenerator.init(256);
  SecretKey symKey = symKeyGenerator.generateKey();
  
  // 创建一个对象
  minioClient.putObject("mybucket", "myobject", bais, bais.available(), "application/octet-stream", symKey);
  bais.close();
  System.out.println("myobject is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="putObject"></a>
### putObject(String bucketName, String objectName, InputStream stream, long size, String contentType, KeyPair key)

`public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType,
		  KeyPair key)`


拿到流的数据，使用随机生成的content key进行加密，并上传到指定存储桶中。同时将加密后的content key和iv做为加密对象有header也上传到存储桶中。content key使用传入到该方法的master key进行加密。

如果对象大于5MB,客户端会自动进行multi part上传。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-java.lang.String-java.security.KeyPair-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``stream``  | _InputStream_  | 要上传的流。 |
| ``size``  | _long_  | 要上传的流的大小。 |
| ``contentType``  | _String_ | Content type。 |
| ``key``  | _KeyPair_ | 一个RSA [KeyPair](https://docs.oracle.com/javase/7/docs/api/java/security/KeyPair.html)的对象。  |

| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |
| 		 | ``InvalidAlgorithmParameterException`` : 错误的加密算法。 |
|		 | ``BadPaddingException`` : 不正确的padding。 |
|		 | ``IllegalBlockSizeException`` : 不正确的block。 |
|		 | ``NoSuchPaddingException`` : 错误的pading类型。 |

__示例__

对象使用随机生成的key进行加密，然后这个用于加密数据的key又被由仅被client知道的master key(封装在encryptionMaterials对象里)进行加密。这个被加密后的key和IV做为对象的header和加密后的对象一起被上传到存储服务上。

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
  
  KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
  keyGenerator.initialize(1024, new SecureRandom());
  KeyPair keypair = keyGenerator.generateKeyPair();
  
  // Create an object
  minioClient.putObject("mybucket", "myobject", bais, bais.available(), "application/octet-stream", keypair);
  bais.close();
  System.out.println("myobject is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="statObject"></a>
### statObject(String bucketName, String objectName)

*`public ObjectStat statObject(String bucketName, String objectName)`*

获取对象的元数据。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#statObject-java.lang.String-java.lang.String-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  ``ObjectStat``: Populated object meta data. | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |

__示例__


```java
try {
  // 获得对象的元数据。
  ObjectStat objectStat = minioClient.statObject("mybucket", "myobject");
  System.out.println(objectStat);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="copyObject"></a>
### copyObject(String bucketName, String objectName, String destBucketName, String destObjectName, CopyConditions cpConds, Map<String, String> metadata)

*`public void copyObject(String bucketName, String objectName, String destBucketName, String destObjectName, CopyConditions cpConds, Map<String, String> metadata)`*

从objectName指定的对象中将数据拷贝到destObjectName指定的对象。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#copyObject-java.lang.String-java.lang.String-java.lang.String-java.lang.String-io.minio.CopyConditions-)

__参数__

|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 源存储桶名称。  |
| ``objectName``  | _String_  | 源存储桶中的源对象名称。 |
| ``destBucketName``  | _String_  | 目标存储桶名称。 |
| ``destObjectName`` | _String_ | 要创建的目标对象名称,如果为空，默认为源对象名称。|
| ``copyConditions`` | _CopyConditions_ | 拷贝操作的一些条件Map。|
| ``metadata``  | _Map_ | 给目标对象的元数据Map。|


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |

__示例__

本API执行了一个服务端的拷贝操作。

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

删除一个对象。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObject-java.lang.String-java.lang.String-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |

| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |



__示例__


```java
try {
      // 从mybucket中删除myobject。
      minioClient.removeObject("mybucket", "myobject");
      System.out.println("successfully removed mybucket/myobject");
} catch (MinioException e) {
      System.out.println("Error: " + e);
}
```

<a name="removeObject"></a>
### removeObject(String bucketName, Iterable<String> objectNames)

`public Iterable<Result<DeleteError>> removeObject(String bucketName, Iterable<String> objectNames)`

删除多个对象。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObject-java.lang.String-java.lang.String-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectNames`` | _Iterable<String>_  | 含有要删除的多个object名称的迭代器对象。 |

|返回值类型	  | 异常   |
|:--- |:--- |
| ``Iterable<Result<DeleteError>>``:an iterator of Result DeleteError.  | _None_  |



__示例__


```java
List<String> objectNames = new LinkedList<String>();
objectNames.add("my-objectname1");
objectNames.add("my-objectname2");
objectNames.add("my-objectname3");
try {
      // 删除my-bucketname里的多个对象
      for (Result<DeleteError> errorResult: minioClient.removeObject("my-bucketname", objectNames)) {
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

删除一个未完整上传的对象。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeIncompleteUpload-java.lang.String-java.lang.String-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  None  | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``NoResponseException`` : 服务器无响应。            |
|        | ``IOException`` : 连接异常。            |
|        | ``org.xmlpull.v1.XmlPullParserException`` : 解析返回的XML异常。            |
|        | ``ErrorResponseException`` : 执行失败异常。            |
|        | ``InternalException`` : 内部错误。        |


__示例__


```java
try {
    // 从存储桶中删除名为myobject的未完整上传的对象。
	minioClient.removeIncompleteUpload("mybucket", "myobject");
	System.out.println("successfully removed all incomplete upload session of my-bucketname/my-objectname");
} catch(MinioException e) {
	System.out.println("Error occurred: " + e);
}
```

## 4. Presigned操作
<a name="presignedGetObject"></a>

### presignedGetObject(String bucketName, String objectName, Integer expires)
`public String presignedGetObject(String bucketName, String objectName, Integer expires)`

生成一个给HTTP GET请求用的presigned URL。浏览器/移动端的客户端可以用这个URL进行下载，即使其所在的存储桶是私有的。这个presigned URL可以设置一个失效时间，默认值是7天。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-)


__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_ | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``expiry``  | _Integer_  | 失效时间（以秒为单位），默认是7天，不得大于七天。 |


| 返回值类型	  | 异常   |
|:--- |:--- |
|  ``String`` : string contains URL to download the object. | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。          |
|        | ``InvalidKeyException`` : 不合法的access key或者secret key。   |
|        | ``IOException`` : 连接异常。            |
|        | ``NoSuchAlgorithmException`` : 找不到相应的签名算法。           |
|        | ``InvalidExpiresRangeException`` : presigned URL已经过期了。   |


__示例__


```java
try {
	String url = minioClient.presignedGetObject("mybucket", "myobject", 60 * 60 * 24);
	System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
}
```

<a name="presignedPutObject"></a>
### presignedPutObject(String bucketName, String objectName, Integer expires)

`public String presignedPutObject(String bucketName, String objectName, Integer expires)`

生成一个给HTTP PUT请求用的presigned URL。浏览器/移动端的客户端可以用这个URL进行上传，即使其所在的存储桶是私有的。这个presigned URL可以设置一个失效时间，默认值是7天。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPutObject-java.lang.String-java.lang.String-java.lang.Integer-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``bucketName``  | _String_  | 存储桶名称。  |
| ``objectName``  | _String_  | 存储桶里的对象名称。 |
| ``expiry``  | _Integer_  | 失效时间（以秒为单位），默认是7天，不得大于七天。 |

| 返回值类型	  | 异常   |
|:--- |:--- |
|  ``String`` : string contains URL to download the object. | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``InvalidKeyException`` : 不合法的access key或者secret key。         |
|        | ``IOException`` : 连接异常。            |
|        | ``NoSuchAlgorithmException`` : 找不到相应的签名算法。           |
|        | ``InvalidExpiresRangeException`` :  presigned URL已经过期了。           |


__示例__

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

允许给POST请求的presigned URL设置策略，比如接收对象上传的存储桶名称的策略，key名称前缀，过期策略。

[查看 Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPostPolicy-io.minio.PostPolicy-)

__参数__


|参数   | 类型	  | 描述  |
|:--- |:--- |:--- |
| ``policy``  | _PostPolicy_  | 对象的post策略 |


| 返回值类型	  | 异常   |
|:--- |:--- |
| ``Map``: Map of strings to construct form-data. | 异常列表： |
|        |  ``InvalidBucketNameException`` : 不合法的存储桶名称。 |
|        | ``InvalidKeyException`` : 不合法的access key或者secret key。            |
|        | ``IOException`` : 连接异常。            |
|        | ``NoSuchAlgorithmException`` : 找不到相应的签名算法。           |



__示例__

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
	System.out.println(" -F file=@/tmp/userpic.png  https://play.min.io/mybucket");
} catch(MinioException e) {
  System.out.println("Error occurred: " + e);
```

## 5. 了解更多

- [创建属于你的照片API服务示例](https://github.com/minio/minio-java-rest-example)
- [完整的JavaDoc](http://minio.github.io/minio-java/)
