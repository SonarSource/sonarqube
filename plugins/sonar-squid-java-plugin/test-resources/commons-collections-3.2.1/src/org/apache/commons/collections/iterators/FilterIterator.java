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
import java.util.NoSuchElementException;

import org.apache.commons.collections.Predicate;

/** 
 * Decorates another {@link Iterator} using a predicate to filter elements.
 * <p>
 * This iterator decorates the underlying iterator, only allowing through
 * those elements that match the specified {@link Predicate Predicate}.
 *
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author James Strachan
 * @author Jan Sorensen
 * @author Ralph Wagner
 * @author Stephen Colebourne
 */
public class FilterIterator implements Iterator {

    /** The iterator being used */
    private Iterator iterator;
    /** The predicate being used */
    private Predicate predicate;
    /** The next object in the iteration */
    private Object nextObject;
    /** Whether the next object has been calculated yet */
    private boolean nextObjectSet = false;

    //-----------------------------------------------------------------------
    /**
     * Constructs a new <code>FilterIterator</code> that will not function
     * until {@link #setIterator(Iterator) setIterator} is invoked.
     */
    public FilterIterator() {
        super();
    }

    /**
     * Constructs a new <code>FilterIterator</code> that will not function
     * until {@link #setPredicate(Predicate) setPredicate} is invoked.
     *
     * @param iterator  the iterator to use
     */
    public FilterIterator(Iterator iterator) {
        super();
        this.iterator = iterator;
    }

    /**
     * Constructs a new <code>FilterIterator</code> that will use the
     * given iterator and predicate.
     *
     * @param iterator  the iterator to use
     * @param predicate  the predicate to use
     */
    public FilterIterator(Iterator iterator, Predicate predicate) {
        super();
        this.iterator = iterator;
        this.predicate = predicate;
    }

    //-----------------------------------------------------------------------
    /** 
     * Returns true if the underlying iterator contains an object that 
     * matches the predicate.
     *
     * @return true if there is another object that matches the predicate
     * @throws NullPointerException if either the iterator or predicate are null
     */
    public boolean hasNext() {
        if (nextObjectSet) {
            return true;
        } else {
            return setNextObject();
        }
    }

    /** 
     * Returns the next object that matches the predicate.
     *
     * @return the next object which matches the given predicate
     * @throws NullPointerException if either the iterator or predicate are null
     * @throws NoSuchElementException if there are no more elements that
     *  match the predicate 
     */
    public Object next() {
        if (!nextObjectSet) {
            if (!setNextObject()) {
                throw new NoSuchElementException();
            }
        }
        nextObjectSet = false;
        return nextObject;
    }

    /**
     * Removes from the underlying collection of the base iterator the last
     * element returned by this iterator.
     * This method can only be called
     * if <code>next()</code> was called, but not after
     * <code>hasNext()</code>, because the <code>hasNext()</code> call
     * changes the base iterator.
     *
     * @throws IllegalStateException if <code>hasNext()</code> has already
     *  been called.
     */
    public void remove() {
        if (nextObjectSet) {
            throw new IllegalStateException("remove() cannot be called");
        }
        iterator.remove();
    }

    //-----------------------------------------------------------------------
    /** 
     * Gets the iterator this iterator is using.
     *
     * @return the iterator
     */
    public Iterator getIterator() {
        return iterator;
    }

    /** 
     * Sets the iterator for this iterator to use.
     * If iteration has started, this effectively resets the iterator.
     *
     * @param iterator  the iterator to use
     */
    public void setIterator(Iterator iterator) {
        this.iterator = iterator;
        nextObject = null;
        nextObjectSet = false;
    }

    //-----------------------------------------------------------------------
    /** 
     * Gets the predicate this iterator is using.
     *
     * @return the predicate
     */
    public Predicate getPredicate() {
        return predicate;
    }

    /** 
     * Sets the predicate this the iterator to use.
     *
     * @param predicate  the predicate to use
     */
    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
        nextObject = null;
        nextObjectSet = false;
    }

    //-----------------------------------------------------------------------
    /**
     * Set nextObject to the next object. If there are no more 
     * objects then return false. Otherwise, return true.
     */
    private boolean setNextObject() {
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (predicate.evaluate(object)) {
                nextObject = object;
                nextObjectSet = true;
                return true;
            }
        }
        return false;
    }

}
