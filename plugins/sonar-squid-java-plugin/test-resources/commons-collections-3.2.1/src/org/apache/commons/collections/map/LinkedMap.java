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
package org.apache.commons.collections.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.collections.iterators.UnmodifiableIterator;
import org.apache.commons.collections.iterators.UnmodifiableListIterator;
import org.apache.commons.collections.list.UnmodifiableList;

/**
 * A <code>Map</code> implementation that maintains the order of the entries.
 * In this implementation order is maintained by original insertion.
 * <p>
 * This implementation improves on the JDK1.4 LinkedHashMap by adding the 
 * {@link org.apache.commons.collections.MapIterator MapIterator}
 * functionality, additional convenience methods and allowing
 * bidirectional iteration. It also implements <code>OrderedMap</code>.
 * In addition, non-interface methods are provided to access the map by index.
 * <p>
 * The <code>orderedMapIterator()</code> method provides direct access to a
 * bidirectional iterator. The iterators from the other views can also be cast
 * to <code>OrderedIterator</code> if required.
 * <p>
 * All the available iterators can be reset back to the start by casting to
 * <code>ResettableIterator</code> and calling <code>reset()</code>.
 * <p>
 * The implementation is also designed to be subclassed, with lots of useful
 * methods exposed.
 * <p>
 * <strong>Note that LinkedMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap(Map)}. This class may throw 
 * exceptions when accessed by concurrent threads without synchronization.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class LinkedMap
        extends AbstractLinkedMap implements Serializable, Cloneable {

    /** Serialisation version */
    private static final long serialVersionUID = 9077234323521161066L;
    
    /**
     * Constructs a new empty map with default size and load factor.
     */
    public LinkedMap() {
        super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity. 
     *
     * @param initialCapacity  the initial capacity
     * @throws IllegalArgumentException if the initial capacity is less than one
     */
    public LinkedMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity and
     * load factor. 
     *
     * @param initialCapacity  the initial capacity
     * @param loadFactor  the load factor
     * @throws IllegalArgumentException if the initial capacity is less than one
     * @throws IllegalArgumentException if the load factor is less than zero
     */
    public LinkedMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor copying elements from another map.
     *
     * @param map  the map to copy
     * @throws NullPointerException if the map is null
     */
    public LinkedMap(Map map) {
        super(map);
    }

    //-----------------------------------------------------------------------
    /**
     * Clones the map without cloning the keys or values.
     *
     * @return a shallow clone
     */
    public Object clone() {
        return super.clone();
    }
    
    /**
     * Write the map out using a custom routine.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        doWriteObject(out);
    }

    /**
     * Read the map in using a custom routine.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        doReadObject(in);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Gets the key at the specified index.
     * 
     * @param index  the index to retrieve
     * @return the key at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public Object get(int index) {
        return getEntry(index).getKey();
    }
    
    /**
     * Gets the value at the specified index.
     * 
     * @param index  the index to retrieve
     * @return the key at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public Object getValue(int index) {
        return getEntry(index).getValue();
    }
    
    /**
     * Gets the index of the specified key.
     * 
     * @param key  the key to find the index of
     * @return the index, or -1 if not found
     */
    public int indexOf(Object key) {
        key = convertKey(key);
        int i = 0;
        for (LinkEntry entry = header.after; entry != header; entry = entry.after, i++) {
            if (isEqualKey(key, entry.key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes the element at the specified index.
     *
     * @param index  the index of the object to remove
     * @return the previous value corresponding the <code>key</code>,
     *  or <code>null</code> if none existed
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public Object remove(int index) {
        return remove(get(index));
    }

    /**
     * Gets an unmodifiable List view of the keys.
     * <p>
     * The returned list is unmodifiable because changes to the values of
     * the list (using {@link java.util.ListIterator#set(Object)}) will
     * effectively remove the value from the list and reinsert that value at
     * the end of the list, which is an unexpected side effect of changing the
     * value of a list.  This occurs because changing the key, changes when the
     * mapping is added to the map and thus where it appears in the list.
     * <p>
     * An alternative to this method is to use {@link #keySet()}.
     *
     * @see #keySet()
     * @return The ordered list of keys.  
     */
    public List asList() {
        return new LinkedMapList(this);
    }

    /**
     * List view of map.
     */
    static class LinkedMapList extends AbstractList {
        
        final LinkedMap parent;
        
        LinkedMapList(LinkedMap parent) {
            this.parent = parent;
        }
        
        public int size() {
            return parent.size();
        }
    
        public Object get(int index) {
            return parent.get(index);
        }
        
        public boolean contains(Object obj) {
            return parent.containsKey(obj);
        }

        public int indexOf(Object obj) {
            return parent.indexOf(obj);
        }
        
        public int lastIndexOf(Object obj) {
            return parent.indexOf(obj);
        }
        
        public boolean containsAll(Collection coll) {
            return parent.keySet().containsAll(coll);
        }
        
        public Object remove(int index) {
            throw new UnsupportedOperationException();
        }
        
        public boolean remove(Object obj) {
            throw new UnsupportedOperationException();
        }
        
        public boolean removeAll(Collection coll) {
            throw new UnsupportedOperationException();
        }
        
        public boolean retainAll(Collection coll) {
            throw new UnsupportedOperationException();
        }
        
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        public Object[] toArray() {
            return parent.keySet().toArray();
        }

        public Object[] toArray(Object[] array) {
            return parent.keySet().toArray(array);
        }
        
        public Iterator iterator() {
            return UnmodifiableIterator.decorate(parent.keySet().iterator());
        }
        
        public ListIterator listIterator() {
            return UnmodifiableListIterator.decorate(super.listIterator());
        }
        
        public ListIterator listIterator(int fromIndex) {
            return UnmodifiableListIterator.decorate(super.listIterator(fromIndex));
        }
        
        public List subList(int fromIndexInclusive, int toIndexExclusive) {
            return UnmodifiableList.decorate(super.subList(fromIndexInclusive, toIndexExclusive));
        }
    }
    
}
