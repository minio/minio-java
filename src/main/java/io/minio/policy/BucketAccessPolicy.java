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

package io.minio.policy;

import com.google.gson.annotations.SerializedName;
import io.minio.Constants;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to parse Amazon AWS S3 policy documents.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class BucketAccessPolicy {
  @SerializedName("Version")
  private String version;

  @SerializedName("Statement")
  private List<Statement> statements;

  public BucketAccessPolicy()  {
    super();
    version = "2012-10-17";
  }

  /**
   * Returns none bucket access policy.
   */
  public static BucketAccessPolicy none() {
    return new BucketAccessPolicy();
  }

  /**
   * Returns version.
   */
  public String version() {
    return version;
  }

  /**
   * Returns statements.
   */
  public List<Statement> statements() {
    if (statements == null) {
      return new ArrayList<Statement>();
    }
    return statements;
  }

  /**
   * Sets statements.
   */
  public void setStatements(List<Statement> statements) {
    this.statements = statements;
  }

  /**
   * Returns if policy has statements.
   */
  public boolean hasStatements() {
    return statements != null && statements.size() > 0;
  }

  /**
   * Returns if statements contains common bucket statement.
   */
  public boolean hasCommonBucketStatement(String bucketName) {
    boolean commonActions = false;

    for (Statement statement: this.statements()) {
      commonActions |= statement.isBucketPolicy(bucketName, Actions.commonBucket);
    }

    return commonActions;
  }

  /**
   * Returns if statements for bucketName and objectPrefix are readWrite policy.
   */
  boolean isBucketPolicyReadWrite(String bucketName, String objectPrefix) {
    boolean commonActions = false;
    boolean readWrite = false;

    for (Statement statement: this.statements()) {
      commonActions |= statement.isBucketPolicy(bucketName, objectPrefix, Actions.readWriteBucket);
      readWrite |= statement.isObjectPolicy(bucketName, objectPrefix, Actions.readWriteObject);
    }

    return commonActions && readWrite;
  }

  /**
   * Returns if statements for bucketName and objectPrefix are writeOnly policy.
   */
  boolean isBucketPolicyWriteOnly(String bucketName, String objectPrefix) {
    boolean commonActions = false;
    boolean writeOnly = false;

    for (Statement statement: this.statements()) {
      commonActions |= statement.isBucketPolicy(bucketName, objectPrefix, Actions.writeOnlyBucket);
      writeOnly |= statement.isObjectPolicy(bucketName, objectPrefix, Actions.writeOnlyObject);
    }

    return commonActions && writeOnly;
  }

  /**
   * Returns if statements for bucketName and objectPrefix are readOnly policy.
   */
  boolean isBucketPolicyReadOnly(String bucketName, String objectPrefix) {
    boolean commonActions = false;
    boolean readOnly = false;

    for (Statement statement: this.statements()) {
      commonActions |= statement.isBucketPolicy(bucketName, objectPrefix, Actions.readOnlyBucket);
      readOnly |= statement.isObjectPolicy(bucketName, objectPrefix, Actions.readOnlyObject);
    }

    return commonActions && readOnly;
  }

  /**
   * Returns bucketPolicy for bucketName and objectPrefix derived from statements.
   */
  public BucketPolicy identifyPolicyType(String bucketName, String objectPrefix) {
    if (!this.hasStatements()) {
      return BucketPolicy.None;
    }

    if (this.isBucketPolicyReadWrite(bucketName, objectPrefix)) {
      return BucketPolicy.ReadWrite;
    } else if (this.isBucketPolicyWriteOnly(bucketName, objectPrefix)) {
      return BucketPolicy.WriteOnly;
    } else if (this.isBucketPolicyReadOnly(bucketName, objectPrefix)) {
      return BucketPolicy.ReadOnly;
    }

    return BucketPolicy.None;
  }

  /**
   * Returns statements without minio defined statements for bucketName and objectPrefix.
   */
  public List<Statement> removeBucketPolicyStatement(String bucketName, String objectPrefix) {
    List<Statement> returnStatements = new ArrayList<>();

    for (Statement statement: this.statements()) {
      // remove all bucket and object policy statements for given bucketName and objectPrefix.
      if (statement.isBucketPolicy(bucketName, objectPrefix, Actions.readWriteBucket)) {
        continue;
      } else if (statement.isBucketPolicy(bucketName, objectPrefix, Actions.readOnlyBucket)) {
        continue;
      } else if (statement.isBucketPolicy(bucketName, objectPrefix, Actions.writeOnlyBucket)) {
        continue;
      } else if (statement.isObjectPolicy(bucketName, objectPrefix, Actions.readWriteObject)) {
        continue;
      } else if (statement.isObjectPolicy(bucketName, objectPrefix, Actions.readOnlyObject)) {
        continue;
      } else if (statement.isObjectPolicy(bucketName, objectPrefix, Actions.writeOnlyObject)) {
        continue;
      }

      returnStatements.add(statement);
    }

    return returnStatements;
  }

  /**
   * Returns condition for s3:prefix with objectPrefix.
   */
  private static Map<String, Map<String, String>> bucketCondition(String objectPrefix) {
    Map<String, String> condition = new HashMap<>();
    condition.put("s3:prefix", objectPrefix);

    Map<String, Map<String, String>> conditions = new HashMap<>();
    conditions.put("StringEquals", condition);

    return conditions;
  }

  /**
   * Returns statements for common bucket.
   */
  public static List<Statement> commonBucketStatement(String bucketName) {
    List<Statement> statements = new ArrayList<>();

    Statement bucketResourceStatement = new Statement();
    bucketResourceStatement.setSid(StringUtils.join("-", "minio-bucket"));
    bucketResourceStatement.setEffect("Allow");
    bucketResourceStatement.setPrincipal(User.all());
    bucketResourceStatement.setResources(
              Arrays.asList(String.format("%s%s", Constants.AWS_RESOURCE_PREFIX, bucketName)));
    bucketResourceStatement.setActions(Actions.commonBucket);
    statements.add(bucketResourceStatement);

    return statements;
  }

  /**
   * Returns statements for readOnly policy with bucketName and objectPrefix.
   */
  static List<Statement> setReadOnlyStatement(String bucketName, String objectPrefix) {
    List<Statement> statements = new ArrayList<>();

    Statement bucketResourceStatement = new Statement();
    bucketResourceStatement.setSid(StringUtils.join("-", "minio-readonly-bucket", objectPrefix));
    bucketResourceStatement.setEffect("Allow");
    bucketResourceStatement.setPrincipal(User.all());
    bucketResourceStatement.setResources(
              Arrays.asList(String.format("%s%s", Constants.AWS_RESOURCE_PREFIX, bucketName)));
    bucketResourceStatement.setActions(Actions.readOnlyBucket);
    bucketResourceStatement.setConditions(bucketCondition(objectPrefix));
    statements.add(bucketResourceStatement);

    Statement objectResourceStatement = new Statement();
    objectResourceStatement.setSid(StringUtils.join("-", "minio-readonly-object", objectPrefix));
    objectResourceStatement.setEffect("Allow");
    objectResourceStatement.setPrincipal(User.all());
    objectResourceStatement.setResources(
              Arrays.asList(String.format("%s%s/%s*", Constants.AWS_RESOURCE_PREFIX, bucketName, objectPrefix)));
    objectResourceStatement.setActions(Actions.readOnlyObject);
    statements.add(objectResourceStatement);

    return statements;
  }

  /**
   * Returns statements for writeOnly policy with bucketName and objectPrefix.
   */
  static List<Statement> setWriteOnlyStatement(String bucketName, String objectPrefix) {
    List<Statement> statements = new ArrayList<>();

    Statement bucketResourceStatement = new Statement();
    bucketResourceStatement.setSid(StringUtils.join("-", "minio-writeonly-bucket", objectPrefix));
    bucketResourceStatement.setEffect("Allow");
    bucketResourceStatement.setPrincipal(User.all());
    bucketResourceStatement.setResources(
                  Arrays.asList(String.format("%s%s", Constants.AWS_RESOURCE_PREFIX, bucketName)));
    bucketResourceStatement.setActions(Actions.writeOnlyBucket);
    bucketResourceStatement.setConditions(bucketCondition(objectPrefix));
    if (bucketResourceStatement.actions().size() > 0) {
      statements.add(bucketResourceStatement);
    }

    Statement objectResourceStatement = new Statement();
    objectResourceStatement.setSid(StringUtils.join("-", "minio-writeonly-object", objectPrefix));
    objectResourceStatement.setEffect("Allow");
    objectResourceStatement.setPrincipal(User.all());
    objectResourceStatement.setResources(
              Arrays.asList(String.format("%s%s/%s*", Constants.AWS_RESOURCE_PREFIX, bucketName, objectPrefix)));
    objectResourceStatement.setActions(Actions.writeOnlyObject);
    statements.add(objectResourceStatement);

    return statements;
  }

  /**
   * Returns statements for readWrite policy with bucketName and objectPrefix.
   */
  static List<Statement> setReadWriteStatement(String bucketName, String objectPrefix) {
    List<Statement> statements = new ArrayList<>();

    Statement bucketResourceStatement = new Statement();
    bucketResourceStatement.setSid(StringUtils.join("-", "minio-readwrite-bucket", objectPrefix));
    bucketResourceStatement.setEffect("Allow");
    bucketResourceStatement.setPrincipal(User.all());
    bucketResourceStatement.setResources(
              Arrays.asList(String.format("%s%s", Constants.AWS_RESOURCE_PREFIX, bucketName)));
    bucketResourceStatement.setActions(Actions.readWriteBucket);
    bucketResourceStatement.setConditions(bucketCondition(objectPrefix));
    statements.add(bucketResourceStatement);

    Statement objectResourceStatement = new Statement();
    objectResourceStatement.setSid(StringUtils.join("-", "minio-readwrite-object", objectPrefix));
    objectResourceStatement.setEffect("Allow");
    objectResourceStatement.setPrincipal(User.all());
    objectResourceStatement.setResources(
              Arrays.asList(String.format("%s%s/%s*", Constants.AWS_RESOURCE_PREFIX, bucketName, objectPrefix)));
    objectResourceStatement.setActions(Actions.readWriteObject);
    statements.add(objectResourceStatement);

    return statements;
  }

  /**
   * Generates the policy statements for policy, bucketName and objectPrefix.
   */
  public static List<Statement> generatePolicyStatements(BucketPolicy policy, String bucketName,
                                              String objectPrefix) {

    List<Statement> statements = new ArrayList<>();

    if (policy == BucketPolicy.ReadWrite) {
      statements = setReadWriteStatement(bucketName, objectPrefix);
    } else if (policy == BucketPolicy.ReadOnly) {
      statements = setReadOnlyStatement(bucketName, objectPrefix);
    } else if (policy == BucketPolicy.WriteOnly) {
      statements = setWriteOnlyStatement(bucketName, objectPrefix);
    }

    return statements;
  }

}
