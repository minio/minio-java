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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/** XML friendly map denotes metadata. */
@Root(name = "Metadata")
@Convert(Metadata.MetadataConverter.class)
public class Metadata {
  Map<String, String> map;

  public Metadata() {}

  public Metadata(Map<String, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public Map<String, String> get() {
    return map;
  }

  /** XML converter class. */
  public static class MetadataConverter implements Converter<Metadata> {
    @Override
    public Metadata read(InputNode node) throws Exception {
      Map<String, String> map = new HashMap<>();
      while (true) {
        InputNode childNode = node.getNext();
        if (childNode == null) {
          break;
        }

        map.put(childNode.getName(), childNode.getValue());
      }

      if (map.size() > 0) {
        return new Metadata(map);
      }

      return null;
    }

    @Override
    public void write(OutputNode node, Metadata metadata) throws Exception {
      for (Map.Entry<String, String> entry : metadata.get().entrySet()) {
        OutputNode childNode = node.getChild(entry.getKey());
        childNode.setValue(entry.getValue());
      }

      node.commit();
    }
  }
}
