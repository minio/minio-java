/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;


/**
 * Helper class to parse Amazon AWS S3 response XML containing Grantee information.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class Grantee extends XmlEntity {
  @Key("ID")
  private String id;
  @Key("DisplayName")
  private String displayName;
  @Key("EmailAddress")
  private String emailAddress;
  @Key("Type")
  private String type;
  @Key("URI")
  private String uri;


  public Grantee() throws XmlPullParserException {
    super();
    this.name = "Grantee";
  }


  /**
   * Returns grantee id.
   */
  public String id() {
    return id;
  }


  /**
   * Returns grantee display name.
   */
  public String displayName() {
    return displayName;
  }


  /**
   * Returns grantee email address.
   */
  public String emailAddress() {
    return emailAddress;
  }


  /**
   * Returns grantee type.
   */
  public String type() {
    return type;
  }


  /**
   * Returns grantee URI.
   */
  public String uri() {
    return uri;
  }
}
