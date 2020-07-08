/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.minio.messages;

import javax.annotation.Nullable;

/**
 * Base class for STS Requests. There are four types of sts requests:
 *
 * <ul>
 *   <li>1. Client grants (with specific params: DurationSeconds, Token, Policy)
 *   <li>2. Web identity (with specific params: DurationSeconds, WebIdentityToken, Policy)
 *   <li>3. Assume Role (with specific params: DurationSeconds, AUTHPARAMS, Policy)
 *   <li>4. Ad/Ldap (with specific params: LDAPUsername, LDAPPassword, Policy)
 * </ul>
 *
 * other parameters (like Version or Action) is static and handled by concrete {@link
 * io.minio.credentials.Provider}.
 */
public class StsRequestToken {

  private final String policy;

  public StsRequestToken(@Nullable String policy) {
    this.policy = policy;
  }

  public String policy() {
    return policy;
  }
}
