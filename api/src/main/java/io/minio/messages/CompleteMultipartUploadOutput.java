/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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

package io.minio.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload
 * API</a>.
 */
@Root(name = "CompleteMultipartUploadOutput")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class CompleteMultipartUploadOutput {
  @Element(name = "Location")
  private String location;

  @Element(name = "Bucket")
  private String bucket;

  @Element(name = "Key")
  private String object;

  @Element(name = "ETag")
  private String etag;

  @Element(name = "ChecksumCRC32", required = false)
  private String checksumCRC32;

  @Element(name = "ChecksumCRC32C", required = false)
  private String checksumCRC32C;

  @Element(name = "ChecksumSHA1", required = false)
  private String checksumSHA1;

  @Element(name = "ChecksumSHA256", required = false)
  private String checksumSHA256;

  public CompleteMultipartUploadOutput() {}

  public String location() {
    return location;
  }

  public String bucket() {
    return bucket;
  }

  public String object() {
    return object;
  }

  public String etag() {
    return etag;
  }

  public String checksumCRC32() {
    return checksumCRC32;
  }

  public String checksumCRC32C() {
    return checksumCRC32C;
  }

  public String checksumSHA1() {
    return checksumSHA1;
  }

  public String checksumSHA256() {
    return checksumSHA256;
  }
}
