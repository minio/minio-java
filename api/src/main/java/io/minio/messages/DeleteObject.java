/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 Minio, Inc.
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

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;


/**
 * Helper class to create Amazon AWS S3 request XML containing object information for Multiple object deletion.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class DeleteObject extends XmlEntity {
  @Key("Key")
  private String name;
  @Key("VersionId")
  private String versionId;


  public DeleteObject(String name) throws XmlPullParserException {
    this(name, null);
  }


  /**
   * Constructs new delete object for given name and version ID.
   *
   */
  public DeleteObject(String name, String versionId) throws XmlPullParserException {
    super();
    super.name = "Object";

    this.name = name;
    this.versionId = versionId;
  }
}
