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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.collections.set.PredicatedSet;
import org.apache.commons.collections.set.PredicatedSortedSet;
import org.apache.commons.collections.set.SynchronizedSet;
import org.apache.commons.collections.set.SynchronizedSortedSet;
import org.apache.commons.collections.set.TransformedSet;
import org.apache.commons.collections.set.TransformedSortedSet;
import org.apache.commons.collections.set.TypedSet;
import org.apache.commons.collections.set.TypedSortedSet;
import org.apache.commons.collections.set.UnmodifiableSet;
import org.apache.commons.collections.set.UnmodifiableSortedSet;

/**
 * Provides utility methods and decorators for
 * {@link Set} and {@link SortedSet} instances.
 *
 * @since Commons Collections 2.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Paul Jack
 * @author Stephen Colebourne
 * @author Neil O'Toole
 * @author Matthew Hawthorne
 */
public class SetUtils {

    /**
     * An empty unmodifiable set.
     * This uses the {@link Collections} implementation 
     * and is provided for completeness.
     */
    public static final Set EMPTY_SET = Collections.EMPTY_SET;
    /**
     * An empty unmodifiable sorted set.
     * This is not provided in the JDK.
     */
    public static final SortedSet EMPTY_SORTED_SET = UnmodifiableSortedSet.decorate(new TreeSet());

    /**
     * <code>SetUtils</code> should not normally be instantiated.
     */
    public SetUtils() {
    }

    //-----------------------------------------------------------------------
    /**
     * Tests two sets for equality as per the <code>equals()</code> contract
     * in {@link java.util.Set#equals(java.lang.Object)}.
     * <p>
     * This method is useful for implementing <code>Set</code> when you cannot
     * extend AbstractSet. The method takes Collection instances to enable other
     * collection types to use the Set implementation algorithm.
     * <p>
     * The relevant text (slightly paraphrased as this is a static method) is:
     * <blockquote>
     * <p>Two sets are considered equal if they have
     * the same size, and every member of the first set is contained in
     * the second. This ensures that the <tt>equals</tt> method works
     * properly across different implementations of the <tt>Set</tt>
     * interface.</p>
     * 
     * <p>
     * This implementation first checks if the two sets are the same object: 
     * if so it returns <tt>true</tt>.  Then, it checks if the two sets are
     * identical in size; if not, it returns false. If so, it returns
     * <tt>a.containsAll((Collection) b)</tt>.</p>
     * </blockquote>
     * 
     * @see java.util.Set
     * @param set1  the first set, may be null
     * @param set2  the second set, may be null
     * @return whether the sets are equal by value comparison
     */
    public static boolean isEqualSet(final Collection set1, final Collection set2) {
        if (set1 == set2) {
            return true;
        }
        if (set1 == null || set2 == null || set1.size() != set2.size()) {
            return false;
        }

        return set1.containsAll(set2);
    }

    /**
     * Generates a hash code using the algorithm specified in 
     * {@link java.util.Set#hashCode()}.
     * <p>
     * This method is useful for implementing <code>Set</code> when you cannot
     * extend AbstractSet. The method takes Collection instances to enable other
     * collection types to use the Set implementation algorithm.
     * 
     * @see java.util.Set#hashCode()
     * @param set  the set to calculate the hash code for, may be null
     * @return the hash code
     */
    public static int hashCodeForSet(final Collection set) {
        if (set == null) {
            return 0;
        }
        int hashCode = 0;
        Iterator it = set.iterator();
        Object obj = null;

        while (it.hasNext()) {
            obj = it.next();
            if (obj != null) {
                hashCode += obj.hashCode();
            }
        }
        return hashCode;
    }
    
    //-----------------------------------------------------------------------
    /**
     * Returns a synchronized set backed by the given set.
     * <p>
     * You must manually synchronize on the returned buffer's iterator to 
     * avoid non-deterministic behavior:
     *  
     * <pre>
     * Set s = SetUtils.synchronizedSet(mySet);
     * synchronized (s) {
     *     Iterator i = s.iterator();
     *     while (i.hasNext()) {
     *         process (i.next());
     *     }
     * }
     * </pre>
     * 
     * This method uses the implementation in the decorators subpackage.
     * 
     * @param set  the set to synchronize, must not be null
     * @return a synchronized set backed by the given set
     * @throws IllegalArgumentException  if the set is null
     */
    public static Set synchronizedSet(Set set) {
        return SynchronizedSet.decorate(set);
    }

    /**
     * Returns an unmodifiable set backed by the given set.
     * <p>
     * This method uses the implementation in the decorators subpackage.
     *
     * @param set  the set to make unmodifiable, must not be null
     * @return an unmodifiable set backed by the given set
     * @throws IllegalArgumentException  if the set is null
     */
    public static Set unmodifiableSet(Set set) {
        return UnmodifiableSet.decorate(set);
    }

    /**
     * Returns a predicated (validating) set backed by the given set.
     * <p>
     * Only objects that pass the test in the given predicate can be added to the set.
     * Trying to add an invalid object results in an IllegalArgumentException.
     * It is important not to use the original set after invoking this method,
     * as it is a backdoor for adding invalid objects.
     *
     * @param set  the set to predicate, must not be null
     * @param predicate  the predicate for the set, must not be null
     * @return a predicated set backed by the given set
     * @throws IllegalArgumentException  if the Set or Predicate is null
     */
    public static Set predicatedSet(Set set, Predicate predicate) {
        return PredicatedSet.decorate(set, predicate);
    }

    /**
     * Returns a typed set backed by the given set.
     * <p>
     * Only objects of the specified type can be added to the set.
     * 
     * @param set  the set to limit to a specific type, must not be null
     * @param type  the type of objects which may be added to the set
     * @return a typed set backed by the specified set
     */
    public static Set typedSet(Set set, Class type) {
        return TypedSet.decorate(set, type);
    }
    
    /**
     * Returns a transformed set backed by the given set.
     * <p>
     * Each object is passed through the transformer as it is added to the
     * Set. It is important not to use the original set after invoking this 
     * method, as it is a backdoor for adding untransformed objects.
     *
     * @param set  the set to transform, must not be null
     * @param transformer  the transformer for the set, must not be null
     * @return a transformed set backed by the given set
     * @throws IllegalArgumentException  if the Set or Transformer is null
     */
    public static Set transformedSet(Set set, Transformer transformer) {
        return TransformedSet.decorate(set, transformer);
    }
    
    /**
     * Returns a set that maintains the order of elements that are added
     * backed by the given set.
     * <p>
     * If an element is added twice, the order is determined by the first add.
     * The order is observed through the iterator or toArray.
     *
     * @param set  the set to order, must not be null
     * @return an ordered set backed by the given set
     * @throws IllegalArgumentException  if the Set is null
     */
    public static Set orderedSet(Set set) {
        return ListOrderedSet.decorate(set);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Returns a synchronized sorted set backed by the given sorted set.
     * <p>
     * You must manually synchronize on the returned buffer's iterator to 
     * avoid non-deterministic behavior:
     *  
     * <pre>
     * Set s = SetUtils.synchronizedSet(mySet);
     * synchronized (s) {
     *     Iterator i = s.iterator();
     *     while (i.hasNext()) {
     *         process (i.next());
     *     }
     * }
     * </pre>
     * 
     * This method uses the implementation in the decorators subpackage.
     * 
     * @param set  the sorted set to synchronize, must not be null
     * @return a synchronized set backed by the given set
     * @throws IllegalArgumentException  if the set is null
     */
    public static SortedSet synchronizedSortedSet(SortedSet set) {
        return SynchronizedSortedSet.decorate(set);
    }

    /**
     * Returns an unmodifiable sorted set backed by the given sorted set.
     * <p>
     * This method uses the implementation in the decorators subpackage.
     *
     * @param set  the sorted set to make unmodifiable, must not be null
     * @return an unmodifiable set backed by the given set
     * @throws IllegalArgumentException  if the set is null
     */
    public static SortedSet unmodifiableSortedSet(SortedSet set) {
        return UnmodifiableSortedSet.decorate(set);
    }

    /**
     * Returns a predicated (validating) sorted set backed by the given sorted set.  
     * <p>
     * Only objects that pass the test in the given predicate can be added to the set.
     * Trying to add an invalid object results in an IllegalArgumentException.
     * It is important not to use the original set after invoking this method,
     * as it is a backdoor for adding invalid objects.
     *
     * @param set  the sorted set to predicate, must not be null
     * @param predicate  the predicate for the sorted set, must not be null
     * @return a predicated sorted set backed by the given sorted set
     * @throws IllegalArgumentException  if the Set or Predicate is null
     */
    public static SortedSet predicatedSortedSet(SortedSet set, Predicate predicate) {
        return PredicatedSortedSet.decorate(set, predicate);
    }

    /**
     * Returns a typed sorted set backed by the given set.
     * <p>
     * Only objects of the specified type can be added to the set.
     * 
     * @param set  the set to limit to a specific type, must not be null
     * @param type  the type of objects which may be added to the set
     * @return a typed set backed by the specified set
     */
    public static SortedSet typedSortedSet(SortedSet set, Class type) {
        return TypedSortedSet.decorate(set, type);
    }
    
    /**
     * Returns a transformed sorted set backed by the given set.
     * <p>
     * Each object is passed through the transformer as it is added to the
     * Set. It is important not to use the original set after invoking this 
     * method, as it is a backdoor for adding untransformed objects.
     *
     * @param set  the set to transform, must not be null
     * @param transformer  the transformer for the set, must not be null
     * @return a transformed set backed by the given set
     * @throws IllegalArgumentException  if the Set or Transformer is null
     */
    public static SortedSet transformedSortedSet(SortedSet set, Transformer transformer) {
        return TransformedSortedSet.decorate(set, transformer);
    }
    
}
