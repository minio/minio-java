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

package io.minio;

import java.util.Date;
import org.joda.time.DateTime;

import io.minio.http.Header;


/**
 * HTTP response header class.
 */
public class ResponseHeader {
  @Header("Content-Length")
  private long contentLength;
  @Header("Content-Type")
  private String contentType;
  @Header("Date")
  private DateTime date;
  @Header("ETag")
  private String etag;
  @Header("Last-Modified")
  private DateTime lastModified;
  @Header("Server")
  private String server;
  @Header("Status Code")
  private String statusCode;
  @Header("Transfer-Encoding")
  private String transferEncoding;
  @Header("x-amz-bucket-region")
  private String xamzBucketRegion;
  @Header("x-amz-id-2")
  private String xamzId2;
  @Header("x-amz-request-id")
  private String xamzRequestId;


  /**
   * Sets content length.
   */
  public void setContentLength(String contentLength) {
    this.contentLength = Long.parseLong(contentLength);
  }


  /**
   * Returns content length.
   */
  public long contentLength() {
    return this.contentLength;
  }


  /**
   * Sets content type.
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }


  /**
   * Returns content type.
   */
  public String contentType() {
    return this.contentType;
  }


  /**
   * Sets date.
   */
  public void setDate(String date) {
    this.date = DateFormat.HTTP_HEADER_DATE_FORMAT.parseDateTime(date);
  }


  /**
   * Returns date.
   */
  public Date date() {
    return this.date.toDate();
  }


  /**
   * Sets ETag.
   */
  public void setEtag(String etag) {
    this.etag = etag.replaceAll("\"", "");
  }


  /**
   * Returns ETag.
   */
  public String etag() {
    return this.etag;
  }


  /**
   * Sets last modified time.
   */
  public void setLastModified(String lastModified) {
    this.lastModified = DateFormat.HTTP_HEADER_DATE_FORMAT.parseDateTime(lastModified);
  }


  /**
   * Returns last modified time.
   */
  public Date lastModified() {
    return this.lastModified.toDate();
  }


  /**
   * Sets server name.
   */
  public void setServer(String server) {
    this.server = server;
  }


  /**
   * Returns server name.
   */
  public String server() {
    return this.server;
  }


  /**
   * Sets status code.
   */
  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }


  /**
   * Returns status code.
   */
  public String statusCode() {
    return this.statusCode;
  }


  /**
   * Sets transfer encoding.
   */
  public void setTransferEncoding(String transferEncoding) {
    this.transferEncoding = transferEncoding;
  }


  /**
   * Returns transfer encoding.
   */
  public String transferEncoding() {
    return this.transferEncoding;
  }


  /**
   * Sets Amazon bucket region.
   */
  public void setXamzBucketRegion(String xamzBucketRegion) {
    this.xamzBucketRegion = xamzBucketRegion;
  }


  /**
   * Returns Amazon bucket region.
   */
  public String xamzBucketRegion() {
    return this.xamzBucketRegion;
  }


  /**
   * Sets Amazon ID2.
   */
  public void setXamzId2(String xamzId2) {
    this.xamzId2 = xamzId2;
  }


  /**
   * Returns Amazon ID2.
   */
  public String xamzId2() {
    return this.xamzId2;
  }


  /**
   * Sets Amazon request ID.
   */
  public void setXamzRequestId(String xamzRequestId) {
    this.xamzRequestId = xamzRequestId;
  }


  /**
   * Returns Amazon request ID.
   */
  public String xamzRequestId() {
    return this.xamzRequestId;
  }
}
