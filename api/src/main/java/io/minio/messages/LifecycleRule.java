/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/** Helper class to denote Rule information for {@link LifecycleConfiguration}. */
@Root(name = "Rule")
public class LifecycleRule {
  @Element(name = "AbortIncompleteMultipartUpload", required = false)
  private AbortIncompleteMultipartUpload abortIncompleteMultipartUpload;

  @Element(name = "Expiration", required = false)
  private Expiration expiration;

  @Element(name = "Filter", required = false)
  private RuleFilter filter;

  @Element(name = "ID", required = false)
  private String id;

  @Element(name = "NoncurrentVersionExpiration", required = false)
  private NoncurrentVersionExpiration noncurrentVersionExpiration;

  @Element(name = "NoncurrentVersionTransition", required = false)
  private NoncurrentVersionTransition noncurrentVersionTransition;

  @Element(name = "Status")
  private Status status;

  @Element(name = "Transition", required = false)
  private Transition transition;

  /** Constructs new server-side encryption configuration rule. */
  public LifecycleRule(
      @Nonnull @Element(name = "Status") Status status,
      @Nullable @Element(name = "AbortIncompleteMultipartUpload", required = false)
          AbortIncompleteMultipartUpload abortIncompleteMultipartUpload,
      @Nullable @Element(name = "Expiration", required = false) Expiration expiration,
      @Nonnull @Element(name = "Filter", required = false) RuleFilter filter,
      @Nullable @Element(name = "ID", required = false) String id,
      @Nullable @Element(name = "NoncurrentVersionExpiration", required = false)
          NoncurrentVersionExpiration noncurrentVersionExpiration,
      @Nullable @Element(name = "NoncurrentVersionTransition", required = false)
          NoncurrentVersionTransition noncurrentVersionTransition,
      @Nullable @Element(name = "Transition", required = false) Transition transition) {
    if (abortIncompleteMultipartUpload == null
        && expiration == null
        && noncurrentVersionExpiration == null
        && noncurrentVersionTransition == null
        && transition == null) {
      throw new IllegalArgumentException(
          "At least one of action (AbortIncompleteMultipartUpload, Expiration, "
              + "NoncurrentVersionExpiration, NoncurrentVersionTransition or Transition) must be "
              + "specified in a rule");
    }

    if (id != null) {
      id = id.trim();
      if (id.isEmpty()) throw new IllegalArgumentException("ID must be non-empty string");
      if (id.length() > 255) throw new IllegalArgumentException("ID must be exceed 255 characters");
    }

    this.abortIncompleteMultipartUpload = abortIncompleteMultipartUpload;
    this.expiration = expiration;
    this.filter = Objects.requireNonNull(filter, "Filter must not be null");
    this.id = id;
    this.noncurrentVersionExpiration = noncurrentVersionExpiration;
    this.noncurrentVersionTransition = noncurrentVersionTransition;
    this.status = Objects.requireNonNull(status, "Status must not be null");
    this.transition = transition;
  }

  public AbortIncompleteMultipartUpload abortIncompleteMultipartUpload() {
    return abortIncompleteMultipartUpload;
  }

  public Expiration expiration() {
    return expiration;
  }

  public RuleFilter filter() {
    return this.filter;
  }

  public String id() {
    return this.id;
  }

  public NoncurrentVersionExpiration noncurrentVersionExpiration() {
    return noncurrentVersionExpiration;
  }

  public NoncurrentVersionTransition noncurrentVersionTransition() {
    return noncurrentVersionTransition;
  }

  public Status status() {
    return this.status;
  }

  public Transition transition() {
    return transition;
  }
}
