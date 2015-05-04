/*
 * Minimal object storage library, (C) 2015 Minio, Inc.
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

package io.minio.objectstorage.client;

import java.util.Date;

public class ObjectMetadata {
    private final String bucket;
    private final String key;
    private final Date createdTime;
    private final long length;

    public ObjectMetadata(String bucket, String name, Date createdTime, long length) {
        this.bucket = bucket;
        this.key = name;
        this.createdTime = (Date)createdTime.clone();
        this.length = length;
    }

    public String getKey() {
        return key;
    }

    public Date getCreatedTime() {
        return (Date) createdTime.clone();
    }

    public long getLength() {
        return length;
    }

    public String getBucket() {
        return bucket;
    }
}
