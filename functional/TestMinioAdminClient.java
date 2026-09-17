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

import io.minio.admin.MinioAdminClient;
import io.minio.admin.Status;
import io.minio.admin.UserInfo;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "REC",
    justification = "Allow catching super class Exception since it's tests")
public class TestMinioAdminClient extends TestArgs {
  private MinioAdminClient client;
  private static String userAccessKey = getRandomName();
  private static String userSecretKey = getRandomName();
  private static String policyName = getRandomName();

  public TestMinioAdminClient(TestArgs args, MinioAdminClient client) {
    super(args);
    this.client = client;
  }

  public void addUser() throws Exception {
    String methodName = "addUser()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      client.addUser(userAccessKey, Status.ENABLED, userSecretKey, null, null);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void addCannedPolicy() throws Exception {
    String methodName = "addCannedPolicy()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      String policyJson =
          "{'Version': '2012-10-17','Statement': [{'Action': ['s3:GetObject'],'Effect':"
              + " 'Allow','Resource': ['arn:aws:s3:::my-bucketname/*'],'Sid': ''}]}";
      client.addCannedPolicy(policyName, policyJson.replaceAll("'", "\""));
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void listCannedPolicies() throws Exception {
    String methodName = "listCannedPolicies()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      Map<String, String> policies = client.listCannedPolicies();
      String policy = policies.get(policyName);
      Assertions.assertTrue(policy != null);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void removeCannedPolicy() throws Exception {
    String methodName = "removeCannedPolicy()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      client.removeCannedPolicy(policyName);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void setPolicy() throws Exception {
    String methodName = "setPolicy()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      client.setPolicy(userAccessKey, false, policyName);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void getUserInfo() throws Exception {
    String methodName = "getUserInfo()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      UserInfo userInfo = client.getUserInfo(userAccessKey);
      Assertions.assertEquals(userInfo.status(), Status.ENABLED);
      Assertions.assertEquals(userInfo.policyName(), policyName);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void listUsers() throws Exception {
    String methodName = "listUsers()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      Map<String, UserInfo> users = client.listUsers();
      Assertions.assertTrue(users.containsKey(userAccessKey));
      Assertions.assertEquals(users.get(userAccessKey).status(), Status.ENABLED);
      Assertions.assertEquals(users.get(userAccessKey).policyName(), policyName);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void deleteUser() throws Exception {
    String methodName = "deleteUser()";
    if (!MINT_ENV) System.out.println(methodName);
    long startTime = System.currentTimeMillis();

    try {
      client.deleteUser(userAccessKey);
    } catch (Exception e) {
      handleException(methodName, null, startTime, e);
    }
  }

  public void runAdminTests() throws Exception {
    addUser();
    addCannedPolicy();
    setPolicy();
    getUserInfo();
    listUsers();
    listCannedPolicies();
    deleteUser();
    removeCannedPolicy();
  }
}
