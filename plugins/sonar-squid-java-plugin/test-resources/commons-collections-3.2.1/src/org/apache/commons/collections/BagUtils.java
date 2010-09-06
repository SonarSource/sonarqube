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

import org.apache.commons.collections.bag.HashBag;
import org.apache.commons.collections.bag.PredicatedBag;
import org.apache.commons.collections.bag.PredicatedSortedBag;
import org.apache.commons.collections.bag.SynchronizedBag;
import org.apache.commons.collections.bag.SynchronizedSortedBag;
import org.apache.commons.collections.bag.TransformedBag;
import org.apache.commons.collections.bag.TransformedSortedBag;
import org.apache.commons.collections.bag.TreeBag;
import org.apache.commons.collections.bag.TypedBag;
import org.apache.commons.collections.bag.TypedSortedBag;
import org.apache.commons.collections.bag.UnmodifiableBag;
import org.apache.commons.collections.bag.UnmodifiableSortedBag;

/**
 * Provides utility methods and decorators for
 * {@link Bag} and {@link SortedBag} instances.
 *
 * @since Commons Collections 2.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Paul Jack
 * @author Stephen Colebourne
 * @author Andrew Freeman
 * @author Matthew Hawthorne
 */
public class BagUtils {

    /**
     * An empty unmodifiable bag.
     */
    public static final Bag EMPTY_BAG = UnmodifiableBag.decorate(new HashBag());

    /**
     * An empty unmodifiable sorted bag.
     */
    public static final Bag EMPTY_SORTED_BAG = UnmodifiableSortedBag.decorate(new TreeBag());

    /**
     * Instantiation of BagUtils is not intended or required.
     * However, some tools require an instance to operate.
     */
    public BagUtils() {
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a synchronized (thread-safe) bag backed by the given bag.
     * In order to guarantee serial access, it is critical that all 
     * access to the backing bag is accomplished through the returned bag.
     * <p>
     * It is imperative that the user manually synchronize on the returned
     * bag when iterating over it:
     *
     * <pre>
     * Bag bag = BagUtils.synchronizedBag(new HashBag());
     * ...
     * synchronized(bag) {
     *     Iterator i = bag.iterator(); // Must be in synchronized block
     *     while (i.hasNext())
     *         foo(i.next());
     *     }
     * }
     * </pre>
     *
     * Failure to follow this advice may result in non-deterministic 
     * behavior.
     *
     * @param bag  the bag to synchronize, must not be null
     * @return a synchronized bag backed by that bag
     * @throws IllegalArgumentException  if the Bag is null
     */
    public static Bag synchronizedBag(Bag bag) {
        return SynchronizedBag.decorate(bag);
    }

    /**
     * Returns an unmodifiable view of the given bag.  Any modification
     * attempts to the returned bag will raise an 
     * {@link UnsupportedOperationException}.
     *
     * @param bag  the bag whose unmodifiable view is to be returned, must not be null
     * @return an unmodifiable view of that bag
     * @throws IllegalArgumentException  if the Bag is null
     */
    public static Bag unmodifiableBag(Bag bag) {
        return UnmodifiableBag.decorate(bag);
    }
    
    /**
     * Returns a predicated (validating) bag backed by the given bag.
     * <p>
     * Only objects that pass the test in the given predicate can be added to the bag.
     * Trying to add an invalid object results in an IllegalArgumentException.
     * It is important not to use the original bag after invoking this method,
     * as it is a backdoor for adding invalid objects.
     *
     * @param bag  the bag to predicate, must not be null
     * @param predicate  the predicate for the bag, must not be null
     * @return a predicated bag backed by the given bag
     * @throws IllegalArgumentException  if the Bag or Predicate is null
     */
    public static Bag predicatedBag(Bag bag, Predicate predicate) {
        return PredicatedBag.decorate(bag, predicate);
    }
    
    /**
     * Returns a typed bag backed by the given bag.
     * <p>
     * Only objects of the specified type can be added to the bag.
     * 
     * @param bag  the bag to limit to a specific type, must not be null
     * @param type  the type of objects which may be added to the bag
     * @return a typed bag backed by the specified bag
     */
    public static Bag typedBag(Bag bag, Class type) {
        return TypedBag.decorate(bag, type);
    }
    
    /**
     * Returns a transformed bag backed by the given bag.
     * <p>
     * Each object is passed through the transformer as it is added to the
     * Bag. It is important not to use the original bag after invoking this 
     * method, as it is a backdoor for adding untransformed objects.
     *
     * @param bag  the bag to predicate, must not be null
     * @param transformer  the transformer for the bag, must not be null
     * @return a transformed bag backed by the given bag
     * @throws IllegalArgumentException  if the Bag or Transformer is null
     */
    public static Bag transformedBag(Bag bag, Transformer transformer) {
        return TransformedBag.decorate(bag, transformer);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Returns a synchronized (thread-safe) sorted bag backed by the given 
     * sorted bag.
     * In order to guarantee serial access, it is critical that all 
     * access to the backing bag is accomplished through the returned bag.
     * <p>
     * It is imperative that the user manually synchronize on the returned
     * bag when iterating over it:
     *
     * <pre>
     * SortedBag bag = BagUtils.synchronizedSortedBag(new TreeBag());
     * ...
     * synchronized(bag) {
     *     Iterator i = bag.iterator(); // Must be in synchronized block
     *     while (i.hasNext())
     *         foo(i.next());
     *     }
     * }
     * </pre>
     *
     * Failure to follow this advice may result in non-deterministic 
     * behavior.
     *
     * @param bag  the bag to synchronize, must not be null
     * @return a synchronized bag backed by that bag
     * @throws IllegalArgumentException  if the SortedBag is null
     */
    public static SortedBag synchronizedSortedBag(SortedBag bag) {
        return SynchronizedSortedBag.decorate(bag);
    }
    
    /**
     * Returns an unmodifiable view of the given sorted bag.  Any modification
     * attempts to the returned bag will raise an 
     * {@link UnsupportedOperationException}.
     *
     * @param bag  the bag whose unmodifiable view is to be returned, must not be null
     * @return an unmodifiable view of that bag
     * @throws IllegalArgumentException  if the SortedBag is null
     */
    public static SortedBag unmodifiableSortedBag(SortedBag bag) {
        return UnmodifiableSortedBag.decorate(bag);
    }
    
    /**
     * Returns a predicated (validating) sorted bag backed by the given sorted bag.
     * <p>
     * Only objects that pass the test in the given predicate can be added to the bag.
     * Trying to add an invalid object results in an IllegalArgumentException.
     * It is important not to use the original bag after invoking this method,
     * as it is a backdoor for adding invalid objects.
     *
     * @param bag  the sorted bag to predicate, must not be null
     * @param predicate  the predicate for the bag, must not be null
     * @return a predicated bag backed by the given bag
     * @throws IllegalArgumentException  if the SortedBag or Predicate is null
     */
    public static SortedBag predicatedSortedBag(SortedBag bag, Predicate predicate) {
        return PredicatedSortedBag.decorate(bag, predicate);
    }
    
    /**
     * Returns a typed sorted bag backed by the given bag.
     * <p>
     * Only objects of the specified type can be added to the bag.
     * 
     * @param bag  the bag to limit to a specific type, must not be null
     * @param type  the type of objects which may be added to the bag
     * @return a typed bag backed by the specified bag
     */
    public static SortedBag typedSortedBag(SortedBag bag, Class type) {
        return TypedSortedBag.decorate(bag, type);
    }
    
    /**
     * Returns a transformed sorted bag backed by the given bag.
     * <p>
     * Each object is passed through the transformer as it is added to the
     * Bag. It is important not to use the original bag after invoking this 
     * method, as it is a backdoor for adding untransformed objects.
     *
     * @param bag  the bag to predicate, must not be null
     * @param transformer  the transformer for the bag, must not be null
     * @return a transformed bag backed by the given bag
     * @throws IllegalArgumentException  if the Bag or Transformer is null
     */
    public static SortedBag transformedSortedBag(SortedBag bag, Transformer transformer) {
        return TransformedSortedBag.decorate(bag, transformer);
    }
    
}
