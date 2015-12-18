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


  public void setContentLength(String contentLength) {
    this.contentLength = Long.parseLong(contentLength);
  }


  public long getContentLength() {
    return this.contentLength;
  }


  public void setContentType(String contentType) {
    this.contentType = contentType;
  }


  public String getContentType() {
    return this.contentType;
  }


  public void setDate(String date) {
    this.date = DateFormat.HTTP_HEADER_DATE_FORMAT.parseDateTime(date);
  }


  public Date getDate() {
    return this.date.toDate();
  }


  public void setEtag(String etag) {
    this.etag = etag.replaceAll("\"", "");
  }


  public String getEtag() {
    return this.etag;
  }


  public void setLastModified(String lastModified) {
    this.lastModified = DateFormat.HTTP_HEADER_DATE_FORMAT.parseDateTime(lastModified);
  }


  public Date getLastModified() {
    return this.lastModified.toDate();
  }


  public void setServer(String server) {
    this.server = server;
  }


  public String getServer() {
    return this.server;
  }


  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }


  public String getStatusCode() {
    return this.statusCode;
  }


  public void setTransferEncoding(String transferEncoding) {
    this.transferEncoding = transferEncoding;
  }


  public String getTransferEncoding() {
    return this.transferEncoding;
  }


  public void setXamzBucketRegion(String xamzBucketRegion) {
    this.xamzBucketRegion = xamzBucketRegion;
  }


  public String getXamzBucketRegion() {
    return this.xamzBucketRegion;
  }


  public void setXamzId2(String xamzId2) {
    this.xamzId2 = xamzId2;
  }


  public String getXamzId2() {
    return this.xamzId2;
  }


  public void setXamzRequestId(String xamzRequestId) {
    this.xamzRequestId = xamzRequestId;
  }


  public String getXamzRequestId() {
    return this.xamzRequestId;
  }
}
