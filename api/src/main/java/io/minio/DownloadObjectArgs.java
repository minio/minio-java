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

public class DownloadObjectArgs extends SsecObjectArgs {
  private String fileName;

  public String fileName() {
    return fileName;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends SsecObjectArgs.Builder<Builder, DownloadObjectArgs> {
    public Builder fileName(String fileName) {
      validateFileName(fileName);
      operations.add(args -> args.fileName = fileName);
      return this;
    }

    private void validateFileName(String fileName) {
      if (fileName == null) {
        return;
      }

      if (fileName.isEmpty()) {
        throw new IllegalArgumentException("filename should be either null or non-empty");
      }

      Path filePath = Paths.get(fileName);
      boolean fileExists = Files.exists(filePath);

      if (fileExists && !Files.isRegularFile(filePath)) {
        throw new IllegalArgumentException(fileName + ": not a regular file");
      }
    }
  }
}
