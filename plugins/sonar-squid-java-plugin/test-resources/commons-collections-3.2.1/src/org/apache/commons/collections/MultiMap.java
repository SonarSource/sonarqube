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
package org.apache.commons.collections;

import java.util.Collection;
import java.util.Map;

/** 
 * Defines a map that holds a collection of values against each key.
 * <p>
 * A <code>MultiMap</code> is a Map with slightly different semantics.
 * Putting a value into the map will add the value to a Collection at that key.
 * Getting a value will return a Collection, holding all the values put to that key.
 * <p>
 * For example:
 * <pre>
 * MultiMap mhm = new MultiHashMap();
 * mhm.put(key, "A");
 * mhm.put(key, "B");
 * mhm.put(key, "C");
 * Collection coll = (Collection) mhm.get(key);</pre>
 * <p>
 * <code>coll</code> will be a collection containing "A", "B", "C".
 * <p>
 * NOTE: Additional methods were added to this interface in Commons Collections 3.1.
 * These were added solely for documentation purposes and do not change the interface
 * as they were defined in the superinterface <code>Map</code> anyway.
 *
 * @since Commons Collections 2.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Christopher Berry
 * @author James Strachan
 * @author Stephen Colebourne
 */
public interface MultiMap extends Map {

    /**
     * Removes a specific value from map.
     * <p>
     * The item is removed from the collection mapped to the specified key.
     * Other values attached to that key are unaffected.
     * <p>
     * If the last value for a key is removed, implementations typically
     * return <code>null</code> from a subsequant <code>get(Object)</code>, however
     * they may choose to return an empty collection.
     * 
     * @param key  the key to remove from
     * @param item  the item to remove
     * @return the value removed (which was passed in), null if nothing removed
     * @throws UnsupportedOperationException if the map is unmodifiable
     * @throws ClassCastException if the key or value is of an invalid type
     * @throws NullPointerException if the key or value is null and null is invalid
     */
    public Object remove(Object key, Object item);

    //-----------------------------------------------------------------------
    /**
     * Gets the number of keys in this map.
     * <p>
     * Implementations typically return only the count of keys in the map
     * This cannot be mandated due to backwards compatability of this interface.
     *
     * @return the number of key-collection mappings in this map
     */
    int size();

    /**
     * Gets the collection of values associated with the specified key.
     * <p>
     * The returned value will implement <code>Collection</code>. Implementations
     * are free to declare that they return <code>Collection</code> subclasses
     * such as <code>List</code> or <code>Set</code>.
     * <p>
     * Implementations typically return <code>null</code> if no values have
     * been mapped to the key, however the implementation may choose to
     * return an empty collection.
     * <p>
     * Implementations may choose to return a clone of the internal collection.
     *
     * @param key  the key to retrieve
     * @return the <code>Collection</code> of values, implementations should
     *  return <code>null</code> for no mapping, but may return an empty collection
     * @throws ClassCastException if the key is of an invalid type
     * @throws NullPointerException if the key is null and null keys are invalid
     */
    Object get(Object key);

    /**
     * Checks whether the map contains the value specified.
     * <p>
     * Implementations typically check all collections against all keys for the value.
     * This cannot be mandated due to backwards compatability of this interface.
     *
     * @param value  the value to search for
     * @return true if the map contains the value
     * @throws ClassCastException if the value is of an invalid type
     * @throws NullPointerException if the value is null and null value are invalid
     */
    boolean containsValue(Object value);

    /**
     * Adds the value to the collection associated with the specified key.
     * <p>
     * Unlike a normal <code>Map</code> the previous value is not replaced.
     * Instead the new value is added to the collection stored against the key.
     * The collection may be a <code>List</code>, <code>Set</code> or other
     * collection dependent on implementation.
     *
     * @param key  the key to store against
     * @param value  the value to add to the collection at the key
     * @return typically the value added if the map changed and null if the map did not change
     * @throws UnsupportedOperationException if the map is unmodifiable
     * @throws ClassCastException if the key or value is of an invalid type
     * @throws NullPointerException if the key or value is null and null is invalid
     * @throws IllegalArgumentException if the key or value is invalid
     */
    Object put(Object key, Object value);

    /**
     * Removes all values associated with the specified key.
     * <p>
     * Implementations typically return <code>null</code> from a subsequant
     * <code>get(Object)</code>, however they may choose to return an empty collection.
     *
     * @param key  the key to remove values from
     * @return the <code>Collection</code> of values removed, implementations should
     *  return <code>null</code> for no mapping found, but may return an empty collection
     * @throws UnsupportedOperationException if the map is unmodifiable
     * @throws ClassCastException if the key is of an invalid type
     * @throws NullPointerException if the key is null and null keys are invalid
     */
    Object remove(Object key);

    /**
     * Gets a collection containing all the values in the map.
     * <p>
     * Inplementations typically return a collection containing the combination
     * of values from all keys.
     * This cannot be mandated due to backwards compatability of this interface.
     *
     * @return a collection view of the values contained in this map
     */
    Collection values();

}
