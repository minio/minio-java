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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadObjectArgs extends ObjectReadArgs {
  private String filename;

  public String filename() {
    return filename;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectReadArgs.Builder<Builder, DownloadObjectArgs> {
    public Builder filename(String filename) {
      validateFileName(filename);
      operations.add(args -> args.filename = filename);
      return this;
    }

    private void validateFileName(String filename) {
      validateNotEmptyString(filename, "filename");

      Path filePath = Paths.get(filename);
      boolean fileExists = Files.exists(filePath);

      if (fileExists && !Files.isRegularFile(filePath)) {
        throw new IllegalArgumentException(filename + ": not a regular file");
      }
    }
  }
}
