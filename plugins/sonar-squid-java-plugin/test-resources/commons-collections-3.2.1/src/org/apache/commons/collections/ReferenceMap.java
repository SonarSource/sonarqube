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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;

/**
 *  Hash-based {@link Map} implementation that allows
 *  mappings to be removed by the garbage collector.<p>
 *
 *  When you construct a <code>ReferenceMap</code>, you can 
 *  specify what kind of references are used to store the
 *  map's keys and values.  If non-hard references are 
 *  used, then the garbage collector can remove mappings
 *  if a key or value becomes unreachable, or if the 
 *  JVM's memory is running low.  For information on how
 *  the different reference types behave, see
 *  {@link Reference}.<p>
 *
 *  Different types of references can be specified for keys
 *  and values.  The keys can be configured to be weak but
 *  the values hard, in which case this class will behave
 *  like a <a href="http://java.sun.com/j2se/1.4/docs/api/java/util/WeakHashMap.html">
 *  <code>WeakHashMap</code></a>.  However, you
 *  can also specify hard keys and weak values, or any other
 *  combination.  The default constructor uses hard keys
 *  and soft values, providing a memory-sensitive cache.<p>
 *
 *  The algorithms used are basically the same as those
 *  in {@link java.util.HashMap}.  In particular, you 
 *  can specify a load factor and capacity to suit your
 *  needs.  All optional {@link Map} operations are 
 *  supported.<p>
 *
 *  However, this {@link Map} implementation does <I>not</I>
 *  allow null elements.  Attempting to add a null key or
 *  or a null value to the map will raise a 
 *  <code>NullPointerException</code>.<p>
 *
 *  As usual, this implementation is not synchronized.  You
 *  can use {@link java.util.Collections#synchronizedMap} to 
 *  provide synchronized access to a <code>ReferenceMap</code>.
 *
 * @see java.lang.ref.Reference
 * 
 * @deprecated Moved to map subpackage. Due to be removed in v4.0.
 * @since Commons Collections 2.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Paul Jack
 */
public class ReferenceMap extends AbstractMap {

    /**
     *  For serialization.
     */
    private static final long serialVersionUID = -3370601314380922368L;


    /**
     *  Constant indicating that hard references should be used.
     */
    final public static int HARD = 0;


    /**
     *  Constant indicating that soft references should be used.
     */
    final public static int SOFT = 1;


    /**
     *  Constant indicating that weak references should be used.
     */
    final public static int WEAK = 2;


    // --- serialized instance variables:


    /**
     *  The reference type for keys.  Must be HARD, SOFT, WEAK.
     *  Note: I originally marked this field as final, but then this class
     *   didn't compile under JDK1.2.2.
     *  @serial
     */
    private int keyType;


    /**
     *  The reference type for values.  Must be HARD, SOFT, WEAK.
     *  Note: I originally marked this field as final, but then this class
     *   didn't compile under JDK1.2.2.
     *  @serial
     */
    private int valueType;


    /**
     *  The threshold variable is calculated by multiplying
     *  table.length and loadFactor.  
     *  Note: I originally marked this field as final, but then this class
     *   didn't compile under JDK1.2.2.
     *  @serial
     */
    private float loadFactor;
    
    /**
     * Should the value be automatically purged when the associated key has been collected?
     */
    private boolean purgeValues = false;


    // -- Non-serialized instance variables

    /**
     *  ReferenceQueue used to eliminate stale mappings.
     *  See purge.
     */
    private transient ReferenceQueue queue = new ReferenceQueue();


    /**
     *  The hash table.  Its length is always a power of two.  
     */
    private transient Entry[] table;


    /**
     *  Number of mappings in this map.
     */
    private transient int size;


    /**
     *  When size reaches threshold, the map is resized.  
     *  See resize().
     */
    private transient int threshold;


    /**
     *  Number of times this map has been modified.
     */
    private transient volatile int modCount;


    /**
     *  Cached key set.  May be null if key set is never accessed.
     */
    private transient Set keySet;


    /**
     *  Cached entry set.  May be null if entry set is never accessed.
     */
    private transient Set entrySet;


    /**
     *  Cached values.  May be null if values() is never accessed.
     */
    private transient Collection values;


    /**
     *  Constructs a new <code>ReferenceMap</code> that will
     *  use hard references to keys and soft references to values.
     */
    public ReferenceMap() {
        this(HARD, SOFT);
    }

    /**
     *  Constructs a new <code>ReferenceMap</code> that will
     *  use the specified types of references.
     *
     *  @param keyType  the type of reference to use for keys;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     *  @param valueType  the type of reference to use for values;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     *  @param purgeValues should the value be automatically purged when the 
     *   key is garbage collected 
     */
    public ReferenceMap(int keyType, int valueType, boolean purgeValues) {
        this(keyType, valueType);
        this.purgeValues = purgeValues;
    }

    /**
     *  Constructs a new <code>ReferenceMap</code> that will
     *  use the specified types of references.
     *
     *  @param keyType  the type of reference to use for keys;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     *  @param valueType  the type of reference to use for values;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     */
    public ReferenceMap(int keyType, int valueType) {
        this(keyType, valueType, 16, 0.75f);
    }

    /**
     *  Constructs a new <code>ReferenceMap</code> with the
     *  specified reference types, load factor and initial
     *  capacity.
     *
     *  @param keyType  the type of reference to use for keys;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     *  @param valueType  the type of reference to use for values;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     *  @param capacity  the initial capacity for the map
     *  @param loadFactor  the load factor for the map
     *  @param purgeValues should the value be automatically purged when the 
     *   key is garbage collected 
     */
    public ReferenceMap(
                        int keyType, 
                        int valueType, 
                        int capacity, 
                        float loadFactor, 
                        boolean purgeValues) {
        this(keyType, valueType, capacity, loadFactor);
        this.purgeValues = purgeValues;
    }

    /**
     *  Constructs a new <code>ReferenceMap</code> with the
     *  specified reference types, load factor and initial
     *  capacity.
     *
     *  @param keyType  the type of reference to use for keys;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     *  @param valueType  the type of reference to use for values;
     *   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     *  @param capacity  the initial capacity for the map
     *  @param loadFactor  the load factor for the map
     */
    public ReferenceMap(int keyType, int valueType, int capacity, float loadFactor) {
        super();

        verify("keyType", keyType);
        verify("valueType", valueType);

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if ((loadFactor <= 0.0f) || (loadFactor >= 1.0f)) {
            throw new IllegalArgumentException("Load factor must be greater than 0 and less than 1.");
        }

        this.keyType = keyType;
        this.valueType = valueType;

        int v = 1;
        while (v < capacity) v *= 2;

        this.table = new Entry[v];
        this.loadFactor = loadFactor;
        this.threshold = (int)(v * loadFactor);
    }


    // used by constructor
    private static void verify(String name, int type) {
        if ((type < HARD) || (type > WEAK)) {
            throw new IllegalArgumentException(name + 
               " must be HARD, SOFT, WEAK.");
        }
    }


    /**
     *  Writes this object to the given output stream.
     *
     *  @param out  the output stream to write to
     *  @throws IOException  if the stream raises it
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(table.length);

        // Have to use null-terminated list because size might shrink
        // during iteration

        for (Iterator iter = entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            out.writeObject(entry.getKey());
            out.writeObject(entry.getValue());
        }
        out.writeObject(null);
    }


    /**
     *  Reads the contents of this object from the given input stream.
     *
     *  @param inp  the input stream to read from
     *  @throws IOException  if the stream raises it
     *  @throws ClassNotFoundException  if the stream raises it
     */
    private void readObject(ObjectInputStream inp) throws IOException, ClassNotFoundException {
        inp.defaultReadObject();
        table = new Entry[inp.readInt()];
        threshold = (int)(table.length * loadFactor);
        queue = new ReferenceQueue();
        Object key = inp.readObject();
        while (key != null) {
            Object value = inp.readObject();
            put(key, value);
            key = inp.readObject();
        }
    }


    /**
     *  Constructs a reference of the given type to the given 
     *  referent.  The reference is registered with the queue
     *  for later purging.
     *
     *  @param type  HARD, SOFT or WEAK
     *  @param referent  the object to refer to
     *  @param hash  the hash code of the <I>key</I> of the mapping;
     *    this number might be different from referent.hashCode() if
     *    the referent represents a value and not a key
     */
    private Object toReference(int type, Object referent, int hash) {
        switch (type) {
            case HARD: return referent;
            case SOFT: return new SoftRef(hash, referent, queue);
            case WEAK: return new WeakRef(hash, referent, queue);
            default: throw new Error();
        }
    }


    /**
     *  Returns the entry associated with the given key.
     *
     *  @param key  the key of the entry to look up
     *  @return  the entry associated with that key, or null
     *    if the key is not in this map
     */
    private Entry getEntry(Object key) {
        if (key == null) return null;
        int hash = key.hashCode();
        int index = indexFor(hash);
        for (Entry entry = table[index]; entry != null; entry = entry.next) {
            if ((entry.hash == hash) && key.equals(entry.getKey())) {
                return entry;
            }
        }
        return null;
    }


    /**
     *  Converts the given hash code into an index into the
     *  hash table.
     */
    private int indexFor(int hash) {
        // mix the bits to avoid bucket collisions...
        hash += ~(hash << 15);
        hash ^= (hash >>> 10);
        hash += (hash << 3);
        hash ^= (hash >>> 6);
        hash += ~(hash << 11);
        hash ^= (hash >>> 16);
        return hash & (table.length - 1);
    }



    /**
     *  Resizes this hash table by doubling its capacity.
     *  This is an expensive operation, as entries must
     *  be copied from the old smaller table to the new 
     *  bigger table.
     */
    private void resize() {
        Entry[] old = table;
        table = new Entry[old.length * 2];

        for (int i = 0; i < old.length; i++) {
            Entry next = old[i];
            while (next != null) {
                Entry entry = next;
                next = next.next;
                int index = indexFor(entry.hash);
                entry.next = table[index];
                table[index] = entry;
            }
            old[i] = null;
        }
        threshold = (int)(table.length * loadFactor);
    }



    /**
     * Purges stale mappings from this map.
     * <p>
     * Ordinarily, stale mappings are only removed during
     * a write operation, although this method is called for both
     * read and write operations to maintain a consistent state.
     * <p>
     * Note that this method is not synchronized!  Special
     * care must be taken if, for instance, you want stale
     * mappings to be removed on a periodic basis by some
     * background thread.
     */
    private void purge() {
        Reference ref = queue.poll();
        while (ref != null) {
            purge(ref);
            ref = queue.poll();
        }
    }


    private void purge(Reference ref) {
        // The hashCode of the reference is the hashCode of the
        // mapping key, even if the reference refers to the 
        // mapping value...
        int hash = ref.hashCode();
        int index = indexFor(hash);
        Entry previous = null;
        Entry entry = table[index];
        while (entry != null) {
            if (entry.purge(ref)) {
                if (previous == null) table[index] = entry.next;
                else previous.next = entry.next;
                this.size--;
                return;
            }
            previous = entry;
            entry = entry.next;
        }

    }


    /**
     *  Returns the size of this map.
     *
     *  @return  the size of this map
     */
    public int size() {
        purge();
        return size;
    }


    /**
     *  Returns <code>true</code> if this map is empty.
     *
     *  @return <code>true</code> if this map is empty
     */
    public boolean isEmpty() {
        purge();
        return size == 0;
    }


    /**
     *  Returns <code>true</code> if this map contains the given key.
     *
     *  @return true if the given key is in this map
     */
    public boolean containsKey(Object key) {
        purge();
        Entry entry = getEntry(key);
        if (entry == null) return false;
        return entry.getValue() != null;
    }


    /**
     *  Returns the value associated with the given key, if any.
     *
     *  @return the value associated with the given key, or <code>null</code>
     *   if the key maps to no value
     */
    public Object get(Object key) {
        purge();
        Entry entry = getEntry(key);
        if (entry == null) return null;
        return entry.getValue();
    }


    /**
     *  Associates the given key with the given value.<p>
     *  Neither the key nor the value may be null.
     *
     *  @param key  the key of the mapping
     *  @param value  the value of the mapping
     *  @return  the last value associated with that key, or 
     *   null if no value was associated with the key
     *  @throws NullPointerException if either the key or value
     *   is null
     */
    public Object put(Object key, Object value) {
        if (key == null) throw new NullPointerException("null keys not allowed");
        if (value == null) throw new NullPointerException("null values not allowed");

        purge();
        if (size + 1 > threshold) resize();

        int hash = key.hashCode();
        int index = indexFor(hash);
        Entry entry = table[index];
        while (entry != null) {
            if ((hash == entry.hash) && key.equals(entry.getKey())) {
                Object result = entry.getValue();
                entry.setValue(value);
                return result;
            }
            entry = entry.next;
        }
        this.size++; 
        modCount++;
        key = toReference(keyType, key, hash);
        value = toReference(valueType, value, hash);
        table[index] = new Entry(key, hash, value, table[index]);
        return null;
    }


    /**
     *  Removes the key and its associated value from this map.
     *
     *  @param key  the key to remove
     *  @return the value associated with that key, or null if
     *   the key was not in the map
     */
    public Object remove(Object key) {
        if (key == null) return null;
        purge();
        int hash = key.hashCode();
        int index = indexFor(hash);
        Entry previous = null;
        Entry entry = table[index];
        while (entry != null) {
            if ((hash == entry.hash) && key.equals(entry.getKey())) {
                if (previous == null) table[index] = entry.next;
                else previous.next = entry.next;
                this.size--;
                modCount++;
                return entry.getValue();
            }
            previous = entry;
            entry = entry.next;
        }
        return null;
    }


    /**
     *  Clears this map.
     */
    public void clear() {
        Arrays.fill(table, null);
        size = 0;
        while (queue.poll() != null); // drain the queue
    }


    /**
     *  Returns a set view of this map's entries.
     *
     *  @return a set view of this map's entries
     */
    public Set entrySet() {
        if (entrySet != null) {
            return entrySet;
        } 
        entrySet = new AbstractSet() {
            public int size() {
                return ReferenceMap.this.size();
            }

            public void clear() {
                ReferenceMap.this.clear();
            }

            public boolean contains(Object o) {
                if (o == null) return false;
                if (!(o instanceof Map.Entry)) return false;
                Map.Entry e = (Map.Entry)o;
                Entry e2 = getEntry(e.getKey());
                return (e2 != null) && e.equals(e2);
            }

            public boolean remove(Object o) {
                boolean r = contains(o);
                if (r) {
                    Map.Entry e = (Map.Entry)o;
                    ReferenceMap.this.remove(e.getKey());
                }
                return r;
            }

            public Iterator iterator() {
                return new EntryIterator();
            }

            public Object[] toArray() {
                return toArray(new Object[0]);
            }

            public Object[] toArray(Object[] arr) {
                ArrayList list = new ArrayList();
                Iterator iterator = iterator();
                while (iterator.hasNext()) {
                    Entry e = (Entry)iterator.next();
                    list.add(new DefaultMapEntry(e.getKey(), e.getValue()));
                }
                return list.toArray(arr);
            }
        };
        return entrySet;
    }


    /**
     *  Returns a set view of this map's keys.
     *
     *  @return a set view of this map's keys
     */
    public Set keySet() {
        if (keySet != null) return keySet;
        keySet = new AbstractSet() {
            public int size() {
                return ReferenceMap.this.size();
            }

            public Iterator iterator() {
                return new KeyIterator();
            }

            public boolean contains(Object o) {
                return containsKey(o);
            }


            public boolean remove(Object o) {
                Object r = ReferenceMap.this.remove(o);
                return r != null;
            }

            public void clear() {
                ReferenceMap.this.clear();
            }

            public Object[] toArray() {
                return toArray(new Object[0]);
            }

            public Object[] toArray(Object[] array) {
                Collection c = new ArrayList(size());
                for (Iterator it = iterator(); it.hasNext(); ) {
                    c.add(it.next());
                }
                return c.toArray(array);
            }
        };
        return keySet;
    }


    /**
     *  Returns a collection view of this map's values.
     *
     *  @return a collection view of this map's values.
     */
    public Collection values() {
        if (values != null) return values;
        values = new AbstractCollection()  {
            public int size() {
                return ReferenceMap.this.size();
            }

            public void clear() {
                ReferenceMap.this.clear();
            }

            public Iterator iterator() {
                return new ValueIterator();
            }

            public Object[] toArray() {
                return toArray(new Object[0]);
            }

            public Object[] toArray(Object[] array) {
                Collection c = new ArrayList(size());
                for (Iterator it = iterator(); it.hasNext(); ) {
                    c.add(it.next());
                }
                return c.toArray(array);
            }
        };
        return values;
    }


    // If getKey() or getValue() returns null, it means
    // the mapping is stale and should be removed.
    private class Entry implements Map.Entry, KeyValue {

        Object key;
        Object value;
        int hash;
        Entry next;


        public Entry(Object key, int hash, Object value, Entry next) {
            this.key = key;
            this.hash = hash;
            this.value = value;
            this.next = next;
        }


        public Object getKey() {
            return (keyType > HARD) ? ((Reference)key).get() : key;
        }


        public Object getValue() {
            return (valueType > HARD) ? ((Reference)value).get() : value;
        }


        public Object setValue(Object object) {
            Object old = getValue();
            if (valueType > HARD) ((Reference)value).clear();
            value = toReference(valueType, object, hash);
            return old;
        }


        public boolean equals(Object o) {
            if (o == null) return false;
            if (o == this) return true;
            if (!(o instanceof Map.Entry)) return false;
            
            Map.Entry entry = (Map.Entry)o;
            Object key = entry.getKey();
            Object value = entry.getValue();
            if ((key == null) || (value == null)) return false;
            return key.equals(getKey()) && value.equals(getValue());
        }


        public int hashCode() {
            Object v = getValue();
            return hash ^ ((v == null) ? 0 : v.hashCode());
        }


        public String toString() {
            return getKey() + "=" + getValue();
        }


        boolean purge(Reference ref) {
            boolean r = (keyType > HARD) && (key == ref);
            r = r || ((valueType > HARD) && (value == ref));
            if (r) {
                if (keyType > HARD) ((Reference)key).clear();
                if (valueType > HARD) {
                    ((Reference)value).clear();
                } else if (purgeValues) {
                    value = null;
                }
            }
            return r;
        }
    }


    private class EntryIterator implements Iterator {
        // These fields keep track of where we are in the table.
        int index;
        Entry entry;
        Entry previous;

        // These Object fields provide hard references to the
        // current and next entry; this assures that if hasNext()
        // returns true, next() will actually return a valid element.
        Object nextKey, nextValue;
        Object currentKey, currentValue;

        int expectedModCount;


        public EntryIterator() {
            index = (size() != 0 ? table.length : 0);
            // have to do this here!  size() invocation above
            // may have altered the modCount.
            expectedModCount = modCount;
        }


        public boolean hasNext() {
            checkMod();
            while (nextNull()) {
                Entry e = entry;
                int i = index;
                while ((e == null) && (i > 0)) {
                    i--;
                    e = table[i];
                }
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    currentValue = null;
                    return false;
                }
                nextKey = e.getKey();
                nextValue = e.getValue();
                if (nextNull()) entry = entry.next;
            }
            return true;
        }


        private void checkMod() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }


        private boolean nextNull() {
            return (nextKey == null) || (nextValue == null);
        }

        protected Entry nextEntry() {    
            checkMod();
            if (nextNull() && !hasNext()) throw new NoSuchElementException();
            previous = entry;
            entry = entry.next;
            currentKey = nextKey;
            currentValue = nextValue;
            nextKey = null;
            nextValue = null;
            return previous;
        }


        public Object next() {
            return nextEntry();
        }


        public void remove() {
            checkMod();
            if (previous == null) throw new IllegalStateException();
            ReferenceMap.this.remove(currentKey);
            previous = null;
            currentKey = null;
            currentValue = null;
            expectedModCount = modCount;
        }

    }


    private class ValueIterator extends EntryIterator {
        public Object next() {
            return nextEntry().getValue();
        }
    }


    private class KeyIterator extends EntryIterator {
        public Object next() {
            return nextEntry().getKey();
        }
    }



    // These two classes store the hashCode of the key of
    // of the mapping, so that after they're dequeued a quick
    // lookup of the bucket in the table can occur.


    private static class SoftRef extends SoftReference {
        private int hash;


        public SoftRef(int hash, Object r, ReferenceQueue q) {
            super(r, q);
            this.hash = hash;
        }


        public int hashCode() {
            return hash;
        }
    }


    private static class WeakRef extends WeakReference {
        private int hash;


        public WeakRef(int hash, Object r, ReferenceQueue q) {
            super(r, q);
            this.hash = hash;
        }


        public int hashCode() {
            return hash;
        }
    }


}
