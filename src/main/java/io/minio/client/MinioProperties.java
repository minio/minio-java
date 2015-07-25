/*
 * Minio Java Library for Amazon S3 compatible cloud storage, (C) 2015 Minio, Inc.
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

package io.minio.client;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

enum MinioProperties {
    INSTANCE;
    private final AtomicReference<String> version = new AtomicReference<String>(null);

    public String getVersion() {
        String result = version.get();
        if (result == null) {
            synchronized (INSTANCE) {
                if (version.get() == null) {
                    try {
                        ClassLoader classLoader = getClass().getClassLoader();
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
                        if (version.get() == null) {
                            version.set("dev");
                        }
                    } catch (IOException e) {
                        version.set("unknown");
                    }
                    result = version.get();
                }
            }
        }
        return result;
    }
}
