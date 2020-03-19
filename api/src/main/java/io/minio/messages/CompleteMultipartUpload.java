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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Denotes complete multipart upload request XML as per
 * https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html.
 */
@Root(name = "CompleteMultipartUpload")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class CompleteMultipartUpload {
  @ElementList(name = "Part", inline = true)
  private List<Part> partList;

  /** Constucts a new CompleteMultipartUpload object with given parts. */
  public CompleteMultipartUpload(Part[] parts) throws IllegalArgumentException {
    if (parts == null || parts.length == 0) {
      throw new IllegalArgumentException("null or empty parts");
    }

    this.partList = Collections.unmodifiableList(Arrays.asList(parts));
  }
}
