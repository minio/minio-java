/*
 * Minio Java Library for Amazon S3 compatible cloud storage, (C) 2015 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio.examples;

import io.minio.client.Client;
import io.minio.client.errors.ClientException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ExamplePutObject {
    public static void main(String[] args) throws IOException, XmlPullParserException, ClientException {
        System.out.println("Example app");

        // Set s3 endpoint, region is calculated automatically
        Client s3Client = Client.getClient("https://s3.amazonaws.com");

        // Set access and secret keys
        s3Client.setKeys("YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 11 * 1024 * 1024; i++) {
            builder.append('a');
        }
        // create object
        s3Client.putObject("mybucket", "my/object", "application/octet-stream", 11 * 1024 * 1024, new ByteArrayInputStream(builder.toString().getBytes("UTF-8")));
    }
}
