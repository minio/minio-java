/*
  * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2018 Minio, Inc.
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

import java.util.Map;

/**
* PUT object options for parameterizing a PUT request.
*/
public class PutOptions extends Options {

  String contentType;

/**
* PutOptions default constructor.
*/
  public PutOptions() {}

/**
* Create a new PutOptions object from the provided options.
* @param options The PUT objects. 
*/
  public PutOptions(PutOptions options) {
    super(options);
    this.contentType = options.contentType;
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> headers = super.getHeaders();
    if (contentType != null) {
      headers.put("Content-Type", this.contentType);
    }
    return headers;
  }

  @Override
  public PutOptions setEncryption(ServerSideEncryption encryption)  {
    super.setEncryption(encryption);
    return this;
  }

  public String getContentType() {
    return this.contentType;
  }

  public PutOptions setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  @Override
  public PutOptions setMetadata(String key, String value) {
    super.setMetadata(key, value);
    return this;
  }

  @Override
  public PutOptions setMetadata(Map<String, String> metadata) {
    super.setMetadata(metadata);
    return this;
  }
}