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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/** CannedAcl representing retrieval cannedAcl value. */
@Root(name = "CannedAcl")
@Convert(CannedAcl.CannedAclConverter.class)
public enum CannedAcl {
  PRIVATE("private"),
  PUBLIC_READ("public-read"),
  PUBLIC_READ_WRITE("public-read-write"),
  AUTHENTICATED_READ("authenticated-read"),
  AWS_EXEC_READ("aws-exec-read"),
  BUCKET_OWNER_READ("bucket-owner-read"),
  BUCKET_OWNER_FULL_CONTROL("bucket-owner-full-control");

  private final String value;

  private CannedAcl(String value) {
    this.value = value;
  }

  public String toString() {
    return this.value;
  }

  /** Returns CannedAcl of given string. */
  @JsonCreator
  public static CannedAcl fromString(String cannedAclString) {
    for (CannedAcl cannedAcl : CannedAcl.values()) {
      if (cannedAclString.equals(cannedAcl.value)) {
        return cannedAcl;
      }
    }

    throw new IllegalArgumentException("Unknown canned ACL '" + cannedAclString + "'");
  }

  /** XML converter class. */
  public static class CannedAclConverter implements Converter<CannedAcl> {
    @Override
    public CannedAcl read(InputNode node) throws Exception {
      return CannedAcl.fromString(node.getValue());
    }

    @Override
    public void write(OutputNode node, CannedAcl cannedAcl) throws Exception {
      node.setValue(cannedAcl.toString());
    }
  }
}
