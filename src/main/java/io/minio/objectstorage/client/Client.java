/*
 * Minimal Object Storage Library, (C) 2015 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio.objectstorage.client;

import io.minio.objectstorage.client.messages.ListAllMyBucketsResult;
import io.minio.objectstorage.client.messages.ListBucketResult;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface Client {
    String ACL_PRIVATE = "private";
    String ACL_PUBLIC_READ = "public-read";
    String ACL_PUBLIC_READ_WRITE = "public-read-write";
    String ACL_AUTHENTICATED_READ = "authenticated-read";
    String ACL_BUCKET_OWNER_READ = "bucket-owner-read";
    String ACL_BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";

    URL getUrl();

    InputStream getObject(String bucket, String key) throws IOException;

    InputStream getObject(String bucket, String key, long offset, long length) throws IOException;

    ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException;

    ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException;

    ListBucketResult listObjectsInBucket(String bucket) throws IOException, XmlPullParserException;

    boolean testBucketAccess(String bucket) throws IOException;

    boolean createBucket(String bucket, String acl) throws IOException;

    void createObject(String bucket, String key, String contentType, long size, InputStream data) throws IOException, XmlPullParserException;

    void setKeys(String foo, String bar);

    void setUserAgent(String foo);

    void setContentType(String foo);
}
