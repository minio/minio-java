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

import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;

import java.io.Reader;
import java.io.IOException;

import io.minio.ErrorCode;


@SuppressWarnings("unused")
public class ErrorResponse extends XmlEntity {
  @Key("Code")
  private String code;
  @Key("Message")
  private String message;
  @Key("BucketName")
  private String bucketName;
  @Key("Key")
  private String objectName;
  @Key("Resource")
  private String resource;
  @Key("RequestId")
  private String requestId;
  @Key("HostId")
  private String hostId;

  private ErrorCode errorCode;


  public ErrorResponse() {
    super();
    super.name = "ErrorResponse";
  }


  /**
   * constructor.
   */
  public ErrorResponse(Reader reader) throws IOException, XmlPullParserException {
    this();
    this.parseXml(reader);
  }


  /**
   * constructor.
   */
  public ErrorResponse(String code, String message, String bucketName, String objectName, String resource,
                       String requestId, String hostId) {
    this();
    this.code       = code;
    this.message    = message;
    this.bucketName = bucketName;
    this.objectName = objectName;
    this.resource   = resource;
    this.requestId  = requestId;
    this.hostId     = hostId;
  }


  public String getCode() {
    return code;
  }


  /**
   * set code.
   */
  public void setCode(String code) {
    this.code = code;
    if (this.errorCode != null) {
      return;
    }
    this.errorCode = ErrorCode.fromString(code);
  }


  public String getMessage() {
    return message;
  }


  public void setMessage(String message) {
    this.message = message;
  }


  public String getBucketName() {
    return bucketName;
  }


  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }


  public String getObjectName() {
    return objectName;
  }


  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }


  // must to be there for XML parsing
  public void setKey(String objectName) {
    this.setObjectName(objectName);
  }


  public String getResource() {
    return resource;
  }


  public void setResource(String resource) {
    this.resource = resource;
  }


  public String getRequestId() {
    return requestId;
  }


  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }


  public String getHostId() {
    return hostId;
  }


  public void setHostId(String hostId) {
    this.hostId = hostId;
  }


  @Override
  public void parseXml(Reader reader) throws IOException, XmlPullParserException {
    XmlNamespaceDictionary namespaceDictionary = new XmlNamespaceDictionary();
    namespaceDictionary.set("", "");
    super.parseXml(reader, namespaceDictionary);
  }
}
