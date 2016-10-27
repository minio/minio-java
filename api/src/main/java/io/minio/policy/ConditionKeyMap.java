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

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;

class ConditionKeyMap extends Hashtable<String, Set<String>> {
  public ConditionKeyMap() {
    super();
  }


  public ConditionKeyMap(ConditionKeyMap map) {
    super(map);
  }


  public ConditionKeyMap(String key, String value) {
    super();

    this.put(key, value);
  }


  @Override
  public Set<String> put(String key, Set<String> value) {
    Set<String> existingValue = super.get(key);

    if (existingValue == null) {
      existingValue = new HashSet<String>(value);
    } else {
      existingValue.addAll(value);
    }

    return super.put(key, existingValue);
  }


  public Set<String> put(String key, String value) {
    Set<String> set = new HashSet<String>();
    set.add(value);

    return this.put(key, set);
  }


  /**
   *  Removes value of given key and key if key has empty value.
   */
  public Set<String> remove(String key, Set<String> value) {
    Set<String> existingValue = super.get(key);

    if (existingValue == null) {
      return null;
    }

    existingValue.removeAll(value);

    if (existingValue.isEmpty()) {
      return super.remove(key);
    }

    return super.put(key, existingValue);
  }
}
