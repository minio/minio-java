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

package io.minio.objectstorage.client.messages;

import com.google.api.client.util.Key;

import java.util.List;

public class ListBucketResult extends XmlEntity {
    @Key("Name")
    private String Name;
    @Key("Prefix")
    private String Prefix;
    @Key("Marker")
    private String Marker;
    @Key("MaxKeys")
    private int MaxKeys;
    @Key("Delimiter")
    private String Delimiter;
    @Key("IsTruncated")
    private boolean IsTruncated;
    @Key("Contents")
    private List<Item> Contents;
    @Key("CommonPrefixes")
    private List<Prefix> CommonPrefixes;

    public ListBucketResult() {
        super.name = "ListBucketResult";
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPrefix() {
        return Prefix;
    }

    public void setPrefix(String prefix) {
        Prefix = prefix;
    }

    public String getMarker() {
        return Marker;
    }

    public void setMarker(String marker) {
        Marker = marker;
    }

    public int getMaxKeys() {
        return MaxKeys;
    }

    public void setMaxKeys(int maxKeys) {
        MaxKeys = maxKeys;
    }

    public String getDelimiter() {
        return Delimiter;
    }

    public void setDelimiter(String delimiter) {
        Delimiter = delimiter;
    }

    public boolean isTruncated() {
        return IsTruncated;
    }

    public void setIsTruncated(boolean isTruncated) {
        IsTruncated = isTruncated;
    }

    public List<Item> getContents() {
        return Contents;
    }

    public void setContents(List<Item> contents) {
        Contents = contents;
    }

    public List<Prefix> getCommonPrefixes() {
        return CommonPrefixes;
    }

    public void setCommonPrefixes(List<Prefix> commonPrefixes) {
        CommonPrefixes = commonPrefixes;
    }
}
