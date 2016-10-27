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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BucketPolicy {
  @JsonIgnore
  private String bucketName;
  
  @JsonProperty("Version")
  private String version;
  
  @JsonProperty("Statement")
  private List<Statement> statements;

  @JsonIgnore
  private static final ObjectMapper objectMapper =
      new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .setSerializationInclusion(Include.NON_NULL);


  public BucketPolicy() {
  }


  public BucketPolicy(String bucketName) {
    this.bucketName = bucketName;
    this.version = "2012-10-17";
  }


  public List<Statement> statements() {
    return statements;
  }


  /**
   * Reads JSON from given {@link Reader} and returns new {@link BucketPolicy} of given bucket name.
   */
  public static BucketPolicy parseJson(Reader reader, String bucketName) throws IOException {
    BucketPolicy bucketPolicy = objectMapper.readValue(reader, BucketPolicy.class);
    bucketPolicy.bucketName = bucketName;

    return bucketPolicy;
  }


  /**
   * Generates JSON of this BucketPolicy object.
   */
  @JsonIgnore
  public String getJson() throws JsonProcessingException {
    return objectMapper.writeValueAsString(this);
  }


  /**
   * Returns new bucket statements for given policy type.
   */
  private List<Statement> newBucketStatement(PolicyType policy, String prefix) {
    List<Statement> statements = new ArrayList<Statement>();

    if (policy == PolicyType.NONE || bucketName == null || bucketName.isEmpty()) {
      return statements;
    }

    Resources resources = new Resources(Constants.AWS_RESOURCE_PREFIX + bucketName);

    Statement statement = new Statement();
    statement.setActions(Constants.COMMON_BUCKET_ACTIONS);
    statement.setEffect("Allow");
    statement.setPrincipal(new Principal("*"));
    statement.setResources(resources);
    statement.setSid("");

    statements.add(statement);

    if (policy == PolicyType.READ_ONLY || policy == PolicyType.READ_WRITE) {
      statement = new Statement();
      statement.setActions(Constants.READ_ONLY_BUCKET_ACTIONS);
      statement.setEffect("Allow");
      statement.setPrincipal(new Principal("*"));
      statement.setResources(resources);
      statement.setSid("");

      if (prefix != null && !prefix.isEmpty()) {
        statement.setConditions(new ConditionMap("StringEquals",
                                                 new ConditionKeyMap("s3:prefix", prefix)));
      }

      statements.add(statement);
    }

    if (policy == PolicyType.WRITE_ONLY || policy == PolicyType.READ_WRITE) {
      statement = new Statement();
      statement.setActions(Constants.WRITE_ONLY_BUCKET_ACTIONS);
      statement.setEffect("Allow");
      statement.setPrincipal(new Principal("*"));
      statement.setResources(resources);
      statement.setSid("");

      statements.add(statement);
    }

    return statements;
  }


  /**
   * Returns new object statements for given policy type.
   */
  private List<Statement> newObjectStatement(PolicyType policy, String prefix) {
    List<Statement> statements = new ArrayList<Statement>();

    if (policy == PolicyType.NONE || bucketName == null || bucketName.isEmpty()) {
      return statements;
    }

    Resources resources = new Resources(Constants.AWS_RESOURCE_PREFIX + bucketName + "/" + prefix + "*");

    Statement statement = new Statement();
    statement.setEffect("Allow");
    statement.setPrincipal(new Principal("*"));
    statement.setResources(resources);
    statement.setSid("");

    if (policy == PolicyType.READ_ONLY) {
      statement.setActions(Constants.READ_ONLY_OBJECT_ACTIONS);
    } else if (policy == PolicyType.WRITE_ONLY) {
      statement.setActions(Constants.WRITE_ONLY_OBJECT_ACTIONS);
    } else if (policy == PolicyType.READ_WRITE) {
      statement.setActions(Constants.READ_WRITE_OBJECT_ACTIONS);
    }

    statements.add(statement);
    return statements;
  }


  /**
   * Returns new statements for given policy type.
   */
  private List<Statement> newStatements(PolicyType policy, String prefix) {
    List<Statement> statements = this.newBucketStatement(policy, prefix);
    List<Statement> objectStatements = this.newObjectStatement(policy, prefix);

    statements.addAll(objectStatements);

    return statements;
  }


  /**
   * Returns whether statements are used by other than given prefix statements.
   */
  @JsonIgnore
  private boolean[] getInUsePolicy(String prefix) {
    String resourcePrefix = Constants.AWS_RESOURCE_PREFIX + bucketName + "/";
    String objectResource = Constants.AWS_RESOURCE_PREFIX + bucketName + "/" + prefix + "*";

    boolean readOnlyInUse = false;
    boolean writeOnlyInUse = false;

    for (Statement statement : statements) {
      if (!statement.resources().contains(objectResource)
          && !statement.resources().startsWith(resourcePrefix).isEmpty()) {

        if (statement.actions().containsAll(Constants.READ_ONLY_OBJECT_ACTIONS)) {
          readOnlyInUse = true;
        }

        if (statement.actions().containsAll(Constants.WRITE_ONLY_OBJECT_ACTIONS)) {
          writeOnlyInUse = true;
        }
      }

      if (readOnlyInUse && writeOnlyInUse) {
        break;
      }
    }

    boolean[] rv = {readOnlyInUse, writeOnlyInUse};
    return rv;
  }


  /**
   * Returns all statements of given prefix.
   */
  private void removeStatements(String prefix) {
    String bucketResource = Constants.AWS_RESOURCE_PREFIX + bucketName;
    String objectResource = Constants.AWS_RESOURCE_PREFIX + bucketName + "/" + prefix + "*";
    boolean[] inUse = getInUsePolicy(prefix);
    boolean readOnlyInUse = inUse[0];
    boolean writeOnlyInUse = inUse[1];

    List<Statement> out = new ArrayList<Statement>();
    Set<String> s3PrefixValues = new HashSet<String>();
    List<Statement> readOnlyBucketStatements = new ArrayList<Statement>();

    for (Statement statement : statements) {
      if (!statement.isValid(bucketName)) {
        out.add(statement);
        continue;
      }

      if (statement.resources().contains(bucketResource)) {
        if (statement.conditions() != null) {
          statement.removeBucketActions(prefix, bucketResource, false, false);
        } else {
          statement.removeBucketActions(prefix, bucketResource, readOnlyInUse, writeOnlyInUse);
        }
      } else if (statement.resources().contains(objectResource)) {
        statement.removeObjectActions(objectResource);
      }

      if (!statement.actions().isEmpty()) {
        if (statement.resources().contains(bucketResource)
            && statement.actions().containsAll(Constants.READ_ONLY_BUCKET_ACTIONS)
            && statement.effect().equals("Allow")
            && statement.principal().aws().contains("*")) {
    
          if (statement.conditions() != null) {
            ConditionKeyMap stringEqualsValue = statement.conditions().get("StringEquals");
            if (stringEqualsValue != null) {
              Set<String> values = stringEqualsValue.get("s3:prefix");
              if (values != null) {
                for (String v : values) {
                  s3PrefixValues.add(bucketResource + "/" + v + "*");
                }
              }
            }
          } else if (!s3PrefixValues.isEmpty()) {
            readOnlyBucketStatements.add(statement);
            continue;
          }
        }

        out.add(statement);
      }
    }

    boolean skipBucketStatement = true;
    String resourcePrefix = Constants.AWS_RESOURCE_PREFIX + bucketName + "/";
    for (Statement statement : out) {
      Set<String> intersection = new HashSet<String>(s3PrefixValues);
      intersection.retainAll(statement.resources());

      if (!statement.resources().startsWith(resourcePrefix).isEmpty()
          && intersection.isEmpty()) {
        skipBucketStatement = false;
        break;
      }
    }

    for (Statement statement : readOnlyBucketStatements) {
      Set<String> aws = statement.principal().aws();
      if (skipBucketStatement
          && statement.resources().contains(bucketResource)
          && statement.effect().equals("Allow")
          && aws != null && aws.contains("*")
          && statement.conditions() == null) {
        continue;
      }

      out.add(statement);
    }

    if (out.size() == 1) {
      Statement statement = out.get(0);
      Set<String> aws = statement.principal().aws();
      if (statement.resources().contains(bucketResource)
          && statement.actions().containsAll(Constants.COMMON_BUCKET_ACTIONS)
          && statement.effect().equals("Allow")
          && aws != null && aws.contains("*")
          && statement.conditions() == null) {
        out = new ArrayList<Statement>();
      }
    }

    statements = out;
  }


  /**
   * Appends given statement into statement list to have unique statements.
   * - If statement already exists in statement list, it ignores.
   * - If statement exists with different conditions, they are merged.
   * - Else the statement is appended to statement list.
   */
  private void appendStatement(Statement statement) {
    for (Statement s : statements) {
      Set<String> aws = s.principal().aws();
      ConditionMap conditions = s.conditions();

      if (s.actions().containsAll(statement.actions())
          && s.effect().equals(statement.effect())
          && aws != null && aws.containsAll(statement.principal().aws())
          && conditions != null && conditions.equals(statement.conditions())) {
        s.resources().addAll(statement.resources());
        return;
      }

      if (s.resources().containsAll(statement.resources())
          && s.effect().equals(statement.effect())
          && aws != null && aws.containsAll(statement.principal().aws())
          && conditions != null && conditions.equals(statement.conditions())) {
        s.actions().addAll(statement.actions());
        return;
      }

      if (s.resources().containsAll(statement.resources())
          && s.actions().containsAll(statement.actions())
          && s.effect().equals(statement.effect())
          && aws != null && aws.containsAll(statement.principal().aws())) {
        if (conditions != null && conditions.equals(statement.conditions())) {
          return;
        }

        if (conditions != null && statement.conditions() != null) {
          conditions.putAll(statement.conditions());
          return;
        }
      }
    }

    if (!(statement.actions().isEmpty() && statement.resources().isEmpty())) {
      statements.add(statement);
    }
  }


  /**
   * Appends new statements for given policy type.
   */
  private void appendStatements(PolicyType policy, String prefix) {
    List<Statement> appendStatements = newStatements(policy, prefix);
    for (Statement statement : appendStatements) {
      appendStatement(statement);
    }
  }


  /**
   * Returns policy type of this bucket policy.
   */
  @JsonIgnore
  public PolicyType getPolicy(String prefix) {
    String bucketResource = Constants.AWS_RESOURCE_PREFIX + bucketName;
    String objectResource = Constants.AWS_RESOURCE_PREFIX + bucketName + "/" + prefix + "*";

    boolean bucketCommonFound = false;
    boolean bucketReadOnly = false;
    boolean bucketWriteOnly = false;
    String matchedResource = "";
    boolean objReadOnly = false;
    boolean objWriteOnly = false;

    for (Statement s : statements) {
      Set<String> matchedObjResources = new HashSet<String>();
      if (s.resources().contains(objectResource)) {
        matchedObjResources.add(objectResource);
      } else {
        matchedObjResources = s.resources().match(objectResource);
      }

      if (!matchedObjResources.isEmpty()) {
        boolean[] rv = s.getObjectPolicy();
        boolean readOnly = rv[0];
        boolean writeOnly = rv[1];

        for (String resource : matchedObjResources) {
          if (matchedResource.length() < resource.length()) {
            objReadOnly = readOnly;
            objWriteOnly = writeOnly;
            matchedResource = resource;
          } else if (matchedResource.length() == resource.length()) {
            objReadOnly = objReadOnly || readOnly;
            objWriteOnly = objWriteOnly || writeOnly;
            matchedResource = resource;
          }
        }
      } else if (s.resources().contains(bucketResource)) {
        boolean[] rv = s.getBucketPolicy(prefix);
        boolean commonFound = rv[0];
        boolean readOnly = rv[1];
        boolean writeOnly = rv[2];
        bucketCommonFound = bucketCommonFound || commonFound;
        bucketReadOnly = bucketReadOnly || readOnly;
        bucketWriteOnly = bucketWriteOnly || writeOnly;
      }
    }

    if (bucketCommonFound) {
      if (bucketReadOnly && bucketWriteOnly && objReadOnly && objWriteOnly) {
        return PolicyType.READ_WRITE;
      } else if (bucketReadOnly && objReadOnly) {
        return PolicyType.READ_ONLY;
      } else if (bucketWriteOnly && objWriteOnly) {
        return PolicyType.WRITE_ONLY;
      }
    }

    return PolicyType.NONE;
  }


  /**
   * Returns policy type of all prefixes.
   */
  @JsonIgnore
  public Map<String, PolicyType> getPolicies() {
    Map<String, PolicyType> policyRules = new Hashtable<String, PolicyType>();
    Set<String> objResources = new HashSet<String>();

    String bucketResource = Constants.AWS_RESOURCE_PREFIX + bucketName;

    // Search all resources related to objects policy
    for (Statement s : statements) {
      objResources.addAll(s.resources().startsWith(bucketResource + "/"));
    }

    // Pretend that policy resource as an actual object and fetch its policy
    for (String r : objResources) {
      // Put trailing * if exists in asterisk
      String asterisk = "";
      if (r.endsWith("*")) {
        r = r.substring(0, r.length() - 1);
        asterisk = "*";
      }

      String objectPath = r.substring(bucketResource.length() + 1, r.length());
      PolicyType policy = this.getPolicy(objectPath);
      policyRules.put(bucketName + "/" + objectPath + asterisk, policy);
    }

    return policyRules;
  }


  /**
   * Sets policy type for given prefix.
   */
  @JsonIgnore
  public void setPolicy(PolicyType policy, String prefix) {
    if (statements == null) {
      statements = new ArrayList<Statement>();
    }

    removeStatements(prefix);
    appendStatements(policy, prefix);
  }
}
