/*
 * Minimalist Object Storage Java Client, (C) 2015 Minio, Inc.
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

import com.google.api.client.xml.Xml;
import com.google.api.client.xml.XmlNamespaceDictionary;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SerializationTests {
    @Test
    public void testListObjectsResponse() throws XmlPullParserException, IOException {

        Owner owner = new Owner();
        owner.setID("id");
        owner.setDisplayName("displayName");

        Item item = new Item();
        item.setKey("key");
        item.setLastModified("modified");
        item.setSize(0);
        item.setStorageClass("storageClass");
        item.setETag("ETag");
        item.setOwner(owner);

        List<Item> items = new LinkedList<>();
        items.add(item);

        ListBucketResult result = new ListBucketResult();
        result.setName("name");
        result.setPrefix("prefix");
        result.setMarker("marker");
        result.setMaxKeys(5);
        result.setDelimiter("delimiter");
        result.setIsTruncated(true);
        result.setContents(items);


        System.out.println(item.toString());

        XmlPullParser parser = Xml.createParser();
        parser.setInput(new StringReader(result.toString()));
        XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
        ListBucketResult parsedItem = new ListBucketResult();
        Xml.parseElement(parser, parsedItem, dictionary, null);
        assertEquals(result, parsedItem);
    }

    @Test
    public void testPrefix() throws XmlPullParserException, IOException {
        Prefix prefix = new Prefix();
        prefix.setPrefix("hello");
        XmlPullParser parser = Xml.createParser();
        parser.setInput(new StringReader(prefix.toString()));
        XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
        Prefix parsedPrefix = new Prefix();
        Xml.parseElement(parser, parsedPrefix, dictionary, null);
        assertEquals(prefix, parsedPrefix);
    }
}