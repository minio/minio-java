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

import java.util.Date;
import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;
import io.minio.DateFormat;


/**
 * Helper class to parse Amazon AWS S3 response XML containing object item information.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class Item extends XmlEntity {
  @Key("Key")
  private String objectName;
  @Key("LastModified")
  private String lastModified;
  @Key("ETag")
  private String etag;
  @Key("Size")
  private long size;
  @Key("StorageClass")
  private String storageClass;
  @Key("Owner")
  private Owner owner;
  private boolean isDir = false;


  public Item() throws XmlPullParserException {
    this(null, false);
  }


  /**
   * Constructs a new Item object with given object name and IsDir flag.
   */
  public Item(String objectName, boolean isDir) throws XmlPullParserException {
    super();
    this.name = "Item";

    this.objectName = objectName;
    this.isDir = isDir;
  }


  /**
   * Returns object name.
   */
  public String objectName() {
    return objectName;
  }


  /**
   * Returns last modified time of the object.
   */
  public Date lastModified() {
    return DateFormat.RESPONSE_DATE_FORMAT.parseDateTime(lastModified).toDate();
  }


  /**
   * Returns ETag of the object.
   */
  public String etag() {
    return etag;
  }


  /**
   * Returns object size.
   */
  public long objectSize() {
    return size;
  }


  /**
   * Returns storage class of the object.
   */
  public String storageClass() {
    return storageClass;
  }


  /**
   * Returns owner object of given the object.
   */
  public Owner owner() {
    return owner;
  }


  /**
   * Returns whether the object is a directory or not.
   */
  public boolean isDir() {
    return isDir;
  }
}
