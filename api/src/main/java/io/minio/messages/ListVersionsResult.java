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

import java.util.List;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html">ListObjectVersions
 * API</a>.
 */
@Root(name = "ListVersionsResult", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class ListVersionsResult extends ListObjectsResult {
  @Element(name = "KeyMarker", required = false)
  private String keyMarker;

  @Element(name = "NextKeyMarker", required = false)
  private String nextKeyMarker;

  @Element(name = "VersionIdMarker", required = false)
  private String versionIdMarker;

  @Element(name = "NextVersionIdMarker", required = false)
  private String nextVersionIdMarker;

  @ElementList(name = "Version", inline = true, required = false)
  private List<Version> contents;

  @ElementList(name = "DeleteMarker", inline = true, required = false)
  private List<DeleteMarker> deleteMarkers;

  public String keyMarker() {
    return decodeIfNeeded(keyMarker);
  }

  public String nextKeyMarker() {
    return decodeIfNeeded(nextKeyMarker);
  }

  public String versionIdMarker() {
    return versionIdMarker;
  }

  public String nextVersionIdMarker() {
    return nextVersionIdMarker;
  }

  @Override
  public List<Version> contents() {
    return emptyIfNull(contents);
  }

  @Override
  public List<DeleteMarker> deleteMarkers() {
    return emptyIfNull(deleteMarkers);
  }
}
