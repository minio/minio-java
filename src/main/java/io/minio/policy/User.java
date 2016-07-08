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

package io.minio;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 * Helper class to parse Amazon AWS S3 policy declarations.
 */
class User {
  @SerializedName("AWS")
  private List<String> aws;

  public User() {
    super();
  }

  /**
   * Returns User for all.
   */
  public static User all() {
    User user = new User();
    user.setAws(Arrays.asList("*"));
    return user;
  }

  /**
   * Returns AWS.
   */
  public List<String> aws() {
    if (aws == null) {
      return new ArrayList<>();
    }
    return aws;
  }

  /**
   * Sets AWS.
   */
  public void setAws(List<String> aws) {
    this.aws = aws;
  }

  public String toString() {
    return String.join(",", this.aws);
  }
}
