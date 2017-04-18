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

import io.minio.errors.InvalidArgumentException;


/**
 * Helper class to parse Amazon AWS S3 response XML containing Filter.
 */
@SuppressWarnings("WeakerAccess")
public class Filter extends XmlEntity {
  @Key("S3Key")
  private S3Key s3Key = new S3Key();


  public Filter() throws XmlPullParserException {
    super();
    super.name = "Filter";
  }


  /**
   * Returns S3 Key.
   */
  public S3Key s3Key() {
    return s3Key;
  }


  /**
   * Sets S3 Key.
   */
  public void setS3Key(S3Key s3Key) {
    this.s3Key = s3Key;
  }


  public void setPrefixRule(String value) throws InvalidArgumentException, XmlPullParserException {
    s3Key.setPrefixRule(value);
  }


  public void setSuffixRule(String value) throws InvalidArgumentException, XmlPullParserException {
    s3Key.setSuffixRule(value);
  }
}
