## API Documentation

### Minio client object creation
Minio client object is created using minio-java:
```java
MinioClient s3Client = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY"); 
```

s3Client can be used to perform operations on S3 storage. APIs are described below.

### Bucket operations
* [`makeBucket`](#makeBucket)
* [`listBuckets`](#listBuckets)
* [`bucketExists`](#bucketExists)
* [`removeBucket`](#removeBucket)
* [`getBucketAcl`](#getBucketAcl)
* [`setBucketAcl`](#setBucketAcl)
* [`listObjects`](#listObjects)
* [`listIncompleteUploads`](#listIncompleteUploads)

### Object operations

* [`getObject`](#getObject)
* [`putObject`](#putObject)
* [`statObject`](#statObject)
* [`removeObject`](#removeObject)
* [`removeIncompleteUpload`](#removeIncompleteUpload)

### Presigned operations

* [`presignedGetObject`](#presignedGetObject)
* [`presignedPutObject`](#presignedPutObject)
* [`presignedPostPolicy`](#presignedPostPolicy)

### Bucket operations
---------------------------------------
<a name="makeBucket">
#### makeBucket(String bucketName)
Creates a bucket with default region and ACL.

__Arguments__
* `bucketName` _String_: Bucket name

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Example__
```java
s3Client.makeBucket("my-bucketname");
System.out.println("my-bucketname is created successfully");
```
---------------------------------------
<a name="listBuckets">
#### listBuckets()
Returns all bucket information owned by the current user.

__Throws__
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Return value__
* `List<Bucket>` : List of bucket type.

__Example__
```java
List<Bucket> bucketList = s3Client.listBuckets();
for (Bucket bucket : bucketList)
{
    System.out.println(bucket.creationDate() + ", " + bucket.name());
}
```
---------------------------------------
<a name="bucketExists">
#### bucketExists(String bucketName)
Checks if given bucket exist and is having read access.

__Arguments__
* `bucketName` _String_: Bucket name

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Return value__
* `boolean` : true if the bucket exists and the user has at least read access

__Example__
```java
boolean found = s3Client.bucketExists("my-bucketname");
if (found) {
    System.out.println("my-bucketname exists");
} else {
    System.out.println("my-bucketname does not exist");
}
```
---------------------------------------
<a name="removeBucket">
#### removeBucket(String bucketName)
Removes a bucket.

*NOTE: - All objects (including all object versions and delete markers) in the bucket must be deleted prior, this API will not recursively delete objects*

__Arguments__
* `bucketName` _String_: Bucket name

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Example__
```java
s3Client.removeBucket("my-bucketname");
System.out.println("my-bucketname is removed successfully");
```
---------------------------------------
<a name="getBucketAcl">
#### getBucketAcl(String bucketName)
Returns ACL of given bucket.

__Arguments__
* `bucketName` _String_: Bucket name

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Returns__
* `Acl` : Acl type

__Example__
```java
Acl acl = s3Client.getBucketAcl("my-bucketname");
System.out.println(acl);
```
---------------------------------------
<a name="setBucketAcl">
#### setBucketAcl(String bucketName, Acl acl)
Sets ACL to given bucket.

__Arguments__
* `bucketName` _String_: Bucket name
* `acl` _Acl_: Canned ACL

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `InvalidAclNameException` : upon invalid ACL is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Example__
```java
s3Client.setBucketAcl("my-bucketname", Acl.PUBLIC_READ_WRITE);
System.out.println("Canned ACL " + Acl.PUBLIC_READ_WRITE + " is set successfully to my-bucketname");
```
---------------------------------------
<a name="listObjects">
#### listObjects(String bucketName, String prefix, boolean recursive)
Returns `Iterable<Result><Item>>` of object information.

__Arguments__
* `bucketName` _String_: Bucket name
* `prefix` _String_: Prefix string. List objects whose name starts with `prefix`
* `recursive` _boolean_: when false, emulates a directory structure where each listing returned is either a full object or part of the object's key up to the first '/'. All objects with the same prefix up to the first '/' will be merged into one entry.

__Returns__
* `Iterable<Result<Item>>` : an iterator of Items

__Example__
```java
Iterable<Result<Item>> myObjects = s3Client.listObjects("my-bucketname");
for (Result<Item> result : myObjects) {
    Item item = result.get();
    System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
}
```
---------------------------------------
<a name="listIncompleteUploads">
#### listIncompleteUploads(String bucketName, String prefix, boolean recursive)
Returns `Iterable<Result><Upload>>` of incomplete uploads.

__Arguments__
* `bucketName` _String_: Bucket name
* `prefix` _String_: Prefix string. List objects whose name starts with `prefix`
* `recursive` _boolean_: when false, emulates a directory structure where each listing returned is either a full object or part of the object's key up to the first '/'. All uploads with the same prefix up to the first '/' will be merged into one entry.

__Returns__
* `Iterable<Result<Upload>>` : an iterator of Upload.

__Example__
```java
Iterable<Result<Upload>> myObjects = s3Client.listIncompleteUploads("my-bucketname");
for (Result<Upload> result : myObjects) {
    Upload upload = result.get();
    System.out.println(upload.uploadId() + ", " + upload.objectName());
}
```
---------------------------------------
### Object operations
<a name="getObject">
#### getObject(String bucketName, String objectName)
Returns an InputStream containing the object. The InputStream must be closed after use else the connection will remain open.

__Arguments__
* `bucketName` _String_: Bucket name
* `objectName` _String_: Object name in the bucket

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Returns__
* `InputStream` : InputStream containing the object

__Example__
```java
InputStream stream = s3Client.getObject("my-bucketname", "my-objectname");
byte[] buf = new byte[16384];
int bytesRead;
while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead));
}
stream.close();
```
---------------------------------------
<a name="getObject">
#### getObject(String bucketName, String objectName, long offset, Long length)
Returns an InputStream containing a subset of the object. The InputStream must be closed after use else the connection will remain open.

__Arguments__
* `bucketName` _String_: Bucket name.
* `objectName` _String_: Object name in the bucket.
* `offset` _long_: Offset to read at.
* `length` _Long_: Length to read.

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Returns__
* `InputStream` : InputStream containing the object.

__Example__
```java
InputStream stream = s3Client.getObject("my-bucketname", "my-objectname", 1024L, 4096L);
byte[] buf = new byte[16384]; int bytesRead;
while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
    System.out.println(new String(buf, 0, bytesRead));
}
stream.close();
```
---------------------------------------
<a name="getObject">
#### getObject(String bucketName, String objectName, String fileName)
Downloads object and store it to given file name.

__Arguments__
* `bucketName` _String_: Bucket name.
* `objectName` _String_: Object name in the bucket.
* `fileName` _String_: file name.

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Example__
```java
s3Client.getObject("my-bucketname", "my-objectname", "photo.jpg");
```
---------------------------------------
<a name="putObject">
#### putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)
Uploads data from given stream as object.

If the object is larger than 5MB, the client will automatically use a multipart session.

If the session fails, the user may attempt to re-upload the object by attempting to create the exact same object again. The client will examine all parts of any current upload session and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail before uploading any more data. Otherwise, it will resume uploading where the session left off.

If the multipart session fails, the user is responsible for resuming or removing the session.

__Arguments__
* `bucketName` _String_: Bucket name
* `objectName` _String_: Object name to create in the bucket
* `stream` _InputStream_: stream to upload
* `size` _long_: Size of all the data that will be uploaded
* `contentType` _String_: Content type of the stream

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

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
s3Client.putObject("my-bucketname", "my-objectname", bais, bais.available(), "application/octet-stream");
bais.close();
System.out.println("my-bucketname is uploaded successfully");
```
---------------------------------------
<a name="putObject">
#### putObject(String bucketName, String objectName, String fileName)
Uploads given file as object.

If the object is larger than 5MB, the client will automatically use a multipart session.

If the session fails, the user may attempt to re-upload the object by attempting to create the exact same object again. The client will examine all parts of any current upload session and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail before uploading any more data. Otherwise, it will resume uploading where the session left off.

If the multipart session fails, the user is responsible for resuming or removing the session.

__Arguments__
* `bucketName` _String_: Bucket name
* `objectName` _String_: Object name to create in the bucket
* `fileName` _String_: File name to upload

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Example__
```java
s3Client.putObject("my-bucketname",  "island.jpg", "/mnt/photos/island.jpg")
System.out.println("island.jpg is uploaded successfully");
```
---------------------------------------
<a name="statObject">
#### statObject(String bucketName, String objectName)
Returns metadata of given object.

__Arguments__
* `bucketName` _String_: Bucket name.
* `objectName` _String_: Object name in the bucket.

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Returns__
* `ObjectStat`: Populated object metadata.

__Example__
```java
ObjectStat objectStat = s3Client.statObject("my-bucketname", "my-objectname");
System.out.println(objectStat);
```
---------------------------------------
<a name="removeObject">
#### removeObject(String bucketName, String objectName)
Removes an object from a bucket.

__Arguments__
* `bucketName` _String_: Bucket name
* `objectName` _String_: Object name in the bucket

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Example__
```java
s3Client.removeObject("my-bucketname", "my-objectname");
System.out.println("my-objectname is removed successfully");
```
---------------------------------------
<a name="removeIncompleteUpload">
#### removeIncompleteUpload(String bucketName, String objectName)
Removes incomplete multipart upload of given object.

__Arguments__
* `bucketName` _String_: Bucket name
* `objectName` _String_: Object name in the bucket

__Throws__
* `InvalidBucketNameException` : upon invalid bucket name is given
* `NoResponseException` : upon no response from server
* `IOException` : upon connection error
* `org.xmlpull.v1.XmlPullParserException` : upon parsing response xml
* `ErrorResponseException` : upon unsuccessful execution
* `InternalException` : upon internal library error

__Example__
```java
s3Client.removeIncompleteUpload("my-bucketname", "my-objectname");
System.out.println("successfully removed all incomplete upload session of my-bucketname/my-objectname");
```
---------------------------------------
### Presigned operations
<a name="presignedGetObject">
#### presignedGetObject(String bucketName, String objectName, Integer expires)
Returns an presigned URL containing the object.

__Arguments__
* `bucketName` _String_: Bucket name
* `objectName` _String_: Object name in the bucket
* `expires` _Integer_: object expiration

__Throws__
* `InvalidBucketNameException` : upon an invalid bucket name
* `InvalidKeyException` : upon an invalid access key or secret key
* `IOException` : upon signature calculation failure
* `NoSuchAlgorithmException` : upon requested algorithm was not found during signature calculation
* `InvalidExpiresRangeException` : upon input expires is out of range

__Return value__
* `String` : string contains URL to download the object

__Example__
```java
String url = s3Client.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24);
System.out.println(url);
```
---------------------------------------
<a name="presignedPutObject">
#### presignedPutObject(String bucketName, String objectName, Integer expires)
Returns an presigned URL for PUT.

__Arguments__
* `bucketName` _String_: Bucket name
* `objectName` _String_: Object name in the bucket
* `expires` _Integer_: object expiration

__Throws__
* `InvalidBucketNameException` : upon an invalid bucket name
* `InvalidKeyException` : upon an invalid access key or secret key
* `IOException` : upon signature calculation failure
* `NoSuchAlgorithmException` : upon requested algorithm was not found during signature calculation
* `InvalidExpiresRangeException` : upon input expires is out of range

__Return value__
* `String` : string contains URL to upload the object

__Example__
```java
String url = s3Client.presignedPutObject("my-bucketname", "my-objectname", 60 * 60 * 24);
System.out.println(url);
```
---------------------------------------
<a name="presignedPostPolicy">
#### presignedPostPolicy(PostPolicy policy)
Returns string map for given PostPolicy.

__Arguments__
* `policy` _PostPolicy_: Post policy

__Throws__
* `InvalidBucketNameException` : upon an invalid bucket name
* `InvalidKeyException` : upon an invalid access key or secret key
* `IOException` : upon signature calculation failure
* `NoSuchAlgorithmException` : upon requested algorithm was not found during signature calculation

__Return value__
* `Map<String,String>` - Map of strings to construct form-data

__Example__
```java
PostPolicy policy = new PostPolicy("my-bucketname", "my-objectname", DateTime.now().plusDays(7));
policy.setContentType("image/png");
Map<String,String> formData = s3Client.presignedPostPolicy(policy);
System.out.print("curl -X POST ");
for (Map.Entry<String,String> entry : formData.entrySet()) {
    System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
}
System.out.println(" -F file=@/tmp/userpic.png https://my-bucketname.s3.amazonaws.com/");
```
---------------------------------------
