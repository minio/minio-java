/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2025 MinIO, Inc.
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
import org.simpleframework.xml.Root;

/**
 * Base part information for {@link ListPartsResult} and {@link
 * GetObjectAttributesOutput.ObjectParts}.
 */
@Root(name = "BasePartsResult", strict = false)
public abstract class BasePartsResult {
  @Element(name = "IsTruncated", required = false)
  private boolean isTruncated;

  @Element(name = "MaxParts", required = false)
  private Integer maxParts;

  @Element(name = "NextPartNumberMarker", required = false)
  private Integer nextPartNumberMarker;

  @Element(name = "PartNumberMarker", required = false)
  private Integer partNumberMarker;

  @ElementList(name = "Part", inline = true, required = false)
  private List<Part> parts;

  public BasePartsResult() {}

  public boolean isTruncated() {
    return isTruncated;
  }

  public Integer maxParts() {
    return maxParts;
  }

  public Integer nextPartNumberMarker() {
    return nextPartNumberMarker;
  }

  public Integer partNumberMarker() {
    return partNumberMarker;
  }

  public List<Part> parts() {
    return Utils.unmodifiableList(parts);
  }

  @Override
  public String toString() {
    return String.format(
        "isTruncated=%s, maxParts=%s, nextPartNumberMarker=%s, partNumberMarker=%s, parts=%s",
        Utils.stringify(isTruncated),
        Utils.stringify(maxParts),
        Utils.stringify(nextPartNumberMarker),
        Utils.stringify(partNumberMarker),
        Utils.stringify(parts));
  }
}
