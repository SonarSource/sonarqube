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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.KeyValue;

/**
 * A StaticBucketMap is an efficient, thread-safe implementation of
 * <code>java.util.Map</code> that performs well in in a highly
 * thread-contentious environment.  The map supports very efficient
 * {@link #get(Object) get}, {@link #put(Object,Object) put}, 
 * {@link #remove(Object) remove} and {@link #containsKey(Object) containsKey}
 * operations, assuming (approximate) uniform hashing and
 * that the number of entries does not exceed the number of buckets.  If the
 * number of entries exceeds the number of buckets or if the hash codes of the
 * objects are not uniformly distributed, these operations have a worst case
 * scenario that is proportional to the number of elements in the map
 * (<i>O(n)</i>).<p>
 *
 * Each bucket in the hash table has its own monitor, so two threads can 
 * safely operate on the map at the same time, often without incurring any 
 * monitor contention.  This means that you don't have to wrap instances
 * of this class with {@link java.util.Collections#synchronizedMap(Map)};
 * instances are already thread-safe.  Unfortunately, however, this means 
 * that this map implementation behaves in ways you may find disconcerting.  
 * Bulk operations, such as {@link #putAll(Map) putAll} or the
 * {@link Collection#retainAll(Collection) retainAll} operation in collection 
 * views, are <i>not</i> atomic.  If two threads are simultaneously 
 * executing 
 *
 * <pre>
 *   staticBucketMapInstance.putAll(map);
 * </pre>
 *
 * and
 *
 * <pre>
 *   staticBucketMapInstance.entrySet().removeAll(map.entrySet());
 * </pre>
 *
 * then the results are generally random.  Those two statement could cancel
 * each other out, leaving <code>staticBucketMapInstance</code> essentially 
 * unchanged, or they could leave some random subset of <code>map</code> in 
 * <code>staticBucketMapInstance</code>.<p>
 *
 * Also, much like an encyclopedia, the results of {@link #size()} and 
 * {@link #isEmpty()} are out-of-date as soon as they are produced.<p>
 *
 * The iterators returned by the collection views of this class are <i>not</i>
 * fail-fast.  They will <i>never</i> raise a 
 * {@link java.util.ConcurrentModificationException}.  Keys and values 
 * added to the map after the iterator is created do not necessarily appear
 * during iteration.  Similarly, the iterator does not necessarily fail to 
 * return keys and values that were removed after the iterator was created.<p>
 *
 * Finally, unlike {@link java.util.HashMap}-style implementations, this
 * class <i>never</i> rehashes the map.  The number of buckets is fixed 
 * at construction time and never altered.  Performance may degrade if 
 * you do not allocate enough buckets upfront.<p>
 *
 * The {@link #atomic(Runnable)} method is provided to allow atomic iterations
 * and bulk operations; however, overuse of {@link #atomic(Runnable) atomic}
 * will basically result in a map that's slower than an ordinary synchronized
 * {@link java.util.HashMap}.
 *
 * Use this class if you do not require reliable bulk operations and 
 * iterations, or if you can make your own guarantees about how bulk 
 * operations will affect the map.<p>
 *
 * @since Commons Collections 3.0 (previously in main package v2.1)
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Berin Loritsch
 * @author Gerhard Froehlich
 * @author Michael A. Smith
 * @author Paul Jack
 * @author Leo Sutic
 * @author Janek Bogucki
 * @author Kazuya Ujihara
 */
public final class StaticBucketMap implements Map {

    /** The default number of buckets to use */
    private static final int DEFAULT_BUCKETS = 255;
    /** The array of buckets, where the actual data is held */
    private Node[] buckets;
    /** The matching array of locks */
    private Lock[] locks;

    /**
     * Initializes the map with the default number of buckets (255).
     */
    public StaticBucketMap() {
        this(DEFAULT_BUCKETS);
    }

    /**
     * Initializes the map with a specified number of buckets.  The number
     * of buckets is never below 17, and is always an odd number (StaticBucketMap
     * ensures this). The number of buckets is inversely proportional to the
     * chances for thread contention.  The fewer buckets, the more chances for
     * thread contention.  The more buckets the fewer chances for thread
     * contention.
     *
     * @param numBuckets  the number of buckets for this map
     */
    public StaticBucketMap(int numBuckets) {
        int size = Math.max(17, numBuckets);

        // Ensure that bucketSize is never a power of 2 (to ensure maximal distribution)
        if (size % 2 == 0) {
            size--;
        }

        buckets = new Node[size];
        locks = new Lock[size];

        for (int i = 0; i < size; i++) {
            locks[i] = new Lock();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Determine the exact hash entry for the key.  The hash algorithm
     * is rather simplistic, but it does the job:
     *
     * <pre>
     *   He = |Hk mod n|
     * </pre>
     *
     * <p>
     *   He is the entry's hashCode, Hk is the key's hashCode, and n is
     *   the number of buckets.
     * </p>
     */
    private final int getHash(Object key) {
        if (key == null) {
            return 0;
        }
        int hash = key.hashCode();
        hash += ~(hash << 15);
        hash ^= (hash >>> 10);
        hash += (hash << 3);
        hash ^= (hash >>> 6);
        hash += ~(hash << 11);
        hash ^= (hash >>> 16);
        hash %= buckets.length;
        return (hash < 0) ? hash * -1 : hash;
    }

    /**
     * Gets the current size of the map.
     * The value is computed fresh each time the method is called.
     * 
     * @return the current size
     */
    public int size() {
        int cnt = 0;

        for (int i = 0; i < buckets.length; i++) {
            cnt += locks[i].size;
        }
        return cnt;
    }

    /**
     * Checks if the size is currently zero.
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return (size() == 0);
    }

    /**
     * Gets the value associated with the key.
     * 
     * @param key  the key to retrieve
     * @return the associated value
     */
    public Object get(final Object key) {
        int hash = getHash(key);

        synchronized (locks[hash]) {
            Node n = buckets[hash];

            while (n != null) {
                if (n.key == key || (n.key != null && n.key.equals(key))) {
                    return n.value;
                }

                n = n.next;
            }
        }
        return null;
    }

    /**
     * Checks if the map contains the specified key.
     * 
     * @param key  the key to check
     * @return true if found
     */
    public boolean containsKey(final Object key) {
        int hash = getHash(key);

        synchronized (locks[hash]) {
            Node n = buckets[hash];

            while (n != null) {
                if (n.key == key || (n.key != null && n.key.equals(key))) {
                    return true;
                }

                n = n.next;
            }
        }
        return false;
    }

    /**
     * Checks if the map contains the specified value.
     * 
     * @param value  the value to check
     * @return true if found
     */
    public boolean containsValue(final Object value) {
        for (int i = 0; i < buckets.length; i++) {
            synchronized (locks[i]) {
                Node n = buckets[i];

                while (n != null) {
                    if (n.value == value || (n.value != null && n.value.equals(value))) {
                        return true;
                    }

                    n = n.next;
                }
            }
        }
        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Puts a new key value mapping into the map.
     * 
     * @param key  the key to use
     * @param value  the value to use
     * @return the previous mapping for the key
     */
    public Object put(final Object key, final Object value) {
        int hash = getHash(key);

        synchronized (locks[hash]) {
            Node n = buckets[hash];

            if (n == null) {
                n = new Node();
                n.key = key;
                n.value = value;
                buckets[hash] = n;
                locks[hash].size++;
                return null;
            }

            // Set n to the last node in the linked list.  Check each key along the way
            //  If the key is found, then change the value of that node and return
            //  the old value.
            for (Node next = n; next != null; next = next.next) {
                n = next;

                if (n.key == key || (n.key != null && n.key.equals(key))) {
                    Object returnVal = n.value;
                    n.value = value;
                    return returnVal;
                }
            }

            // The key was not found in the current list of nodes, add it to the end
            //  in a new node.
            Node newNode = new Node();
            newNode.key = key;
            newNode.value = value;
            n.next = newNode;
            locks[hash].size++;
        }
        return null;
    }

    /**
     * Removes the specified key from the map.
     * 
     * @param key  the key to remove
     * @return the previous value at this key
     */
    public Object remove(Object key) {
        int hash = getHash(key);

        synchronized (locks[hash]) {
            Node n = buckets[hash];
            Node prev = null;

            while (n != null) {
                if (n.key == key || (n.key != null && n.key.equals(key))) {
                    // Remove this node from the linked list of nodes.
                    if (null == prev) {
                        // This node was the head, set the next node to be the new head.
                        buckets[hash] = n.next;
                    } else {
                        // Set the next node of the previous node to be the node after this one.
                        prev.next = n.next;
                    }
                    locks[hash].size--;
                    return n.value;
                }

                prev = n;
                n = n.next;
            }
        }
        return null;
    }
    
    //-----------------------------------------------------------------------
    /**
     * Gets the key set.
     * 
     * @return the key set
     */
    public Set keySet() {
        return new KeySet();
    }

    /**
     * Gets the values.
     * 
     * @return the values
     */
    public Collection values() {
        return new Values();
    }

    /**
     * Gets the entry set.
     * 
     * @return the entry set
     */
    public Set entrySet() {
        return new EntrySet();
    }

    //-----------------------------------------------------------------------
    /**
     * Puts all the entries from the specified map into this map.
     * This operation is <b>not atomic</b> and may have undesired effects.
     * 
     * @param map  the map of entries to add
     */
    public void putAll(Map map) {
        Iterator i = map.keySet().iterator();

        while (i.hasNext()) {
            Object key = i.next();
            put(key, map.get(key));
        }
    }

    /**
     * Clears the map of all entries.
     */
    public void clear() {
        for (int i = 0; i < buckets.length; i++) {
            Lock lock = locks[i];
            synchronized (lock) {
                buckets[i] = null;
                lock.size = 0;
            }
        }
    }

    /**
     * Compares this map to another, as per the Map specification.
     * 
     * @param obj  the object to compare to
     * @return true if equal
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Map == false) {
            return false;
        }
        Map other = (Map) obj;
        return entrySet().equals(other.entrySet());
    }

    /**
     * Gets the hash code, as per the Map specification.
     * 
     * @return the hash code
     */
    public int hashCode() {
        int hashCode = 0;

        for (int i = 0; i < buckets.length; i++) {
            synchronized (locks[i]) {
                Node n = buckets[i];

                while (n != null) {
                    hashCode += n.hashCode();
                    n = n.next;
                }
            }
        }
        return hashCode;
    }

    //-----------------------------------------------------------------------
    /**
     * The Map.Entry for the StaticBucketMap.
     */
    private static final class Node implements Map.Entry, KeyValue {
        protected Object key;
        protected Object value;
        protected Node next;

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public int hashCode() {
            return ((key == null ? 0 : key.hashCode()) ^
                    (value == null ? 0 : value.hashCode()));
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Map.Entry == false) {
                return false;
            }

            Map.Entry e2 = (Map.Entry) obj;
            return (
                (key == null ? e2.getKey() == null : key.equals(e2.getKey())) &&
                (value == null ? e2.getValue() == null : value.equals(e2.getValue())));
        }

        public Object setValue(Object obj) {
            Object retVal = value;
            value = obj;
            return retVal;
        }
    }


    /**
     * The lock object, which also includes a count of the nodes in this lock.
     */
    private final static class Lock {
        public int size;
    }


    //-----------------------------------------------------------------------
    private class EntryIterator implements Iterator {

        private ArrayList current = new ArrayList();
        private int bucket;
        private Map.Entry last;


        public boolean hasNext() {
            if (current.size() > 0) return true;
            while (bucket < buckets.length) {
                synchronized (locks[bucket]) {
                    Node n = buckets[bucket];
                    while (n != null) {
                        current.add(n);
                        n = n.next;
                    }
                    bucket++;
                    if (current.size() > 0) return true;
                }
            }
            return false;
        }

        protected Map.Entry nextEntry() {
            if (!hasNext()) throw new NoSuchElementException();
            last = (Map.Entry)current.remove(current.size() - 1);
            return last;
        }

        public Object next() {
            return nextEntry();
        }

        public void remove() {
            if (last == null) throw new IllegalStateException();
            StaticBucketMap.this.remove(last.getKey());
            last = null;
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

    private class EntrySet extends AbstractSet {

        public int size() {
            return StaticBucketMap.this.size();
        }

        public void clear() {
            StaticBucketMap.this.clear();
        }

        public Iterator iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object obj) {
            Map.Entry entry = (Map.Entry) obj;
            int hash = getHash(entry.getKey());
            synchronized (locks[hash]) {
                for (Node n = buckets[hash]; n != null; n = n.next) {
                    if (n.equals(entry)) return true;
                }
            }
            return false;
        }

        public boolean remove(Object obj) {
            if (obj instanceof Map.Entry == false) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            int hash = getHash(entry.getKey());
            synchronized (locks[hash]) {
                for (Node n = buckets[hash]; n != null; n = n.next) {
                    if (n.equals(entry)) {
                        StaticBucketMap.this.remove(n.getKey());
                        return true;
                    }
                }
            }
            return false;
        }

    }


    private class KeySet extends AbstractSet {

        public int size() {
            return StaticBucketMap.this.size();
        }

        public void clear() {
            StaticBucketMap.this.clear();
        }

        public Iterator iterator() {
            return new KeyIterator();
        }

        public boolean contains(Object obj) {
            return StaticBucketMap.this.containsKey(obj);
        }

        public boolean remove(Object obj) {
            int hash = getHash(obj);
            synchronized (locks[hash]) {
                for (Node n = buckets[hash]; n != null; n = n.next) {
                    Object k = n.getKey();
                    if ((k == obj) || ((k != null) && k.equals(obj))) {
                        StaticBucketMap.this.remove(k);
                        return true;
                    }
                }
            }
            return false;

        }

    }


    private class Values extends AbstractCollection {

        public int size() {
            return StaticBucketMap.this.size();
        }

        public void clear() {
            StaticBucketMap.this.clear();
        }

        public Iterator iterator() {
            return new ValueIterator();
        }

    }


    /**
     *  Prevents any operations from occurring on this map while the
     *  given {@link Runnable} executes.  This method can be used, for
     *  instance, to execute a bulk operation atomically: 
     *
     *  <pre>
     *    staticBucketMapInstance.atomic(new Runnable() {
     *        public void run() {
     *            staticBucketMapInstance.putAll(map);
     *        }
     *    });
     *  </pre>
     *
     *  It can also be used if you need a reliable iterator:
     *
     *  <pre>
     *    staticBucketMapInstance.atomic(new Runnable() {
     *        public void run() {
     *            Iterator iterator = staticBucketMapInstance.iterator();
     *            while (iterator.hasNext()) {
     *                foo(iterator.next();
     *            }
     *        }
     *    });
     *  </pre>
     *
     *  <b>Implementation note:</b> This method requires a lot of time
     *  and a ton of stack space.  Essentially a recursive algorithm is used
     *  to enter each bucket's monitor.  If you have twenty thousand buckets
     *  in your map, then the recursive method will be invoked twenty thousand
     *  times.  You have been warned.
     *
     *  @param r  the code to execute atomically
     */
    public void atomic(Runnable r) {
        if (r == null) throw new NullPointerException();
        atomic(r, 0);
    }

    private void atomic(Runnable r, int bucket) {
        if (bucket >= buckets.length) {
            r.run();
            return;
        }
        synchronized (locks[bucket]) {
            atomic(r, bucket + 1);
        }
    }

}
