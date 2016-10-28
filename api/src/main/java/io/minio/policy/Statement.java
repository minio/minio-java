/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2016 Minio, Inc.
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

package io.minio.policy;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Helper class to parse Amazon AWS S3 policy statement object.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Statement {
  @JsonProperty("Action")
  private Set<String> actions;

  @JsonProperty("Condition")
  private ConditionMap conditions;

  @JsonProperty("Effect")
  private String effect;

  @JsonProperty("Principal")
  private Principal principal;

  @JsonProperty("Resource")
  private Resources resources;

  @JsonProperty("Sid")
  private String sid;


  public Set<String> actions() {
    return this.actions;
  }


  public void setActions(Set<String> actions) {
    this.actions = actions;
  }


  public ConditionMap conditions() {
    return this.conditions;
  }


  public void setConditions(ConditionMap conditions) {
    this.conditions = conditions;
  }


  public String effect() {
    return this.effect;
  }


  public void setEffect(String effect) {
    this.effect = effect;
  }


  public Principal principal() {
    return this.principal;
  }


  public void setPrincipal(Principal principal) {
    this.principal = principal;
  }


  public Resources resources() {
    return this.resources;
  }


  public void setResources(Resources resources) {
    this.resources = resources;
  }


  public String sid() {
    return this.sid;
  }


  public void setSid(String sid) {
    this.sid = sid;
  }


  /**
   * Returns whether given statement is valid to process for given bucket name.
   */
  public boolean isValid(String bucketName) {
    Set<String> intersection = new HashSet<String>(this.actions);
    intersection.retainAll(Constants.VALID_ACTIONS);
    if (intersection.isEmpty()) {
      return false;
    }

    if (!this.effect.equals("Allow")) {
      return false;
    }

    Set<String> aws = this.principal.aws();
    if (aws == null || !aws.contains("*")) {
      return false;
    }

    String bucketResource = Constants.AWS_RESOURCE_PREFIX + bucketName;

    if (this.resources.contains(bucketResource)) {
      return true;
    }

    if (this.resources.startsWith(bucketResource + "/").isEmpty()) {
      return false;
    }

    return true;
  }


  /**
   * Removes object actions for given object resource.
   */
  public void removeObjectActions(String objectResource) {
    if (this.conditions != null) {
      return;
    }

    if (this.resources.size() > 1) {
      this.resources.remove(objectResource);
    } else {
      this.actions.removeAll(Constants.READ_WRITE_OBJECT_ACTIONS);
    }
  }


  private void removeReadOnlyBucketActions(String prefix) {
    if (!this.actions.containsAll(Constants.READ_ONLY_BUCKET_ACTIONS)) {
      return;
    }

    this.actions.removeAll(Constants.READ_ONLY_BUCKET_ACTIONS);

    if (this.conditions == null) {
      return;
    }

    if (prefix == null || prefix.isEmpty()) {
      return;
    }
    
    ConditionKeyMap stringEqualsValue = this.conditions.get("StringEquals");
    if (stringEqualsValue == null) {
      return;
    }

    Set<String> values = stringEqualsValue.get("s3:prefix");
    if (values != null) {
      values.remove(prefix);
    }

    if (values == null || values.isEmpty()) {
      stringEqualsValue.remove("s3:prefix");
    }

    if (stringEqualsValue.isEmpty()) {
      this.conditions.remove("StringEquals");
    }

    if (this.conditions.isEmpty()) {
      this.conditions = null;
    }
  }


  private void removeWriteOnlyBucketActions() {
    if (this.conditions == null) {
      this.actions.removeAll(Constants.WRITE_ONLY_BUCKET_ACTIONS);
    }
  }


  /**
   * Removes bucket actions for given prefix and bucketResource.
   */
  public void removeBucketActions(String prefix, String bucketResource,
                                  boolean readOnlyInUse, boolean writeOnlyInUse) {
    if (this.resources.size() > 1) {
      this.resources.remove(bucketResource);
      return;
    }

    if (!readOnlyInUse) {
      removeReadOnlyBucketActions(prefix);
    }

    if (!writeOnlyInUse) {
      removeWriteOnlyBucketActions();
    }

    return;
  }


  /**
   * Returns bucket policy types for given prefix.
   */
  @JsonIgnore
  public boolean[] getBucketPolicy(String prefix) {
    boolean commonFound = false;
    boolean readOnly = false;
    boolean writeOnly = false;

    Set<String> aws = this.principal.aws();
    if (!(this.effect.equals("Allow") && aws != null && aws.contains("*"))) {
      return new boolean[]{commonFound, readOnly, writeOnly};
    }

    if (this.actions.containsAll(Constants.COMMON_BUCKET_ACTIONS) && this.conditions == null) {
      commonFound = true;
    }

    if (this.actions.containsAll(Constants.WRITE_ONLY_BUCKET_ACTIONS) && this.conditions == null) {
      writeOnly = true;
    }

    if (this.actions.containsAll(Constants.READ_ONLY_BUCKET_ACTIONS)) {
      if (prefix != null && !prefix.isEmpty() && this.conditions != null) {
        ConditionKeyMap stringEqualsValue = this.conditions.get("StringEquals");
        if (stringEqualsValue != null) {
          Set<String> s3PrefixValues = stringEqualsValue.get("s3:prefix");
          if (s3PrefixValues != null && s3PrefixValues.contains(prefix)) {
            readOnly = true;
          }
        } else {
          ConditionKeyMap stringNotEqualsValue = this.conditions.get("StringNotEquals");
          if (stringNotEqualsValue != null) {
            Set<String> s3PrefixValues = stringNotEqualsValue.get("s3:prefix");
            if (s3PrefixValues != null && !s3PrefixValues.contains(prefix)) {
              readOnly = true;
            }
          }
        }
      } else if ((prefix == null || prefix.isEmpty()) && this.conditions == null) {
        readOnly = true;
      } else if (prefix != null && !prefix.isEmpty() && this.conditions == null) {
        readOnly = true;
      }
    }

    return new boolean[]{commonFound, readOnly, writeOnly};
  }


  /**
   * Returns object policy types.
   */
  @JsonIgnore
  public boolean[] getObjectPolicy() {
    boolean readOnly = false;
    boolean writeOnly = false;

    Set<String> aws = null;
    if (this.principal != null) {
      aws = this.principal.aws();
    }

    if (this.effect.equals("Allow")
        && aws != null && aws.contains("*")
        && this.conditions == null) {
      if (this.actions.containsAll(Constants.READ_ONLY_OBJECT_ACTIONS)) {
        readOnly = true;
      }
      if (this.actions.containsAll(Constants.WRITE_ONLY_OBJECT_ACTIONS)) {
        writeOnly = true;
      }
    }

    return new boolean[]{readOnly, writeOnly};
  }
}
