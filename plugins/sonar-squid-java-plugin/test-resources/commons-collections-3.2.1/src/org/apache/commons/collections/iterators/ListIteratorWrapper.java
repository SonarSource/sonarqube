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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.collections.ResettableListIterator;

/**
 * Converts an iterator into a list iterator by caching the returned entries.
 * <p>
 * The <code>ListIterator</code> interface has additional useful methods
 * for navigation - <code>previous()</code> and the index methods.
 * This class allows a regular <code>Iterator</code> to behave as a
 * <code>ListIterator</code>. It achieves this by building a list internally
 * of as the underlying iterator is traversed.
 * <p>
 * The optional operations of <code>ListIterator</code> are not supported.
 * <p>
 * This class implements ResettableListIterator from Commons Collections 3.2.
 *
 * @since Commons Collections 2.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Morgan Delagrange
 * @author Stephen Colebourne
 */
public class ListIteratorWrapper implements ResettableListIterator {

    /** Message used when remove, set or add are called. */
    private static final String UNSUPPORTED_OPERATION_MESSAGE =
        "ListIteratorWrapper does not support optional operations of ListIterator.";

    /** The underlying iterator being decorated. */
    private final Iterator iterator;
    /** The list being used to cache the iterator. */
    private final List list = new ArrayList();

    /** The current index of this iterator. */
    private int currentIndex = 0;
    /** The current index of the wrapped iterator. */
    private int wrappedIteratorIndex = 0;

    // Constructor
    //-------------------------------------------------------------------------
    /**
     * Constructs a new <code>ListIteratorWrapper</code> that will wrap
     * the given iterator.
     *
     * @param iterator  the iterator to wrap
     * @throws NullPointerException if the iterator is null
     */
    public ListIteratorWrapper(Iterator iterator) {
        super();
        if (iterator == null) {
            throw new NullPointerException("Iterator must not be null");
        }
        this.iterator = iterator;
    }

    // ListIterator interface
    //-------------------------------------------------------------------------
    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * @param obj  the object to add, ignored
     * @throws UnsupportedOperationException always
     */
    public void add(Object obj) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * Returns true if there are more elements in the iterator.
     *
     * @return true if there are more elements
     */
    public boolean hasNext() {
        if (currentIndex == wrappedIteratorIndex) {
            return iterator.hasNext();
        }
        return true;
    }

    /**
     * Returns true if there are previous elements in the iterator.
     *
     * @return true if there are previous elements
     */
    public boolean hasPrevious() {
        if (currentIndex == 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns the next element from the iterator.
     *
     * @return the next element from the iterator
     * @throws NoSuchElementException if there are no more elements
     */
    public Object next() throws NoSuchElementException {
        if (currentIndex < wrappedIteratorIndex) {
            ++currentIndex;
            return list.get(currentIndex - 1);
        }

        Object retval = iterator.next();
        list.add(retval);
        ++currentIndex;
        ++wrappedIteratorIndex;
        return retval;
    }

    /**
     * Returns in the index of the next element.
     *
     * @return the index of the next element
     */
    public int nextIndex() {
        return currentIndex;
    }

    /**
     * Returns the the previous element.
     *
     * @return the previous element
     * @throws NoSuchElementException  if there are no previous elements
     */
    public Object previous() throws NoSuchElementException {
        if (currentIndex == 0) {
            throw new NoSuchElementException();
        }
        --currentIndex;
        return list.get(currentIndex);    
    }

    /**
     * Returns the index of the previous element.
     *
     * @return  the index of the previous element
     */
    public int previousIndex() {
        return currentIndex - 1;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException always
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * @param obj  the object to set, ignored
     * @throws UnsupportedOperationException always
     */
    public void set(Object obj) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    // ResettableIterator interface
    //-------------------------------------------------------------------------
    /**
     * Resets this iterator back to the position at which the iterator
     * was created.
     *
     * @since Commons Collections 3.2
     */
    public void reset()  {
        currentIndex = 0;
    }

}
