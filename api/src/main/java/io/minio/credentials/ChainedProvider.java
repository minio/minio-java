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

import java.security.ProviderException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

/** Chained credential provider work with list of credential providers. */
public class ChainedProvider implements Provider {
  private final List<Provider> providers;
  private Provider currentProvider;
  private Credentials credentials;

  public ChainedProvider(@Nonnull Provider... providers) {
    this.providers = Arrays.asList(providers);
  }

  @Override
  public synchronized Credentials fetch() {
    if (credentials != null && !credentials.isExpired()) return credentials;

    if (currentProvider != null) {
      try {
        credentials = currentProvider.fetch();
        return credentials;
      } catch (ProviderException e) {
        // Ignore and fallback to iteration.
      }
    }

    for (Provider provider : providers) {
      try {
        credentials = provider.fetch();
        currentProvider = provider;
        return credentials;
      } catch (ProviderException e) {
        // Ignore and continue to next iteration.
      }
    }

    throw new ProviderException("All providers fail to fetch credentials");
  }
}
