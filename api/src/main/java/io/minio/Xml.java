/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2020 MinIO, Inc.
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

package io.minio;

import io.minio.errors.XmlParserException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

/** XML marshaller and unmarshaller. */
public class Xml {
  /**
   * This marshal method will traverse the provided object checking for field annotations in order
   * to compose the XML data.
   */
  public static String marshal(Object source) throws XmlParserException {
    try {
      Serializer serializer = new Persister(new AnnotationStrategy(), new Format(0));
      StringWriter writer = new StringWriter();
      serializer.write(source, writer);
      return writer.toString();
    } catch (Exception e) {
      throw new XmlParserException(e);
    }
  }

  /**
   * This unmarshal method will read the contents of the XML document from the provided source and
   * populate the object with the values deserialized.
   */
  public static <T> T unmarshal(Class<? extends T> type, Reader source) throws XmlParserException {
    try {
      Serializer serializer = new Persister(new AnnotationStrategy());
      return serializer.read(type, source);
    } catch (Exception e) {
      throw new XmlParserException(e);
    }
  }

  /**
   * This unmarshal method will read the contents of the XML document from the provided source and
   * populate the object with the values deserialized.
   */
  public static <T> T unmarshal(Class<? extends T> type, String source) throws XmlParserException {
    try {
      Serializer serializer = new Persister(new AnnotationStrategy());
      return serializer.read(type, new StringReader(source));
    } catch (Exception e) {
      throw new XmlParserException(e);
    }
  }

  /**
   * This validate method will validate the contents of the XML document against the specified XML
   * class schema.
   */
  public static boolean validate(Class type, String source) throws XmlParserException {
    try {
      Serializer serializer = new Persister(new AnnotationStrategy());
      return serializer.validate(type, source);
    } catch (Exception e) {
      throw new XmlParserException(e);
    }
  }
}
