package io.minio.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
  ENABLED("enabled"),
  DISABLED("disabled");

  private final String value;

  Status(String value) {
    this.value = value;
  }

  @JsonValue
  public String toString() {
    return this.value;
  }

  @JsonCreator
  public static Status fromString(String statusString) {
    if ("enabled".equals(statusString)) {
      return ENABLED;
    }

    if ("disabled".equals(statusString)) {
      return DISABLED;
    }

    if (statusString.isEmpty()) {
      return null;
    }

    throw new IllegalArgumentException("Unknown status " + statusString);
  }
}
