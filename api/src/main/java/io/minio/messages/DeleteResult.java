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

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;


/**
 * Helper class to create Amazon AWS S3 request XML containing information for Multiple object deletion.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class DeleteResult extends XmlEntity {
  @Key("Deleted")
  private List<DeletedObject> objectList = new LinkedList<>();
  @Key("Error")
  private List<DeleteError> errorList = new LinkedList<>();


  /**
   * Constructs new delete result by parsing content on given reader.
   */
  public DeleteResult(Reader reader) throws IOException, XmlPullParserException {
    super();
    super.name = "DeleteResult";
    this.parseXml(reader);
  }


  public List<DeletedObject> objectList() {
    return objectList;
  }


  public List<DeleteError> errorList() {
    return errorList;
  }
}
