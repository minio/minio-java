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
 * Helper class to parse Amazon AWS S3 response XML containing ListAllMyBucketsResult information.
 */
public class ListAllMyBucketsResult extends XmlEntity {
  @Key("Owner")
  private Owner owner;
  @Key("Buckets")
  private Buckets buckets;


  public ListAllMyBucketsResult() throws XmlPullParserException {
    super();
    this.name = "ListAllMyBucketsResult";
  }


  /**
   * Returns owner.
   */
  @SuppressWarnings("unused")
  public Owner owner() {
    return owner;
  }


  /**
   * Returns List of buckets.
   */
  public List<Bucket> buckets() {
    if (buckets == null) {
      return new LinkedList<>();
    }

    return buckets.bucketList();
  }
}
