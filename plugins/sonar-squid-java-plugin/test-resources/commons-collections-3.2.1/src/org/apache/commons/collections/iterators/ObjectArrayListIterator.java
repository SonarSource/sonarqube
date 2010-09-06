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

import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections.ResettableListIterator;

/**
 * Implements a {@link ListIterator} over an array of objects.
 * <p>
 * This iterator does not support {@link #add} or {@link #remove}, as the object array 
 * cannot be structurally modified. The {@link #set} method is supported however.
 * <p>
 * The iterator implements a {@link #reset} method, allowing the reset of the iterator
 * back to the start if required.
 *
 * @see org.apache.commons.collections.iterators.ObjectArrayIterator
 * @see java.util.Iterator
 * @see java.util.ListIterator
 *
 * @since Commons Collections 3.0
 * @version $Revision: 647116 $ $Date: 2008-04-11 12:23:08 +0100 (Fri, 11 Apr 2008) $
 * 
 * @author Neil O'Toole
 * @author Stephen Colebourne
 * @author Phil Steitz
 */
public class ObjectArrayListIterator extends ObjectArrayIterator
        implements ListIterator, ResettableListIterator {

    /**
     * Holds the index of the last item returned by a call to <code>next()</code> 
     * or <code>previous()</code>. This is set to <code>-1</code> if neither method
     * has yet been invoked. <code>lastItemIndex</code> is used to to implement the
     * {@link #set} method.
     */
    protected int lastItemIndex = -1;

    /**
     * Constructor for use with <code>setArray</code>.
     * <p>
     * Using this constructor, the iterator is equivalent to an empty iterator
     * until {@link #setArray} is  called to establish the array to iterate over.
     */
    public ObjectArrayListIterator() {
        super();
    }

    /**
     * Constructs an ObjectArrayListIterator that will iterate over the values in the
     * specified array.
     *
     * @param array the array to iterate over
     * @throws NullPointerException if <code>array</code> is <code>null</code>
     */
    public ObjectArrayListIterator(Object[] array) {
        super(array);
    }

    /**
     * Constructs an ObjectArrayListIterator that will iterate over the values in the
     * specified array from a specific start index.
     *
     * @param array  the array to iterate over
     * @param start  the index to start iterating at
     * @throws NullPointerException if <code>array</code> is <code>null</code>
     * @throws IndexOutOfBoundsException if the start index is out of bounds
     */
    public ObjectArrayListIterator(Object[] array, int start) {
        super(array, start);
    }
    
    /**
     * Construct an ObjectArrayListIterator that will iterate over a range of values 
     * in the specified array.
     *
     * @param array  the array to iterate over
     * @param start  the index to start iterating at
     * @param end  the index (exclusive) to finish iterating at
     * @throws IndexOutOfBoundsException if the start or end index is out of bounds
     * @throws IllegalArgumentException if end index is before the start
     * @throws NullPointerException if <code>array</code> is <code>null</code>
     */
    public ObjectArrayListIterator(Object[] array, int start, int end) {
        super(array, start, end);
    }

    // ListIterator interface
    //-------------------------------------------------------------------------

    /**
     * Returns true if there are previous elements to return from the array.
     *
     * @return true if there is a previous element to return
     */
    public boolean hasPrevious() {
        return (this.index > this.startIndex);
    }

    /**
     * Gets the previous element from the array.
     * 
     * @return the previous element
     * @throws NoSuchElementException if there is no previous element
     */
    public Object previous() {
        if (hasPrevious() == false) {
            throw new NoSuchElementException();
        }
        this.lastItemIndex = --this.index;
        return this.array[this.index];
    }

    /**
     * Gets the next element from the array.
     * 
     * @return the next element
     * @throws NoSuchElementException if there is no next element
     */
    public Object next() {
        if (hasNext() == false) {
            throw new NoSuchElementException();
        }
        this.lastItemIndex = this.index;
        return this.array[this.index++];
    }

    /**
     * Gets the next index to be retrieved.
     * 
     * @return the index of the item to be retrieved next
     */
    public int nextIndex() {
        return this.index - this.startIndex;
    }

    /**
     * Gets the index of the item to be retrieved if {@link #previous()} is called.
     * 
     * @return the index of the item to be retrieved next
     */
    public int previousIndex() {
        return this.index - this.startIndex - 1;
    }

    /**
     * This iterator does not support modification of its backing array's size, and so will
     * always throw an {@link UnsupportedOperationException} when this method is invoked.
     *
     * @param obj  the object to add
     * @throws UnsupportedOperationException always thrown.
     */
    public void add(Object obj) {
        throw new UnsupportedOperationException("add() method is not supported");
    }

    /**
     * Sets the element under the cursor.
     * <p>
     * This method sets the element that was returned by the last call 
     * to {@link #next()} of {@link #previous()}. 
     * 
     * <b>Note:</b> {@link ListIterator} implementations that support <code>add()</code>
     * and <code>remove()</code> only allow <code>set()</code> to be called once per call 
     * to <code>next()</code> or <code>previous</code> (see the {@link ListIterator}
     * javadoc for more details). Since this implementation does not support 
     * <code>add()</code> or <code>remove()</code>, <code>set()</code> may be
     * called as often as desired.
     * 
     * @param obj  the object to set into the array
     * @throws IllegalStateException if next() has not yet been called.
     * @throws ClassCastException if the object type is unsuitable for the array
     */
    public void set(Object obj) {
        if (this.lastItemIndex == -1) {
            throw new IllegalStateException("must call next() or previous() before a call to set()");
        }

        this.array[this.lastItemIndex] = obj;
    }

    /**
     * Resets the iterator back to the start index.
     */
    public void reset() {
        super.reset();
        this.lastItemIndex = -1;
    }

}
