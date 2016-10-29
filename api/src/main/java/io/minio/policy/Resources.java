/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2016 Minio, Inc.
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

import java.util.HashSet;
import java.util.Set;

class Resources extends HashSet<String> {
  public Resources() {
    super();
  }


  public Resources(String resource) {
    super();

    super.add(resource);
  }


  public Set<String> startsWith(String resourcePrefix) {
    Set<String> rv = new HashSet<String>();

    for (String resource : this) {
      if (resource.startsWith(resourcePrefix)) {
        rv.add(resource);
      }
    }

    return rv;
  }


  private boolean matched(String pattern, String resource) {
    if (pattern.isEmpty()) {
      return (resource == pattern);
    }

    if (pattern.equals("*")) {
      return true;
    }

    String[] parts = pattern.split("\\*");
    if (parts.length == 1) {
      return (resource.equals(pattern));
    }

    boolean tGlob = pattern.endsWith("*");
    int end = parts.length - 1;

    if (!resource.startsWith(parts[0])) {
      return false;
    }

    for (int i = 1; i < end; i++) {
      if (!resource.contains(parts[i])) {
        return false;
      }

      int idx = resource.indexOf(parts[i]) + parts[i].length();
      resource = resource.substring(idx);
    }

    return (tGlob || resource.endsWith(parts[end]));
  }


  public Set<String> match(String resource) {
    Set<String> rv = new HashSet<String>();

    for (String pattern : this) {
      if (matched(pattern, resource)) {
        rv.add(pattern);
      }
    }

    return rv;
  }
}
