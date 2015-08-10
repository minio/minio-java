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

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class AccessControlList extends XmlEntity {
  @Key("Grant")
  private List<Grant> grant;

  @SuppressWarnings("unused")
  public AccessControlList() {
    super();
    super.name = "AccessControlList";
  }

  public List<Grant> getGrant() {
    return grant;
  }

  public void setGrant(List<Grant> grant) {
    this.grant = grant;
  }
}
