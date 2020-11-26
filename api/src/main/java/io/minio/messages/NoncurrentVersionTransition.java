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

import javax.annotation.Nonnull;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote noncurrent version transition information for {@link LifecycleRule}. */
@Root(name = "NoncurrentVersionTransition")
public class NoncurrentVersionTransition extends NoncurrentVersionExpiration {
  @Element(name = "StorageClass")
  private String storageClass;

  public NoncurrentVersionTransition(
      @Element(name = "NoncurrentDays", required = false) int noncurrentDays,
      @Nonnull @Element(name = "StorageClass", required = false) String storageClass) {
    super(noncurrentDays);
    if (storageClass == null || storageClass.isEmpty()) {
      throw new IllegalArgumentException("StorageClass must be provided");
    }
    this.storageClass = storageClass;
  }

  public String storageClass() {
    return storageClass;
  }
}
