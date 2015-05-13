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

package io.minio.objectstorage.example;

import com.google.api.client.util.IOUtils;
import io.minio.objectstorage.client.Client;
import io.minio.objectstorage.client.ObjectMetadata;
import io.minio.objectstorage.client.messages.ListAllMyBucketsResult;
import io.minio.objectstorage.client.messages.ListBucketResult;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ObjectStorageExample {
    public static void main(String[] args) throws IOException, XmlPullParserException {
        System.out.println("Example app");

        // create client
        Client client = Client.getClient("http://localhost:9000");

        // create bucket
        client.makeBucket("mybucket", Client.ACL_PUBLIC_READ_WRITE);

        // list buckets
        ListAllMyBucketsResult allMyBucketsResult = client.listBuckets();
        System.out.println(allMyBucketsResult);

        // create object
        client.putObject("mybucket", "myobject", "application/octet-stream", 11, new ByteArrayInputStream("hello world".getBytes("UTF-8")));

        // list objects
        ListBucketResult myObjects = client.listObjectsInBucket("mybucket");
        System.out.println(myObjects);

        // get object metadata
        ObjectMetadata objectMetadata = client.getObjectMetadata("mybucket", "myobject");
        System.out.println(objectMetadata);

        // get object
        InputStream object = client.getObject("mybucket", "myobject");
        try  {
            System.out.println("Printing object: ");
            IOUtils.copy(object, System.out);
        } finally {
            object.close();
        }
    }
}
