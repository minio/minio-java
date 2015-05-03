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

import static org.junit.Assert.assertEquals;

public class MessageTests {
    @Test
    public void testSerializeMessage() throws XmlPullParserException, IOException {
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

        XmlPullParser parser = Xml.createParser();
        parser.setInput(new StringReader(item.toString()));
        XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
        Item parsedItem = new Item();
        Xml.parseElement(parser, parsedItem, dictionary, null);
        assertEquals(item, parsedItem);
    }
}