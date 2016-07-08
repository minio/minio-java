# Java Client API Reference

## Initialize Minio Client object. 

``1. Minio`` 
```java
MinioClient minioClient = new MinioClient("https://play.minio.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
```
``2. AWS S3``
```java
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY"); 
```

| Bucket operations |  Object operations | Presigned operations  |
|:--- |:--- |:--- |
| [`makeBucket`](#makeBucket)  |[`getObject`](#getObject)   |[`presignedGetObject`](#presignedGetObject)   |
| [`listBuckets`](#listBuckets)  | [`putObject`](#putObject)  | [`presignedPutObject`](#presignedPutObject)  |
| [`bucketExists`](#bucketExists)  | [`statObject`](#statObject)  | [`presignedPostPolicy`](#presignedPostPolicy)  |
| [`removeBucket`](#removeBucket)  |  [`removeObject`](#removeObject) |   |
| [`listObjects`](#listObjects)  | [`removeIncompleteUpload`](#removeIncompleteUpload)  |   | 
|[`listIncompleteUploads`](#listIncompleteUploads)  |   |  

## 1. Constructors 

<a name="constructors">

|  |
|---|
|`public MinioClient(String endpoint) throws NullPointerException, InvalidEndpointException, InvalidPortException`   |
| Creates Minio client object with given endpoint using anonymous access.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-)  |  


|   |
|---|
|`public MinioClient(URL url) throws NullPointerException, InvalidEndpointException, InvalidPortException`   |
| Creates Minio client object with given url using anonymous access.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-)  |  


|  |
|---|
| `public MinioClient(com.squareup.okhttp.HttpUrl url) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
|Creates Minio client object with given HttpUrl object using anonymous access.   |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-com.squareup.okhttp.HttpUrl-)  |  

|   |
|---|
| `public MinioClient(String endpoint, String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
|  Creates Minio client object with given endpoint, access key and secret key. |
|   [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-)|  

|   |
|---|
| `public MinioClient(String endpoint, int port,  String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
| Creates Minio client object with given endpoint, port, access key and secret key using secure (HTTPS) connection.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-)  |  


|   |
|---|
| `public MinioClient(String endpoint, String accessKey, String secretKey, boolean secure) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
| Creates Minio client object with given endpoint, access key and secret key using secure (HTTPS) connection.  |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-java.lang.String-java.lang.String-boolean-) |  


|   |
|---|
| `public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
| Creates Minio client object using given endpoint, port, access key, secret key and secure option.  |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.lang.String-int-java.lang.String-java.lang.String-boolean-) |  

|   |
|---|
| `public MinioClient(com.squareup.okhttp.HttpUrl url, String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
| Creates Minio client object with given URL object, access key and secret key.  |
| [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-com.squareup.okhttp.HttpUrl-java.lang.String-java.lang.String-)  |  


|   |
|---|
| `public MinioClient(URL url, String accessKey, String secretKey) throws NullPointerException, InvalidEndpointException, InvalidPortException`  |
|  Creates Minio client object with given URL object, access key and secret key. |
|  [View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#MinioClient-java.net.URL-java.lang.String-java.lang.String-) |  

__Parameters__

<table>
    <thead>
        <tr>
            <th>Param</th>
            <th>Type</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
           endpoint
            </td>
            <td>
            String
            </td>
            <td>
            Endpoint is an URL, domain name, IPv4 or IPv6 address.<br/>
Valid endpoints:
                <ul>
                    <li>https://s3.amazonaws.com</li>
                    <li>https://s3.amazonaws.com/</li>
                    <li>https://play.minio.io:9000</li>
                    <li>http://play.minio.io:9010/</li>
                    <li>localhost</li>
                    <li>localhost.localdomain</li>
                    <li>play.minio.io</li>
                    <li>127.0.0.1</li>
                    <li>192.168.1.60</li>
                    <li>::1</li>
                </ul>
            </td>
        </tr>
        <tr>
         <td>
           port
            </td>
            <td>
            int
            </td>
            <td>
            TCP/IP port number. This input is optional. Default value set to 80 for HTTP and 443 for HTTPs.
            </td>
        </tr>
        <tr>
           <td>accessKey</td>
           <td>String</td>
           <td>Access key to access service in endpoint.</td>
        </tr>
        <tr>
                <td>secretKey</td>
                <td>String</td>
               <td>Secret key to access service in endpoint.</td>
        </tr>
        <tr>
                <td>secure</td>
                <td>boolean</td>
                <td>If true, access endpoint using HTTPS else access it using HTTP.</td>
        </tr>
        <tr>
                <td>url</td>
                <td>URL</td>
               <td>Endpoint URL object.</td>
        </tr>
        <tr>
               <td>url</td>
                <td>HttpURL</td>
               <td>Endpoint HttpUrl object.</td>
        </tr>
        
    </tbody>
</table>

__Example__

``1. Minio``
```java
// 1. public MinioClient(String endpoint)
MinioClient minioClient = new MinioClient("https://play.minio.io:9000");

// 2. public MinioClient(URL url)
MinioClient minioClient = new MinioClient(new URL("https://play.minio.io:9000"));

// 3. public MinioClient(com.squareup.okhttp.HttpUrl url) 
 MinioClient minioClient = new MinioClient(new HttpUrl.parse("https://play.minio.io:9000")); 

// 4. public MinioClient(String endpoint, String accessKey, String secretKey)
MinioClient minioClient = new MinioClient("https://play.minio.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG"); 

// 5. public MinioClient(String endpoint, int port,  String accessKey, String secretKey)
MinioClient minioClient = new MinioClient("https://play.minio.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG"); 

// 6. public MinioClient(String endpoint, String accessKey, String secretKey, boolean insecure)
MinioClient minioClient = new MinioClient("https://play.minio.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);

// 7. public MinioClient(String endpoint, int port,  String accessKey, String secretKey, boolean insecure)
MinioClient minioClient = new MinioClient("https://play.minio.io", 9000, "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);

// 8. public MinioClient(com.squareup.okhttp.HttpUrl url, String accessKey, String secretKey)
 MinioClient minioClient = new MinioClient(new URL("https://play.minio.io:9000"), "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

// 9. public MinioClient(URL url, String accessKey, String secretKey)
MinioClient minioClient = new MinioClient(HttpUrl.parse("https://play.minio.io:9000"), "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
```
``2. AWS S3``
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
## 2. Bucket operations

<a name="makeBucket">
#### makeBucket(String bucketName)
`public void makeBucket(String bucketName)`

Creates a new bucket.      

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#makeBucket-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
           None
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>

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
  System.out.println("Error occured: " + e);
}
```

<a name="listBuckets">
#### listBuckets()
`public List<Bucket> listBuckets()`

Lists all buckets.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listBuckets--)


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
           List Bucket : List of bucket type.
            </td>
            <td>
            <ul>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>

__Example__
```java
try {
  // List buckets that have read access.
	List<Bucket> bucketList = minioClient.listBuckets();
	for (Bucket bucket : bucketList) {
  	  System.out.println(bucket.creationDate() + ", " + bucket.name());
	}
} catch (MinioException e) {
  System.out.println("Error occured: " + e);
}
```

<a name="bucketExists">
#### bucketExists(String bucketName)
`public boolean bucketExists(String bucketName)`

Checks if a bucket exists.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#bucketExists-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
           boolean: true if the bucket exists
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


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
  System.out.println("Error occured: " + e);
}
```


<a name="removeBucket">
#### removeBucket(String bucketName)
`public void removeBucket(String bucketName)`

Removes a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeBucket-java.lang.String-)
 
NOTE: -  removeBucket does not delete the objects inside the bucket. The objects need to be deleted using the removeObject API. 

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |

<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
           None
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>



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
  System.out.println("Error occured: " + e);
}
```

<a name="listObjects">
#### listObjects(String bucketName, String prefix, boolean recursive)
`public Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive)`

Lists all objects in a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listObjects-java.lang.String-java.lang.String-boolean-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``prefix``  | *String*  | Prefix string. List objects whose name starts with ``prefix``. |
| ``recursive``  | *boolean*  | when false, emulates a directory structure where each listing returned is either a full object or part of the object's key up to the first '/'. All objects with the same prefix up to the first '/' will be merged into one entry. |

|Return Type	  | Exceptions	  | 
|:--- |:--- |
| ``Iterable<Result<Item>>``:an iterator of Result Items.  | *None*  |

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
  System.out.println("Error occured: " + e);
}
```


<a name="listIncompleteUploads">
#### listIncompleteUploads(String bucketName, String prefix, boolean recursive)
`public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix, boolean recursive)`

Lists partially uploaded objects in a bucket.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#listIncompleteUploads-java.lang.String-java.lang.String-boolean-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``prefix``  | *String*  | Prefix string. List objects whose name starts with ``prefix``. |
| ``recursive``  | *boolean*  | when false, emulates a directory structure where each listing returned is either a full object or part of the object's key up to the first '/'. All objects with the same prefix up to the first '/' will be merged into one entry. |

|Return Type	  | Exceptions	  | 
|:--- |:--- |
| ``Iterable<Result<Upload>>``: an iterator of Upload.  | *None*  |

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
   System.out.println("Error occured: " + e);
}
```

## 3. Object operations
<a name="getObject">
#### getObject(String bucketName, String objectName)
`public InputStream getObject(String bucketName, String objectName, long offset)`

Downloads an object as a stream.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |

<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
           InputStream: InputStream containing the object data.
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>

__Example__

```java
try {
  
      // Check whether the object exists using statObject().  If the object is not found, statObject() throws an exception,  else it means that the object exists.
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
    System.out.println("Error occured: " + e);
}
```


<a name="getObject">
#### getObject(String bucketName, String objectName, long offset, Long length)
`public InputStream getObject(String bucketName,  String objectName, long offset, Long length)`
                                          
Downloads the specified range bytes of an object as a stream.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-long-java.lang.Long-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |
| ``offset``  | *Long*  | ``offset`` of the object from where the stream will start. |
| ``length``  | *Long*  | ``length`` of the object that will be read in the stream (optional, if not specified we read the rest of the file from the offset). |


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
           InputStream: InputStream containing the object's data.
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


__Example__

```java
 try {
  
      // Check whether the object exists using statObject().  If the object is not found, statObject() throws an exception,  else it means that the object exists.
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
    System.out.println("Error occured: " + e);
}
```

<a name="getObject">
#### getObject(String bucketName, String objectName, String fileName)
`public void getObject(String bucketName, String objectName, String fileName)`
                  
Downloads and saves the object as a file in the local filesystem.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#getObject-java.lang.String-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |
| ``fileName``  | *String*  | File name. |


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> None
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>

__Example__
```java
try {
  
      // Check whether the object exists using statObject().  If the object is not found, statObject() throws an exception,  else it means that the object exists.
     // Execution is successful.
     minioClient.statObject("mybucket", "myobject");
  
    // Gets the object's data and stores it in photo.jpg 
     minioClient.getObject("mybucket", "myobject", "photo.jpg");
    
  } catch (MinioException e) {
    System.out.println("Error occured: " + e);
```

<a name="putObject">
#### putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)
`public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)`
                     
Uploads an object from an InputStream.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.io.InputStream-long-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |
| ``stream``  | *InputStream*  | stream to upload. |
| ``size``  | *long*  | Size of the data to read from ``stream`` that will be uploaded. |
| ``contentType``  | *String*  | Content type of the stream. |



<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> None
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


__Example__

The maximum size of a single object is limited to 5TB. putObject transparently uploads objects larger than 5MiB in multiple parts. This allows failed uploads to resume safely by only uploading the missing parts. Uploaded data is carefully verified using MD5SUM signatures.

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
	minioClient.putObject("mybucket", "myobject", bais, bais.available(), "application/octet-stream");
	bais.close();
	System.out.println("myobject is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occured: " + e);
}
```

<a name="putObject">
#### putObject(String bucketName, String objectName, String fileName)
`public void putObject(String bucketName, String objectName, String fileName)`

Uploads contents from a file to objectName.                                 
[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#putObject-java.lang.String-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |
| ``fileName``  | *String*  | File name. |


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> None
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>

__Example__

The maximum size of a single object is limited to 5TB. putObject transparently uploads objects larger than 5MB in multiple parts. This allows failed uploads to resume safely by only uploading the missing parts. Uploaded data is carefully verified using MD5SUM signatures.

```java
try {
	minioClient.putObject("mybucket",  "island.jpg", "/mnt/photos/island.jpg")
	System.out.println("island.jpg is uploaded successfully");
} catch(MinioException e) {
  System.out.println("Error occured: " + e);
}
```

<a name="statObject">
#### statObject(String bucketName, String objectName)   
*`ObjectStat statObject(String bucketName, String objectName)`*

Gets metadata of an object.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#statObject-java.lang.String-java.lang.String-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |




<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> ObjectStat: Populated object meta data.
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


__Example__

```java
try {
  //Get the metadata of the object.
	ObjectStat objectStat = minioClient.statObject("mybucket", "myobject");
	System.out.println(objectStat);
} catch(MinioException e) {
 System.out.println("Error occured: " + e); 
}
```

<a name="removeObject">
#### removeObject(String bucketName, String objectName)
`public void removeObject(String bucketName, String objectName)`

Removes an object.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeObject-java.lang.String-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> None
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


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

<a name="removeIncompleteUpload">
#### removeIncompleteUpload(String bucketName, String objectName)
`public void removeIncompleteUpload(String bucketName, String objectName)`
                                  
Removes a partially uploaded object.  

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#removeIncompleteUpload-java.lang.String-java.lang.String-)

__Parameters__


|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |




<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> None
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>NoResponseException : upon no response from server.</li>
            <li>IOException : upon connection error.</li>
            <li>org.xmlpull.v1.XmlPullParserException : upon parsing response XML.</li>
            <li>ErrorResponseException : upon unsuccessful execution.</li>
            <li>InternalException : upon internal library error.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


__Example__

```java
try {
  // Removes partially uploaded objects from buckets.
	minioClient.removeIncompleteUpload("mybucket", "myobject");
	System.out.println("successfully removed all incomplete upload session of my-bucketname/my-objectname");
} catch(MinioException e) {
  System.out.println("Error occured: " + e);
}
```

## 4. Presigned operations
<a name="presignedGetObject">
#### presignedGetObject(String bucketName, String objectName, Integer expires)
`public String presignedGetObject(String bucketName, String objectName, Integer expires)`
                                
Generates a presigned URL for HTTP GET operations. Browsers/Mobile clients may point to this URL to directly download objects even if the bucket is private. This presigned URL can have an associated expiration time in seconds after which it is no longer operational. The default expiry is set to 7 days.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedGetObject-java.lang.String-java.lang.String-java.lang.Integer-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |
| ``expiry``  | *Integer*  | Expiry in seconds. Default expiry is set to 7 days. |

<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> String: string contains URL to download the object.
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>InvalidKeyException : upon an invalid access key or secret key.</li>
            <li>IOException : upon connection error.</li>
            <li>NoSuchAlgorithmException : upon requested algorithm was not found during signature calculation.</li>
            <li>InvalidExpiresRangeException : upon input expires is out of range.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


__Example__

```java
try {
	String url = minioClient.presignedGetObject("mybucket", "myobject", 60 * 60 * 24);
	System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occured: " + e);
}
```

<a name="presignedPutObject">
#### presignedPutObject(String bucketName, String objectName, Integer expires)
`public String presignedPutObject(String bucketName, String objectName, Integer expires)`
                                                              
Generates a presigned URL for HTTP PUT operations. Browsers/Mobile clients may point to this URL to upload objects directly to a bucket even if it is private. This presigned URL can have an associated expiration time in seconds after which it is no longer operational. The default expiry is set to 7 days.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPutObject-java.lang.String-java.lang.String-java.lang.Integer-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``bucketName``  | *String*  | Name of the bucket.  |
| ``objectName``  | *String*  | Object name in the bucket. |
| ``expiry``  | *Integer*  | Expiry in seconds. Default expiry is set to 7 days. |

<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> String: string contains URL to download the object.
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>InvalidKeyException : upon an invalid access key or secret key.</li>
            <li>IOException : upon connection error.</li>
            <li>NoSuchAlgorithmException : upon requested algorithm was not found during signature calculation.</li>
            <li>InvalidExpiresRangeException : upon input expires is out of range.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>


__Example__

```java
try {
	String url = minioClient.presignedPutObject("mybucket", "myobject", 60 * 60 * 24);
	System.out.println(url);
} catch(MinioException e) {
  System.out.println("Error occured: " + e);
}
```

<a name="presignedPostPolicy">
#### presignedPostPolicy(PostPolicy policy)
`public Map<String,String> presignedPostPolicy(PostPolicy policy)`

Allows setting policy conditions to a presigned URL for POST operations. Policies such as bucket name to receive object uploads, key name prefixes, expiry policy may be set.

[View Javadoc](http://minio.github.io/minio-java/io/minio/MinioClient.html#presignedPostPolicy-io.minio.PostPolicy-)

__Parameters__

|Param   | Type	  | Description  |
|:--- |:--- |:--- |
| ``policy``  | *PostPolicy*  | Post policy of an object.  |


<table>
    <thead>
        <tr>
            <th>Return Type	</th>
            <th>Exceptions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> Map<String,String>: Map of strings to construct form-data.
            </td>
            <td>
            <ul>
            <li>InvalidBucketNameException : upon invalid bucket name.</li>
            <li>InvalidKeyException : upon an invalid access key or secret key.</li>
            <li>IOException : upon connection error.</li>
            <li>NoSuchAlgorithmException : upon requested algorithm was not found during signature calculation.</li>
            </ul>
            </td>
            </tr>
               </tbody>
</table>



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
	System.out.println(" -F file=@/tmp/userpic.png  https://play.minio.io:9000/mybucket");
} catch(MinioException e) {
  System.out.println("Error occured: " + e);
```
## 5. Explore Further
 
- [Build your own photo API Service Example](/docs/java-photo-api-service)
- [Complete JavaDoc](http://minio.github.io/minio-java/)








