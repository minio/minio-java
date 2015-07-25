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

package io.minio.client.messages;

import com.google.api.client.util.Key;

@SuppressWarnings({"SameParameterValue", "unused"})
public class InitiateMultipartUploadResult extends XmlEntity {
    @Key("Bucket")
    private String bucket;
    @Key("Key")
    private String key;
    @Key("UploadId")
    private String UploadId;

    public InitiateMultipartUploadResult() {
        super();
        this.name = "InitiateMultipartUploadResult";
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUploadId() {
        return UploadId;
    }

    public void setUploadId(String uploadId) {
        UploadId = uploadId;
    }
}
