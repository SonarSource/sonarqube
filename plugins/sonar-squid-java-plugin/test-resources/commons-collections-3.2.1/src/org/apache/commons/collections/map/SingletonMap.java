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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.BoundedMap;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.OrderedMapIterator;
import org.apache.commons.collections.ResettableIterator;
import org.apache.commons.collections.iterators.SingletonIterator;
import org.apache.commons.collections.keyvalue.TiedMapEntry;

/**
 * A <code>Map</code> implementation that holds a single item and is fixed size.
 * <p>
 * The single key/value pair is specified at creation.
 * The map is fixed size so any action that would change the size is disallowed.
 * However, the <code>put</code> or <code>setValue</code> methods can <i>change</i>
 * the value associated with the key.
 * <p>
 * If trying to remove or clear the map, an UnsupportedOperationException is thrown.
 * If trying to put a new mapping into the map, an  IllegalArgumentException is thrown.
 * The put method will only suceed if the key specified is the same as the 
 * singleton key.
 * <p>
 * The key and value can be obtained by:
 * <ul>
 * <li>normal Map methods and views
 * <li>the <code>MapIterator</code>, see {@link #mapIterator()}
 * <li>the <code>KeyValue</code> interface (just cast - no object creation)
 * </ul>
 * 
 * @since Commons Collections 3.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class SingletonMap
        implements OrderedMap, BoundedMap, KeyValue, Serializable, Cloneable {

    /** Serialization version */
    private static final long serialVersionUID = -8931271118676803261L;

    /** Singleton key */
    private final Object key;
    /** Singleton value */
    private Object value;

    /**
     * Constructor that creates a map of <code>null</code> to <code>null</code>.
     */
    public SingletonMap() {
        super();
        this.key = null;
    }

    /**
     * Constructor specifying the key and value.
     *
     * @param key  the key to use
     * @param value  the value to use
     */
    public SingletonMap(Object key, Object value) {
        super();
        this.key = key;
        this.value = value;
    }

    /**
     * Constructor specifying the key and value as a <code>KeyValue</code>.
     *
     * @param keyValue  the key value pair to use
     */
    public SingletonMap(KeyValue keyValue) {
        super();
        this.key = keyValue.getKey();
        this.value = keyValue.getValue();
    }

    /**
     * Constructor specifying the key and value as a <code>MapEntry</code>.
     *
     * @param mapEntry  the mapEntry to use
     */
    public SingletonMap(Map.Entry mapEntry) {
        super();
        this.key = mapEntry.getKey();
        this.value = mapEntry.getValue();
    }

    /**
     * Constructor copying elements from another map.
     *
     * @param map  the map to copy, must be size 1
     * @throws NullPointerException if the map is null
     * @throws IllegalArgumentException if the size is not 1
     */
    public SingletonMap(Map map) {
        super();
        if (map.size() != 1) {
            throw new IllegalArgumentException("The map size must be 1");
        }
        Map.Entry entry = (Map.Entry) map.entrySet().iterator().next();
        this.key = entry.getKey();
        this.value = entry.getValue();
    }

    // KeyValue
    //-----------------------------------------------------------------------
    /**
     * Gets the key.
     *
     * @return the key 
     */
    public Object getKey() {
        return key;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value  the new value to set
     * @return the old value
     */
    public Object setValue(Object value) {
        Object old = this.value;
        this.value = value;
        return old;
    }

    // BoundedMap
    //-----------------------------------------------------------------------
    /**
     * Is the map currently full, always true.
     *
     * @return true always
     */
    public boolean isFull() {
        return true;
    }

    /**
     * Gets the maximum size of the map, always 1.
     * 
     * @return 1 always
     */
    public int maxSize() {
        return 1;
    }

    // Map
    //-----------------------------------------------------------------------
    /**
     * Gets the value mapped to the key specified.
     * 
     * @param key  the key
     * @return the mapped value, null if no match
     */
    public Object get(Object key) {
        if (isEqualKey(key)) {
            return value;
        }
        return null;
    }

    /**
     * Gets the size of the map, always 1.
     * 
     * @return the size of 1
     */
    public int size() {
        return 1;
    }

    /**
     * Checks whether the map is currently empty, which it never is.
     * 
     * @return false always
     */
    public boolean isEmpty() {
        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks whether the map contains the specified key.
     * 
     * @param key  the key to search for
     * @return true if the map contains the key
     */
    public boolean containsKey(Object key) {
        return (isEqualKey(key));
    }

    /**
     * Checks whether the map contains the specified value.
     * 
     * @param value  the value to search for
     * @return true if the map contains the key
     */
    public boolean containsValue(Object value) {
        return (isEqualValue(value));
    }

    //-----------------------------------------------------------------------
    /**
     * Puts a key-value mapping into this map where the key must match the existing key.
     * <p>
     * An IllegalArgumentException is thrown if the key does not match as the map
     * is fixed size.
     * 
     * @param key  the key to set, must be the key of the map
     * @param value  the value to set
     * @return the value previously mapped to this key, null if none
     * @throws IllegalArgumentException if the key does not match
     */
    public Object put(Object key, Object value) {
        if (isEqualKey(key)) {
            return setValue(value);
        }
        throw new IllegalArgumentException("Cannot put new key/value pair - Map is fixed size singleton");
    }

    /**
     * Puts the values from the specified map into this map.
     * <p>
     * The map must be of size 0 or size 1.
     * If it is size 1, the key must match the key of this map otherwise an
     * IllegalArgumentException is thrown.
     * 
     * @param map  the map to add, must be size 0 or 1, and the key must match
     * @throws NullPointerException if the map is null
     * @throws IllegalArgumentException if the key does not match
     */
    public void putAll(Map map) {
        switch (map.size()) {
            case 0:
                return;
            
            case 1:
                Map.Entry entry = (Map.Entry) map.entrySet().iterator().next();
                put(entry.getKey(), entry.getValue());
                return;
            
            default:
                throw new IllegalArgumentException("The map size must be 0 or 1");
        }
    }

    /**
     * Unsupported operation.
     * 
     * @param key  the mapping to remove
     * @return the value mapped to the removed key, null if key not in map
     * @throws UnsupportedOperationException always
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the entrySet view of the map.
     * Changes made via <code>setValue</code> affect this map.
     * To simply iterate through the entries, use {@link #mapIterator()}.
     * 
     * @return the entrySet view
     */
    public Set entrySet() {
        Map.Entry entry = new TiedMapEntry(this, getKey());
        return Collections.singleton(entry);
    }
    
    /**
     * Gets the unmodifiable keySet view of the map.
     * Changes made to the view affect this map.
     * To simply iterate through the keys, use {@link #mapIterator()}.
     * 
     * @return the keySet view
     */
    public Set keySet() {
        return Collections.singleton(key);
    }

    /**
     * Gets the unmodifiable values view of the map.
     * Changes made to the view affect this map.
     * To simply iterate through the values, use {@link #mapIterator()}.
     * 
     * @return the values view
     */
    public Collection values() {
        return new SingletonValues(this);
    }

    /**
     * Gets an iterator over the map.
     * Changes made to the iterator using <code>setValue</code> affect this map.
     * The <code>remove</code> method is unsupported.
     * <p>
     * A MapIterator returns the keys in the map. It also provides convenient
     * methods to get the key and value, and set the value.
     * It avoids the need to create an entrySet/keySet/values object.
     * It also avoids creating the Map Entry object.
     * 
     * @return the map iterator
     */
    public MapIterator mapIterator() {
        return new SingletonMapIterator(this);
    }

    // OrderedMap
    //-----------------------------------------------------------------------
    /**
     * Obtains an <code>OrderedMapIterator</code> over the map.
     * <p>
     * A ordered map iterator is an efficient way of iterating over maps
     * in both directions.
     * 
     * @return an ordered map iterator
     */
    public OrderedMapIterator orderedMapIterator() {
        return new SingletonMapIterator(this);
    }

    /**
     * Gets the first (and only) key in the map.
     * 
     * @return the key
     */
    public Object firstKey() {
        return getKey();
    }

    /**
     * Gets the last (and only) key in the map.
     * 
     * @return the key
     */
    public Object lastKey() {
        return getKey();
    }

    /**
     * Gets the next key after the key specified, always null.
     * 
     * @param key  the next key
     * @return null always
     */
    public Object nextKey(Object key) {
        return null;
    }

    /**
     * Gets the previous key before the key specified, always null.
     * 
     * @param key  the next key
     * @return null always
     */
    public Object previousKey(Object key) {
        return null;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares the specified key to the stored key.
     * 
     * @param key  the key to compare
     * @return true if equal
     */
    protected boolean isEqualKey(Object key) {
        return (key == null ? getKey() == null : key.equals(getKey()));
    }

    /**
     * Compares the specified value to the stored value.
     * 
     * @param value  the value to compare
     * @return true if equal
     */
    protected boolean isEqualValue(Object value) {
        return (value == null ? getValue() == null : value.equals(getValue()));
    }

    //-----------------------------------------------------------------------
    /**
     * SingletonMapIterator.
     */
    static class SingletonMapIterator implements OrderedMapIterator, ResettableIterator {
        private final SingletonMap parent;
        private boolean hasNext = true;
        private boolean canGetSet = false;
        
        SingletonMapIterator(SingletonMap parent) {
            super();
            this.parent = parent;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public Object next() {
            if (hasNext == false) {
                throw new NoSuchElementException(AbstractHashedMap.NO_NEXT_ENTRY);
            }
            hasNext = false;
            canGetSet = true;
            return parent.getKey();
        }

        public boolean hasPrevious() {
            return (hasNext == false);
        }

        public Object previous() {
            if (hasNext == true) {
                throw new NoSuchElementException(AbstractHashedMap.NO_PREVIOUS_ENTRY);
            }
            hasNext = true;
            return parent.getKey();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public Object getKey() {
            if (canGetSet == false) {
                throw new IllegalStateException(AbstractHashedMap.GETKEY_INVALID);
            }
            return parent.getKey();
        }

        public Object getValue() {
            if (canGetSet == false) {
                throw new IllegalStateException(AbstractHashedMap.GETVALUE_INVALID);
            }
            return parent.getValue();
        }

        public Object setValue(Object value) {
            if (canGetSet == false) {
                throw new IllegalStateException(AbstractHashedMap.SETVALUE_INVALID);
            }
            return parent.setValue(value);
        }
        
        public void reset() {
            hasNext = true;
        }
        
        public String toString() {
            if (hasNext) {
                return "Iterator[]";
            } else {
                return "Iterator[" + getKey() + "=" + getValue() + "]";
            }
        }
    }
    
    /**
     * Values implementation for the SingletonMap.
     * This class is needed as values is a view that must update as the map updates.
     */
    static class SingletonValues extends AbstractSet implements Serializable {
        private static final long serialVersionUID = -3689524741863047872L;
        private final SingletonMap parent;

        SingletonValues(SingletonMap parent) {
            super();
            this.parent = parent;
        }

        public int size() {
            return 1;
        }
        public boolean isEmpty() {
            return false;
        }
        public boolean contains(Object object) {
            return parent.containsValue(object);
        }
        public void clear() {
            throw new UnsupportedOperationException();
        }
        public Iterator iterator() {
            return new SingletonIterator(parent.getValue(), false);
        }
    }
    
    //-----------------------------------------------------------------------
    /**
     * Clones the map without cloning the key or value.
     *
     * @return a shallow clone
     */
    public Object clone() {
        try {
            SingletonMap cloned = (SingletonMap) super.clone();
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
        if (obj instanceof Map == false) {
            return false;
        }
        Map other = (Map) obj;
        if (other.size() != 1) {
            return false;
        }
        Map.Entry entry = (Map.Entry) other.entrySet().iterator().next();
        return isEqualKey(entry.getKey()) && isEqualValue(entry.getValue());
    }

    /**
     * Gets the standard Map hashCode.
     * 
     * @return the hash code defined in the Map interface
     */
    public int hashCode() {
        return (getKey() == null ? 0 : getKey().hashCode()) ^
               (getValue() == null ? 0 : getValue().hashCode()); 
    }

    /**
     * Gets the map as a String.
     * 
     * @return a string version of the map
     */
    public String toString() {
        return new StringBuffer(128)
            .append('{')
            .append((getKey() == this ? "(this Map)" : getKey()))
            .append('=')
            .append((getValue() == this ? "(this Map)" : getValue()))
            .append('}')
            .toString();
    }

}
