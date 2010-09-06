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

import java.util.Enumeration;
import java.util.Iterator;

/** 
 * Adapter to make an {@link Iterator Iterator} instance appear to be
 * an {@link Enumeration Enumeration} instance.
 *
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 */
public class IteratorEnumeration implements Enumeration {
    
    /** The iterator being decorated. */
    private Iterator iterator;
    
    /**
     * Constructs a new <code>IteratorEnumeration</code> that will not 
     * function until {@link #setIterator(Iterator) setIterator} is  
     * invoked.
     */
    public IteratorEnumeration() {
        super();
    }

    /**
     * Constructs a new <code>IteratorEnumeration</code> that will use
     * the given iterator. 
     * 
     * @param iterator  the iterator to use
     */
    public IteratorEnumeration( Iterator iterator ) {
        super();
        this.iterator = iterator;
    }

    // Iterator interface
    //-------------------------------------------------------------------------

    /**
     *  Returns true if the underlying iterator has more elements.
     *
     *  @return true if the underlying iterator has more elements
     */
    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    /**
     *  Returns the next element from the underlying iterator.
     *
     *  @return the next element from the underlying iterator.
     *  @throws java.util.NoSuchElementException  if the underlying iterator has no
     *    more elements
     */
    public Object nextElement() {
        return iterator.next();
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
     *  Returns the underlying iterator.
     * 
     *  @return the underlying iterator
     */
    public Iterator getIterator() {
        return iterator;
    }

    /**
     *  Sets the underlying iterator.
     *
     *  @param iterator  the new underlying iterator
     */
    public void setIterator( Iterator iterator ) {
        this.iterator = iterator;
    }
    
}
