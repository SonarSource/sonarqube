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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.ResettableIterator;
import org.apache.commons.collections.collection.AbstractCollectionDecorator;
import org.apache.commons.collections.iterators.AbstractIteratorDecorator;
import org.apache.commons.collections.keyvalue.AbstractMapEntryDecorator;

/**
 * Abstract <code>BidiMap</code> implemented using two maps.
 * <p>
 * An implementation can be written simply by implementing the
 * <code>createMap</code> method.
 * 
 * @see DualHashBidiMap
 * @see DualTreeBidiMap
 * @since Commons Collections 3.0
 * @version $Id: AbstractDualBidiMap.java 646777 2008-04-10 12:33:15Z niallp $
 * 
 * @author Matthew Hawthorne
 * @author Stephen Colebourne
 */
public abstract class AbstractDualBidiMap implements BidiMap {

    /**
     * Delegate map array.  The first map contains standard entries, and the 
     * second contains inverses.
     */
    protected transient final Map[] maps = new Map[2];
    /**
     * Inverse view of this map.
     */
    protected transient BidiMap inverseBidiMap = null;
    /**
     * View of the keys.
     */
    protected transient Set keySet = null;
    /**
     * View of the values.
     */
    protected transient Collection values = null;
    /**
     * View of the entries.
     */
    protected transient Set entrySet = null;

    /**
     * Creates an empty map, initialised by <code>createMap</code>.
     * <p>
     * This constructor remains in place for deserialization.
     * All other usage is deprecated in favour of
     * {@link #AbstractDualBidiMap(Map, Map)}.
     */
    protected AbstractDualBidiMap() {
        super();
        maps[0] = createMap();
        maps[1] = createMap();
    }

    /**
     * Creates an empty map using the two maps specified as storage.
     * <p>
     * The two maps must be a matching pair, normal and reverse.
     * They will typically both be empty.
     * <p>
     * Neither map is validated, so nulls may be passed in.
     * If you choose to do this then the subclass constructor must populate
     * the <code>maps[]</code> instance variable itself.
     * 
     * @param normalMap  the normal direction map
     * @param reverseMap  the reverse direction map
     * @since Commons Collections 3.1
     */
    protected AbstractDualBidiMap(Map normalMap, Map reverseMap) {
        super();
        maps[0] = normalMap;
        maps[1] = reverseMap;
    }

    /** 
     * Constructs a map that decorates the specified maps,
     * used by the subclass <code>createBidiMap</code> implementation.
     *
     * @param normalMap  the normal direction map
     * @param reverseMap  the reverse direction map
     * @param inverseBidiMap  the inverse BidiMap
     */
    protected AbstractDualBidiMap(Map normalMap, Map reverseMap, BidiMap inverseBidiMap) {
        super();
        maps[0] = normalMap;
        maps[1] = reverseMap;
        this.inverseBidiMap = inverseBidiMap;
    }

    /**
     * Creates a new instance of the map used by the subclass to store data.
     * <p>
     * This design is deeply flawed and has been deprecated.
     * It relied on subclass data being used during a superclass constructor.
     * 
     * @return the map to be used for internal storage
     * @deprecated For constructors, use the new two map constructor.
     * For deserialization, populate the maps array directly in readObject.
     */
    protected Map createMap() {
        return null;
    }

    /**
     * Creates a new instance of the subclass.
     * 
     * @param normalMap  the normal direction map
     * @param reverseMap  the reverse direction map
     * @param inverseMap  this map, which is the inverse in the new map
     * @return the inverse map
     */
    protected abstract BidiMap createBidiMap(Map normalMap, Map reverseMap, BidiMap inverseMap);

    // Map delegation
    //-----------------------------------------------------------------------
    public Object get(Object key) {
        return maps[0].get(key);
    }

    public int size() {
        return maps[0].size();
    }

    public boolean isEmpty() {
        return maps[0].isEmpty();
    }

    public boolean containsKey(Object key) {
        return maps[0].containsKey(key);
    }

    public boolean equals(Object obj) {
        return maps[0].equals(obj);
    }

    public int hashCode() {
        return maps[0].hashCode();
    }

    public String toString() {
        return maps[0].toString();
    }

    // BidiMap changes
    //-----------------------------------------------------------------------
    public Object put(Object key, Object value) {
        if (maps[0].containsKey(key)) {
            maps[1].remove(maps[0].get(key));
        }
        if (maps[1].containsKey(value)) {
            maps[0].remove(maps[1].get(value));
        }
        final Object obj = maps[0].put(key, value);
        maps[1].put(value, key);
        return obj;
    }
    
    public void putAll(Map map) {
        for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Object remove(Object key) {
        Object value = null;
        if (maps[0].containsKey(key)) {
            value = maps[0].remove(key);
            maps[1].remove(value);
        }
        return value;
    }

    public void clear() {
        maps[0].clear();
        maps[1].clear();
    }

    public boolean containsValue(Object value) {
        return maps[1].containsKey(value);
    }

    // BidiMap
    //-----------------------------------------------------------------------
    /**
     * Obtains a <code>MapIterator</code> over the map.
     * The iterator implements <code>ResetableMapIterator</code>.
     * This implementation relies on the entrySet iterator.
     * <p>
     * The setValue() methods only allow a new value to be set.
     * If the value being set is already in the map, an IllegalArgumentException
     * is thrown (as setValue cannot change the size of the map).
     * 
     * @return a map iterator
     */
    public MapIterator mapIterator() {
        return new BidiMapIterator(this);
    }
    
    public Object getKey(Object value) {
        return maps[1].get(value);
    }

    public Object removeValue(Object value) {
        Object key = null;
        if (maps[1].containsKey(value)) {
            key = maps[1].remove(value);
            maps[0].remove(key);
        }
        return key;
    }

    public BidiMap inverseBidiMap() {
        if (inverseBidiMap == null) {
            inverseBidiMap = createBidiMap(maps[1], maps[0], this);
        }
        return inverseBidiMap;
    }
    
    // Map views
    //-----------------------------------------------------------------------
    /**
     * Gets a keySet view of the map.
     * Changes made on the view are reflected in the map.
     * The set supports remove and clear but not add.
     * 
     * @return the keySet view
     */
    public Set keySet() {
        if (keySet == null) {
            keySet = new KeySet(this);
        }
        return keySet;
    }

    /**
     * Creates a key set iterator.
     * Subclasses can override this to return iterators with different properties.
     * 
     * @param iterator  the iterator to decorate
     * @return the keySet iterator
     */
    protected Iterator createKeySetIterator(Iterator iterator) {
        return new KeySetIterator(iterator, this);
    }

    /**
     * Gets a values view of the map.
     * Changes made on the view are reflected in the map.
     * The set supports remove and clear but not add.
     * 
     * @return the values view
     */
    public Collection values() {
        if (values == null) {
            values = new Values(this);
        }
        return values;
    }

    /**
     * Creates a values iterator.
     * Subclasses can override this to return iterators with different properties.
     * 
     * @param iterator  the iterator to decorate
     * @return the values iterator
     */
    protected Iterator createValuesIterator(Iterator iterator) {
        return new ValuesIterator(iterator, this);
    }

    /**
     * Gets an entrySet view of the map.
     * Changes made on the set are reflected in the map.
     * The set supports remove and clear but not add.
     * <p>
     * The Map Entry setValue() method only allow a new value to be set.
     * If the value being set is already in the map, an IllegalArgumentException
     * is thrown (as setValue cannot change the size of the map).
     * 
     * @return the entrySet view
     */
    public Set entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet(this);
        }
        return entrySet;
    }
    
    /**
     * Creates an entry set iterator.
     * Subclasses can override this to return iterators with different properties.
     * 
     * @param iterator  the iterator to decorate
     * @return the entrySet iterator
     */
    protected Iterator createEntrySetIterator(Iterator iterator) {
        return new EntrySetIterator(iterator, this);
    }

    //-----------------------------------------------------------------------
    /**
     * Inner class View.
     */
    protected static abstract class View extends AbstractCollectionDecorator {
        
        /** The parent map */
        protected final AbstractDualBidiMap parent;
        
        /**
         * Constructs a new view of the BidiMap.
         * 
         * @param coll  the collection view being decorated
         * @param parent  the parent BidiMap
         */
        protected View(Collection coll, AbstractDualBidiMap parent) {
            super(coll);
            this.parent = parent;
        }

        public boolean removeAll(Collection coll) {
            if (parent.isEmpty() || coll.isEmpty()) {
                return false;
            }
            boolean modified = false;
            Iterator it = iterator();
            while (it.hasNext()) {
                if (coll.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public boolean retainAll(Collection coll) {
            if (parent.isEmpty()) {
                return false;
            }
            if (coll.isEmpty()) {
                parent.clear();
                return true;
            }
            boolean modified = false;
            Iterator it = iterator();
            while (it.hasNext()) {
                if (coll.contains(it.next()) == false) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }
        
        public void clear() {
            parent.clear();
        }
    }
    
    //-----------------------------------------------------------------------
    /**
     * Inner class KeySet.
     */
    protected static class KeySet extends View implements Set {
        
        /**
         * Constructs a new view of the BidiMap.
         * 
         * @param parent  the parent BidiMap
         */
        protected KeySet(AbstractDualBidiMap parent) {
            super(parent.maps[0].keySet(), parent);
        }

        public Iterator iterator() {
            return parent.createKeySetIterator(super.iterator());
        }
        
        public boolean contains(Object key) {
            return parent.maps[0].containsKey(key);
        }

        public boolean remove(Object key) {
            if (parent.maps[0].containsKey(key)) {
                Object value = parent.maps[0].remove(key);
                parent.maps[1].remove(value);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Inner class KeySetIterator.
     */
    protected static class KeySetIterator extends AbstractIteratorDecorator {
        
        /** The parent map */
        protected final AbstractDualBidiMap parent;
        /** The last returned key */
        protected Object lastKey = null;
        /** Whether remove is allowed at present */
        protected boolean canRemove = false;
        
        /**
         * Constructor.
         * @param iterator  the iterator to decorate
         * @param parent  the parent map
         */
        protected KeySetIterator(Iterator iterator, AbstractDualBidiMap parent) {
            super(iterator);
            this.parent = parent;
        }
        
        public Object next() {
            lastKey = super.next();
            canRemove = true;
            return lastKey;
        }
        
        public void remove() {
            if (canRemove == false) {
                throw new IllegalStateException("Iterator remove() can only be called once after next()");
            }
            Object value = parent.maps[0].get(lastKey);
            super.remove();
            parent.maps[1].remove(value);
            lastKey = null;
            canRemove = false;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Inner class Values.
     */
    protected static class Values extends View implements Set {
        
        /**
         * Constructs a new view of the BidiMap.
         * 
         * @param parent  the parent BidiMap
         */
        protected Values(AbstractDualBidiMap parent) {
            super(parent.maps[0].values(), parent);
        }

        public Iterator iterator() {
            return parent.createValuesIterator(super.iterator());
        }
        
        public boolean contains(Object value) {
            return parent.maps[1].containsKey(value);
        }

        public boolean remove(Object value) {
            if (parent.maps[1].containsKey(value)) {
                Object key = parent.maps[1].remove(value);
                parent.maps[0].remove(key);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Inner class ValuesIterator.
     */
    protected static class ValuesIterator extends AbstractIteratorDecorator {
        
        /** The parent map */
        protected final AbstractDualBidiMap parent;
        /** The last returned value */
        protected Object lastValue = null;
        /** Whether remove is allowed at present */
        protected boolean canRemove = false;
        
        /**
         * Constructor.
         * @param iterator  the iterator to decorate
         * @param parent  the parent map
         */
        protected ValuesIterator(Iterator iterator, AbstractDualBidiMap parent) {
            super(iterator);
            this.parent = parent;
        }
        
        public Object next() {
            lastValue = super.next();
            canRemove = true;
            return lastValue;
        }
        
        public void remove() {
            if (canRemove == false) {
                throw new IllegalStateException("Iterator remove() can only be called once after next()");
            }
            super.remove(); // removes from maps[0]
            parent.maps[1].remove(lastValue);
            lastValue = null;
            canRemove = false;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Inner class EntrySet.
     */
    protected static class EntrySet extends View implements Set {
        
        /**
         * Constructs a new view of the BidiMap.
         * 
         * @param parent  the parent BidiMap
         */
        protected EntrySet(AbstractDualBidiMap parent) {
            super(parent.maps[0].entrySet(), parent);
        }

        public Iterator iterator() {
            return parent.createEntrySetIterator(super.iterator());
        }
        
        public boolean remove(Object obj) {
            if (obj instanceof Map.Entry == false) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            if (parent.containsKey(key)) {
                Object value = parent.maps[0].get(key);
                if (value == null ? entry.getValue() == null : value.equals(entry.getValue())) {
                    parent.maps[0].remove(key);
                    parent.maps[1].remove(value);
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Inner class EntrySetIterator.
     */
    protected static class EntrySetIterator extends AbstractIteratorDecorator {
        
        /** The parent map */
        protected final AbstractDualBidiMap parent;
        /** The last returned entry */
        protected Map.Entry last = null;
        /** Whether remove is allowed at present */
        protected boolean canRemove = false;
        
        /**
         * Constructor.
         * @param iterator  the iterator to decorate
         * @param parent  the parent map
         */
        protected EntrySetIterator(Iterator iterator, AbstractDualBidiMap parent) {
            super(iterator);
            this.parent = parent;
        }
        
        public Object next() {
            last = new MapEntry((Map.Entry) super.next(), parent);
            canRemove = true;
            return last;
        }
        
        public void remove() {
            if (canRemove == false) {
                throw new IllegalStateException("Iterator remove() can only be called once after next()");
            }
            // store value as remove may change the entry in the decorator (eg.TreeMap)
            Object value = last.getValue();
            super.remove();
            parent.maps[1].remove(value);
            last = null;
            canRemove = false;
        }
    }

    /**
     * Inner class MapEntry.
     */
    protected static class MapEntry extends AbstractMapEntryDecorator {

        /** The parent map */        
        protected final AbstractDualBidiMap parent;
        
        /**
         * Constructor.
         * @param entry  the entry to decorate
         * @param parent  the parent map
         */
        protected MapEntry(Map.Entry entry, AbstractDualBidiMap parent) {
            super(entry);
            this.parent = parent;
        }
        
        public Object setValue(Object value) {
            Object key = MapEntry.this.getKey();
            if (parent.maps[1].containsKey(value) &&
                parent.maps[1].get(value) != key) {
                throw new IllegalArgumentException("Cannot use setValue() when the object being set is already in the map");
            }
            parent.put(key, value);
            final Object oldValue = super.setValue(value);
            return oldValue;
        }
    }
    
    /**
     * Inner class MapIterator.
     */
    protected static class BidiMapIterator implements MapIterator, ResettableIterator {
        
        /** The parent map */
        protected final AbstractDualBidiMap parent;
        /** The iterator being wrapped */
        protected Iterator iterator;
        /** The last returned entry */
        protected Map.Entry last = null;
        /** Whether remove is allowed at present */
        protected boolean canRemove = false;
        
        /**
         * Constructor.
         * @param parent  the parent map
         */
        protected BidiMapIterator(AbstractDualBidiMap parent) {
            super();
            this.parent = parent;
            this.iterator = parent.maps[0].entrySet().iterator();
        }
        
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        public Object next() {
            last = (Map.Entry) iterator.next();
            canRemove = true;
            return last.getKey();
        }
        
        public void remove() {
            if (canRemove == false) {
                throw new IllegalStateException("Iterator remove() can only be called once after next()");
            }
            // store value as remove may change the entry in the decorator (eg.TreeMap)
            Object value = last.getValue();
            iterator.remove();
            parent.maps[1].remove(value);
            last = null;
            canRemove = false;
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
            iterator = parent.maps[0].entrySet().iterator();
            last = null;
            canRemove = false;
        }
        
        public String toString() {
            if (last != null) {
                return "MapIterator[" + getKey() + "=" + getValue() + "]";
            } else {
                return "MapIterator[]";
            }
        }
    }
    
}
