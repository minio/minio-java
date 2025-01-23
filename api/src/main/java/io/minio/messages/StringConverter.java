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

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * XML converter for string due to SimpleXML limitation in converting empty element like
 * <Element></Element> to empty string.
 */
public class StringConverter implements Converter<String> {
  @Override
  public String read(InputNode node) throws Exception {
    String value = node.getValue();
    return value != null ? value : "";
  }

  @Override
  public void write(OutputNode node, String value) throws Exception {
    node.setValue(value);
  }
}
