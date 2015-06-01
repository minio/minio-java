# Minimal object storage library in Java [![Build Status](https://travis-ci.org/minio/minio-java.svg)](https://travis-ci.org/minio/minio-java)

## API

### Bucket

~~~
 MakeBucket(String bucket) throws ClientException
 ListBuckets(bucket string) Iterator<BucketStat>
 BucketExists(String bucket) boolean throw ClientException
 RemoveBucket(String bucket) throw ClientException
 GetBucketACL(String bucket) BucketACL throws ClientException
 SetBucketACL(String bucket, BucketACL acl) throws ClientException
 DropAllIncompleteUploads(String bucket) throws ClientException
~~~

### Object

~~~
 GetObject(String bucket, String key) java.io.Reader throws ClientException
 PutObject(bucket, key string) throws ClientException
 ListObjects(bucket) ExceptionIterator<ObjectStat>
 ListObjects(bucket, prefix) ExceptionIterator<ObjectStat>
 ListObjects(bucket, recursive) ExceptionIterator<ObjectStat>
 ListObjects(bucket, prefix string, recursive bool) ExceptionIterator<ObjectStat>
 StatObject(bucket, key string) ObjectStat throws ClientException
 RemoveObject(bucket, key string) throws ClientException
 DropIncompleteUpload(bucket, key string) throws ClientException
~~~

### Error

~~~
 ClientIOException extends IOException
 BucketNotExistException extends ClientException extends Exception
 ObjectNotExistException extends ClientException
 ...
~~~

## Install

```sh
$ git clone https://github.com/minio/minio-java
$ ./gradlew jar
```

## Join The Community
* Community hangout on Gitter    [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Minio/minio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
* Ask questions on Quora  [![Quora](http://upload.wikimedia.org/wikipedia/commons/thumb/5/57/Quora_logo.svg/55px-Quora_logo.svg.png)](http://www.quora.com/Minio)

## Contribute

[Contributors Guide](./CONTRIBUTING.md)
