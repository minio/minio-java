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

import java.io.IOException;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParserException;


/**
 * Helper class to parse Amazon AWS S3 error response XML.
 */
@SuppressWarnings("unused")
public class DeleteError extends ErrorResponse {
  /**
   * Constructs a new ErrorResponse object by reading given reader stream.
   */
  public DeleteError() throws XmlPullParserException {
    super();
    super.name = "Error";
  }


  /**
   * Constructs a new ErrorResponse object by reading given reader stream.
   */
  public DeleteError(Reader reader) throws IOException, XmlPullParserException {
    this();
    this.parseXml(reader);
  }
}
