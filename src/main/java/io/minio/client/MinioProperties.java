/*
 * Minimal Object Storage Library, (C) 2015 Minio, Inc.
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
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Created by fkautz on 6/10/15.
 */
enum MinioProperties {
    INSTANCE;
    private static String versionString = "Minio-Client-Java-Version";
    String version = null;

    public String getVersion() {
        if (version == null) {
            synchronized (INSTANCE) {
                if (version == null) {
                    try {
                        Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
                        while (resources.hasMoreElements()) {
                            Manifest manifest = new Manifest(resources.nextElement().openStream());
                            for (Object k : manifest.getMainAttributes().keySet()) {
                                if (k.toString().equals(versionString)) {
                                    version = manifest.getMainAttributes().getValue((Attributes.Name) k);
                                }
                            }
                        }
                        if (version == null) {
                            version = "dev";
                        }
                    } catch (IOException e) {
                        version = "unknown";
                    }
                }
            }
        }
        return version;
    }
}
