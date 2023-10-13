package io.minio.admin.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddServiceAccountRequest {
  @JsonProperty("targetUser")
  private final String targetUser;

  public AddServiceAccountRequest(String targetUser) {
    this.targetUser = targetUser;
  }

  public String targetUser() {
    return targetUser;
  }
}
