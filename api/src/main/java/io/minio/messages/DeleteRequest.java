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

import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;


/**
 * Helper class to create Amazon AWS S3 request XML containing information for Multiple object deletion.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class DeleteRequest extends XmlEntity {
  @Key("Quiet")
  private boolean quiet;
  @Key("Object")
  private List<DeleteObject> objectList;


  public DeleteRequest(List<DeleteObject> objectList) throws XmlPullParserException {
    this(objectList, true);
  }


  /**
   * Constructs new delete request for given object list and quiet flag.
   */
  public DeleteRequest(List<DeleteObject> objectList, boolean quiet) throws XmlPullParserException {
    super();
    super.name = "Delete";
    super.namespaceDictionary.set("", "http://s3.amazonaws.com/doc/2006-03-01/");

    this.objectList = objectList;
    this.quiet = quiet;
  }
}
