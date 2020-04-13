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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/** Helper class to denote Rule information for {@link SseConfiguration}. */
@Root(name = "Rule")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class SseConfigurationRule {
  @Element(name = "ApplyServerSideEncryptionByDefault")
  ApplySseByDefault sseDefault;

  public SseConfigurationRule() {}

  /** Constructs new server-side encryption configuration rule. */
  public SseConfigurationRule(String kmsMasterKeyId, SseAlgorithm sseAlgorithm) {
    this.sseDefault = new ApplySseByDefault(kmsMasterKeyId, sseAlgorithm);
  }

  public String kmsMasterKeyId() {
    if (this.sseDefault == null) {
      return null;
    }

    return this.sseDefault.kmsMasterKeyId();
  }

  public SseAlgorithm sseAlgorithm() {
    if (this.sseDefault == null) {
      return null;
    }

    return this.sseDefault.sseAlgorithm();
  }
}
