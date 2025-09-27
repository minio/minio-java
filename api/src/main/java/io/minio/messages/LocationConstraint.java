/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

/**
 * Response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLocation.html">GetBucketLocation
 * API</a>.
 */
@Root(name = "LocationConstraint", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class LocationConstraint {
  @Text(required = false)
  private String location = "";

  public LocationConstraint() {}

  public String location() {
    return location;
  }

  @Override
  public String toString() {
    return String.format("LocationConstraint{location=%s}", Utils.stringify(location));
  }
}
