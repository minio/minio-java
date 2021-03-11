/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of request XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketMetricsConfiguration.html">PutBucketMetricsConfiguration
 * API</a> and response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketMetricsConfiguration.html">GetBucketMetricsConfiguration
 * API</a>.
 */
@Root(name = "MetricsConfiguration")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class MetricsConfiguration {
  @Element(name = "Id")
  private String id;

  @Element(name = "Filter", required = false)
  private RuleFilter filter;

  /** Constructs new metrics configuration. */
  public MetricsConfiguration(
      @Nullable @Element(name = "Id") String id,
      @Nonnull @Element(name = "Filter", required = false) RuleFilter filter) {
    if (id == null || id.isEmpty()) throw new IllegalArgumentException("id must be provided");
    this.id = id;
    this.filter = filter;
  }

  public String id() {
    return this.id;
  }

  public RuleFilter filter() {
    return this.filter;
  }
}
