/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseArgs {
  public abstract static class Builder<B extends Builder<B, A>, A extends BaseArgs> {
    protected List<Consumer<A>> operations;

    public Builder() {
      this.operations = new ArrayList<>();
    }

    @SuppressWarnings("unchecked") // safe as B will always be the builder of the current args class
    public A build() throws IllegalArgumentException {
      try {
        A args = (A) this.getClass().getEnclosingClass().getDeclaredConstructor().newInstance();
        operations.forEach(operation -> operation.accept(args));
        return args;
      } catch (InstantiationException
          | IllegalAccessException
          | InvocationTargetException
          | NoSuchMethodException
          | SecurityException e) {
        // This should never happen as we'll always have the
        // Builder class as an enclosed class of the args class
        e.printStackTrace();
        return null;
      }
    }
  }
}
