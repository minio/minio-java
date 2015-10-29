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

package io.minio;

import io.minio.errors.*;

import com.google.common.io.BaseEncoding;
import com.google.common.base.Strings;
import com.google.common.base.Joiner;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class PostPolicy {
  private DateTimeFormatter expirationDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH':'mm':'ss'.'SSS'Z'").withZoneUTC();
  private DateTimeFormatter dateFormatyyyyMMddThhmmssZ = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'").withZoneUTC();

  private String expiration;
  private ArrayList<String[]> conditions;
  private Map<String, String> formData;

  public PostPolicy() {
    conditions = new ArrayList<String[]>();
    formData = new HashMap<String, String>();
  }

  public void setExpires(DateTime date) throws InvalidArgumentException {
    if (date == null) {
      throw new InvalidArgumentException();
    }
    expiration = date.toString(expirationDateFormat);
  }

  public void setKey(String key) throws InvalidArgumentException {
    if (Strings.isNullOrEmpty(key)) {
      throw new InvalidArgumentException();
    }
    conditions.add(new String[]{"eq","$key",key});
    formData.put("key", key);
  }

  public void setKeyStartsWith(String prefix) throws InvalidArgumentException {
    if (Strings.isNullOrEmpty(prefix)) {
      throw new InvalidArgumentException();
    }
    conditions.add(new String[]{"starts-with","$key",prefix});
    formData.put("key", prefix);
  }

  public void setBucket(String bucket) throws InvalidArgumentException {
    if (Strings.isNullOrEmpty(bucket)) {
      throw new InvalidArgumentException();
    }
    conditions.add(new String[]{"eq","$bucket",bucket});
    formData.put("bucket", bucket);
  }

  public void setContentType(String type) throws InvalidArgumentException {
    if (Strings.isNullOrEmpty(type)) {
      throw new InvalidArgumentException();
    }
    conditions.add(new String[]{"eq","$Content-Type",type});
    formData.put("Content-Type", type);
  }

  public void setAlgorithm(String algorithm) {
    conditions.add(new String[]{"eq","$x-amz-algorithm",algorithm});
    formData.put("x-amz-algorithm", algorithm);
  }

  public void setCredential(String credential) {
    conditions.add(new String[]{"eq","$x-amz-credential",credential});
    formData.put("x-amz-credential", credential);
  }

  public void setDate(DateTime date) {
    String dateStr = date.toString(dateFormatyyyyMMddThhmmssZ);
    conditions.add(new String[]{"eq","$x-amz-date",dateStr});
    formData.put("x-amz-date", dateStr);
  }

  // Set only formData
  public void setSignature(String signature) {
    formData.put("x-amz-signature", signature);
  }

  // Set only formData
  public void setPolicy(String policybase64) {
    formData.put("policy", policybase64);
  }

  public byte[] marshalJson() throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    Joiner joiner = Joiner.on("\",\"");
    sb.append("{");
    if (expiration != null) {
      sb.append("\"expiration\":" + "\"" + expiration + "\"");
    }
    if (conditions.size() > 0) {
      sb.append(",\"conditions\":[");
      ListIterator<String[]> iterator = conditions.listIterator();
      while (iterator.hasNext()) {
        sb.append("[\"" + joiner.join(iterator.next()) + "\"]");
        if (iterator.hasNext()) {
          sb.append(",");
        }
      }
      sb.append("]");
    }
    sb.append("}");
    return sb.toString().getBytes("UTF-8");
  }
  public String base64() throws UnsupportedEncodingException {
    return BaseEncoding.base64().encode(marshalJson());
  }
  public Map<String, String> getFormData() {
    return formData;
  }
}
