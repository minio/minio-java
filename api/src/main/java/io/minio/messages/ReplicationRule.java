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
import org.simpleframework.xml.convert.Convert;

/** Helper class to denote Rule information for {@link ReplicationConfiguration}. */
@Root(name = "Rule")
public class ReplicationRule {
  @Element(name = "DeleteMarkerReplication", required = false)
  private DeleteMarkerReplication deleteMarkerReplication;

  @Element(name = "Destination")
  private ReplicationDestination destination;

  @Element(name = "ExistingObjectReplication", required = false)
  private ExistingObjectReplication existingObjectReplication;

  @Element(name = "Filter", required = false)
  private RuleFilter filter;

  @Element(name = "ID", required = false)
  private String id;

  @Element(name = "Prefix", required = false)
  @Convert(PrefixConverter.class)
  private String prefix;

  @Element(name = "Priority", required = false)
  private Integer priority;

  @Element(name = "SourceSelectionCriteria", required = false)
  private SourceSelectionCriteria sourceSelectionCriteria;

  @Element(name = "DeleteReplication", required = false)
  private DeleteReplication deleteReplication; // This is MinIO specific extension.

  @Element(name = "Status")
  private Status status;

  /** Constructs new server-side encryption configuration rule. */
  public ReplicationRule(
      @Nullable @Element(name = "DeleteMarkerReplication", required = false)
          DeleteMarkerReplication deleteMarkerReplication,
      @Nonnull @Element(name = "Destination") ReplicationDestination destination,
      @Nullable @Element(name = "ExistingObjectReplication", required = false)
          ExistingObjectReplication existingObjectReplication,
      @Nullable @Element(name = "Filter", required = false) RuleFilter filter,
      @Nullable @Element(name = "ID", required = false) String id,
      @Nullable @Element(name = "Prefix", required = false) String prefix,
      @Nullable @Element(name = "Priority", required = false) Integer priority,
      @Nullable @Element(name = "SourceSelectionCriteria", required = false)
          SourceSelectionCriteria sourceSelectionCriteria,
      @Nullable @Element(name = "DeleteReplication", required = false)
          DeleteReplication deleteReplication,
      @Nonnull @Element(name = "Status") Status status) {

    if (filter != null && deleteMarkerReplication == null) {
      deleteMarkerReplication = new DeleteMarkerReplication(null);
    }

    if (id != null) {
      id = id.trim();
      if (id.isEmpty()) throw new IllegalArgumentException("ID must be non-empty string");
      if (id.length() > 255) throw new IllegalArgumentException("ID must be exceed 255 characters");
    }

    this.deleteMarkerReplication = deleteMarkerReplication;
    this.destination = Objects.requireNonNull(destination, "Destination must not be null");
    this.existingObjectReplication = existingObjectReplication;
    this.filter = filter;
    this.id = id;
    this.prefix = prefix;
    this.priority = priority;
    this.sourceSelectionCriteria = sourceSelectionCriteria;
    this.deleteReplication = deleteReplication;
    this.status = Objects.requireNonNull(status, "Status must not be null");
  }

  /** Constructs new server-side encryption configuration rule. */
  public ReplicationRule(
      @Nullable @Element(name = "DeleteMarkerReplication", required = false)
          DeleteMarkerReplication deleteMarkerReplication,
      @Nonnull @Element(name = "Destination") ReplicationDestination destination,
      @Nullable @Element(name = "ExistingObjectReplication", required = false)
          ExistingObjectReplication existingObjectReplication,
      @Nullable @Element(name = "Filter", required = false) RuleFilter filter,
      @Nullable @Element(name = "ID", required = false) String id,
      @Nullable @Element(name = "Prefix", required = false) String prefix,
      @Nullable @Element(name = "Priority", required = false) Integer priority,
      @Nullable @Element(name = "SourceSelectionCriteria", required = false)
          SourceSelectionCriteria sourceSelectionCriteria,
      @Nonnull @Element(name = "Status") Status status) {
    this(
        deleteMarkerReplication,
        destination,
        existingObjectReplication,
        filter,
        id,
        prefix,
        priority,
        sourceSelectionCriteria,
        null,
        status);
  }

  public DeleteMarkerReplication deleteMarkerReplication() {
    return this.deleteMarkerReplication;
  }

  public ReplicationDestination destination() {
    return this.destination;
  }

  public ExistingObjectReplication existingObjectReplication() {
    return this.existingObjectReplication;
  }

  public RuleFilter filter() {
    return this.filter;
  }

  public String id() {
    return this.id;
  }

  public String prefix() {
    return this.prefix;
  }

  public Integer priority() {
    return this.priority;
  }

  public SourceSelectionCriteria sourceSelectionCriteria() {
    return this.sourceSelectionCriteria;
  }

  public DeleteReplication deleteReplication() {
    return this.deleteReplication;
  }

  public Status status() {
    return this.status;
  }
}
