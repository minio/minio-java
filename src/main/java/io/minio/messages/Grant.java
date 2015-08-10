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

@SuppressWarnings({"SameParameterValue", "unused"})
public class Grant extends XmlEntity {
  @Key("Grantee")
  private Grantee grantee;
  @Key("Permission")
  private String permission;

  public Grant() {
    super();
    this.name = "Grant";
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public Grantee getGrantee() {
    return grantee;
  }

  public void setGrantee(Grantee grantee) {
    this.grantee = grantee;
  }
}
