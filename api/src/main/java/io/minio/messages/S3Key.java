/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 Minio, Inc.
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

import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;

import io.minio.errors.InvalidArgumentException;


/**
 * Helper class to parse Amazon AWS S3 response XML containing S3Key.
 */
@SuppressWarnings("WeakerAccess")
public class S3Key extends XmlEntity {
  @Key("FilterRule")
  private List<FilterRule> filterRuleList = new LinkedList<>();


  public S3Key() throws XmlPullParserException {
    super();
    super.name = "S3Key";
  }


  /**
   * Returns filter rule list.
   */
  public List<FilterRule> filterRuleList() {
    return filterRuleList;
  }


  /**
   * Sets filter rule to list.
   * As per Amazon AWS S3 server behavior, its not possible to set more than one rule for "prefix" or "suffix".
   * However the spec http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTnotification.html
   * is not clear about this behavior.
   */
  private void setRule(String name, String value) throws InvalidArgumentException, XmlPullParserException {
    if (value.length() > 1024) {
      throw new InvalidArgumentException("value '" + value + "' is more than 1024 long");
    }

    for (FilterRule rule: filterRuleList) {
      // Remove rule.name is same as given name.
      if (rule.name().equals(name)) {
        filterRuleList.remove(rule);
      }
    }

    FilterRule newRule = new FilterRule();
    newRule.setName(name);
    newRule.setValue(value);
    filterRuleList.add(newRule);
  }


  public void setPrefixRule(String value) throws InvalidArgumentException, XmlPullParserException {
    setRule("prefix", value);
  }


  public void setSuffixRule(String value) throws InvalidArgumentException, XmlPullParserException {
    setRule("suffix", value);
  }
}
