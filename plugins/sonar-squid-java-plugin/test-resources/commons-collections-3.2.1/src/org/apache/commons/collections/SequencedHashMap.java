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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.list.UnmodifiableList;

/**
 * A map of objects whose mapping entries are sequenced based on the order in
 * which they were added.  This data structure has fast <i>O(1)</i> search
 * time, deletion time, and insertion time.
 * <p>
 * Although this map is sequenced, it cannot implement
 * {@link java.util.List} because of incompatible interface definitions.
 * The remove methods in List and Map have different return values 
 * (see: {@link java.util.List#remove(Object)} and {@link java.util.Map#remove(Object)}).
 * <p>
 * This class is not thread safe.  When a thread safe implementation is
 * required, use {@link java.util.Collections#synchronizedMap(Map)} as it is documented,
 * or use explicit synchronization controls.
 *
 * @deprecated Replaced by LinkedMap and ListOrderedMap in map subpackage. Due to be removed in v4.0.
 * @see org.apache.commons.collections.map.LinkedMap
 * @see org.apache.commons.collections.map.ListOrderedMap
 * @since Commons Collections 2.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Michael A. Smith
 * @author Daniel Rall
 * @author Henning P. Schmiedehausen
 * @author Stephen Colebourne
 */
public class SequencedHashMap implements Map, Cloneable, Externalizable {

    /**
     * {@link java.util.Map.Entry} that doubles as a node in the linked list
     * of sequenced mappings.  
     */
    private static class Entry implements Map.Entry, KeyValue {
        // Note: This class cannot easily be made clonable.  While the actual
        // implementation of a clone would be simple, defining the semantics is
        // difficult.  If a shallow clone is implemented, then entry.next.prev !=
        // entry, which is unintuitive and probably breaks all sorts of assumptions
        // in code that uses this implementation.  If a deep clone is
        // implemented, then what happens when the linked list is cyclical (as is
        // the case with SequencedHashMap)?  It's impossible to know in the clone
        // when to stop cloning, and thus you end up in a recursive loop,
        // continuously cloning the "next" in the list.

        private final Object key;
        private Object value;

        // package private to allow the SequencedHashMap to access and manipulate
        // them.
        Entry next = null;
        Entry prev = null;

        public Entry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        // per Map.Entry.getKey()
        public Object getKey() {
            return this.key;
        }

        // per Map.Entry.getValue()
        public Object getValue() {
            return this.value;
        }

        // per Map.Entry.setValue()
        public Object setValue(Object value) {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public int hashCode() {
            // implemented per api docs for Map.Entry.hashCode()
            return ((getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode()));
        }

        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof Map.Entry))
                return false;

            Map.Entry other = (Map.Entry) obj;

            // implemented per api docs for Map.Entry.equals(Object) 
            return (
                (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()))
                    && (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue())));
        }
        public String toString() {
            return "[" + getKey() + "=" + getValue() + "]";
        }
    }

    /**
     *  Construct an empty sentinel used to hold the head (sentinel.next) and the
     *  tail (sentinel.prev) of the list.  The sentinel has a <code>null</code>
     *  key and value.
     */
    private static final Entry createSentinel() {
        Entry s = new Entry(null, null);
        s.prev = s;
        s.next = s;
        return s;
    }

    /**
     *  Sentinel used to hold the head and tail of the list of entries.
     */
    private Entry sentinel;

    /**
     *  Map of keys to entries
     */
    private HashMap entries;

    /**
     *  Holds the number of modifications that have occurred to the map,
     *  excluding modifications made through a collection view's iterator
     *  (e.g. entrySet().iterator().remove()).  This is used to create a
     *  fail-fast behavior with the iterators.
     */
    private transient long modCount = 0;

    /**
     *  Construct a new sequenced hash map with default initial size and load
     *  factor.
     */
    public SequencedHashMap() {
        sentinel = createSentinel();
        entries = new HashMap();
    }

    /**
     *  Construct a new sequenced hash map with the specified initial size and
     *  default load factor.
     *
     *  @param initialSize the initial size for the hash table 
     *
     *  @see HashMap#HashMap(int)
     */
    public SequencedHashMap(int initialSize) {
        sentinel = createSentinel();
        entries = new HashMap(initialSize);
    }

    /**
     *  Construct a new sequenced hash map with the specified initial size and
     *  load factor.
     *
     *  @param initialSize the initial size for the hash table 
     *
     *  @param loadFactor the load factor for the hash table.
     *
     *  @see HashMap#HashMap(int,float)
     */
    public SequencedHashMap(int initialSize, float loadFactor) {
        sentinel = createSentinel();
        entries = new HashMap(initialSize, loadFactor);
    }

    /**
     *  Construct a new sequenced hash map and add all the elements in the
     *  specified map.  The order in which the mappings in the specified map are
     *  added is defined by {@link #putAll(Map)}.  
     */
    public SequencedHashMap(Map m) {
        this();
        putAll(m);
    }

    /**
     *  Removes an internal entry from the linked list.  This does not remove
     *  it from the underlying map.
     */
    private void removeEntry(Entry entry) {
        entry.next.prev = entry.prev;
        entry.prev.next = entry.next;
    }

    /**
     *  Inserts a new internal entry to the tail of the linked list.  This does
     *  not add the entry to the underlying map.
     */
    private void insertEntry(Entry entry) {
        entry.next = sentinel;
        entry.prev = sentinel.prev;
        sentinel.prev.next = entry;
        sentinel.prev = entry;
    }

    // per Map.size()

    /**
     *  Implements {@link Map#size()}.
     */
    public int size() {
        // use the underlying Map's size since size is not maintained here.
        return entries.size();
    }

    /**
     *  Implements {@link Map#isEmpty()}.
     */
    public boolean isEmpty() {
        // for quick check whether the map is entry, we can check the linked list
        // and see if there's anything in it.
        return sentinel.next == sentinel;
    }

    /**
     *  Implements {@link Map#containsKey(Object)}.
     */
    public boolean containsKey(Object key) {
        // pass on to underlying map implementation
        return entries.containsKey(key);
    }

    /**
     *  Implements {@link Map#containsValue(Object)}.
     */
    public boolean containsValue(Object value) {
        // unfortunately, we cannot just pass this call to the underlying map
        // because we are mapping keys to entries, not keys to values.  The
        // underlying map doesn't have an efficient implementation anyway, so this
        // isn't a big deal.

        // do null comparison outside loop so we only need to do it once.  This
        // provides a tighter, more efficient loop at the expense of slight
        // code duplication.
        if (value == null) {
            for (Entry pos = sentinel.next; pos != sentinel; pos = pos.next) {
                if (pos.getValue() == null)
                    return true;
            }
        } else {
            for (Entry pos = sentinel.next; pos != sentinel; pos = pos.next) {
                if (value.equals(pos.getValue()))
                    return true;
            }
        }
        return false;
    }

    /**
     *  Implements {@link Map#get(Object)}.
     */
    public Object get(Object o) {
        // find entry for the specified key object
        Entry entry = (Entry) entries.get(o);
        if (entry == null)
            return null;

        return entry.getValue();
    }

    /**
     *  Return the entry for the "oldest" mapping.  That is, return the Map.Entry
     *  for the key-value pair that was first put into the map when compared to
     *  all the other pairings in the map.  This behavior is equivalent to using
     *  <code>entrySet().iterator().next()</code>, but this method provides an
     *  optimized implementation.
     *
     *  @return The first entry in the sequence, or <code>null</code> if the
     *  map is empty.
     */
    public Map.Entry getFirst() {
        // sentinel.next points to the "first" element of the sequence -- the head
        // of the list, which is exactly the entry we need to return.  We must test
        // for an empty list though because we don't want to return the sentinel!
        return (isEmpty()) ? null : sentinel.next;
    }

    /**
     *  Return the key for the "oldest" mapping.  That is, return the key for the
     *  mapping that was first put into the map when compared to all the other
     *  objects in the map.  This behavior is equivalent to using
     *  <code>getFirst().getKey()</code>, but this method provides a slightly
     *  optimized implementation.
     *
     *  @return The first key in the sequence, or <code>null</code> if the
     *  map is empty.
     */
    public Object getFirstKey() {
        // sentinel.next points to the "first" element of the sequence -- the head
        // of the list -- and the requisite key is returned from it.  An empty list
        // does not need to be tested.  In cases where the list is empty,
        // sentinel.next will point to the sentinel itself which has a null key,
        // which is exactly what we would want to return if the list is empty (a
        // nice convenient way to avoid test for an empty list)
        return sentinel.next.getKey();
    }

    /**
     *  Return the value for the "oldest" mapping.  That is, return the value for
     *  the mapping that was first put into the map when compared to all the
     *  other objects in the map.  This behavior is equivalent to using
     *  <code>getFirst().getValue()</code>, but this method provides a slightly
     *  optimized implementation.
     *
     *  @return The first value in the sequence, or <code>null</code> if the
     *  map is empty.
     */
    public Object getFirstValue() {
        // sentinel.next points to the "first" element of the sequence -- the head
        // of the list -- and the requisite value is returned from it.  An empty
        // list does not need to be tested.  In cases where the list is empty,
        // sentinel.next will point to the sentinel itself which has a null value,
        // which is exactly what we would want to return if the list is empty (a
        // nice convenient way to avoid test for an empty list)
        return sentinel.next.getValue();
    }

    /**
     *  Return the entry for the "newest" mapping.  That is, return the Map.Entry
     *  for the key-value pair that was first put into the map when compared to
     *  all the other pairings in the map.  The behavior is equivalent to:
     *
     *  <pre>
     *    Object obj = null;
     *    Iterator iter = entrySet().iterator();
     *    while(iter.hasNext()) {
     *      obj = iter.next();
     *    }
     *    return (Map.Entry)obj;
     *  </pre>
     *
     *  However, the implementation of this method ensures an O(1) lookup of the
     *  last key rather than O(n).
     *
     *  @return The last entry in the sequence, or <code>null</code> if the map
     *  is empty.
     */
    public Map.Entry getLast() {
        // sentinel.prev points to the "last" element of the sequence -- the tail
        // of the list, which is exactly the entry we need to return.  We must test
        // for an empty list though because we don't want to return the sentinel!
        return (isEmpty()) ? null : sentinel.prev;
    }

    /**
     *  Return the key for the "newest" mapping.  That is, return the key for the
     *  mapping that was last put into the map when compared to all the other
     *  objects in the map.  This behavior is equivalent to using
     *  <code>getLast().getKey()</code>, but this method provides a slightly
     *  optimized implementation.
     *
     *  @return The last key in the sequence, or <code>null</code> if the map is
     *  empty.
     */
    public Object getLastKey() {
        // sentinel.prev points to the "last" element of the sequence -- the tail
        // of the list -- and the requisite key is returned from it.  An empty list
        // does not need to be tested.  In cases where the list is empty,
        // sentinel.prev will point to the sentinel itself which has a null key,
        // which is exactly what we would want to return if the list is empty (a
        // nice convenient way to avoid test for an empty list)
        return sentinel.prev.getKey();
    }

    /**
     *  Return the value for the "newest" mapping.  That is, return the value for
     *  the mapping that was last put into the map when compared to all the other
     *  objects in the map.  This behavior is equivalent to using
     *  <code>getLast().getValue()</code>, but this method provides a slightly
     *  optimized implementation.
     *
     *  @return The last value in the sequence, or <code>null</code> if the map
     *  is empty.
     */
    public Object getLastValue() {
        // sentinel.prev points to the "last" element of the sequence -- the tail
        // of the list -- and the requisite value is returned from it.  An empty
        // list does not need to be tested.  In cases where the list is empty,
        // sentinel.prev will point to the sentinel itself which has a null value,
        // which is exactly what we would want to return if the list is empty (a
        // nice convenient way to avoid test for an empty list)
        return sentinel.prev.getValue();
    }

    /**
     *  Implements {@link Map#put(Object, Object)}.
     */
    public Object put(Object key, Object value) {
        modCount++;

        Object oldValue = null;

        // lookup the entry for the specified key
        Entry e = (Entry) entries.get(key);

        // check to see if it already exists
        if (e != null) {
            // remove from list so the entry gets "moved" to the end of list
            removeEntry(e);

            // update value in map
            oldValue = e.setValue(value);

            // Note: We do not update the key here because its unnecessary.  We only
            // do comparisons using equals(Object) and we know the specified key and
            // that in the map are equal in that sense.  This may cause a problem if
            // someone does not implement their hashCode() and/or equals(Object)
            // method properly and then use it as a key in this map.  
        } else {
            // add new entry
            e = new Entry(key, value);
            entries.put(key, e);
        }
        // assert(entry in map, but not list)

        // add to list
        insertEntry(e);

        return oldValue;
    }

    /**
     *  Implements {@link Map#remove(Object)}.
     */
    public Object remove(Object key) {
        Entry e = removeImpl(key);
        return (e == null) ? null : e.getValue();
    }

    /**
     *  Fully remove an entry from the map, returning the old entry or null if
     *  there was no such entry with the specified key.
     */
    private Entry removeImpl(Object key) {
        Entry e = (Entry) entries.remove(key);
        if (e == null)
            return null;
        modCount++;
        removeEntry(e);
        return e;
    }

    /**
     *  Adds all the mappings in the specified map to this map, replacing any
     *  mappings that already exist (as per {@link Map#putAll(Map)}).  The order
     *  in which the entries are added is determined by the iterator returned
     *  from {@link Map#entrySet()} for the specified map.
     *
     *  @param t the mappings that should be added to this map.
     *
     *  @throws NullPointerException if <code>t</code> is <code>null</code>
     */
    public void putAll(Map t) {
        Iterator iter = t.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     *  Implements {@link Map#clear()}.
     */
    public void clear() {
        modCount++;

        // remove all from the underlying map
        entries.clear();

        // and the list
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
    }

    /**
     *  Implements {@link Map#equals(Object)}.
     */
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;

        if (!(obj instanceof Map))
            return false;

        return entrySet().equals(((Map) obj).entrySet());
    }

    /**
     *  Implements {@link Map#hashCode()}.
     */
    public int hashCode() {
        return entrySet().hashCode();
    }

    /**
     *  Provides a string representation of the entries within the map.  The
     *  format of the returned string may change with different releases, so this
     *  method is suitable for debugging purposes only.  If a specific format is
     *  required, use {@link #entrySet()}.{@link Set#iterator() iterator()} and
     *  iterate over the entries in the map formatting them as appropriate.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('[');
        for (Entry pos = sentinel.next; pos != sentinel; pos = pos.next) {
            buf.append(pos.getKey());
            buf.append('=');
            buf.append(pos.getValue());
            if (pos.next != sentinel) {
                buf.append(',');
            }
        }
        buf.append(']');

        return buf.toString();
    }

    /**
     *  Implements {@link Map#keySet()}.
     */
    public Set keySet() {
        return new AbstractSet() {

            // required impls
            public Iterator iterator() {
                return new OrderedIterator(KEY);
            }
            public boolean remove(Object o) {
                Entry e = SequencedHashMap.this.removeImpl(o);
                return (e != null);
            }

            // more efficient impls than abstract set
            public void clear() {
                SequencedHashMap.this.clear();
            }
            public int size() {
                return SequencedHashMap.this.size();
            }
            public boolean isEmpty() {
                return SequencedHashMap.this.isEmpty();
            }
            public boolean contains(Object o) {
                return SequencedHashMap.this.containsKey(o);
            }

        };
    }

    /**
     *  Implements {@link Map#values()}.
     */
    public Collection values() {
        return new AbstractCollection() {
            // required impl
            public Iterator iterator() {
                return new OrderedIterator(VALUE);
            }
            public boolean remove(Object value) {
                // do null comparison outside loop so we only need to do it once.  This
                // provides a tighter, more efficient loop at the expense of slight
                // code duplication.
                if (value == null) {
                    for (Entry pos = sentinel.next; pos != sentinel; pos = pos.next) {
                        if (pos.getValue() == null) {
                            SequencedHashMap.this.removeImpl(pos.getKey());
                            return true;
                        }
                    }
                } else {
                    for (Entry pos = sentinel.next; pos != sentinel; pos = pos.next) {
                        if (value.equals(pos.getValue())) {
                            SequencedHashMap.this.removeImpl(pos.getKey());
                            return true;
                        }
                    }
                }

                return false;
            }

            // more efficient impls than abstract collection
            public void clear() {
                SequencedHashMap.this.clear();
            }
            public int size() {
                return SequencedHashMap.this.size();
            }
            public boolean isEmpty() {
                return SequencedHashMap.this.isEmpty();
            }
            public boolean contains(Object o) {
                return SequencedHashMap.this.containsValue(o);
            }
        };
    }

    /**
     *  Implements {@link Map#entrySet()}.
     */
    public Set entrySet() {
        return new AbstractSet() {
            // helper
            private Entry findEntry(Object o) {
                if (o == null)
                    return null;
                if (!(o instanceof Map.Entry))
                    return null;

                Map.Entry e = (Map.Entry) o;
                Entry entry = (Entry) entries.get(e.getKey());
                if (entry != null && entry.equals(e))
                    return entry;
                else
                    return null;
            }

            // required impl
            public Iterator iterator() {
                return new OrderedIterator(ENTRY);
            }
            public boolean remove(Object o) {
                Entry e = findEntry(o);
                if (e == null)
                    return false;

                return SequencedHashMap.this.removeImpl(e.getKey()) != null;
            }

            // more efficient impls than abstract collection
            public void clear() {
                SequencedHashMap.this.clear();
            }
            public int size() {
                return SequencedHashMap.this.size();
            }
            public boolean isEmpty() {
                return SequencedHashMap.this.isEmpty();
            }
            public boolean contains(Object o) {
                return findEntry(o) != null;
            }
        };
    }

    // constants to define what the iterator should return on "next"
    private static final int KEY = 0;
    private static final int VALUE = 1;
    private static final int ENTRY = 2;
    private static final int REMOVED_MASK = 0x80000000;

    private class OrderedIterator implements Iterator {
        /** 
         *  Holds the type that should be returned from the iterator.  The value
         *  should be either {@link #KEY}, {@link #VALUE}, or {@link #ENTRY}.  To
         *  save a tiny bit of memory, this field is also used as a marker for when
         *  remove has been called on the current object to prevent a second remove
         *  on the same element.  Essentially, if this value is negative (i.e. the
         *  bit specified by {@link #REMOVED_MASK} is set), the current position
         *  has been removed.  If positive, remove can still be called.
         */
        private int returnType;

        /**
         *  Holds the "current" position in the iterator.  When pos.next is the
         *  sentinel, we've reached the end of the list.
         */
        private Entry pos = sentinel;

        /**
         *  Holds the expected modification count.  If the actual modification
         *  count of the map differs from this value, then a concurrent
         *  modification has occurred.
         */
        private transient long expectedModCount = modCount;

        /**
         *  Construct an iterator over the sequenced elements in the order in which
         *  they were added.  The {@link #next()} method returns the type specified
         *  by <code>returnType</code> which must be either {@link #KEY}, {@link
         *  #VALUE}, or {@link #ENTRY}.
         */
        public OrderedIterator(int returnType) {
            //// Since this is a private inner class, nothing else should have
            //// access to the constructor.  Since we know the rest of the outer
            //// class uses the iterator correctly, we can leave of the following
            //// check:
            //if(returnType >= 0 && returnType <= 2) {
            //  throw new IllegalArgumentException("Invalid iterator type");
            //}

            // Set the "removed" bit so that the iterator starts in a state where
            // "next" must be called before "remove" will succeed.
            this.returnType = returnType | REMOVED_MASK;
        }

        /**
         *  Returns whether there is any additional elements in the iterator to be
         *  returned.
         *
         *  @return <code>true</code> if there are more elements left to be
         *  returned from the iterator; <code>false</code> otherwise.
         */
        public boolean hasNext() {
            return pos.next != sentinel;
        }

        /**
         *  Returns the next element from the iterator.
         *
         *  @return the next element from the iterator.
         *
         *  @throws NoSuchElementException if there are no more elements in the
         *  iterator.
         *
         *  @throws ConcurrentModificationException if a modification occurs in
         *  the underlying map.
         */
        public Object next() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (pos.next == sentinel) {
                throw new NoSuchElementException();
            }

            // clear the "removed" flag
            returnType = returnType & ~REMOVED_MASK;

            pos = pos.next;
            switch (returnType) {
                case KEY :
                    return pos.getKey();
                case VALUE :
                    return pos.getValue();
                case ENTRY :
                    return pos;
                default :
                    // should never happen
                    throw new Error("bad iterator type: " + returnType);
            }

        }

        /**
         *  Removes the last element returned from the {@link #next()} method from
         *  the sequenced map.
         *
         *  @throws IllegalStateException if there isn't a "last element" to be
         *  removed.  That is, if {@link #next()} has never been called, or if
         *  {@link #remove()} was already called on the element.
         *
         *  @throws ConcurrentModificationException if a modification occurs in
         *  the underlying map.
         */
        public void remove() {
            if ((returnType & REMOVED_MASK) != 0) {
                throw new IllegalStateException("remove() must follow next()");
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            SequencedHashMap.this.removeImpl(pos.getKey());

            // update the expected mod count for the remove operation
            expectedModCount++;

            // set the removed flag
            returnType = returnType | REMOVED_MASK;
        }
    }

    // APIs maintained from previous version of SequencedHashMap for backwards
    // compatibility

    /**
     * Creates a shallow copy of this object, preserving the internal structure
     * by copying only references.  The keys and values themselves are not
     * <code>clone()</code>'d.  The cloned object maintains the same sequence.
     *
     * @return A clone of this instance.  
     *
     * @throws CloneNotSupportedException if clone is not supported by a
     * subclass.
     */
    public Object clone() throws CloneNotSupportedException {
        // yes, calling super.clone() silly since we're just blowing away all
        // the stuff that super might be doing anyway, but for motivations on
        // this, see:
        // http://www.javaworld.com/javaworld/jw-01-1999/jw-01-object.html
        SequencedHashMap map = (SequencedHashMap) super.clone();

        // create new, empty sentinel
        map.sentinel = createSentinel();

        // create a new, empty entry map
        // note: this does not preserve the initial capacity and load factor.
        map.entries = new HashMap();

        // add all the mappings
        map.putAll(this);

        // Note: We cannot just clone the hashmap and sentinel because we must
        // duplicate our internal structures.  Cloning those two will not clone all
        // the other entries they reference, and so the cloned hash map will not be
        // able to maintain internal consistency because there are two objects with
        // the same entries.  See discussion in the Entry implementation on why we
        // cannot implement a clone of the Entry (and thus why we need to recreate
        // everything).

        return map;
    }

    /**
     *  Returns the Map.Entry at the specified index
     *
     *  @throws ArrayIndexOutOfBoundsException if the specified index is
     *  <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
     */
    private Map.Entry getEntry(int index) {
        Entry pos = sentinel;

        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index + " < 0");
        }

        // loop to one before the position
        int i = -1;
        while (i < (index - 1) && pos.next != sentinel) {
            i++;
            pos = pos.next;
        }
        // pos.next is the requested position

        // if sentinel is next, past end of list
        if (pos.next == sentinel) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + (i + 1));
        }

        return pos.next;
    }

    /**
     * Gets the key at the specified index.
     *
     * @param index  the index to retrieve
     * @return the key at the specified index, or null
     * @throws ArrayIndexOutOfBoundsException if the <code>index</code> is
     *  <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
     */
    public Object get(int index) {
        return getEntry(index).getKey();
    }

    /**
     * Gets the value at the specified index.
     *
     * @param index  the index to retrieve
     * @return the value at the specified index, or null
     * @throws ArrayIndexOutOfBoundsException if the <code>index</code> is
     *  <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
     */
    public Object getValue(int index) {
        return getEntry(index).getValue();
    }

    /**
     * Gets the index of the specified key.
     * 
     * @param key  the key to find the index of
     * @return the index, or -1 if not found
     */
    public int indexOf(Object key) {
        Entry e = (Entry) entries.get(key);
        if (e == null) {
            return -1;
        }
        int pos = 0;
        while (e.prev != sentinel) {
            pos++;
            e = e.prev;
        }
        return pos;
    }

    /**
     * Gets an iterator over the keys.
     * 
     * @return an iterator over the keys
     */
    public Iterator iterator() {
        return keySet().iterator();
    }

    /**
     * Gets the last index of the specified key.
     * 
     * @param key  the key to find the index of
     * @return the index, or -1 if not found
     */
    public int lastIndexOf(Object key) {
        // keys in a map are guaranteed to be unique
        return indexOf(key);
    }

    /**
     * Returns a List view of the keys rather than a set view.  The returned
     * list is unmodifiable.  This is required because changes to the values of
     * the list (using {@link java.util.ListIterator#set(Object)}) will
     * effectively remove the value from the list and reinsert that value at
     * the end of the list, which is an unexpected side effect of changing the
     * value of a list.  This occurs because changing the key, changes when the
     * mapping is added to the map and thus where it appears in the list.
     *
     * <p>An alternative to this method is to use {@link #keySet()}
     *
     * @see #keySet()
     * @return The ordered list of keys.  
     */
    public List sequence() {
        List l = new ArrayList(size());
        Iterator iter = keySet().iterator();
        while (iter.hasNext()) {
            l.add(iter.next());
        }

        return UnmodifiableList.decorate(l);
    }

    /**
     * Removes the element at the specified index.
     *
     * @param index The index of the object to remove.
     * @return      The previous value corresponding the <code>key</code>, or
     *              <code>null</code> if none existed.
     *
     * @throws ArrayIndexOutOfBoundsException if the <code>index</code> is
     * <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
     */
    public Object remove(int index) {
        return remove(get(index));
    }

    // per Externalizable.readExternal(ObjectInput)

    /**
     * Deserializes this map from the given stream.
     *
     * @param in the stream to deserialize from
     * @throws IOException if the stream raises it
     * @throws ClassNotFoundException if the stream raises it
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            Object key = in.readObject();
            Object value = in.readObject();
            put(key, value);
        }
    }

    /**
     * Serializes this map to the given stream.
     *
     * @param out  the stream to serialize to
     * @throws IOException  if the stream raises it
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(size());
        for (Entry pos = sentinel.next; pos != sentinel; pos = pos.next) {
            out.writeObject(pos.getKey());
            out.writeObject(pos.getValue());
        }
    }

    // add a serial version uid, so that if we change things in the future
    // without changing the format, we can still deserialize properly.
    private static final long serialVersionUID = 3380552487888102930L;

}
