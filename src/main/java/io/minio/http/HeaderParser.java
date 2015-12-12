/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

package io.minio.http;

import java.lang.Class;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.Annotation;
import java.util.Hashtable;
import java.util.Map;

import com.squareup.okhttp.Headers;


public class HeaderParser {
  /**
   * method to set destination from header map.
   */
  public static void set(Map<String,String> headerMap, Object destination) {
    Field[] publicFields;
    Field[] privateFields;
    Field[] fields;

    Class<?> cls = destination.getClass();
    publicFields = cls.getFields();
    privateFields = cls.getDeclaredFields();
    fields = new Field[publicFields.length + privateFields.length];
    System.arraycopy(publicFields, 0, fields, 0, publicFields.length);
    System.arraycopy(privateFields, 0, fields, publicFields.length, privateFields.length);

    for (Field field : fields) {
      Annotation annotation = field.getAnnotation(Header.class);
      if (annotation == null) {
        continue;
      }

      Header httpHeader = (Header) annotation;
      String value = httpHeader.value();
      String setter = httpHeader.setter();
      if (setter.isEmpty()) {
        // assume setter name as 'setFieldName'
        String name = field.getName();
        setter = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
      }

      try {
        Method setterMethod = cls.getMethod(setter, new Class[]{String.class});
        setterMethod.invoke(destination, headerMap.get(value));
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
               | IllegalArgumentException e) {
        // ignore these exceptions
        System.out.println(e);
      }
    }
  }


  /**
   * method to set destination from Headers object.
   */
  public static void set(Headers headers, Object destination) {
    Field[] publicFields;
    Field[] privateFields;
    Field[] fields;

    Class<?> cls = destination.getClass();
    publicFields = cls.getFields();
    privateFields = cls.getDeclaredFields();
    fields = new Field[publicFields.length + privateFields.length];
    System.arraycopy(publicFields, 0, fields, 0, publicFields.length);
    System.arraycopy(privateFields, 0, fields, publicFields.length, privateFields.length);

    for (Field field : fields) {
      Annotation annotation = field.getAnnotation(Header.class);
      if (annotation == null) {
        continue;
      }

      Header httpHeader = (Header) annotation;
      String value = httpHeader.value();
      String setter = httpHeader.setter();
      if (setter.isEmpty()) {
        // assume setter name as 'setFieldName'
        String name = field.getName();
        setter = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
      }

      try {
        Method setterMethod = cls.getMethod(setter, new Class[]{String.class});
        setterMethod.invoke(destination, headers.get(value));
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
               | IllegalArgumentException e) {
        // ignore these exceptions
        System.out.println(e);
      }
    }
  }


  /**
   * parse list of header strings and set to destination.
   */
  public static void parse(String[] headerStrings, Object destination) {
    Map<String,String> headerMap = new Hashtable<String,String>();
    for (String s : headerStrings) {
      String[] tokens = s.split(": ");
      headerMap.put(tokens[0], tokens[1]);
    }

    set(headerMap, destination);
  }
}
