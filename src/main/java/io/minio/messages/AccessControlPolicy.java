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

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"SameParameterValue", "unused"})
public class AccessControlPolicy extends XmlEntity {
  @Key("Owner")
  private Owner owner;
  @Key("AccessControlList")
  private AccessControlList grants;

  public AccessControlPolicy() {
    super();
    this.name = "AccessControlPolicy";
  }

  public Owner getOwner() {
    return owner;
  }

  public void setOwner(Owner owner) {
    this.owner = owner;
  }

  /**
   * get access control list.
   */
  public List<Grant> getAccessControlList() {
    if (grants == null) {
      return new LinkedList<Grant>();
    }
    return grants.getGrant();
  }

  public void setAccessControlList(AccessControlList grants) {
    this.grants = grants;
  }

}
