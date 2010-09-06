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

import java.util.Comparator;
import java.util.SortedMap;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.Transformer;

/**
 * Decorates another <code>SortedMap</code> to create objects in the map on demand.
 * <p>
 * When the {@link #get(Object)} method is called with a key that does not
 * exist in the map, the factory is used to create the object. The created
 * object will be added to the map using the requested key.
 * <p>
 * For instance:
 * <pre>
 * Factory factory = new Factory() {
 *     public Object create() {
 *         return new Date();
 *     }
 * }
 * SortedMap lazy = Lazy.sortedMap(new HashMap(), factory);
 * Object obj = lazy.get("NOW");
 * </pre>
 *
 * After the above code is executed, <code>obj</code> will contain
 * a new <code>Date</code> instance. Furthermore, that <code>Date</code>
 * instance is mapped to the "NOW" key in the map.
 * <p>
 * <strong>Note that LazySortedMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedSortedMap}. This class may throw 
 * exceptions when accessed by concurrent threads without synchronization.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 * @author Paul Jack
 */
public class LazySortedMap
        extends LazyMap
        implements SortedMap {

    /** Serialization version */
    private static final long serialVersionUID = 2715322183617658933L;

    /**
     * Factory method to create a lazily instantiated sorted map.
     * 
     * @param map  the map to decorate, must not be null
     * @param factory  the factory to use, must not be null
     * @throws IllegalArgumentException if map or factory is null
     */
    public static SortedMap decorate(SortedMap map, Factory factory) {
        return new LazySortedMap(map, factory);
    }

    /**
     * Factory method to create a lazily instantiated sorted map.
     * 
     * @param map  the map to decorate, must not be null
     * @param factory  the factory to use, must not be null
     * @throws IllegalArgumentException if map or factory is null
     */
    public static SortedMap decorate(SortedMap map, Transformer factory) {
        return new LazySortedMap(map, factory);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param factory  the factory to use, must not be null
     * @throws IllegalArgumentException if map or factory is null
     */
    protected LazySortedMap(SortedMap map, Factory factory) {
        super(map, factory);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param factory  the factory to use, must not be null
     * @throws IllegalArgumentException if map or factory is null
     */
    protected LazySortedMap(SortedMap map, Transformer factory) {
        super(map, factory);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the map being decorated.
     * 
     * @return the decorated map
     */
    protected SortedMap getSortedMap() {
        return (SortedMap) map;
    }

    //-----------------------------------------------------------------------
    public Object firstKey() {
        return getSortedMap().firstKey();
    }

    public Object lastKey() {
        return getSortedMap().lastKey();
    }

    public Comparator comparator() {
        return getSortedMap().comparator();
    }

    public SortedMap subMap(Object fromKey, Object toKey) {
        SortedMap map = getSortedMap().subMap(fromKey, toKey);
        return new LazySortedMap(map, factory);
    }

    public SortedMap headMap(Object toKey) {
        SortedMap map = getSortedMap().headMap(toKey);
        return new LazySortedMap(map, factory);
    }

    public SortedMap tailMap(Object fromKey) {
        SortedMap map = getSortedMap().tailMap(fromKey);
        return new LazySortedMap(map, factory);
    }

}
