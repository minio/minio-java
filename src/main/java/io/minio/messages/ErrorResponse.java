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


  public ErrorResponse() throws XmlPullParserException {
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
  public ErrorResponse(ErrorCode errorCode, String bucketName, String objectName, String resource, String requestId,
                       String hostId) throws XmlPullParserException {
    this();
    this.errorCode  = errorCode;
    this.code       = errorCode.code();
    this.message    = errorCode.message();
    this.bucketName = bucketName;
    this.objectName = objectName;
    this.resource   = resource;
    this.requestId  = requestId;
    this.hostId     = hostId;
  }


  /**
   * returns ErrorCode.
   */
  public ErrorCode errorCode() {
    if (this.errorCode == null) {
      this.errorCode = ErrorCode.fromString(this.code);
    }

    return this.errorCode;
  }


  /**
   * returns `message` or `errorCode.message`
   */
  public String message() {
    if (this.message != null) {
      return this.message;
    } else {
      return this.errorCode.message();
    }
  }


  public String bucketName() {
    return bucketName;
  }


  public String objectName() {
    return objectName;
  }


  public String hostId() {
    return hostId;
  }


  public String requestId() {
    return requestId;
  }


  public String resource() {
    return resource;
  }


  @Override
  public void parseXml(Reader reader) throws IOException, XmlPullParserException {
    XmlNamespaceDictionary namespaceDictionary = new XmlNamespaceDictionary();
    namespaceDictionary.set("", "");
    super.parseXml(reader, namespaceDictionary);
  }


  /**
   * returns string with field values.
   */
  public String getString() {
    return "ErrorResponse("
        + "code=" + code + ", "
        + "message=" + message + ", "
        + "bucketName=" + bucketName + ", "
        + "objectName=" + objectName + ", "
        + "resouce=" + resource + ", "
        + "requestId=" + requestId + ", "
        + "hostId=" + hostId
        + ")";
  }
}
