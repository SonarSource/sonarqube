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

import org.apache.commons.collections.ResettableIterator;

/** 
 * <code>SingletonIterator</code> is an {@link Iterator} over a single 
 * object instance.
 *
 * @since Commons Collections 2.0
 * @version $Revision: 647116 $ $Date: 2008-04-11 12:23:08 +0100 (Fri, 11 Apr 2008) $
 * 
 * @author James Strachan
 * @author Stephen Colebourne
 * @author Rodney Waldhoff
 */
public class SingletonIterator
        implements Iterator, ResettableIterator {

    /** Whether remove is allowed */
    private final boolean removeAllowed;
    /** Is the cursor before the first element */
    private boolean beforeFirst = true;
    /** Has the element been removed */
    private boolean removed = false;
    /** The object */
    private Object object;

    /**
     * Constructs a new <code>SingletonIterator</code> where <code>remove</code>
     * is a permitted operation.
     *
     * @param object  the single object to return from the iterator
     */
    public SingletonIterator(Object object) {
        this(object, true);
    }

    /**
     * Constructs a new <code>SingletonIterator</code> optionally choosing if
     * <code>remove</code> is a permitted operation.
     *
     * @param object  the single object to return from the iterator
     * @param removeAllowed  true if remove is allowed
     * @since Commons Collections 3.1
     */
    public SingletonIterator(Object object, boolean removeAllowed) {
        super();
        this.object = object;
        this.removeAllowed = removeAllowed;
    }

    //-----------------------------------------------------------------------
    /**
     * Is another object available from the iterator?
     * <p>
     * This returns true if the single object hasn't been returned yet.
     * 
     * @return true if the single object hasn't been returned yet
     */
    public boolean hasNext() {
        return (beforeFirst && !removed);
    }

    /**
     * Get the next object from the iterator.
     * <p>
     * This returns the single object if it hasn't been returned yet.
     *
     * @return the single object
     * @throws NoSuchElementException if the single object has already 
     *    been returned
     */
    public Object next() {
        if (!beforeFirst || removed) {
            throw new NoSuchElementException();
        }
        beforeFirst = false;
        return object;
    }

    /**
     * Remove the object from this iterator.
     * 
     * @throws IllegalStateException if the <tt>next</tt> method has not
     *        yet been called, or the <tt>remove</tt> method has already
     *        been called after the last call to the <tt>next</tt>
     *        method.
     * @throws UnsupportedOperationException if remove is not supported
     */
    public void remove() {
        if (removeAllowed) {
            if (removed || beforeFirst) {
                throw new IllegalStateException();
            } else {
                object = null;
                removed = true;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Reset the iterator to the start.
     */
    public void reset() {
        beforeFirst = true;
    }
    
}
