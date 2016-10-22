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

import java.util.LinkedList;
import java.util.List;


/**
 * Helper class to parse Amazon AWS S3 response XML containing list of bucket information.
 */
@SuppressWarnings("WeakerAccess")
public class Buckets extends XmlEntity {
  @Key("Bucket")
  private List<Bucket> bucketList = new LinkedList<>();


  public Buckets() throws XmlPullParserException {
    super();
    super.name = "Buckets";
  }


  /**
   * Returns List of Buckets.
   */
  public List<Bucket> bucketList() {
    return bucketList;
  }
}
