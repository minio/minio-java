# Args class structure

Every public operation takes one immutable `XxxArgs` object built via
`XxxArgs.builder()`. When adding an operation, subclass the level that already
carries the fields you need rather than re-declaring them.

## Core hierarchy

```
                +------------------+
                |     BaseArgs     |
                |~~~~~~~~~~~~~~~~~~|
                | location         |
                | extraHeaders     |
                | extraQueryParams |
                +------------------+
                         |
             +-----------+-----------+
             |                       |
             v                       v
       +------------+        +-----------------+
       | BucketArgs |        | ListBucketsArgs |
       |~~~~~~~~~~~~|        +-----------------+
       | bucketName |
       | region     |
       +------------+
             |
             v
       +------------+
       | ObjectArgs |
 +-----|~~~~~~~~~~~~|-----+
 |     | objectName |     |
 |     +------------+     |
 |                        |
 v                        v
+-----------------+     +-------------------+
| ObjectWriteArgs |     | ObjectVersionArgs |
|~~~~~~~~~~~~~~~~~|     |~~~~~~~~~~~~~~~~~~~|
| headers         |     | versionId         |
| userMetadata    |     +-------------------+
| sse             |              |
| tags            |              v
| retention       |      +----------------+
| legalHold       |      | ObjectReadArgs |
+-----------------+      |~~~~~~~~~~~~~~~~|
                         | ssec           |
                         +----------------+
                                 |
                                 v
                   +---------------------------+
                   | ObjectConditionalReadArgs |
                   |~~~~~~~~~~~~~~~~~~~~~~~~~~~|
                   | offset                    |
                   | length                    |
                   | matchETag                 |
                   | notMatchETag              |
                   | modifiedSince             |
                   | unmodifiedSince           |
                   | fetchChecksum             |
                   +---------------------------+
```

| Class                       | Extends             | Adds                                                                                                 |
|-----------------------------|---------------------|------------------------------------------------------------------------------------------------------|
| `BaseArgs`                  | —                   | `location`, `extraHeaders`, `extraQueryParams`                                                       |
| `BucketArgs`                | `BaseArgs`          | `bucketName`, `region`                                                                               |
| `ObjectArgs`                | `BucketArgs`        | `objectName`                                                                                         |
| `ObjectWriteArgs`           | `ObjectArgs`        | `headers`, `userMetadata`, `sse`, `tags`, `retention`, `legalHold`                                   |
| `ObjectVersionArgs`         | `ObjectArgs`        | `versionId`                                                                                          |
| `ObjectReadArgs`            | `ObjectVersionArgs` | `ssec`                                                                                               |
| `ObjectConditionalReadArgs` | `ObjectReadArgs`    | `offset`, `length`, `matchETag`, `notMatchETag`, `modifiedSince`, `unmodifiedSince`, `fetchChecksum` |

Rules of thumb:

- All bucket arg classes inherit `BucketArgs`, except `ListBucketsArgs`, which is
  not scoped to a bucket and inherits `BaseArgs` directly.
- Object arg classes with no SSE requirement inherit `ObjectVersionArgs`.
- Object arg classes doing an object read with SSE-C context inherit `ObjectReadArgs`.
- Object arg classes doing object creation inherit `ObjectWriteArgs`.

## Intermediate base classes

These sit between the core classes and the concrete arg classes. Each is abstract
and exists to share fields or behaviour between exactly two operations.

| Class                  | Extends                     | Adds                                                                                                         |
|------------------------|-----------------------------|--------------------------------------------------------------------------------------------------------------|
| `CreateBucketBaseArgs` | `BucketArgs`                | `objectLock`, `locationConfig`, `bucket`, `tags`, `forceCreate`                                              |
| `HeadBucketBaseArgs`   | `BucketArgs`                | —                                                                                                            |
| `PutObjectAPIBaseArgs` | `ObjectArgs`                | `file`, `buffer`, `data`, `length`, `headers`                                                                |
| `PutObjectBaseArgs`    | `ObjectWriteArgs`           | `objectSize`, `partSize`, `partCount`, `contentType`, `checksum`, `parallelUploads`, `delayMs`, `maxRetries` |
| `HeadObjectBaseArgs`   | `ObjectConditionalReadArgs` | —                                                                                                            |

## Concrete arg classes by direct parent

### `BaseArgs`

`ListBucketsArgs`

### `BucketArgs`

`DeleteBucketCorsArgs`, `DeleteBucketEncryptionArgs`, `DeleteBucketLifecycleArgs`,
`DeleteBucketNotificationArgs`, `DeleteBucketPolicyArgs`, `DeleteBucketReplicationArgs`,
`DeleteBucketTagsArgs`, `DeleteObjectLockConfigurationArgs`, `DeleteObjectsArgs`,
`GetBucketCorsArgs`, `GetBucketEncryptionArgs`, `GetBucketLifecycleArgs`,
`GetBucketLocationArgs`, `GetBucketNotificationArgs`, `GetBucketPolicyArgs`,
`GetBucketReplicationArgs`, `GetBucketTagsArgs`, `GetBucketVersioningArgs`,
`GetObjectLockConfigurationArgs`, `ListMultipartUploadsArgs`, `ListObjectVersionsArgs`,
`ListObjectsArgs`, `ListObjectsV1Args`, `ListObjectsV2Args`, `ListenBucketNotificationArgs`,
`PutObjectFanOutArgs`, `RemoveBucketArgs`, `RemoveObjectsArgs`, `SetBucketCorsArgs`,
`SetBucketEncryptionArgs`, `SetBucketLifecycleArgs`, `SetBucketNotificationArgs`,
`SetBucketPolicyArgs`, `SetBucketReplicationArgs`, `SetBucketTagsArgs`,
`SetBucketVersioningArgs`, `SetObjectLockConfigurationArgs`

### `CreateBucketBaseArgs`

`CreateBucketArgs`, `MakeBucketArgs`

### `HeadBucketBaseArgs`

`BucketExistsArgs`, `HeadBucketArgs`

### `ObjectArgs`

`AbortMultipartUploadArgs`, `AppendObjectArgs`, `CompleteMultipartUploadArgs`,
`CreateMultipartUploadArgs`, `ListPartsArgs`, `UploadPartCopyArgs`

### `PutObjectAPIBaseArgs`

`PutObjectAPIArgs`, `UploadPartArgs`

### `ObjectWriteArgs`

`ComposeObjectArgs`, `CopyObjectArgs`, `UploadSnowballObjectsArgs`

### `PutObjectBaseArgs`

`PutObjectArgs`, `UploadObjectArgs`

### `ObjectVersionArgs`

`DeleteObjectTagsArgs`, `DisableObjectLegalHoldArgs`, `EnableObjectLegalHoldArgs`,
`GetObjectAclArgs`, `GetObjectRetentionArgs`, `GetObjectTagsArgs`,
`GetPresignedObjectUrlArgs`, `IsObjectLegalHoldEnabledArgs`, `PromptObjectArgs`,
`RemoveObjectArgs`, `RestoreObjectArgs`, `SetObjectRetentionArgs`, `SetObjectTagsArgs`

### `ObjectReadArgs`

`DownloadObjectArgs`, `GetObjectAttributesArgs`, `SelectObjectContentArgs`

### `ObjectConditionalReadArgs`

`GetObjectArgs`, `SourceObject`

### `HeadObjectBaseArgs`

`HeadObjectArgs`, `StatObjectArgs`
