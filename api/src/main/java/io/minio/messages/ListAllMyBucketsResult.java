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

import io.minio.Utils;
import java.util.List;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html">ListBuckets API</a>.
 */
@Root(name = "ListAllMyBucketsResult", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class ListAllMyBucketsResult {
  @Element(name = "Owner")
  private Owner owner;

  @ElementList(name = "Buckets")
  private List<Bucket> buckets;

  @Element(name = "prefix", required = false)
  private String prefix;

  @Element(name = "ContinuationToken", required = false)
  private String continuationToken;

  public ListAllMyBucketsResult() {}

  /** Returns owner. */
  public Owner owner() {
    return owner;
  }

  /** Returns List of buckets. */
  public List<Bucket> buckets() {
    return Utils.unmodifiableList(buckets);
  }

  public String prefix() {
    return prefix;
  }

  public String continuationToken() {
    return continuationToken;
  }
}
