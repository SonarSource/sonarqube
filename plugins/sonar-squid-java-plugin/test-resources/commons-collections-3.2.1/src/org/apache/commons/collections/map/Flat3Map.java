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
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.IterableMap;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.ResettableIterator;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.EmptyMapIterator;

/**
 * A <code>Map</code> implementation that stores data in simple fields until
 * the size is greater than 3.
 * <p>
 * This map is designed for performance and can outstrip HashMap.
 * It also has good garbage collection characteristics.
 * <ul>
 * <li>Optimised for operation at size 3 or less.
 * <li>Still works well once size 3 exceeded.
 * <li>Gets at size 3 or less are about 0-10% faster than HashMap,
 * <li>Puts at size 3 or less are over 4 times faster than HashMap.
 * <li>Performance 5% slower than HashMap once size 3 exceeded once.
 * </ul>
 * The design uses two distinct modes of operation - flat and delegate.
 * While the map is size 3 or less, operations map straight onto fields using
 * switch statements. Once size 4 is reached, the map switches to delegate mode
 * and only switches back when cleared. In delegate mode, all operations are
 * forwarded straight to a HashMap resulting in the 5% performance loss.
 * <p>
 * The performance gains on puts are due to not needing to create a Map Entry
 * object. This is a large saving not only in performance but in garbage collection.
 * <p>
 * Whilst in flat mode this map is also easy for the garbage collector to dispatch.
 * This is because it contains no complex objects or arrays which slow the progress.
 * <p>
 * Do not use <code>Flat3Map</code> if the size is likely to grow beyond 3.
 * <p>
 * <strong>Note that Flat3Map is not synchronized and is not thread-safe.</strong>
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
public class Flat3Map implements IterableMap, Serializable, Cloneable {

    /** Serialization version */
    private static final long serialVersionUID = -6701087419741928296L;

    /** The size of the map, used while in flat mode */
    private transient int size;
    /** Hash, used while in flat mode */
    private transient int hash1;
    /** Hash, used while in flat mode */
    private transient int hash2;
    /** Hash, used while in flat mode */
    private transient int hash3;
    /** Key, used while in flat mode */
    private transient Object key1;
    /** Key, used while in flat mode */
    private transient Object key2;
    /** Key, used while in flat mode */
    private transient Object key3;
    /** Value, used while in flat mode */
    private transient Object value1;
    /** Value, used while in flat mode */
    private transient Object value2;
    /** Value, used while in flat mode */
    private transient Object value3;
    /** Map, used while in delegate mode */
    private transient AbstractHashedMap delegateMap;

    /**
     * Constructor.
     */
    public Flat3Map() {
        super();
    }

    /**
     * Constructor copying elements from another map.
     *
     * @param map  the map to copy
     * @throws NullPointerException if the map is null
     */
    public Flat3Map(Map map) {
        super();
        putAll(map);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the value mapped to the key specified.
     * 
     * @param key  the key
     * @return the mapped value, null if no match
     */
    public Object get(Object key) {
        if (delegateMap != null) {
            return delegateMap.get(key);
        }
        if (key == null) {
            switch (size) {
                // drop through
                case 3:
                    if (key3 == null) return value3;
                case 2:
                    if (key2 == null) return value2;
                case 1:
                    if (key1 == null) return value1;
            }
        } else {
            if (size > 0) {
                int hashCode = key.hashCode();
                switch (size) {
                    // drop through
                    case 3:
                        if (hash3 == hashCode && key.equals(key3)) return value3;
                    case 2:
                        if (hash2 == hashCode && key.equals(key2)) return value2;
                    case 1:
                        if (hash1 == hashCode && key.equals(key1)) return value1;
                }
            }
        }
        return null;
    }

    /**
     * Gets the size of the map.
     * 
     * @return the size
     */
    public int size() {
        if (delegateMap != null) {
            return delegateMap.size();
        }
        return size;
    }

    /**
     * Checks whether the map is currently empty.
     * 
     * @return true if the map is currently size zero
     */
    public boolean isEmpty() {
        return (size() == 0);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks whether the map contains the specified key.
     * 
     * @param key  the key to search for
     * @return true if the map contains the key
     */
    public boolean containsKey(Object key) {
        if (delegateMap != null) {
            return delegateMap.containsKey(key);
        }
        if (key == null) {
            switch (size) {  // drop through
                case 3:
                    if (key3 == null) return true;
                case 2:
                    if (key2 == null) return true;
                case 1:
                    if (key1 == null) return true;
            }
        } else {
            if (size > 0) {
                int hashCode = key.hashCode();
                switch (size) {  // drop through
                    case 3:
                        if (hash3 == hashCode && key.equals(key3)) return true;
                    case 2:
                        if (hash2 == hashCode && key.equals(key2)) return true;
                    case 1:
                        if (hash1 == hashCode && key.equals(key1)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the map contains the specified value.
     * 
     * @param value  the value to search for
     * @return true if the map contains the key
     */
    public boolean containsValue(Object value) {
        if (delegateMap != null) {
            return delegateMap.containsValue(value);
        }
        if (value == null) {  // drop through
            switch (size) {
                case 3:
                    if (value3 == null) return true;
                case 2:
                    if (value2 == null) return true;
                case 1:
                    if (value1 == null) return true;
            }
        } else {
            switch (size) {  // drop through
                case 3:
                    if (value.equals(value3)) return true;
                case 2:
                    if (value.equals(value2)) return true;
                case 1:
                    if (value.equals(value1)) return true;
            }
        }
        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Puts a key-value mapping into this map.
     * 
     * @param key  the key to add
     * @param value  the value to add
     * @return the value previously mapped to this key, null if none
     */
    public Object put(Object key, Object value) {
        if (delegateMap != null) {
            return delegateMap.put(key, value);
        }
        // change existing mapping
        if (key == null) {
            switch (size) {  // drop through
                case 3:
                    if (key3 == null) {
                        Object old = value3;
                        value3 = value;
                        return old;
                    }
                case 2:
                    if (key2 == null) {
                        Object old = value2;
                        value2 = value;
                        return old;
                    }
                case 1:
                    if (key1 == null) {
                        Object old = value1;
                        value1 = value;
                        return old;
                    }
            }
        } else {
            if (size > 0) {
                int hashCode = key.hashCode();
                switch (size) {  // drop through
                    case 3:
                        if (hash3 == hashCode && key.equals(key3)) {
                            Object old = value3;
                            value3 = value;
                            return old;
                        }
                    case 2:
                        if (hash2 == hashCode && key.equals(key2)) {
                            Object old = value2;
                            value2 = value;
                            return old;
                        }
                    case 1:
                        if (hash1 == hashCode && key.equals(key1)) {
                            Object old = value1;
                            value1 = value;
                            return old;
                        }
                }
            }
        }
        
        // add new mapping
        switch (size) {
            default:
                convertToMap();
                delegateMap.put(key, value);
                return null;
            case 2:
                hash3 = (key == null ? 0 : key.hashCode());
                key3 = key;
                value3 = value;
                break;
            case 1:
                hash2 = (key == null ? 0 : key.hashCode());
                key2 = key;
                value2 = value;
                break;
            case 0:
                hash1 = (key == null ? 0 : key.hashCode());
                key1 = key;
                value1 = value;
                break;
        }
        size++;
        return null;
    }

    /**
     * Puts all the values from the specified map into this map.
     * 
     * @param map  the map to add
     * @throws NullPointerException if the map is null
     */
    public void putAll(Map map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        if (delegateMap != null) {
            delegateMap.putAll(map);
            return;
        }
        if (size < 4) {
            for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                put(entry.getKey(), entry.getValue());
            }
        } else {
            convertToMap();
            delegateMap.putAll(map);
        }
    }

    /**
     * Converts the flat map data to a map.
     */
    private void convertToMap() {
        delegateMap = createDelegateMap();
        switch (size) {  // drop through
            case 3:
                delegateMap.put(key3, value3);
            case 2:
                delegateMap.put(key2, value2);
            case 1:
                delegateMap.put(key1, value1);
        }
        
        size = 0;
        hash1 = hash2 = hash3 = 0;
        key1 = key2 = key3 = null;
        value1 = value2 = value3 = null;
    }

    /**
     * Create an instance of the map used for storage when in delegation mode.
     * <p>
     * This can be overridden by subclasses to provide a different map implementation.
     * Not every AbstractHashedMap is suitable, identity and reference based maps
     * would be poor choices.
     *
     * @return a new AbstractHashedMap or subclass
     * @since Commons Collections 3.1
     */
    protected AbstractHashedMap createDelegateMap() {
        return new HashedMap();
    }

    /**
     * Removes the specified mapping from this map.
     * 
     * @param key  the mapping to remove
     * @return the value mapped to the removed key, null if key not in map
     */
    public Object remove(Object key) {
        if (delegateMap != null) {
            return delegateMap.remove(key);
        }
        if (size == 0) {
            return null;
        }
        if (key == null) {
            switch (size) {  // drop through
                case 3:
                    if (key3 == null) {
                        Object old = value3;
                        hash3 = 0;
                        key3 = null;
                        value3 = null;
                        size = 2;
                        return old;
                    }
                    if (key2 == null) {
                        Object old = value3;
                        hash2 = hash3;
                        key2 = key3;
                        value2 = value3;
                        hash3 = 0;
                        key3 = null;
                        value3 = null;
                        size = 2;
                        return old;
                    }
                    if (key1 == null) {
                        Object old = value3;
                        hash1 = hash3;
                        key1 = key3;
                        value1 = value3;
                        hash3 = 0;
                        key3 = null;
                        value3 = null;
                        size = 2;
                        return old;
                    }
                    return null;
                case 2:
                    if (key2 == null) {
                        Object old = value2;
                        hash2 = 0;
                        key2 = null;
                        value2 = null;
                        size = 1;
                        return old;
                    }
                    if (key1 == null) {
                        Object old = value2;
                        hash1 = hash2;
                        key1 = key2;
                        value1 = value2;
                        hash2 = 0;
                        key2 = null;
                        value2 = null;
                        size = 1;
                        return old;
                    }
                    return null;
                case 1:
                    if (key1 == null) {
                        Object old = value1;
                        hash1 = 0;
                        key1 = null;
                        value1 = null;
                        size = 0;
                        return old;
                    }
            }
        } else {
            if (size > 0) {
                int hashCode = key.hashCode();
                switch (size) {  // drop through
                    case 3:
                        if (hash3 == hashCode && key.equals(key3)) {
                            Object old = value3;
                            hash3 = 0;
                            key3 = null;
                            value3 = null;
                            size = 2;
                            return old;
                        }
                        if (hash2 == hashCode && key.equals(key2)) {
                            Object old = value3;
                            hash2 = hash3;
                            key2 = key3;
                            value2 = value3;
                            hash3 = 0;
                            key3 = null;
                            value3 = null;
                            size = 2;
                            return old;
                        }
                        if (hash1 == hashCode && key.equals(key1)) {
                            Object old = value3;
                            hash1 = hash3;
                            key1 = key3;
                            value1 = value3;
                            hash3 = 0;
                            key3 = null;
                            value3 = null;
                            size = 2;
                            return old;
                        }
                        return null;
                    case 2:
                        if (hash2 == hashCode && key.equals(key2)) {
                            Object old = value2;
                            hash2 = 0;
                            key2 = null;
                            value2 = null;
                            size = 1;
                            return old;
                        }
                        if (hash1 == hashCode && key.equals(key1)) {
                            Object old = value2;
                            hash1 = hash2;
                            key1 = key2;
                            value1 = value2;
                            hash2 = 0;
                            key2 = null;
                            value2 = null;
                            size = 1;
                            return old;
                        }
                        return null;
                    case 1:
                        if (hash1 == hashCode && key.equals(key1)) {
                            Object old = value1;
                            hash1 = 0;
                            key1 = null;
                            value1 = null;
                            size = 0;
                            return old;
                        }
                }
            }
        }
        return null;
    }

    /**
     * Clears the map, resetting the size to zero and nullifying references
     * to avoid garbage collection issues.
     */
    public void clear() {
        if (delegateMap != null) {
            delegateMap.clear();  // should aid gc
            delegateMap = null;  // switch back to flat mode
        } else {
            size = 0;
            hash1 = hash2 = hash3 = 0;
            key1 = key2 = key3 = null;
            value1 = value2 = value3 = null;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Gets an iterator over the map.
     * Changes made to the iterator affect this map.
     * <p>
     * A MapIterator returns the keys in the map. It also provides convenient
     * methods to get the key and value, and set the value.
     * It avoids the need to create an entrySet/keySet/values object.
     * It also avoids creating the Map Entry object.
     * 
     * @return the map iterator
     */
    public MapIterator mapIterator() {
        if (delegateMap != null) {
            return delegateMap.mapIterator();
        }
        if (size == 0) {
            return EmptyMapIterator.INSTANCE;
        }
        return new FlatMapIterator(this);
    }

    /**
     * FlatMapIterator
     */
    static class FlatMapIterator implements MapIterator, ResettableIterator {
        private final Flat3Map parent;
        private int nextIndex = 0;
        private boolean canRemove = false;
        
        FlatMapIterator(Flat3Map parent) {
            super();
            this.parent = parent;
        }

        public boolean hasNext() {
            return (nextIndex < parent.size);
        }

        public Object next() {
            if (hasNext() == false) {
                throw new NoSuchElementException(AbstractHashedMap.NO_NEXT_ENTRY);
            }
            canRemove = true;
            nextIndex++;
            return getKey();
        }

        public void remove() {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.REMOVE_INVALID);
            }
            parent.remove(getKey());
            nextIndex--;
            canRemove = false;
        }

        public Object getKey() {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.GETKEY_INVALID);
            }
            switch (nextIndex) {
                case 3:
                    return parent.key3;
                case 2:
                    return parent.key2;
                case 1:
                    return parent.key1;
            }
            throw new IllegalStateException("Invalid map index");
        }

        public Object getValue() {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.GETVALUE_INVALID);
            }
            switch (nextIndex) {
                case 3:
                    return parent.value3;
                case 2:
                    return parent.value2;
                case 1:
                    return parent.value1;
            }
            throw new IllegalStateException("Invalid map index");
        }

        public Object setValue(Object value) {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.SETVALUE_INVALID);
            }
            Object old = getValue();
            switch (nextIndex) {
                case 3: 
                    parent.value3 = value;
                case 2:
                    parent.value2 = value;
                case 1:
                    parent.value1 = value;
            }
            return old;
        }
        
        public void reset() {
            nextIndex = 0;
            canRemove = false;
        }
        
        public String toString() {
            if (canRemove) {
                return "Iterator[" + getKey() + "=" + getValue() + "]";
            } else {
                return "Iterator[]";
            }
        }
    }
    
    /**
     * Gets the entrySet view of the map.
     * Changes made to the view affect this map.
     * The Map Entry is not an independent object and changes as the 
     * iterator progresses.
     * To simply iterate through the entries, use {@link #mapIterator()}.
     * 
     * @return the entrySet view
     */
    public Set entrySet() {
        if (delegateMap != null) {
            return delegateMap.entrySet();
        }
        return new EntrySet(this);
    }
    
    /**
     * EntrySet
     */
    static class EntrySet extends AbstractSet {
        private final Flat3Map parent;
        
        EntrySet(Flat3Map parent) {
            super();
            this.parent = parent;
        }

        public int size() {
            return parent.size();
        }
        
        public void clear() {
            parent.clear();
        }
        
        public boolean remove(Object obj) {
            if (obj instanceof Map.Entry == false) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            boolean result = parent.containsKey(key);
            parent.remove(key);
            return result;
        }

        public Iterator iterator() {
            if (parent.delegateMap != null) {
                return parent.delegateMap.entrySet().iterator();
            }
            if (parent.size() == 0) {
                return EmptyIterator.INSTANCE;
            }
            return new EntrySetIterator(parent);
        }
    }

    /**
     * EntrySetIterator and MapEntry
     */
    static class EntrySetIterator implements Iterator, Map.Entry {
        private final Flat3Map parent;
        private int nextIndex = 0;
        private boolean canRemove = false;
        
        EntrySetIterator(Flat3Map parent) {
            super();
            this.parent = parent;
        }

        public boolean hasNext() {
            return (nextIndex < parent.size);
        }

        public Object next() {
            if (hasNext() == false) {
                throw new NoSuchElementException(AbstractHashedMap.NO_NEXT_ENTRY);
            }
            canRemove = true;
            nextIndex++;
            return this;
        }

        public void remove() {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.REMOVE_INVALID);
            }
            parent.remove(getKey());
            nextIndex--;
            canRemove = false;
        }

        public Object getKey() {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.GETKEY_INVALID);
            }
            switch (nextIndex) {
                case 3:
                    return parent.key3;
                case 2:
                    return parent.key2;
                case 1:
                    return parent.key1;
            }
            throw new IllegalStateException("Invalid map index");
        }

        public Object getValue() {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.GETVALUE_INVALID);
            }
            switch (nextIndex) {
                case 3:
                    return parent.value3;
                case 2:
                    return parent.value2;
                case 1:
                    return parent.value1;
            }
            throw new IllegalStateException("Invalid map index");
        }

        public Object setValue(Object value) {
            if (canRemove == false) {
                throw new IllegalStateException(AbstractHashedMap.SETVALUE_INVALID);
            }
            Object old = getValue();
            switch (nextIndex) {
                case 3: 
                    parent.value3 = value;
                case 2:
                    parent.value2 = value;
                case 1:
                    parent.value1 = value;
            }
            return old;
        }
        
        public boolean equals(Object obj) {
            if (canRemove == false) {
                return false;
            }
            if (obj instanceof Map.Entry == false) {
                return false;
            }
            Map.Entry other = (Map.Entry) obj;
            Object key = getKey();
            Object value = getValue();
            return (key == null ? other.getKey() == null : key.equals(other.getKey())) &&
                   (value == null ? other.getValue() == null : value.equals(other.getValue()));
        }
        
        public int hashCode() {
            if (canRemove == false) {
                return 0;
            }
            Object key = getKey();
            Object value = getValue();
            return (key == null ? 0 : key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }
        
        public String toString() {
            if (canRemove) {
                return getKey() + "=" + getValue();
            } else {
                return "";
            }
        }
    }
    
    /**
     * Gets the keySet view of the map.
     * Changes made to the view affect this map.
     * To simply iterate through the keys, use {@link #mapIterator()}.
     * 
     * @return the keySet view
     */
    public Set keySet() {
        if (delegateMap != null) {
            return delegateMap.keySet();
        }
        return new KeySet(this);
    }

    /**
     * KeySet
     */
    static class KeySet extends AbstractSet {
        private final Flat3Map parent;
        
        KeySet(Flat3Map parent) {
            super();
            this.parent = parent;
        }

        public int size() {
            return parent.size();
        }
        
        public void clear() {
            parent.clear();
        }
        
        public boolean contains(Object key) {
            return parent.containsKey(key);
        }

        public boolean remove(Object key) {
            boolean result = parent.containsKey(key);
            parent.remove(key);
            return result;
        }

        public Iterator iterator() {
            if (parent.delegateMap != null) {
                return parent.delegateMap.keySet().iterator();
            }
            if (parent.size() == 0) {
                return EmptyIterator.INSTANCE;
            }
            return new KeySetIterator(parent);
        }
    }

    /**
     * KeySetIterator
     */
    static class KeySetIterator extends EntrySetIterator {
        
        KeySetIterator(Flat3Map parent) {
            super(parent);
        }

        public Object next() {
            super.next();
            return getKey();
        }
    }
    
    /**
     * Gets the values view of the map.
     * Changes made to the view affect this map.
     * To simply iterate through the values, use {@link #mapIterator()}.
     * 
     * @return the values view
     */
    public Collection values() {
        if (delegateMap != null) {
            return delegateMap.values();
        }
        return new Values(this);
    }

    /**
     * Values
     */
    static class Values extends AbstractCollection {
        private final Flat3Map parent;
        
        Values(Flat3Map parent) {
            super();
            this.parent = parent;
        }

        public int size() {
            return parent.size();
        }
        
        public void clear() {
            parent.clear();
        }
        
        public boolean contains(Object value) {
            return parent.containsValue(value);
        }

        public Iterator iterator() {
            if (parent.delegateMap != null) {
                return parent.delegateMap.values().iterator();
            }
            if (parent.size() == 0) {
                return EmptyIterator.INSTANCE;
            }
            return new ValuesIterator(parent);
        }
    }

    /**
     * ValuesIterator
     */
    static class ValuesIterator extends EntrySetIterator {
        
        ValuesIterator(Flat3Map parent) {
            super(parent);
        }

        public Object next() {
            super.next();
            return getValue();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Write the map out using a custom routine.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(size());
        for (MapIterator it = mapIterator(); it.hasNext();) {
            out.writeObject(it.next());  // key
            out.writeObject(it.getValue());  // value
        }
    }

    /**
     * Read the map in using a custom routine.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int count = in.readInt();
        if (count > 3) {
            delegateMap = createDelegateMap();
        }
        for (int i = count; i > 0; i--) {
            put(in.readObject(), in.readObject());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Clones the map without cloning the keys or values.
     *
     * @return a shallow clone
     * @since Commons Collections 3.1
     */
    public Object clone() {
        try {
            Flat3Map cloned = (Flat3Map) super.clone();
            if (cloned.delegateMap != null) {
                cloned.delegateMap = (HashedMap) cloned.delegateMap.clone();
            }
            return cloned;
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    /**
     * Compares this map with another.
     * 
     * @param obj  the object to compare to
     * @return true if equal
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (delegateMap != null) {
            return delegateMap.equals(obj);
        }
        if (obj instanceof Map == false) {
            return false;
        }
        Map other = (Map) obj;
        if (size != other.size()) {
            return false;
        }
        if (size > 0) {
            Object otherValue = null;
            switch (size) {  // drop through
                case 3:
                    if (other.containsKey(key3) == false) {
                        return false;
                    }
                    otherValue = other.get(key3);
                    if (value3 == null ? otherValue != null : !value3.equals(otherValue)) {
                        return false;
                    }
                case 2:
                    if (other.containsKey(key2) == false) {
                        return false;
                    }
                    otherValue = other.get(key2);
                    if (value2 == null ? otherValue != null : !value2.equals(otherValue)) {
                        return false;
                    }
                case 1:
                    if (other.containsKey(key1) == false) {
                        return false;
                    }
                    otherValue = other.get(key1);
                    if (value1 == null ? otherValue != null : !value1.equals(otherValue)) {
                        return false;
                    }
            }
        }
        return true;
    }

    /**
     * Gets the standard Map hashCode.
     * 
     * @return the hash code defined in the Map interface
     */
    public int hashCode() {
        if (delegateMap != null) {
            return delegateMap.hashCode();
        }
        int total = 0;
        switch (size) {  // drop through
            case 3:
                total += (hash3 ^ (value3 == null ? 0 : value3.hashCode()));
            case 2:
                total += (hash2 ^ (value2 == null ? 0 : value2.hashCode()));
            case 1:
                total += (hash1 ^ (value1 == null ? 0 : value1.hashCode()));
        }
        return total;
    }

    /**
     * Gets the map as a String.
     * 
     * @return a string version of the map
     */
    public String toString() {
        if (delegateMap != null) {
            return delegateMap.toString();
        }
        if (size == 0) {
            return "{}";
        }
        StringBuffer buf = new StringBuffer(128);
        buf.append('{');
        switch (size) {  // drop through
            case 3:
                buf.append((key3 == this ? "(this Map)" : key3));
                buf.append('=');
                buf.append((value3 == this ? "(this Map)" : value3));
                buf.append(',');
            case 2:
                buf.append((key2 == this ? "(this Map)" : key2));
                buf.append('=');
                buf.append((value2 == this ? "(this Map)" : value2));
                buf.append(',');
            case 1:
                buf.append((key1 == this ? "(this Map)" : key1));
                buf.append('=');
                buf.append((value1 == this ? "(this Map)" : value1));
        }
        buf.append('}');
        return buf.toString();
    }

}
