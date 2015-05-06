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

public interface Client {
    URL getUrl();

    InputStream getObject(String bucket, String key) throws IOException;

    InputStream getObject(String bucket, String key, long offset, long length) throws IOException;

    ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException;

    ListBucketResult listObjectsInBucket(String bucket) throws IOException, XmlPullParserException;

    ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException;

    boolean testBucketAccess(String bucket) throws IOException;
}

