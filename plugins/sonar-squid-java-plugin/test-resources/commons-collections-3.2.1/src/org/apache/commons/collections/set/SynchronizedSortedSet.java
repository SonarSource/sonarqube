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
package org.apache.commons.collections.set;

import java.util.Comparator;
import java.util.SortedSet;

import org.apache.commons.collections.collection.SynchronizedCollection;

/**
 * Decorates another <code>SortedSet</code> to synchronize its behaviour
 * for a multi-threaded environment.
 * <p>
 * Methods are synchronized, then forwarded to the decorated set.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public class SynchronizedSortedSet extends SynchronizedCollection implements SortedSet {

    /** Serialization version */
    private static final long serialVersionUID = 2775582861954500111L;

    /**
     * Factory method to create a synchronized set.
     * 
     * @param set  the set to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    public static SortedSet decorate(SortedSet set) {
        return new SynchronizedSortedSet(set);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * 
     * @param set  the set to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    protected SynchronizedSortedSet(SortedSet set) {
        super(set);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param set  the set to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if set is null
     */
    protected SynchronizedSortedSet(SortedSet set, Object lock) {
        super(set, lock);
    }

    /**
     * Gets the decorated set.
     * 
     * @return the decorated set
     */
    protected SortedSet getSortedSet() {
        return (SortedSet) collection;
    }

    //-----------------------------------------------------------------------
    public SortedSet subSet(Object fromElement, Object toElement) {
        synchronized (lock) {
            SortedSet set = getSortedSet().subSet(fromElement, toElement);
            // the lock is passed into the constructor here to ensure that the
            // subset is synchronized on the same lock as the parent
            return new SynchronizedSortedSet(set, lock);
        }
    }

    public SortedSet headSet(Object toElement) {
        synchronized (lock) {
            SortedSet set = getSortedSet().headSet(toElement);
            // the lock is passed into the constructor here to ensure that the
            // headset is synchronized on the same lock as the parent
            return new SynchronizedSortedSet(set, lock);
        }
    }

    public SortedSet tailSet(Object fromElement) {
        synchronized (lock) {
            SortedSet set = getSortedSet().tailSet(fromElement);
            // the lock is passed into the constructor here to ensure that the
            // tailset is synchronized on the same lock as the parent
            return new SynchronizedSortedSet(set, lock);
        }
    }

    public Object first() {
        synchronized (lock) {
            return getSortedSet().first();
        }
    }

    public Object last() {
        synchronized (lock) {
            return getSortedSet().last();
        }
    }

    public Comparator comparator() {
        synchronized (lock) {
            return getSortedSet().comparator();
        }
    }

}
