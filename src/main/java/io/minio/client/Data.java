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

package io.minio.client;

import java.util.Arrays;

@SuppressWarnings("unused")
public class Data {
    private byte[] data;
    private byte[] md5;

    public Data() {
        super();
    }

    public byte[] getData() {
        return data.clone();
    }

    public void setData(byte[] data) {
        this.data = data.clone();
    }

    public byte[] getMD5() {
        return md5.clone();
    }

    public void setMD5(byte[] md5) {
        this.md5 = md5.clone();
    }

    @Override
    public String toString() {
        return "Data{" +
                "data='" + "**bytes**" + '\'' +
                ", md5='" + "**bytes**" +
                '}';
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data that = (Data) o;

        if (!Arrays.equals(data, that.data)) return false;
        if (!Arrays.equals(md5, that.md5)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(data);
        result = 31 * result + Arrays.hashCode(md5);
        return result;
    }
}
