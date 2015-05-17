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

package io.minio.objectstorage.client;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

abstract class ListObjectsIterator<T> implements Iterator<T> {
    private List<T> items = new LinkedList<T>();

    @Override
    public boolean hasNext() {
        populateIfEmpty();
        return !items.isEmpty();
    }

    @Override
    public T next() {
        populateIfEmpty();
        if (items.isEmpty()) {
            throw new NoSuchElementException();
        }
        return items.remove(0);
    }

    private void populateIfEmpty() {
        if (items.isEmpty()) {
            List<T> list = populate();
            this.items.addAll(list);
        }
    }

    protected abstract List<T> populate();
}
