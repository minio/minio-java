/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Identifies and stores version information of minio-java package at run time. */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "MS_EXPOSE_REP")
public enum MinioProperties {
  INSTANCE;

  private static final Logger LOGGER = Logger.getLogger(MinioProperties.class.getName());

  private final AtomicReference<String> version = new AtomicReference<>(null);

  public String getVersion() {
    String result = version.get();
    if (result != null) {
      return result;
    }
    setVersion();
    return version.get();
  }

  private synchronized void setVersion() {
    if (version.get() != null) {
      return;
    }
    version.set("dev");
    ClassLoader classLoader = getClass().getClassLoader();
    if (classLoader == null) {
      return;
    }

    try {
      Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        try (InputStream is = resources.nextElement().openStream()) {
          Manifest manifest = new Manifest(is);
          if ("minio".equals(manifest.getMainAttributes().getValue("Implementation-Title"))) {
            version.set(manifest.getMainAttributes().getValue("Implementation-Version"));
            return;
          }
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "IOException occurred", e);
      version.set("unknown");
    }
  }

  public String getDefaultUserAgent() {
    return "MinIO ("
        + System.getProperty("os.name")
        + "; "
        + System.getProperty("os.arch")
        + ") minio-java/"
        + getVersion();
  }
}
