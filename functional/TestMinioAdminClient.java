/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015-2021 MinIO, Inc.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.admin.*;
import java.util.Map;
import org.junit.Assert;

@SuppressFBWarnings(
    value = "REC",
    justification = "Allow catching super class Exception since it's tests")
public class TestMinioAdminClient {

  private final MinioAdminClient adminClient;
  private final boolean mintEnv;

  private static String userAccessKey = "minio-test-user";
  private static String userSecretKey = "minio-rocks-123";
  private static String policyName = FunctionalTest.getRandomName();

  public TestMinioAdminClient(MinioAdminClient adminClient, boolean mintEnv) {
    this.adminClient = adminClient;
    this.mintEnv = mintEnv;
  }

  public void addCannedPolicy() throws Exception {
    String methodName = "addCannedPolicy()";
    if (!mintEnv) {
      System.out.println(methodName);
    }
    long startTime = System.currentTimeMillis();
    try {
      adminClient.addCannedPolicy(
          AddPolicyArgs.builder()
              .policyName(policyName)
              .policyString(
                  "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Action\": [\"s3:GetObject\"],\"Effect\": \"Allow\",\"Resource\": [\"arn:aws:s3:::my-bucketname/*\"],\"Sid\": \"\"}]}")
              .build());
    } catch (Exception e) {
      FunctionalTest.handleException(methodName, null, startTime, e);
    }
  }

  public void listCannedPolicies() throws Exception {
    String methodName = "listCannedPolicies()";
    if (!mintEnv) {
      System.out.println(methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      Map<String, String> policies = adminClient.listCannedPolicies();
      Assert.assertTrue(policies.containsKey(policyName));
    } catch (Exception e) {
      FunctionalTest.handleException(methodName, null, startTime, e);
    }
  }

  public void removeCannedPolicy() throws Exception {
    String methodName = "removeCannedPolicy()";
    if (!mintEnv) {
      System.out.println(methodName);
    }
    long startTime = System.currentTimeMillis();
    try {
      adminClient.removeCannedPolicy(policyName);
    } catch (Exception e) {
      FunctionalTest.handleException(methodName, null, startTime, e);
    }
  }

  public void setPolicy() throws Exception {
    String methodName = "setPolicy()";
    if (!mintEnv) {
      System.out.println(methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      adminClient.setPolicy(
          SetPolicyArgs.builder().userOrGroup(userAccessKey).policyName(policyName).build());
    } catch (Exception e) {
      FunctionalTest.handleException(methodName, null, startTime, e);
    }
  }

  public void createUser() throws Exception {
    String methodName = "createUser()";
    if (!mintEnv) {
      System.out.println(methodName);
    }
    long startTime = System.currentTimeMillis();
    try {
      adminClient.addUser(
          AddUserArgs.builder().accessKey(userAccessKey).secretKey(userSecretKey).build());
    } catch (Exception e) {
      FunctionalTest.handleException(methodName, null, startTime, e);
    }
  }

  public void listUsers() throws Exception {
    String methodName = "listUsers()";
    if (!mintEnv) {
      System.out.println(methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      Map<String, UserInfo> users = adminClient.listUsers();
      Assert.assertTrue(users.containsKey(userAccessKey));
      Assert.assertEquals(users.get(userAccessKey).getStatus(), UserInfo.STATUS_ENABLED);
      Assert.assertEquals(users.get(userAccessKey).getPolicyName(), policyName);
    } catch (Exception e) {
      FunctionalTest.handleException(methodName, null, startTime, e);
    }
  }

  public void deleteUser() throws Exception {
    String methodName = "deleteUser()";
    if (!mintEnv) {
      System.out.println(methodName);
    }

    long startTime = System.currentTimeMillis();
    try {
      adminClient.deleteUser(userAccessKey);
    } catch (Exception e) {
      FunctionalTest.handleException(methodName, null, startTime, e);
    }
  }

  public void runAdminTests() throws Exception {
    createUser();
    addCannedPolicy();
    setPolicy();
    listUsers();
    listCannedPolicies();
    deleteUser();
    removeCannedPolicy();
  }
}
