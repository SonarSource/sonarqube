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
import java.util.Map;

/**
 * A <code>Map</code> implementation that matches keys and values based
 * on <code>==</code> not <code>equals()</code>.
 * <p>
 * This map will violate the detail of various Map and map view contracts.
 * As a general rule, don't compare this map to other maps.
 * <p>
 * <strong>Note that IdentityMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap(Map)}. This class may throw 
 * exceptions when accessed by concurrent threads without synchronization.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author java util HashMap
 * @author Stephen Colebourne
 */
public class IdentityMap
        extends AbstractHashedMap implements Serializable, Cloneable {

    /** Serialisation version */
    private static final long serialVersionUID = 2028493495224302329L;

    /**
     * Constructs a new empty map with default size and load factor.
     */
    public IdentityMap() {
        super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity. 
     *
     * @param initialCapacity  the initial capacity
     * @throws IllegalArgumentException if the initial capacity is less than one
     */
    public IdentityMap(int initialCapacity) {
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
    public IdentityMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor copying elements from another map.
     *
     * @param map  the map to copy
     * @throws NullPointerException if the map is null
     */
    public IdentityMap(Map map) {
        super(map);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the hash code for the key specified.
     * This implementation uses the identity hash code.
     * 
     * @param key  the key to get a hash code for
     * @return the hash code
     */
    protected int hash(Object key) {
        return System.identityHashCode(key);
    }
    
    /**
     * Compares two keys for equals.
     * This implementation uses <code>==</code>.
     * 
     * @param key1  the first key to compare
     * @param key2  the second key to compare
     * @return true if equal by identity
     */
    protected boolean isEqualKey(Object key1, Object key2) {
        return (key1 == key2);
    }
    
    /**
     * Compares two values for equals.
     * This implementation uses <code>==</code>.
     * 
     * @param value1  the first value to compare
     * @param value2  the second value to compare
     * @return true if equal by identity
     */
    protected boolean isEqualValue(Object value1, Object value2) {
        return (value1 == value2);
    }
    
    /**
     * Creates an entry to store the data.
     * This implementation creates an IdentityEntry instance.
     * 
     * @param next  the next entry in sequence
     * @param hashCode  the hash code to use
     * @param key  the key to store
     * @param value  the value to store
     * @return the newly created entry
     */
    protected HashEntry createEntry(HashEntry next, int hashCode, Object key, Object value) {
        return new IdentityEntry(next, hashCode, key, value);
    }
    
    //-----------------------------------------------------------------------
    /**
     * HashEntry
     */
    protected static class IdentityEntry extends HashEntry {
        
        protected IdentityEntry(HashEntry next, int hashCode, Object key, Object value) {
            super(next, hashCode, key, value);
        }
        
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Map.Entry == false) {
                return false;
            }
            Map.Entry other = (Map.Entry) obj;
            return
                (getKey() == other.getKey()) &&
                (getValue() == other.getValue());
        }
        
        public int hashCode() {
            return System.identityHashCode(getKey()) ^
                   System.identityHashCode(getValue());
        }
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
    
}
