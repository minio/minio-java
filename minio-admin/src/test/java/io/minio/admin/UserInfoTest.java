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
