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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.collection.CompositeCollection;

/**
 * Decorates a set of other sets to provide a single unified view.
 * <p>
 * Changes made to this set will actually be made on the decorated set.
 * Add operations require the use of a pluggable strategy.
 * If no strategy is provided then add is unsupported.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Brian McCallister
 */
public class CompositeSet extends CompositeCollection implements Set {
    /**
     * Create an empty CompositeSet
     */
    public CompositeSet() {
        super();
    }
    
    /**
     * Create a CompositeSet with just <code>set</code> composited
     * @param set The initial set in the composite
     */
    public CompositeSet(Set set) {
        super(set);
    }
    
    /**
     * Create a composite set with sets as the initial set of composited Sets
     */
    public CompositeSet(Set[] sets) {
        super(sets);
    }
    
    /**
     * Add a Set to this composite
     *
     * @param c Must implement Set
     * @throws IllegalArgumentException if c does not implement java.util.Set
     *         or if a SetMutator is set, but fails to resolve a collision
     * @throws UnsupportedOperationException if there is no SetMutator set, or
     *         a CollectionMutator is set instead of a SetMutator
     * @see org.apache.commons.collections.collection.CompositeCollection.CollectionMutator
     * @see SetMutator
     */
    public synchronized void addComposited(Collection c) {
        if (!(c instanceof Set)) {
            throw new IllegalArgumentException("Collections added must implement java.util.Set");
        }
        
        for (Iterator i = this.getCollections().iterator(); i.hasNext();) {
            Set set = (Set) i.next();
            Collection intersects = CollectionUtils.intersection(set, c);
            if (intersects.size() > 0) {
                if (this.mutator == null) {
                    throw new UnsupportedOperationException(
                        "Collision adding composited collection with no SetMutator set");
                }
                else if (!(this.mutator instanceof SetMutator)) {
                    throw new UnsupportedOperationException(
                        "Collision adding composited collection to a CompositeSet with a CollectionMutator instead of a SetMutator");
                }
                ((SetMutator) this.mutator).resolveCollision(this, set, (Set) c, intersects);
                if (CollectionUtils.intersection(set, c).size() > 0) {
                    throw new IllegalArgumentException(
                        "Attempt to add illegal entry unresolved by SetMutator.resolveCollision()");
                }
            }
        }
        super.addComposited(new Collection[]{c});
    }
    
    /**
     * Add two sets to this composite
     *
     * @throws IllegalArgumentException if c or d does not implement java.util.Set
     */
    public synchronized void addComposited(Collection c, Collection d) {
        if (!(c instanceof Set)) throw new IllegalArgumentException("Argument must implement java.util.Set");
        if (!(d instanceof Set)) throw new IllegalArgumentException("Argument must implement java.util.Set");
        this.addComposited(new Set[]{(Set) c, (Set) d});
    }
    
    /**
     * Add an array of sets to this composite
     * @param comps
     * @throws IllegalArgumentException if any of the collections in comps do not implement Set
     */
    public synchronized void addComposited(Collection[] comps) {
        for (int i = comps.length - 1; i >= 0; --i) {
            this.addComposited(comps[i]);
        }
    }
    
    /**
     * This can receive either a <code>CompositeCollection.CollectionMutator</code>
     * or a <code>CompositeSet.SetMutator</code>. If a
     * <code>CompositeCollection.CollectionMutator</code> is used than conflicts when adding
     * composited sets will throw IllegalArgumentException
     * <p>
     */
    public void setMutator(CollectionMutator mutator) {
        super.setMutator(mutator);
    }
    
    /* Set operations */
    
    /**
     * If a <code>CollectionMutator</code> is defined for this CompositeSet then this
     * method will be called anyway.
     *
     * @param obj Object to be removed
     * @return true if the object is removed, false otherwise
     */
    public boolean remove(Object obj) {
        for (Iterator i = this.getCollections().iterator(); i.hasNext();) {
            Set set = (Set) i.next();
            if (set.contains(obj)) return set.remove(obj);
        }
        return false;
    }
    
    
    /**
     * @see Set#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof Set) {
            Set set = (Set) obj;
            if (set.containsAll(this) && set.size() == this.size()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @see Set#hashCode
     */
    public int hashCode() {
        int code = 0;
        for (Iterator i = this.iterator(); i.hasNext();) {
            Object next = i.next();
            code += (next != null ? next.hashCode() : 0);
        }
        return code;
    }
    
    /**
     * Define callbacks for mutation operations.
     * <p>
     * Defining remove() on implementations of SetMutator is pointless
     * as they are never called by CompositeSet.
     */
    public static interface SetMutator extends CompositeCollection.CollectionMutator {
        /**
         * <p>
         * Called when a Set is added to the CompositeSet and there is a
         * collision between existing and added sets.
         * </p>
         * <p>
         * If <code>added</code> and <code>existing</code> still have any intersects
         * after this method returns an IllegalArgumentException will be thrown.
         * </p>
         * @param comp The CompositeSet being modified
         * @param existing The Set already existing in the composite
         * @param added the Set being added to the composite
         * @param intersects the intersection of th existing and added sets
         */
        public void resolveCollision(CompositeSet comp, Set existing, Set added, Collection intersects);
    }
}
