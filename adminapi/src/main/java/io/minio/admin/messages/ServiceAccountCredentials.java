package io.minio.admin.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.minio.messages.ResponseDate;

public class ServiceAccountCredentials {
  @JsonProperty("accessKey")
  private String accessKey;

  @JsonProperty("secretKey")
  private String secretKey;

  @JsonProperty("sessionToken")
  private String sessionToken;

  @JsonProperty("expiration")
  private ResponseDate expiration;

  public String accessKey() {
    return accessKey;
  }

  public String secretKey() {
    return secretKey;
  }

  public String sessionToken() {
    return sessionToken;
  }

  public ResponseDate expiration() {
    return expiration;
  }
}
