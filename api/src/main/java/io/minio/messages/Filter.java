/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
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
import java.util.LinkedList;
import java.util.List;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Helper class to denote Filter configuration of {@link CloudFunctionConfiguration}, {@link
 * QueueConfiguration} or {@link TopicConfiguration}.
 */
@Root(name = "Filter", strict = false)
public class Filter {
  @ElementList(name = "S3Key")
  private List<FilterRule> filterRuleList;

  public Filter() {}

  /**
   * Sets filter rule to list. As per Amazon AWS S3 server behavior, its not possible to set more
   * than one rule for "prefix" or "suffix". However the spec
   * http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTnotification.html is not clear
   * about this behavior.
   */
  private void setRule(String name, String value) throws IllegalArgumentException {
    if (value.length() > 1024) {
      throw new IllegalArgumentException("value '" + value + "' is more than 1024 long");
    }

    if (filterRuleList == null) {
      filterRuleList = new LinkedList<>();
    }

    for (FilterRule rule : filterRuleList) {
      // Remove rule.name is same as given name.
      if (rule.name().equals(name)) {
        filterRuleList.remove(rule);
      }
    }

    filterRuleList.add(new FilterRule(name, value));
  }

  public void setPrefixRule(String value) throws IllegalArgumentException {
    setRule("prefix", value);
  }

  public void setSuffixRule(String value) throws IllegalArgumentException {
    setRule("suffix", value);
  }

  public List<FilterRule> filterRuleList() {
    return Collections.unmodifiableList(
        filterRuleList == null ? new LinkedList<>() : filterRuleList);
  }
}
