/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
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

import javax.annotation.Nullable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of request XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_RestoreObject.html">RestoreObject
 * API</a>.
 */
@Root(name = "RestoreRequest")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class RestoreRequest {
  @Element(name = "Days", required = false)
  private Integer days;

  @Element(name = "GlacierJobParameters", required = false)
  private GlacierJobParameters glacierJobParameters;

  @Element(name = "Type", required = false)
  private String type;

  @Element(name = "Tier", required = false)
  private Tier tier;

  @Element(name = "Description", required = false)
  private String description;

  @Element(name = "SelectParameters", required = false)
  private SelectParameters selectParameters;

  @Element(name = "OutputLocation", required = false)
  private OutputLocation outputLocation;

  /** Constructs new RestoreRequest object for given parameters. */
  public RestoreRequest(
      @Nullable Integer days,
      @Nullable GlacierJobParameters glacierJobParameters,
      @Nullable Tier tier,
      @Nullable String description,
      @Nullable SelectParameters selectParameters,
      @Nullable OutputLocation outputLocation) {
    this.days = days;
    this.glacierJobParameters = glacierJobParameters;
    if (selectParameters != null) this.type = "SELECT";
    this.tier = tier;
    this.description = description;
    this.selectParameters = selectParameters;
    this.outputLocation = outputLocation;
  }
}
