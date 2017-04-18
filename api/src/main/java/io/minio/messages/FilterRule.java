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
 * Helper class to parse Amazon AWS S3 response XML containing filter rule.
 */
public class FilterRule extends XmlEntity {
  @Key("Name")
  private String name;
  @Key("Value")
  private String value;


  public FilterRule() throws XmlPullParserException {
    super();
    super.name = "FilterRule";
  }


  /**
   * Returns filter name.
   */
  public String name() {
    return name;
  }


  /**
   * Sets filter name.
   */
  public void setName(String name) {
    this.name = name;
  }


  /**
   * Returns filter value.
   */
  public String value() {
    return value;
  }


  /**
   * Sets filter value.
   */
  public void setValue(String value) {
    this.value = value;
  }
}
