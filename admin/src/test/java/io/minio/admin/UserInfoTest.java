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

package io.minio.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

public class UserInfoTest {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void userInfo_fullSerialization() throws JsonProcessingException {
    UserInfo userInfo = new UserInfo();
    userInfo.setSecretKey("bar");
    userInfo.setStatus(UserInfo.STATUS_DISABLED);
    userInfo.setMemberOf(ImmutableList.of("test"));
    userInfo.setPolicyName("example");
    String rawJson = objectMapper.writeValueAsString(userInfo);
    UserInfo readUserInfo = objectMapper.readValue(rawJson, UserInfo.class);
    Assert.assertEquals(userInfo.getSecretKey(), readUserInfo.getSecretKey());
    Assert.assertEquals(userInfo.getStatus(), readUserInfo.getStatus());
    Assert.assertEquals(userInfo.getPolicyName(), readUserInfo.getPolicyName());
    Assert.assertEquals(userInfo.getMemberOf(), readUserInfo.getMemberOf());
  }

  @Test
  public void userInfo_partialDeserialization() throws JsonProcessingException {
    UserInfo userInfo = new UserInfo();
    userInfo.setSecretKey("bar");
    userInfo.setStatus(UserInfo.STATUS_DISABLED);
    String rawJson = objectMapper.writeValueAsString(userInfo);
    UserInfo readUserInfo = objectMapper.readValue(rawJson, UserInfo.class);
    Assert.assertEquals(userInfo.getSecretKey(), readUserInfo.getSecretKey());
    Assert.assertEquals(userInfo.getStatus(), readUserInfo.getStatus());
    Assert.assertEquals(userInfo.getPolicyName(), readUserInfo.getPolicyName());
    Assert.assertEquals(userInfo.getMemberOf(), readUserInfo.getMemberOf());
  }
}
