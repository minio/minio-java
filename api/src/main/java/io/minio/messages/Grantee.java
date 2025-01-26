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

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/** Helper class to denote for the person being granted permissions of {@link Grant}. */
@Root(name = "Grantee")
@Namespace(prefix = "xsi", reference = "http://www.w3.org/2001/XMLSchema-instance")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class Grantee {
  @Attribute(name = "type")
  private String xsiType;

  @Element(name = "DisplayName", required = false)
  private String displayName;

  @Element(name = "EmailAddress", required = false)
  private String emailAddress;

  @Element(name = "ID", required = false)
  private String id;

  @Element(name = "Type")
  private GranteeType type;

  @Element(name = "URI", required = false)
  private String uri;

  public Grantee(
      @Nonnull GranteeType type,
      @Nullable String displayName,
      @Nullable String emailAddress,
      @Nullable String id,
      @Nullable String uri) {
    this.type = Objects.requireNonNull(type, "Type must not be null");
    this.displayName = displayName;
    this.emailAddress = emailAddress;
    this.id = id;
    this.uri = uri;
  }

  public Grantee(
      @Nonnull @Attribute(name = "type") String xsiType,
      @Nonnull @Element(name = "Type") GranteeType type,
      @Nullable @Element(name = "DisplayName", required = false) String displayName,
      @Nullable @Element(name = "EmailAddress", required = false) String emailAddress,
      @Nullable @Element(name = "ID", required = false) String id,
      @Nullable @Element(name = "URI", required = false) String uri) {
    this(type, displayName, emailAddress, id, uri);
    this.xsiType = xsiType;
  }

  public String displayName() {
    return displayName;
  }

  public String emailAddress() {
    return emailAddress;
  }

  public String id() {
    return id;
  }

  public GranteeType type() {
    return type;
  }

  public String uri() {
    return uri;
  }
}
