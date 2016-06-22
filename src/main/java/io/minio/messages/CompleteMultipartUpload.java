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

import java.util.List;
import java.util.Arrays;


/**
 * Helper class to construct complete multipart upload request XML for Amazon AWS S3.
 */
@SuppressWarnings("unused")
public class CompleteMultipartUpload extends XmlEntity {
  @Key("Part")
  private List<Part> partList;


  public CompleteMultipartUpload() throws XmlPullParserException {
    this(null);
  }


  /**
   * Constucts a new CompleteMultipartUpload object with given parts.
   */
  public CompleteMultipartUpload(Part[] parts) throws XmlPullParserException {
    super();
    super.name = "CompleteMultipartUpload";
    super.namespaceDictionary.set("", "http://s3.amazonaws.com/doc/2006-03-01/");

    if (parts == null) {
      this.partList = null;
    } else {
      this.partList = Arrays.asList(parts);
    }
  }


  /**
   * Returns List of Parts of mulitpart upload.
   */
  public List<Part> partList() {
    return partList;
  }
}
