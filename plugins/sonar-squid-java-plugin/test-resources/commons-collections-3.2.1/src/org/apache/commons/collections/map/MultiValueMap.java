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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.FunctorException;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.IteratorChain;

/**
 * A MultiValueMap decorates another map, allowing it to have
 * more than one value for a key.
 * <p>
 * A <code>MultiMap</code> is a Map with slightly different semantics.
 * Putting a value into the map will add the value to a Collection at that key.
 * Getting a value will return a Collection, holding all the values put to that key.
 * <p>
 * This implementation is a decorator, allowing any Map implementation
 * to be used as the base.
 * <p>
 * In addition, this implementation allows the type of collection used
 * for the values to be controlled. By default, an <code>ArrayList</code>
 * is used, however a <code>Class</code> to instantiate may be specified,
 * or a factory that returns a <code>Collection</code> instance.
 * <p>
 * <strong>Note that MultiValueMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. This class may throw exceptions when accessed
 * by concurrent threads without synchronization.
 *
 * @author James Carman
 * @author Christopher Berry
 * @author James Strachan
 * @author Steve Downey
 * @author Stephen Colebourne
 * @author Julien Buret
 * @author Serhiy Yevtushenko
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * @since Commons Collections 3.2
 */
public class MultiValueMap extends AbstractMapDecorator implements MultiMap {

    /** The factory for creating value collections. */
    private final Factory collectionFactory;
    /** The cached values. */
    private transient Collection values;

    /**
     * Creates a map which wraps the given map and
     * maps keys to ArrayLists.
     *
     * @param map  the map to wrap
     */
    public static MultiValueMap decorate(Map map) {
        return new MultiValueMap(map, new ReflectionFactory(ArrayList.class));
    }

    /**
     * Creates a map which decorates the given <code>map</code> and
     * maps keys to collections of type <code>collectionClass</code>.
     *
     * @param map  the map to wrap
     * @param collectionClass  the type of the collection class
     */
    public static MultiValueMap decorate(Map map, Class collectionClass) {
        return new MultiValueMap(map, new ReflectionFactory(collectionClass));
    }

    /**
     * Creates a map which decorates the given <code>map</code> and
     * creates the value collections using the supplied <code>collectionFactory</code>.
     *
     * @param map  the map to decorate
     * @param collectionFactory  the collection factory (must return a Collection object).
     */
    public static MultiValueMap decorate(Map map, Factory collectionFactory) {
        return new MultiValueMap(map, collectionFactory);
    }

    //-----------------------------------------------------------------------
    /**
     * Creates a MultiValueMap based on a <code>HashMap</code> and
     * storing the multiple values in an <code>ArrayList</code>.
     */
    public MultiValueMap() {
        this(new HashMap(), new ReflectionFactory(ArrayList.class));
    }

    /**
     * Creates a MultiValueMap which decorates the given <code>map</code> and
     * creates the value collections using the supplied <code>collectionFactory</code>.
     *
     * @param map  the map to decorate
     * @param collectionFactory  the collection factory which must return a Collection instance
     */
    protected MultiValueMap(Map map, Factory collectionFactory) {
        super(map);
        if (collectionFactory == null) {
            throw new IllegalArgumentException("The factory must not be null");
        }
        this.collectionFactory = collectionFactory;
    }

    //-----------------------------------------------------------------------
    /**
     * Clear the map.
     */
    public void clear() {
        // If you believe that you have GC issues here, try uncommenting this code
//        Set pairs = getMap().entrySet();
//        Iterator pairsIterator = pairs.iterator();
//        while (pairsIterator.hasNext()) {
//            Map.Entry keyValuePair = (Map.Entry) pairsIterator.next();
//            Collection coll = (Collection) keyValuePair.getValue();
//            coll.clear();
//        }
        getMap().clear();
    }

    /**
     * Removes a specific value from map.
     * <p>
     * The item is removed from the collection mapped to the specified key.
     * Other values attached to that key are unaffected.
     * <p>
     * If the last value for a key is removed, <code>null</code> will be returned
     * from a subsequant <code>get(key)</code>.
     *
     * @param key  the key to remove from
     * @param value the value to remove
     * @return the value removed (which was passed in), null if nothing removed
     */
    public Object remove(Object key, Object value) {
        Collection valuesForKey = getCollection(key);
        if (valuesForKey == null) {
            return null;
        }
        boolean removed = valuesForKey.remove(value);
        if (removed == false) {
            return null;
        }
        if (valuesForKey.isEmpty()) {
            remove(key);
        }
        return value;
    }

    /**
     * Checks whether the map contains the value specified.
     * <p>
     * This checks all collections against all keys for the value, and thus could be slow.
     *
     * @param value  the value to search for
     * @return true if the map contains the value
     */
    public boolean containsValue(Object value) {
        Set pairs = getMap().entrySet();
        if (pairs == null) {
            return false;
        }
        Iterator pairsIterator = pairs.iterator();
        while (pairsIterator.hasNext()) {
            Map.Entry keyValuePair = (Map.Entry) pairsIterator.next();
            Collection coll = (Collection) keyValuePair.getValue();
            if (coll.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the value to the collection associated with the specified key.
     * <p>
     * Unlike a normal <code>Map</code> the previous value is not replaced.
     * Instead the new value is added to the collection stored against the key.
     *
     * @param key  the key to store against
     * @param value  the value to add to the collection at the key
     * @return the value added if the map changed and null if the map did not change
     */
    public Object put(Object key, Object value) {
        boolean result = false;
        Collection coll = getCollection(key);
        if (coll == null) {
            coll = createCollection(1);
            result = coll.add(value);
            if (coll.size() > 0) {
                // only add if non-zero size to maintain class state
                getMap().put(key, coll);
                result = false;
            }
        } else {
            result = coll.add(value);
        }
        return (result ? value : null);
    }

    /**
     * Override superclass to ensure that MultiMap instances are
     * correctly handled.
     * <p>
     * If you call this method with a normal map, each entry is
     * added using <code>put(Object,Object)</code>.
     * If you call this method with a multi map, each entry is
     * added using <code>putAll(Object,Collection)</code>.
     *
     * @param map  the map to copy (either a normal or multi map)
     */
    public void putAll(Map map) {
        if (map instanceof MultiMap) {
            for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                Collection coll = (Collection) entry.getValue();
                putAll(entry.getKey(), coll);
            }
        } else {
            for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Gets a collection containing all the values in the map.
     * <p>
     * This returns a collection containing the combination of values from all keys.
     *
     * @return a collection view of the values contained in this map
     */
    public Collection values() {
        Collection vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    /**
     * Checks whether the collection at the specified key contains the value.
     *
     * @param value  the value to search for
     * @return true if the map contains the value
     */
    public boolean containsValue(Object key, Object value) {
        Collection coll = getCollection(key);
        if (coll == null) {
            return false;
        }
        return coll.contains(value);
    }

    /**
     * Gets the collection mapped to the specified key.
     * This method is a convenience method to typecast the result of <code>get(key)</code>.
     *
     * @param key  the key to retrieve
     * @return the collection mapped to the key, null if no mapping
     */
    public Collection getCollection(Object key) {
        return (Collection) getMap().get(key);
    }

    /**
     * Gets the size of the collection mapped to the specified key.
     *
     * @param key  the key to get size for
     * @return the size of the collection at the key, zero if key not in map
     */
    public int size(Object key) {
        Collection coll = getCollection(key);
        if (coll == null) {
            return 0;
        }
        return coll.size();
    }

    /**
     * Adds a collection of values to the collection associated with
     * the specified key.
     *
     * @param key  the key to store against
     * @param values  the values to add to the collection at the key, null ignored
     * @return true if this map changed
     */
    public boolean putAll(Object key, Collection values) {
        if (values == null || values.size() == 0) {
            return false;
        }
        Collection coll = getCollection(key);
        if (coll == null) {
            coll = createCollection(values.size());
            boolean result = coll.addAll(values);
            if (coll.size() > 0) {
                // only add if non-zero size to maintain class state
                getMap().put(key, coll);
                result = false;
            }
            return result;
        } else {
            return coll.addAll(values);
        }
    }

    /**
     * Gets an iterator for the collection mapped to the specified key.
     *
     * @param key  the key to get an iterator for
     * @return the iterator of the collection at the key, empty iterator if key not in map
     */
    public Iterator iterator(Object key) {
        if (!containsKey(key)) {
            return EmptyIterator.INSTANCE;
        } else {
            return new ValuesIterator(key);
        }
    }

    /**
     * Gets the total size of the map by counting all the values.
     *
     * @return the total size of the map counting all values
     */
    public int totalSize() {
        int total = 0;
        Collection values = getMap().values();
        for (Iterator it = values.iterator(); it.hasNext();) {
            Collection coll = (Collection) it.next();
            total += coll.size();
        }
        return total;
    }

    /**
     * Creates a new instance of the map value Collection container
     * using the factory.
     * <p>
     * This method can be overridden to perform your own processing
     * instead of using the factory.
     *
     * @param size  the collection size that is about to be added
     * @return the new collection
     */
    protected Collection createCollection(int size) {
        return (Collection) collectionFactory.create();
    }

    //-----------------------------------------------------------------------
    /**
     * Inner class that provides the values view.
     */
    private class Values extends AbstractCollection {
        public Iterator iterator() {
            final IteratorChain chain = new IteratorChain();
            for (Iterator it = keySet().iterator(); it.hasNext();) {
                chain.addIterator(new ValuesIterator(it.next()));
            }
            return chain;
        }

        public int size() {
            return totalSize();
        }

        public void clear() {
            MultiValueMap.this.clear();
        }
    }

    /**
     * Inner class that provides the values iterator.
     */
    private class ValuesIterator implements Iterator {
        private final Object key;
        private final Collection values;
        private final Iterator iterator;

        public ValuesIterator(Object key) {
            this.key = key;
            this.values = getCollection(key);
            this.iterator = values.iterator();
        }

        public void remove() {
            iterator.remove();
            if (values.isEmpty()) {
                MultiValueMap.this.remove(key);
            }
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Object next() {
            return iterator.next();
        }
    }

    /**
     * Inner class that provides a simple reflection factory.
     */
    private static class ReflectionFactory implements Factory {
        private final Class clazz;

        public ReflectionFactory(Class clazz) {
            this.clazz = clazz;
        }

        public Object create() {
            try {
                return clazz.newInstance();
            } catch (Exception ex) {
                throw new FunctorException("Cannot instantiate class: " + clazz, ex);
            }
        }
    }

}
