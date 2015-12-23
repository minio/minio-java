/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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


import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.errors.ClientException;

import com.google.api.client.util.IOUtils;

import org.xmlpull.v1.XmlPullParserException;

import org.joda.time.DateTime;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class PresignedPostPolicy {
  public static void main(String[] args) throws IOException, XmlPullParserException, ClientException, NoSuchAlgorithmException, InvalidKeyException {

    // Note: YOUR-ACCESSKEYID, YOUR-SECRETACCESSKEY, my-bucketname and my-objectname
    // are dummy values, please replace them with original values.
    // Set s3 endpoint, region is calculated automatically.
    MinioClient s3Client = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");

    PostPolicy policy = s3Client.newPostPolicy();
    DateTime date = new DateTime();
    date = date.plusMonths(1); // Expire in one month.

    // set policy parameters.
    policy.setKey("my-objectname");
    policy.setBucket("my-bucketname");
    policy.setExpires(date);
    policy.setContentType("image/png");

    Map<String, String> formData = s3Client.presignedPostPolicy(policy);
    System.out.print("curl -X POST ");

    for (Map.Entry<String, String> entry : formData.entrySet()) {
        System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
    }
    System.out.print("-F file=@/tmp/userpic.png https://s3.amazonaws.com/my-bucketname\n");
  }
}
