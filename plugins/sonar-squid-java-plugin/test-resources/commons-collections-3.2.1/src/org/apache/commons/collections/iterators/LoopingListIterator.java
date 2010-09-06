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

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections.ResettableListIterator;

/**
 * A ListIterator that restarts when it reaches the end or when it
 * reaches the beginning.
 * <p>
 * The iterator will loop continuously around the provided list,
 * unless there are no elements in the collection to begin with, or
 * all of the elements have been {@link #remove removed}.
 * <p>
 * Concurrent modifications are not directly supported, and for most
 * collection implementations will throw a
 * ConcurrentModificationException.
 *
 * @since Commons Collections 3.2
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Eric Crampton <ccesc@eonomine.com>
 */
public class LoopingListIterator implements ResettableListIterator {

    /** The list to base the iterator on */
    private List list;
    /** The current list iterator */
    private ListIterator iterator;

    /**
     * Constructor that wraps a list.
     * <p>
     * There is no way to reset a ListIterator instance without
     * recreating it from the original source, so the List must be
     * passed in and a reference to it held.
     *
     * @param list the list to wrap
     * @throws NullPointerException if the list it null
     */
    public LoopingListIterator(List list) {
        if (list == null) {
            throw new NullPointerException("The list must not be null");
        }
        this.list = list;
        reset();
    }

    /**
     * Returns whether this iterator has any more elements.
     * <p>
     * Returns false only if the list originally had zero elements, or
     * all elements have been {@link #remove removed}.
     *
     * @return <code>true</code> if there are more elements
     */
    public boolean hasNext() {
        return !list.isEmpty();
    }

    /**
     * Returns the next object in the list.
     * <p>
     * If at the end of the list, returns the first element.
     *
     * @return the object after the last element returned
     * @throws NoSuchElementException if there are no elements in the list
     */
    public Object next() {
        if (list.isEmpty()) {
            throw new NoSuchElementException(
                "There are no elements for this iterator to loop on");
        }
        if (iterator.hasNext() == false) {
            reset();
        }
        return iterator.next();
    }

    /**
     * Returns the index of the element that would be returned by a
     * subsequent call to {@link #next}.
     * <p>
     * As would be expected, if the iterator is at the physical end of
     * the underlying list, 0 is returned, signifying the beginning of
     * the list.
     *
     * @return the index of the element that would be returned if next() were called
     * @throws NoSuchElementException if there are no elements in the list
     */
    public int nextIndex() {
        if (list.isEmpty()) {
            throw new NoSuchElementException(
                "There are no elements for this iterator to loop on");
        }
        if (iterator.hasNext() == false) {
            return 0;
        } else {
            return iterator.nextIndex();
        }
    }

    /**
     * Returns whether this iterator has any more previous elements.
     * <p>
     * Returns false only if the list originally had zero elements, or
     * all elements have been {@link #remove removed}.
     *
     * @return <code>true</code> if there are more elements
     */
    public boolean hasPrevious() {
        return !list.isEmpty();
    }

    /**
     * Returns the previous object in the list.
     * <p>
     * If at the beginning of the list, return the last element. Note
     * that in this case, traversal to find that element takes linear time.
     *
     * @return the object before the last element returned
     * @throws NoSuchElementException if there are no elements in the list
     */
    public Object previous() {
        if (list.isEmpty()) {
            throw new NoSuchElementException(
                "There are no elements for this iterator to loop on");
        }
        if (iterator.hasPrevious() == false) {
            Object result = null;
            while (iterator.hasNext()) {
                result = iterator.next();
            }
            iterator.previous();
            return result;
        } else {
            return iterator.previous();
        }
    }

    /**
     * Returns the index of the element that would be returned by a
     * subsequent call to {@link #previous}.
     * <p>
     * As would be expected, if at the iterator is at the physical
     * beginning of the underlying list, the list's size minus one is
     * returned, signifying the end of the list.
     *
     * @return the index of the element that would be returned if previous() were called
     * @throws NoSuchElementException if there are no elements in the list
     */
    public int previousIndex() {
        if (list.isEmpty()) {
            throw new NoSuchElementException(
                "There are no elements for this iterator to loop on");
        }
        if (iterator.hasPrevious() == false) {
            return list.size() - 1;
        } else {
            return iterator.previousIndex();
        }
    }

    /**
     * Removes the previously retrieved item from the underlying list.
     * <p>
     * This feature is only supported if the underlying list's
     * {@link List#iterator iterator} method returns an implementation
     * that supports it.
     * <p>
     * This method can only be called after at least one {@link #next}
     * or {@link #previous} method call. After a removal, the remove
     * method may not be called again until another {@link #next} or
     * {@link #previous} has been performed. If the {@link #reset} is
     * called, then remove may not be called until {@link #next} or
     * {@link #previous} is called again.
     *
     * @throws UnsupportedOperationException if the remove method is
     * not supported by the iterator implementation of the underlying
     * list
     */
    public void remove() {
        iterator.remove();
    }

    /**
     * Inserts the specified element into the underlying list.
     * <p>
     * The element is inserted before the next element that would be
     * returned by {@link #next}, if any, and after the next element
     * that would be returned by {@link #previous}, if any.
     * <p>
     * This feature is only supported if the underlying list's
     * {@link List#listIterator} method returns an implementation
     * that supports it.
     *
     * @param obj  the element to insert
     * @throws UnsupportedOperationException if the add method is not
     *  supported by the iterator implementation of the underlying list
     */
    public void add(Object obj) {
        iterator.add(obj);
    }

    /**
     * Replaces the last element that was returned by {@link #next} or
     * {@link #previous}.
     * <p>
     * This feature is only supported if the underlying list's
     * {@link List#listIterator} method returns an implementation
     * that supports it.
     *
     * @param obj  the element with which to replace the last element returned
     * @throws UnsupportedOperationException if the set method is not
     *  supported by the iterator implementation of the underlying list
     */
    public void set(Object obj) {
        iterator.set(obj);
    }

    /**
     * Resets the iterator back to the start of the list.
     */
    public void reset() {
        iterator = list.listIterator();
    }

    /**
     * Gets the size of the list underlying the iterator.
     *
     * @return the current list size
     */
    public int size() {
        return list.size();
    }

}
