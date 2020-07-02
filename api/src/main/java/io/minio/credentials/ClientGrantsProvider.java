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
import io.minio.errors.InvalidResponseException;
import io.minio.errors.XmlParserException;
import io.minio.messages.AssumeRoleWithClientGrantsResponse;
import io.minio.messages.ClientGrantsToken;
import io.minio.messages.Credentials;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("unused")
public class ClientGrantsProvider extends StsProvider {

  private Credentials credentials;
  private final Supplier<ClientGrantsToken> tokenProducer;

  public ClientGrantsProvider(
      @Nonnull String stsEndpoint, @Nonnull Supplier<ClientGrantsToken> tokenProducer) {
    super(stsEndpoint);
    this.tokenProducer = Objects.requireNonNull(tokenProducer, "Token producer must not be null");
  }

  /**
   * Returns a pointer to a new, temporary credentials, obtained via STS assume role with client
   * grants api.
   *
   * @return temporary credentials to access minio api.
   */
  @Override
  public Credentials fetch() {
    if (credentials != null && !isExpired(credentials)) {
      return credentials;
    }
    synchronized (this) {
      if (credentials == null || isExpired(credentials)) {
        try (Response response = callSecurityTokenService()) {
          final ResponseBody body = response.body();
          if (body == null) {
            // should not happen
            throw new IllegalStateException("Received empty response");
          }
          credentials =
              Xml.unmarshal(AssumeRoleWithClientGrantsResponse.class, body.charStream())
                  .credentials();
        } catch (XmlParserException | IOException | InvalidResponseException e) {
          throw new IllegalStateException("Failed to process STS call", e);
        }
      }
    }
    return credentials;
  }

  @Override
  protected Map<String, String> queryParams() {
    final ClientGrantsToken grantsToken = tokenProducer.get();
    final Map<String, String> queryParamenters = new HashMap<>();
    queryParamenters.put("Action", "AssumeRoleWithClientGrants");
    queryParamenters.put("DurationSeconds", tokenDuration(grantsToken.expiredAfter()));
    queryParamenters.put("Token", grantsToken.token());
    queryParamenters.put("Version", "2011-06-15");
    if (grantsToken.policy() != null) {
      queryParamenters.put("Policy", grantsToken.policy());
    }
    return queryParamenters;
  }
}
