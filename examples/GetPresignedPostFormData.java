/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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
import io.minio.errors.MinioException;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GetPresignedPostFormData {
  /** MinioClient.presignedPostPolicy() example. */
  public static void main(String[] args) throws IOException, MinioException {
    /* play.min.io for test and development. */
    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://play.min.io")
            .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
            .build();

    /* Amazon S3: */
    // MinioClient minioClient =
    //     MinioClient.builder()
    //         .endpoint("https://s3.amazonaws.com")
    //         .credentials("YOUR-ACCESSKEY", "YOUR-SECRETACCESSKEY")
    //         .build();

    // Create new post policy for 'my-bucket' with 7 days expiry from now.
    PostPolicy policy = new PostPolicy("my-bucket", ZonedDateTime.now().plusDays(7));

    // Add condition that 'key' (object name) equals to 'my-object'.
    policy.addEqualsCondition("key", "my-object");

    // Add condition that 'Content-Type' starts with 'image/'.
    policy.addStartsWithCondition("Content-Type", "image/");

    // Add condition that 'content-length-range' is between 64kiB to 10MiB.
    policy.addContentLengthRangeCondition(64 * 1024, 10 * 1024 * 1024);

    Map<String, String> formData = minioClient.getPresignedPostFormData(policy);

    // Upload an image using POST object with form-data.
    MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
    multipartBuilder.setType(MultipartBody.FORM);
    for (Map.Entry<String, String> entry : formData.entrySet()) {
      multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
    }
    multipartBuilder.addFormDataPart("key", "my-object");
    multipartBuilder.addFormDataPart("Content-Type", "image/png");

    // "file" must be added at last.
    multipartBuilder.addFormDataPart(
        "file", "my-object", RequestBody.create(new File("Pictures/avatar.png"), null));

    Request request =
        new Request.Builder()
            .url("https://play.min.io/my-bucket")
            .post(multipartBuilder.build())
            .build();
    OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
    Response response = httpClient.newCall(request).execute();
    if (response.isSuccessful()) {
      System.out.println("Pictures/avatar.png is uploaded successfully using POST object");
    } else {
      System.out.println("Failed to upload Pictures/avatar.png");
    }

    // Print curl command usage to upload file /tmp/userpic.jpg.
    System.out.print("curl -X POST ");
    for (Map.Entry<String, String> entry : formData.entrySet()) {
      System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
    }
    System.out.print(" -F key=my-object -F Content-Type=image/jpg");
    System.out.println(" -F file=@/tmp/userpic.jpg https://play.min.io/my-bucket");
  }
}
