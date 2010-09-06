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
package org.apache.commons.collections.bidimap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.OrderedBidiMap;
import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.OrderedMapIterator;
import org.apache.commons.collections.ResettableIterator;
import org.apache.commons.collections.SortedBidiMap;
import org.apache.commons.collections.map.AbstractSortedMapDecorator;

/**
 * Implementation of <code>BidiMap</code> that uses two <code>TreeMap</code> instances.
 * <p>
 * The setValue() method on iterators will succeed only if the new value being set is
 * not already in the bidimap.
 * <p>
 * When considering whether to use this class, the {@link TreeBidiMap} class should
 * also be considered. It implements the interface using a dedicated design, and does
 * not store each object twice, which can save on memory use.
 * <p>
 * NOTE: From Commons Collections 3.1, all subclasses will use <code>TreeMap</code>
 * and the flawed <code>createMap</code> method is ignored.
 * 
 * @since Commons Collections 3.0
 * @version $Id: DualTreeBidiMap.java 646777 2008-04-10 12:33:15Z niallp $
 * 
 * @author Matthew Hawthorne
 * @author Stephen Colebourne
 */
public class DualTreeBidiMap
        extends AbstractDualBidiMap implements SortedBidiMap, Serializable {

    /** Ensure serialization compatibility */
    private static final long serialVersionUID = 721969328361809L;
    /** The comparator to use */
    protected final Comparator comparator;
    
    /**
     * Creates an empty <code>DualTreeBidiMap</code>
     */
    public DualTreeBidiMap() {
        super(new TreeMap(), new TreeMap());
        this.comparator = null;
    }

    /** 
     * Constructs a <code>DualTreeBidiMap</code> and copies the mappings from
     * specified <code>Map</code>.  
     *
     * @param map  the map whose mappings are to be placed in this map
     */
    public DualTreeBidiMap(Map map) {
        super(new TreeMap(), new TreeMap());
        putAll(map);
        this.comparator = null;
    }

    /** 
     * Constructs a <code>DualTreeBidiMap</code> using the specified Comparator.
     *
     * @param comparator  the Comparator
     */
    public DualTreeBidiMap(Comparator comparator) {
        super(new TreeMap(comparator), new TreeMap(comparator));
        this.comparator = comparator;
    }

    /** 
     * Constructs a <code>DualTreeBidiMap</code> that decorates the specified maps.
     *
     * @param normalMap  the normal direction map
     * @param reverseMap  the reverse direction map
     * @param inverseBidiMap  the inverse BidiMap
     */
    protected DualTreeBidiMap(Map normalMap, Map reverseMap, BidiMap inverseBidiMap) {
        super(normalMap, reverseMap, inverseBidiMap);
        this.comparator = ((SortedMap) normalMap).comparator();
    }

    /**
     * Creates a new instance of this object.
     * 
     * @param normalMap  the normal direction map
     * @param reverseMap  the reverse direction map
     * @param inverseMap  the inverse BidiMap
     * @return new bidi map
     */
    protected BidiMap createBidiMap(Map normalMap, Map reverseMap, BidiMap inverseMap) {
        return new DualTreeBidiMap(normalMap, reverseMap, inverseMap);
    }

    //-----------------------------------------------------------------------
    public Comparator comparator() {
        return ((SortedMap) maps[0]).comparator();
    }

    public Object firstKey() {
        return ((SortedMap) maps[0]).firstKey();
    }

    public Object lastKey() {
        return ((SortedMap) maps[0]).lastKey();
    }

    public Object nextKey(Object key) {
        if (isEmpty()) {
            return null;
        }
        if (maps[0] instanceof OrderedMap) {
            return ((OrderedMap) maps[0]).nextKey(key);
        }
        SortedMap sm = (SortedMap) maps[0];
        Iterator it = sm.tailMap(key).keySet().iterator();
        it.next();
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    public Object previousKey(Object key) {
        if (isEmpty()) {
            return null;
        }
        if (maps[0] instanceof OrderedMap) {
            return ((OrderedMap) maps[0]).previousKey(key);
        }
        SortedMap sm = (SortedMap) maps[0];
        SortedMap hm = sm.headMap(key);
        if (hm.isEmpty()) {
            return null;
        }
        return hm.lastKey();
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an ordered map iterator.
     * <p>
     * This implementation copies the elements to an ArrayList in order to
     * provide the forward/backward behaviour.
     * 
     * @return a new ordered map iterator
     */
    public OrderedMapIterator orderedMapIterator() {
        return new BidiOrderedMapIterator(this);
    }

    public SortedBidiMap inverseSortedBidiMap() {
        return (SortedBidiMap) inverseBidiMap();
    }

    public OrderedBidiMap inverseOrderedBidiMap() {
        return (OrderedBidiMap) inverseBidiMap();
    }

    //-----------------------------------------------------------------------
    public SortedMap headMap(Object toKey) {
        SortedMap sub = ((SortedMap) maps[0]).headMap(toKey);
        return new ViewMap(this, sub);
    }

    public SortedMap tailMap(Object fromKey) {
        SortedMap sub = ((SortedMap) maps[0]).tailMap(fromKey);
        return new ViewMap(this, sub);
    }

    public SortedMap subMap(Object fromKey, Object toKey) {
        SortedMap sub = ((SortedMap) maps[0]).subMap(fromKey, toKey);
        return new ViewMap(this, sub);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Internal sorted map view.
     */
    protected static class ViewMap extends AbstractSortedMapDecorator {
        /** The parent bidi map. */
        final DualTreeBidiMap bidi;
        
        /**
         * Constructor.
         * @param bidi  the parent bidi map
         * @param sm  the subMap sorted map
         */
        protected ViewMap(DualTreeBidiMap bidi, SortedMap sm) {
            // the implementation is not great here...
            // use the maps[0] as the filtered map, but maps[1] as the full map
            // this forces containsValue and clear to be overridden
            super((SortedMap) bidi.createBidiMap(sm, bidi.maps[1], bidi.inverseBidiMap));
            this.bidi = (DualTreeBidiMap) map;
        }
        
        public boolean containsValue(Object value) {
            // override as default implementation jumps to [1]
            return bidi.maps[0].containsValue(value);
        }
        
        public void clear() {
            // override as default implementation jumps to [1]
            for (Iterator it = keySet().iterator(); it.hasNext();) {
                it.next();
                it.remove();
            }
        }
        
        public SortedMap headMap(Object toKey) {
            return new ViewMap(bidi, super.headMap(toKey));
        }

        public SortedMap tailMap(Object fromKey) {
            return new ViewMap(bidi, super.tailMap(fromKey));
        }

        public SortedMap subMap(Object fromKey, Object toKey) {
            return new ViewMap(bidi, super.subMap(fromKey, toKey));
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Inner class MapIterator.
     */
    protected static class BidiOrderedMapIterator implements OrderedMapIterator, ResettableIterator {
        
        /** The parent map */
        protected final AbstractDualBidiMap parent;
        /** The iterator being decorated */
        protected ListIterator iterator;
        /** The last returned entry */
        private Map.Entry last = null;
        
        /**
         * Constructor.
         * @param parent  the parent map
         */
        protected BidiOrderedMapIterator(AbstractDualBidiMap parent) {
            super();
            this.parent = parent;
            iterator = new ArrayList(parent.entrySet()).listIterator();
        }
        
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        public Object next() {
            last = (Map.Entry) iterator.next();
            return last.getKey();
        }
        
        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }
        
        public Object previous() {
            last = (Map.Entry) iterator.previous();
            return last.getKey();
        }
        
        public void remove() {
            iterator.remove();
            parent.remove(last.getKey());
            last = null;
        }
        
        public Object getKey() {
            if (last == null) {
                throw new IllegalStateException("Iterator getKey() can only be called after next() and before remove()");
            }
            return last.getKey();
        }

        public Object getValue() {
            if (last == null) {
                throw new IllegalStateException("Iterator getValue() can only be called after next() and before remove()");
            }
            return last.getValue();
        }
        
        public Object setValue(Object value) {
            if (last == null) {
                throw new IllegalStateException("Iterator setValue() can only be called after next() and before remove()");
            }
            if (parent.maps[1].containsKey(value) &&
                parent.maps[1].get(value) != last.getKey()) {
                throw new IllegalArgumentException("Cannot use setValue() when the object being set is already in the map");
            }
            return parent.put(last.getKey(), value);
        }
        
        public void reset() {
            iterator = new ArrayList(parent.entrySet()).listIterator();
            last = null;
        }
        
        public String toString() {
            if (last != null) {
                return "MapIterator[" + getKey() + "=" + getValue() + "]";
            } else {
                return "MapIterator[]";
            }
        }
    }
    
    // Serialization
    //-----------------------------------------------------------------------
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(maps[0]);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        maps[0] = new TreeMap(comparator);
        maps[1] = new TreeMap(comparator);
        Map map = (Map) in.readObject();
        putAll(map);
    }

}
