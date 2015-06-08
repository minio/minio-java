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

package io.minio.client;

import io.minio.client.acl.Acl;
import io.minio.client.errors.ClientException;
import io.minio.client.messages.Bucket;
import io.minio.client.messages.Upload;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class IntegrationTest {

    @Test
    @Ignore
    public void testSigning() throws IOException, XmlPullParserException, ClientException {
        Client client = Client.getClient("https://s3-us-west-2.amazonaws.com");
//        client.enableLogging();
        Iterator<Bucket> bucketIterator = client.listBuckets();
        while (bucketIterator.hasNext()) {
            Bucket bucket = bucketIterator.next();
            System.out.println(bucket.getName());
        }
    }

    @Test
    @Ignore
    public void testClient() throws IOException, XmlPullParserException, ClientException {
        Client client = Client.getClient("https://s3-us-west-2.amazonaws.com");
//        Client client = Client.getClient("http://localhost:9000");
        client.enableLogging();
        client.makeBucket("examplebucket", Acl.PUBLIC_READ_WRITE);

        client.setBucketACL("foo", Acl.PRIVATE);

        String inputString = "hello world";
        ByteArrayInputStream data = new ByteArrayInputStream(inputString.getBytes("UTF-8"));
        client.putObject("examplebucket", "bar", "application/octet-stream", 11, data);

        InputStream object = client.getObject("examplebucket", "bar");
        byte[] result = new byte[11];
        int read = object.read(result);
        assertEquals(read, 11);
        object.close();
        String resultString = new String(result, "UTF-8");
        assertEquals(inputString, resultString);

        byte[] largeObject = new byte[10 * 1024 * 1024];
        for (int i = 0; i < 10 * 1024 * 1024; i++) {
            largeObject[i] = 'a';
        }

        InputStream largeObjectStream = new ByteArrayInputStream(largeObject);
        client.putObject("examplebucket", "bar2", "application/octet-stream", largeObject.length, largeObjectStream);

        InputStream object1 = client.getObject("examplebucket", "bar2");
        byte[] largeResult = new byte[10 * 1024 * 1024];
        int amountRead = 0;
        while (amountRead != largeResult.length) {
            amountRead += object1.read(largeResult, amountRead, largeResult.length - amountRead);
        }
        assertEquals(amountRead, largeResult.length);

        assertArrayEquals(largeObject, largeResult);
    }

    @Test
    @Ignore
    public void testMultipart() throws IOException, XmlPullParserException, ClientException {
        byte[] largeObject = new byte[10 * 1024 * 1024];
        for (int i = 0; i < 10 * 1024 * 1024; i++) {
            largeObject[i] = 'a';
        }
//        Client client = Client.getClient("http://localhost:9000");
        Client client = Client.getClient("https://s3-us-west-2.amazonaws.com");
        client.enableLogging();
        try {
            client.putObject("examplebucket", "bar2", "application/octet-stream", largeObject.length * 2, new ByteArrayInputStream(largeObject));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        largeObject = new byte[20 * 1024 * 1024];
        for (int i = 0; i < 20 * 1024 * 1024; i++) {
            largeObject[i] = 'a';
        }
        client.enableLogging();
        Iterator<Result<Upload>> examplebucket = client.listAllUnfinishedUploads("examplebucket");
        System.out.println(examplebucket.next().getResult());
        client.putObject("examplebucket", "bar2", "application/octet-stream", largeObject.length, new ByteArrayInputStream(largeObject));
        examplebucket = client.listAllUnfinishedUploads("examplebucket");
        System.out.println(examplebucket.hasNext());
    }

    @Test
    @Ignore
    public void testInternationalCharacters() throws XmlPullParserException, IOException, ClientException {
        String input = "hello 世界";
        byte[] inputBytes = input.getBytes("UTF-8");
        Client client = Client.getClient("https://s3-us-west-2.amazonaws.com");
        client.enableLogging();
        client.putObject("examplebucket", "世界", "application/octet-stream", inputBytes.length, new ByteArrayInputStream(inputBytes));
        InputStream object = client.getObject("examplebucket", "世界");
        byte[] result = new byte[inputBytes.length];
        int read = object.read(result);
        assertEquals(read, inputBytes.length);
        object.close();
        String resultString = new String(result, "UTF-8");
        assertEquals(input, resultString);
    }
}
