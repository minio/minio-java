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

package io.minio.credentials;

import io.minio.Xml;
import io.minio.errors.XmlParserException;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

/**
 * Credential provider using <a
 * href="https://github.com/minio/minio/blob/master/docs/sts/ldap.md">AssumeRoleWithLDAPIdentity
 * API</a>.
 */
public class LdapIdentityProvider extends AssumeRoleBaseProvider {
  private static final RequestBody EMPTY_BODY =
      RequestBody.create(new byte[] {}, MediaType.parse("application/octet-stream"));
  private final Request request;

  public LdapIdentityProvider(
      @Nonnull String stsEndpoint,
      @Nonnull String ldapUsername,
      @Nonnull String ldapPassword,
      @Nullable OkHttpClient customHttpClient) {
    super(customHttpClient);
    stsEndpoint = Objects.requireNonNull(stsEndpoint, "STS endpoint cannot be empty");
    HttpUrl url = Objects.requireNonNull(HttpUrl.parse(stsEndpoint), "Invalid STS endpoint");
    Objects.requireNonNull(ldapUsername, "LDAP username must not be null");
    if (ldapUsername.isEmpty()) {
      throw new IllegalArgumentException("LDAP username must not be empty");
    }
    Objects.requireNonNull(ldapPassword, "LDAP password must not be null");

    HttpUrl.Builder urlBuilder =
        newUrlBuilder(url, "AssumeRoleWithLDAPIdentity", 0, null, null, null);
    url =
        urlBuilder
            .addQueryParameter("LDAPUsername", ldapUsername)
            .addQueryParameter("LDAPPassword", ldapPassword)
            .build();
    this.request = new Request.Builder().url(url).method("POST", EMPTY_BODY).build();
  }

  @Override
  protected Request getRequest() {
    return this.request;
  }

  @Override
  protected Credentials parseResponse(Response response) throws XmlParserException, IOException {
    AssumeRoleWithLdapIdentityResponse result =
        Xml.unmarshal(AssumeRoleWithLdapIdentityResponse.class, response.body().charStream());
    return result.credentials();
  }

  /** Object representation of response XML of AssumeRoleWithLDAPIdentity API. */
  @Root(name = "AssumeRoleWithLDAPIdentityResponse", strict = false)
  @Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
  public static class AssumeRoleWithLdapIdentityResponse {
    @Path(value = "AssumeRoleWithLDAPIdentityResult")
    @Element(name = "Credentials")
    private Credentials credentials;

    public Credentials credentials() {
      return credentials;
    }
  }
}
