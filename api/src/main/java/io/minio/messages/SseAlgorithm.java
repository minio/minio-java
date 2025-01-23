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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Server-side encryption algorithm type of {@link RestoreRequest.Encryption} and {@link
 * SseConfiguration.Rule}.
 */
@Root(name = "SSEAlgorithm")
@Convert(SseAlgorithm.SseAlgorithmConverter.class)
public enum SseAlgorithm {
  AES256("AES256"),
  AWS_KMS("aws:kms");

  private final String value;

  private SseAlgorithm(String value) {
    this.value = value;
  }

  public String toString() {
    return this.value;
  }

  /** Returns SseAlgorithm of given string. */
  @JsonCreator
  public static SseAlgorithm fromString(String sseAlgorithmString) {
    for (SseAlgorithm sa : SseAlgorithm.values()) {
      if (sseAlgorithmString.equals(sa.value)) return sa;
    }
    throw new IllegalArgumentException("Unknown SSE algorithm '" + sseAlgorithmString + "'");
  }

  /** XML converter of {@link SseAlgorithm}. */
  public static class SseAlgorithmConverter implements Converter<SseAlgorithm> {
    @Override
    public SseAlgorithm read(InputNode node) throws Exception {
      return SseAlgorithm.fromString(node.getValue());
    }

    @Override
    public void write(OutputNode node, SseAlgorithm sseAlgorithm) throws Exception {
      node.setValue(sseAlgorithm.toString());
    }
  }
}
