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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Identifies and stores version information of minio-java package at run time.
 */
enum MinioProperties {
  INSTANCE;

  private static final Logger LOGGER = Logger.getLogger(MinioProperties.class.getName());

  private final AtomicReference<String> version = new AtomicReference<>(null);

  public String getVersion() {
    String result = version.get();
    if (result == null) {
      synchronized (INSTANCE) {
        if (version.get() == null) {
          try {
            ClassLoader classLoader = getClass().getClassLoader();
            setMinioClientJavaVersion(classLoader);
            setDevelopmentVersion();
          } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occured", e);
            version.set("unknown");
          }
          result = version.get();
        }
      }
    }
    return result;
  }

  private void setDevelopmentVersion() {
    if (version.get() == null) {
      version.set("dev");
    }
  }

  private void setMinioClientJavaVersion(ClassLoader classLoader) throws IOException {
    if (classLoader != null) {
      Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        Manifest manifest = new Manifest(resources.nextElement().openStream());
        for (Object k : manifest.getMainAttributes().keySet()) {
          String versionString = "Minio-Client-Java-Version";
          if (k.toString().equals(versionString)) {
            version.set(manifest.getMainAttributes().getValue((Attributes.Name) k));
          }
        }
      }
    }
  }
}
