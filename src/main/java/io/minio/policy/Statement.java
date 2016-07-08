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

import com.google.gson.annotations.SerializedName;
import org.apache.commons.collections4.CollectionUtils;
import java.util.*;

/**
 * Helper class to parse Amazon AWS S3 policy statement object.
 */
class Statement {
  @SerializedName("Sid")
  private String sid;
  @SerializedName("Effect")
  private String effect;
  @SerializedName("Principal")
  private User principal;
  @SerializedName("Action")
  private List<String> actions;
  @SerializedName("Resource")
  private List<String> resources;
  @SerializedName("Condition")
  Map<String, Map<String, String>> conditions;

  public Statement() {
    super();
  }

  /**
   * Returns sid.
   */
  public String sid() {
    return sid;
  }

  /**
   * Returns sid.
   */
  public void setSid(String sid) {
    this.sid = sid;
  }

  /**
   * Returns effect.
   */
  public String effect() {
    return effect;
  }

  /**
   * Sets effect.
   */
  public void setEffect(String effect) {
    this.effect = effect;
  }

  /**
   * Returns principal.
   */
  public User principal() {
    return principal;
  }

  /**
   * Sets principal.
   */
  public void setPrincipal(User principal) {
    this.principal = principal;
  }

  /**
   * Returns actions.
   */
  public List<String> actions() {
    if (actions == null) {
      return new ArrayList<String>();
    }
    return actions;
  }

  /**
   * Set actions.
   */
  public void setActions(List<String> actions) {
    this.actions = actions;
  }

  /**
   * Returns resources.
   */
  public List<String> resources() {
    if (resources == null) {
      return new ArrayList<String>();
    }
    return resources;
  }

  /**
   * Sets resources.
   */
  public void setResources(List<String> resources) {
    this.resources = resources;
  }

  /**
   * Returns if policy has resources.
   */
  public boolean hasResources() {
    return resources != null && resources.size() > 0;
  }

  /**
   * Returns conditions.
   */
  public Map<String, Map<String, String>> conditions() {
    if (conditions == null) {
      return new HashMap<String, Map<String, String>>();
    }
    return conditions;
  }

  /**
   * Sets conditions.
   */
  public void setConditions(Map<String, Map<String, String>> conditions) {
    this.conditions = conditions;
  }

  /**
   * Returns if policy has conditions.
   */
  public boolean hasConditions() {
    return conditions != null && conditions.size() > 0;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    
    for (String operator: this.conditions().keySet()) {
      for (String key: this.conditions().get(operator).keySet()) {
        buf.append(String.format("%s %s %s", operator, key, this.conditions().get(operator).get(key)));
      }
    }

    return "Sid: " + this.sid 
              + "\nEffect: " + this.effect
              + "\nPrincipal: " + this.principal.toString()
              + "\nActions: " + String.join(", ", this.actions)
              + "\nResources: " + String.join(", ", this.resources)
              + "\nConditions: \n" + buf.toString();
  }

  /**
   * Returns if the statement actions are equal to actions.
   */
  private boolean isEqualActions(List<String> actions) {
    return CollectionUtils.isEqualCollection(actions, this.actions());
  }

  /**
   * Returns if principal is public identifier.
   */
  private boolean hasPrincipalPublic() {
    return principal().aws().contains("*");
  }

  /**
   * Returns if statement has StringEquals condition for s3:prefix with objectPrefix.
   */
  private boolean hasConditionForPrefix(String objectPrefix) {
    for (String operator : this.conditions().keySet()) {
      if (!operator.equals("StringEquals")) {
        continue;
      }

      for (String key : this.conditions().get(operator).keySet()) {
        String value = this.conditions().get(operator).get(key);
        if (!key.equals("s3:prefix")) {
          continue;
        }

        // TODO(nl5887): return startsWith or exact match?
        if (value.equals(objectPrefix)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns if resource is a bucket policy for bucket and bucketActions.
   */
  protected boolean isBucketPolicy(String bucketName, List<String> bucketActions) {
    boolean hasBucketActions = false;

    if (!this.hasPrincipalPublic()) {
      return false;
    }

    if (!this.effect.equals("Allow")) {
      return false;
    }

    for (String resource: resources()) {
      if (resource.equals(String.format("%s%s", Constants.AWS_RESOURCE_PREFIX, bucketName))) {
        hasBucketActions |= this.isEqualActions(bucketActions);
      }
    }

    return hasBucketActions;
  }

  /**
   * Returns if resource is a bucket policy for bucket, objectPrefix and bucketActions.
   */
  protected boolean isBucketPolicy(String bucketName, String objectPrefix, List<String> bucketActions) {
    boolean hasBucketActions = false;

    if (!this.hasPrincipalPublic()) {
      return false;
    }

    if (!this.effect.equals("Allow")) {
      return false;
    }

    for (String resource: resources()) {
      if (resource.equals(String.format("%s%s", Constants.AWS_RESOURCE_PREFIX, bucketName))) {
        hasBucketActions |= this.isEqualActions(bucketActions) && this.hasConditionForPrefix(objectPrefix);
      }
    }

    return hasBucketActions;
  }

  /**
   * Returns if resource matches arn pattern.
   */
  static boolean resourceMatch(String pattern, String resource) {
    if (pattern.equals("")) {
      return resource.equals(pattern);
    } else if (pattern.equals("*")) {
      return true;
    }

    String[] parts = pattern.split("\\*");
    if (parts.length == 1) {
      return pattern.equals(resource);
    }

    boolean tGlob = pattern.endsWith("*");

    int end = parts.length - 1;
    if (!resource.startsWith(parts[0])) {
      return false;
    }

    for (int i = 0; i < end; i++) {
      if (!resource.contains(parts[i])) {
        return false;
      }

      int idx = resource.indexOf(parts[i]) + parts[i].length();
      resource = resource.substring(idx);
    }

    return tGlob || resource.endsWith(parts[end]);
  }

  /**
   * Returns if resource is a object policy for bucket, objectPrefix and objectActions.
   */
  protected boolean isObjectPolicy(String bucketName, String objectPrefix, List<String> objectActions) {
    boolean hasObjectActions = false;

    if (!this.hasPrincipalPublic()) {
      return false;
    }

    if (!this.effect.equals("Allow")) {
      return false;
    }

    for (String resource: resources()) {
      if (resourceMatch(resource, 
                String.format("%s%s/%s*", Constants.AWS_RESOURCE_PREFIX, bucketName, objectPrefix))) {
        hasObjectActions |= this.isEqualActions(objectActions);
      }
    }

    return hasObjectActions;
  }

}
