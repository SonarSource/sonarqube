/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections.list;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections.BoundedCollection;
import org.apache.commons.collections.iterators.AbstractListIteratorDecorator;
import org.apache.commons.collections.iterators.UnmodifiableIterator;

/**
 * Decorates another <code>List</code> to fix the size preventing add/remove.
 * <p>
 * The add, remove, clear and retain operations are unsupported.
 * The set method is allowed (as it doesn't change the list size).
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 * @author Paul Jack
 */
public class FixedSizeList
        extends AbstractSerializableListDecorator
        implements BoundedCollection {

    /** Serialization version */
    private static final long serialVersionUID = -2218010673611160319L;

    /**
     * Factory method to create a fixed size list.
     * 
     * @param list  the list to decorate, must not be null
     * @throws IllegalArgumentException if list is null
     */
    public static List decorate(List list) {
        return new FixedSizeList(list);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * 
     * @param list  the list to decorate, must not be null
     * @throws IllegalArgumentException if list is null
     */
    protected FixedSizeList(List list) {
        super(list);
    }

    //-----------------------------------------------------------------------
    public boolean add(Object object) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public void add(int index, Object object) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public boolean addAll(int index, Collection coll) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public void clear() {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public Object get(int index) {
        return getList().get(index);
    }

    public int indexOf(Object object) {
        return getList().indexOf(object);
    }

    public Iterator iterator() {
        return UnmodifiableIterator.decorate(getCollection().iterator());
    }

    public int lastIndexOf(Object object) {
        return getList().lastIndexOf(object);
    }

    public ListIterator listIterator() {
        return new FixedSizeListIterator(getList().listIterator(0));
    }

    public ListIterator listIterator(int index) {
        return new FixedSizeListIterator(getList().listIterator(index));
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public boolean remove(Object object) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException("List is fixed size");
    }

    public Object set(int index, Object object) {
        return getList().set(index, object);
    }

    public List subList(int fromIndex, int toIndex) {
        List sub = getList().subList(fromIndex, toIndex);
        return new FixedSizeList(sub);
    }

    /**
     * List iterator that only permits changes via set()
     */
    static class FixedSizeListIterator extends AbstractListIteratorDecorator {
        protected FixedSizeListIterator(ListIterator iterator) {
            super(iterator);
        }
        public void remove() {
            throw new UnsupportedOperationException("List is fixed size");
        }
        public void add(Object object) {
            throw new UnsupportedOperationException("List is fixed size");
        }
    }

    public boolean isFull() {
        return true;
    }

    public int maxSize() {
        return size();
    }

}
