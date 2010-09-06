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
package org.apache.commons.collections.set;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Decorates a <code>Map</code> to obtain <code>Set</code> behaviour.
 * <p>
 * This class is used to create a <code>Set</code> with the same properties as
 * the key set of any map. Thus, a ReferenceSet can be created by wrapping a
 * <code>ReferenceMap</code> in an instance of this class.
 * <p>
 * Most map implementation can be used to create a set by passing in dummy values.
 * Exceptions include <code>BidiMap</code> implementations, as they require unique values.
 *
 * @since Commons Collections 3.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public final class MapBackedSet implements Set, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 6723912213766056587L;

    /** The map being used as the backing store */
    protected final Map map;
    /** The dummyValue to use */
    protected final Object dummyValue;

    /**
     * Factory method to create a set from a map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    public static Set decorate(Map map) {
        return decorate(map, null);
    }

    /**
     * Factory method to create a set from a map.
     * 
     * @param map  the map to decorate, must not be null
     * @param dummyValue  the dummy value to use
     * @throws IllegalArgumentException if map is null
     */
    public static Set decorate(Map map, Object dummyValue) {
        if (map == null) {
            throw new IllegalArgumentException("The map must not be null");
        }
        return new MapBackedSet(map, dummyValue);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param dummyValue  the dummy value to use
     * @throws IllegalArgumentException if map is null
     */
    private MapBackedSet(Map map, Object dummyValue) {
        super();
        this.map = map;
        this.dummyValue = dummyValue;
    }

    //-----------------------------------------------------------------------
    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Iterator iterator() {
        return map.keySet().iterator();
    }

    public boolean contains(Object obj) {
        return map.containsKey(obj);
    }

    public boolean containsAll(Collection coll) {
        return map.keySet().containsAll(coll);
    }

    public boolean add(Object obj) {
        int size = map.size();
        map.put(obj, dummyValue);
        return (map.size() != size);
    }

    public boolean addAll(Collection coll) {
        int size = map.size();
        for (Iterator it = coll.iterator(); it.hasNext();) {
            Object obj = it.next();
            map.put(obj, dummyValue);
        }
        return (map.size() != size);
    }

    public boolean remove(Object obj) {
        int size = map.size();
        map.remove(obj);
        return (map.size() != size);
    }

    public boolean removeAll(Collection coll) {
        return map.keySet().removeAll(coll);
    }

    public boolean retainAll(Collection coll) {
        return map.keySet().retainAll(coll);
    }

    public void clear() {
        map.clear();
    }

    public Object[] toArray() {
        return map.keySet().toArray();
    }

    public Object[] toArray(Object[] array) {
        return map.keySet().toArray(array);
    }

    public boolean equals(Object obj) {
        return map.keySet().equals(obj);
    }

    public int hashCode() {
        return map.keySet().hashCode();
    }

}
