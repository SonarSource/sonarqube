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
import java.util.Set;

/** 
 * <p>This <code>Map</code> wraps another <code>Map</code>
 * implementation, using the wrapped instance for its default
 * implementation.  This class is used as a framework on which to
 * build to extensions for its wrapped <code>Map</code> object which
 * would be unavailable or inconvenient via sub-classing (but usable
 * via composition).</p>
 * 
 * <p>This implementation does not perform any special processing with
 * {@link #entrySet()}, {@link #keySet()} or {@link #values()}. Instead
 * it simply returns the set/collection from the wrapped map. This may be
 * undesirable, for example if you are trying to write a validating
 * implementation it would provide a loophole around the validation. But,
 * you might want that loophole, so this class is kept simple.</p>
 *
 * @deprecated Moved to map subpackage as AbstractMapDecorator. It will be removed in v4.0.
 * @since Commons Collections 2.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Daniel Rall
 * @author Stephen Colebourne
 */
public abstract class ProxyMap implements Map {
    
    /**
     * The <code>Map</code> to delegate to.
     */
    protected Map map;

    /**
     * Constructor that uses the specified map to delegate to.
     * <p>
     * Note that the map is used for delegation, and is not copied. This is
     * different to the normal use of a <code>Map</code> parameter in
     * collections constructors.
     *
     * @param map  the <code>Map</code> to delegate to
     */
    public ProxyMap(Map map) {
        this.map = map;
    }

    /**
     * Invokes the underlying {@link Map#clear()} method.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Invokes the underlying {@link Map#containsKey(Object)} method.
     */
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * Invokes the underlying {@link Map#containsValue(Object)} method.
     */
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     * Invokes the underlying {@link Map#entrySet()} method.
     */
    public Set entrySet() {
        return map.entrySet();
    }

    /**
     * Invokes the underlying {@link Map#equals(Object)} method.
     */
    public boolean equals(Object m) {
        return map.equals(m);
    }

    /**
     * Invokes the underlying {@link Map#get(Object)} method.
     */
    public Object get(Object key) {
        return map.get(key);
    }

    /**
     * Invokes the underlying {@link Map#hashCode()} method.
     */
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Invokes the underlying {@link Map#isEmpty()} method.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Invokes the underlying {@link Map#keySet()} method.
     */
    public Set keySet() {
        return map.keySet();
    }

    /**
     * Invokes the underlying {@link Map#put(Object,Object)} method.
     */
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    /**
     * Invokes the underlying {@link Map#putAll(Map)} method.
     */
    public void putAll(Map t) {
        map.putAll(t);
    }

    /**
     * Invokes the underlying {@link Map#remove(Object)} method.
     */
    public Object remove(Object key) {
        return map.remove(key);
    }

    /**
     * Invokes the underlying {@link Map#size()} method.
     */
    public int size() {
        return map.size();
    }

    /**
     * Invokes the underlying {@link Map#values()} method.
     */
    public Collection values() {
        return map.values();
    }
   
}
