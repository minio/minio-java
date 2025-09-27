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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Common arguments of {@link BucketArgs}, {@link ListBucketsArgs} and {@link PutObjectFanOutEntry}.
 */
public abstract class BaseArgs {
  protected String location;
  protected Http.Headers extraHeaders;
  protected Http.QueryParameters extraQueryParams;

  protected BaseArgs() {}

  protected BaseArgs(BaseArgs args) {
    this.location = args.location;
    this.extraHeaders = args.extraHeaders;
    this.extraQueryParams = args.extraQueryParams;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String location() {
    return location;
  }

  public Http.Headers extraHeaders() {
    return extraHeaders;
  }

  public Http.QueryParameters extraQueryParams() {
    return extraQueryParams;
  }

  protected void checkSse(ServerSideEncryption sse, boolean isHttps) {
    if (sse == null) return;
    if (sse.tlsRequired() && !isHttps) {
      throw new IllegalArgumentException(
          sse + " operations must be performed over a secure connection.");
    }
  }

  /** Base builder which builds arguments. */
  public abstract static class Builder<B extends Builder<B, A>, A extends BaseArgs> {
    protected List<Consumer<A>> operations;

    protected abstract void validate(A args);

    public Builder() {
      this.operations = new ArrayList<>();
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B extends this class.
    public B extraHeaders(Http.Headers headers) {
      final Http.Headers extraHeaders = new Http.Headers(headers);
      operations.add(args -> args.extraHeaders = extraHeaders);
      return (B) this;
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B extends this class.
    public B extraQueryParams(Http.QueryParameters queryParams) {
      final Http.QueryParameters extraQueryParams = new Http.QueryParameters(queryParams);
      operations.add(args -> args.extraQueryParams = extraQueryParams);
      return (B) this;
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B extends this class.
    public B extraHeaders(Map<String, String> headers) {
      return extraHeaders(new Http.Headers(headers));
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B extends this class.
    public B extraQueryParams(Map<String, String> queryParams) {
      return extraQueryParams(new Http.QueryParameters(queryParams));
    }

    @SuppressWarnings("unchecked") // safe as B will always be the builder of the current args class
    private A newInstance() {
      try {
        for (Constructor<?> constructor :
            this.getClass().getEnclosingClass().getDeclaredConstructors()) {
          if (constructor.getParameterCount() == 0) {
            return (A) constructor.newInstance();
          }
        }

        throw new RuntimeException(
            this.getClass().getEnclosingClass() + " must have no argument constructor");
      } catch (InstantiationException
          | IllegalAccessException
          | InvocationTargetException
          | SecurityException e) {
        // Args class must have no argument constructor with at least protected access.
        throw new RuntimeException(e);
      }
    }

    /** Creates derived Args class with each attribute populated. */
    public A build() {
      A args = newInstance();
      operations.forEach(operation -> operation.accept(args));
      validate(args);
      return args;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseArgs)) return false;
    BaseArgs baseArgs = (BaseArgs) o;
    return Objects.equals(extraHeaders, baseArgs.extraHeaders)
        && Objects.equals(extraQueryParams, baseArgs.extraQueryParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(extraHeaders, extraQueryParams);
  }
}
