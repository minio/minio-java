package io.minio.admin.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddServiceAccountResponse {
  @JsonProperty("credentials")
  private ServiceAccountCredentials credentials;

  public ServiceAccountCredentials credentials() {
    return credentials;
  }
}
