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
 * Helper class to construct create bucket configuration request XML for Amazon AWS S3.
 */
public class CreateBucketConfiguration extends XmlEntity {
  @Key("LocationConstraint")
  private String locationConstraint;


  /**
   * Constructs a new CreateBucketConfiguration object with given location constraint.
   */
  public CreateBucketConfiguration(String locationConstraint) throws XmlPullParserException {
    super();
    super.name = "CreateBucketConfiguration";
    super.namespaceDictionary.set("", "http://s3.amazonaws.com/doc/2006-03-01/");

    this.locationConstraint = locationConstraint;
  }


  /**
   * Returns location constraint.
   */
  @SuppressWarnings("unused")
  public String locationConstraint() {
    return locationConstraint;
  }
}
