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

import java.util.Date;

@SuppressWarnings("unused")
public class ObjectMetadata {
    private final String bucket;
    private final String key;
    private final Date createdTime;
    private final long length;
    private final String md5sum;

    public ObjectMetadata(String bucket, String name, Date createdTime, long length, String md5sum) {
        this.bucket = bucket;
        this.key = name;
        this.createdTime = (Date) createdTime.clone();
        this.length = length;
        this.md5sum = md5sum;
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

    public String getMd5sum() {
        return md5sum;
    }

    @Override
    public String toString() {
        return "ObjectMetadata{" +
                "bucket='" + bucket + '\'' +
                ", key='" + key + '\'' +
                ", createdTime=" + createdTime +
                ", length=" + length +
                ", md5sum='" + md5sum + '\'' +
                '}';
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectMetadata that = (ObjectMetadata) o;

        if (length != that.length) return false;
        if (!bucket.equals(that.bucket)) return false;
        if (!key.equals(that.key)) return false;
        if (!createdTime.equals(that.createdTime)) return false;
        if (!md5sum.equals(that.md5sum)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = bucket.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + createdTime.hashCode();
        result = 31 * result + (int) (length ^ (length >>> 32));
        result = 31 * result + md5sum.hashCode();
        return result;
    }
}
