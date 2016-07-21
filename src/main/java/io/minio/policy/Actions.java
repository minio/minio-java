/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

package io.minio;

import java.util.*;

/**
 * Helper class to parse Amazon AWS S3 policy documents.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
class Actions {
  static final List<String> commonBucket = Arrays.asList(
      "s3:GetBucketLocation"
  );

  static final List<String> readWriteBucket = Arrays.asList(
      "s3:ListBucket",
      "s3:ListBucketMultipartUploads"
  );

  static final List<String> readWriteObject = Arrays.asList(
      "s3:AbortMultipartUpload",
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:ListMultipartUploadParts",
      "s3:PutObject"
      // Add more object level read-write actions here.
  );

  static final List<String> writeOnlyBucket = Arrays.asList(
      // Add more bucket level write actions here.
      "s3:ListBucketMultipartUploads"
  );

  static final List<String> writeOnlyObject = Arrays.asList(
      "s3:AbortMultipartUpload",
      "s3:DeleteObject",
      "s3:ListMultipartUploadParts",
      "s3:PutObject"
      // Add more object level write actions here.
  );

  static final List<String> readOnlyBucket = Arrays.asList(
      "s3:ListBucket"
      // Add more bucket level read actions here.
  );

  static final List<String> readOnlyObject = Arrays.asList(
      "s3:GetObject"
      // Add more object level read actions here.
  );
}

