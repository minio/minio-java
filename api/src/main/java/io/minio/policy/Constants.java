/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2016 Minio, Inc.
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

package io.minio.policy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class Constants {
  // Resource prefix for all aws resources.
  public static final String AWS_RESOURCE_PREFIX = "arn:aws:s3:::";

  // Common bucket actions for both read and write policies.
  public static final Set<String> COMMON_BUCKET_ACTIONS = new HashSet<String>(Arrays.asList("s3:GetBucketLocation"));

  // Read only bucket actions.
  public static final Set<String> READ_ONLY_BUCKET_ACTIONS = new HashSet<String>(Arrays.asList("s3:ListBucket"));

  // Write only bucket actions.
  public static final Set<String> WRITE_ONLY_BUCKET_ACTIONS =
      new HashSet<String>(Arrays.asList("s3:ListBucketMultipartUploads"));

  // Read only object actions.
  public static final Set<String> READ_ONLY_OBJECT_ACTIONS = new HashSet<String>(Arrays.asList("s3:GetObject"));

  // Write only object actions.
  public static final Set<String> WRITE_ONLY_OBJECT_ACTIONS =
      new HashSet<String>(Arrays.asList("s3:AbortMultipartUpload",
                                        "s3:DeleteObject",
                                        "s3:ListMultipartUploadParts",
                                        "s3:PutObject"));

  // Read and write object actions.
  public static final Set<String> READ_WRITE_OBJECT_ACTIONS = new HashSet<String>();

  // All valid bucket and object actions.
  public static final Set<String> VALID_ACTIONS =  new HashSet<String>();

  static {
    READ_WRITE_OBJECT_ACTIONS.addAll(READ_ONLY_OBJECT_ACTIONS);
    READ_WRITE_OBJECT_ACTIONS.addAll(WRITE_ONLY_OBJECT_ACTIONS);

    VALID_ACTIONS.addAll(COMMON_BUCKET_ACTIONS);
    VALID_ACTIONS.addAll(READ_ONLY_BUCKET_ACTIONS);
    VALID_ACTIONS.addAll(WRITE_ONLY_BUCKET_ACTIONS);
    VALID_ACTIONS.addAll(READ_WRITE_OBJECT_ACTIONS);
  }
}
