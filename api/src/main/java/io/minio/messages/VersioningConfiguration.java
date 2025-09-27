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

import io.minio.Utils;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Request XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketVersioning.html">PutBucketVersioning
 * API</a> and response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html">GetBucketVersioning
 * API</a>.
 */
@Root(name = "VersioningConfiguration", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class VersioningConfiguration {
  @Element(name = "Status", required = false)
  private Status status;

  @Element(name = "MfaDelete", required = false)
  private Status mfaDelete;

  @ElementList(name = "ExcludedPrefixes", inline = true, required = false)
  private List<Prefix> excludedPrefixes;

  @Element(name = "ExcludeFolders", required = false)
  private Boolean excludeFolders;

  public VersioningConfiguration() {}

  /** Constructs a new VersioningConfiguration object with given parameters. */
  public VersioningConfiguration(
      @Nonnull Status status,
      @Nullable Status mfaDelete,
      @Nullable List<Prefix> excludedPrefixes,
      @Nullable Boolean excludeFolders) {
    Objects.requireNonNull(status, "Status must not be null");
    if (status == Status.OFF) {
      throw new IllegalArgumentException("Status must be ENABLED or SUSPENDED");
    }
    if (mfaDelete == Status.OFF) {
      throw new IllegalArgumentException("Status must be ENABLED or SUSPENDED");
    }

    this.status = status;
    this.mfaDelete = mfaDelete;
    this.excludedPrefixes = excludedPrefixes;
    this.excludeFolders = excludeFolders;
  }

  public Status status() {
    return status == null ? Status.OFF : status;
  }

  public Status mfaDelete() {
    return mfaDelete;
  }

  public Boolean isMfaDeleteEnabled() {
    return mfaDelete == Status.ENABLED;
  }

  public List<Prefix> excludedPrefixes() {
    return excludedPrefixes;
  }

  public Boolean excludeFolders() {
    return excludeFolders;
  }

  @Override
  public String toString() {
    return String.format(
        "VersioningConfiguration{status=%s, mfaDelete=%s, excludedPrefixes=%s, excludeFolders=%s}",
        Utils.stringify(status),
        Utils.stringify(mfaDelete),
        Utils.stringify(excludedPrefixes),
        Utils.stringify(excludeFolders));
  }

  @Root(name = "Status")
  @Convert(Status.StatusConverter.class)
  public static enum Status {
    OFF(""),
    ENABLED("Enabled"),
    SUSPENDED("Suspended");

    private final String value;

    private Status(String value) {
      this.value = value;
    }

    public String toString() {
      return this.value;
    }

    public static Status fromString(String statusString) {
      if ("Enabled".equals(statusString)) {
        return ENABLED;
      }

      if ("Suspended".equals(statusString)) {
        return SUSPENDED;
      }

      return OFF;
    }

    /** XML converter of {@link Status}. */
    public static class StatusConverter implements Converter<Status> {
      @Override
      public Status read(InputNode node) throws Exception {
        return Status.fromString(node.getValue());
      }

      @Override
      public void write(OutputNode node, Status status) throws Exception {
        node.setValue(status.toString());
      }
    }
  }

  @Root(name = "ExcludedPrefixes")
  public static class Prefix {
    @Element(name = "Prefix")
    private String prefix;

    public Prefix(@Nonnull @Element(name = "Prefix") String prefix) {
      this.prefix = Objects.requireNonNull(prefix, "prefix must not be null");
    }

    public String get() {
      return prefix;
    }

    @Override
    public String toString() {
      return String.format("Prefix{%s}", Utils.stringify(prefix));
    }
  }
}
