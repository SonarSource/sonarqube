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
package org.apache.commons.collections.iterators;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.ResettableIterator;

/**
 * Implements a <code>MapIterator</code> using a Map entrySet.
 * Reverse iteration is not supported.
 * <pre>
 * MapIterator it = map.mapIterator();
 * while (it.hasNext()) {
 *   Object key = it.next();
 *   Object value = it.getValue();
 *   it.setValue(newValue);
 * }
 * </pre>
 *  
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class EntrySetMapIterator implements MapIterator, ResettableIterator {
    
    private final Map map;
    private Iterator iterator;
    private Map.Entry last;
    private boolean canRemove = false;
    
    /**
     * Constructor.
     * 
     * @param map  the map to iterate over
     */
    public EntrySetMapIterator(Map map) {
        super();
        this.map = map;
        this.iterator = map.entrySet().iterator();
    }

    //-----------------------------------------------------------------------    
    /**
     * Checks to see if there are more entries still to be iterated.
     *
     * @return <code>true</code> if the iterator has more elements
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Gets the next <em>key</em> from the <code>Map</code>.
     *
     * @return the next key in the iteration
     * @throws java.util.NoSuchElementException if the iteration is finished
     */
    public Object next() {
        last = (Map.Entry) iterator.next();
        canRemove = true;
        return last.getKey();
    }

    //-----------------------------------------------------------------------
    /**
     * Removes the last returned key from the underlying <code>Map</code>.
     * <p>
     * This method can be called once per call to <code>next()</code>.
     *
     * @throws UnsupportedOperationException if remove is not supported by the map
     * @throws IllegalStateException if <code>next()</code> has not yet been called
     * @throws IllegalStateException if <code>remove()</code> has already been called
     *  since the last call to <code>next()</code>
     */
    public void remove() {
        if (canRemove == false) {
            throw new IllegalStateException("Iterator remove() can only be called once after next()");
        }
        iterator.remove();
        last = null;
        canRemove = false;
    }
    
    //-----------------------------------------------------------------------
    /**
     * Gets the current key, which is the key returned by the last call
     * to <code>next()</code>.
     *
     * @return the current key
     * @throws IllegalStateException if <code>next()</code> has not yet been called
     */
    public Object getKey() {
        if (last == null) {
            throw new IllegalStateException("Iterator getKey() can only be called after next() and before remove()");
        }
        return last.getKey();
    }

    /**
     * Gets the current value, which is the value associated with the last key
     * returned by <code>next()</code>.
     *
     * @return the current value
     * @throws IllegalStateException if <code>next()</code> has not yet been called
     */
    public Object getValue() {
        if (last == null) {
            throw new IllegalStateException("Iterator getValue() can only be called after next() and before remove()");
        }
        return last.getValue();
    }

    /**
     * Sets the value associated with the current key.
     *
     * @param value  the new value
     * @return the previous value
     * @throws UnsupportedOperationException if setValue is not supported by the map
     * @throws IllegalStateException if <code>next()</code> has not yet been called
     * @throws IllegalStateException if <code>remove()</code> has been called since the
     *  last call to <code>next()</code>
     */
    public Object setValue(Object value) {
        if (last == null) {
            throw new IllegalStateException("Iterator setValue() can only be called after next() and before remove()");
        }
        return last.setValue(value);
    }

    //-----------------------------------------------------------------------
    /**
     * Resets the state of the iterator.
     */
    public void reset() {
        iterator = map.entrySet().iterator();
        last = null;
        canRemove = false;
    }
    
    /**
     * Gets the iterator as a String.
     * 
     * @return a string version of the iterator
     */    
    public String toString() {
        if (last != null) {
            return "MapIterator[" + getKey() + "=" + getValue() + "]";
        } else {
            return "MapIterator[]";
        }
    }
    
}
