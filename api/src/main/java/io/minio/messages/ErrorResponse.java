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


/**
 * Helper class to parse Amazon AWS S3 error response XML.
 */
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
   * Constructs a new ErrorResponse object by reading given reader stream.
   */
  public ErrorResponse(Reader reader) throws IOException, XmlPullParserException {
    this();
    this.parseXml(reader);
  }


  /**
   * Constructs a new ErrorResponse object with error code, bucket name, object name, resource, request ID and host ID.
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
   * Returns error code.
   */
  public ErrorCode errorCode() {
    if (this.errorCode == null) {
      this.errorCode = ErrorCode.fromString(this.code);
    }

    return this.errorCode;
  }

  /**
   * Returns error code string.
   */
  public String code() {
    return this.code;
  }


  /**
   * Returns error message.
   */
  public String message() {
    return this.message;
  }


  /**
   * Returns bucket name.
   */
  public String bucketName() {
    return bucketName;
  }


  /**
   * Returns object name.
   */
  public String objectName() {
    return objectName;
  }


  /**
   * Returns host ID.
   */
  public String hostId() {
    return hostId;
  }


  /**
   * Returns request ID.
   */
  public String requestId() {
    return requestId;
  }


  /**
   * Returns resource.
   */
  public String resource() {
    return resource;
  }


  /**
   * Fills up this ErrorResponse object's fields by reading/parsing values from given Reader input stream.
   */
  @Override
  public void parseXml(Reader reader) throws IOException, XmlPullParserException {
    XmlNamespaceDictionary namespaceDictionary = new XmlNamespaceDictionary();
    namespaceDictionary.set("", "");
    super.parseXml(reader, namespaceDictionary);
  }


  /**
   * Returns string with field values.
   */
  public String getString() {
    return "ErrorResponse("
        + "code=" + code + ", "
        + "message=" + message + ", "
        + "bucketName=" + bucketName + ", "
        + "objectName=" + objectName + ", "
        + "resource=" + resource + ", "
        + "requestId=" + requestId + ", "
        + "hostId=" + hostId
        + ")";
  }
}
