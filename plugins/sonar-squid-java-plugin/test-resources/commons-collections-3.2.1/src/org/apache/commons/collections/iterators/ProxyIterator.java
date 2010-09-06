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

/** 
 * A Proxy {@link Iterator Iterator} which delegates its methods to a proxy instance.
 *
 * @deprecated Use AbstractIteratorDecorator. Will be removed in v4.0
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author James Strachan
 */
public class ProxyIterator implements Iterator {
    
    /** Holds value of property iterator. */
    private Iterator iterator;
    
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructs a new <code>ProxyIterator</code> that will not function
     * until {@link #setIterator(Iterator)} is called.
     */
    public ProxyIterator() {
        super();
    }
    
    /**
     * Constructs a new <code>ProxyIterator</code> that will use the
     * given iterator.
     *
     * @param iterator  the underlying iterator
     */
    public ProxyIterator(Iterator iterator) {
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
    public boolean hasNext() {
        return getIterator().hasNext();
    }

    /**
     *  Returns the next element from the underlying iterator.
     *
     *  @return the next element from the underlying iterator
     *  @throws java.util.NoSuchElementException  if the underlying iterator 
     *    raises it because it has no more elements
     */
    public Object next() {
        return getIterator().next();
    }

    /**
     *  Removes the last returned element from the collection that spawned
     *  the underlying iterator.
     */
    public void remove() {
        getIterator().remove();
    }

    // Properties
    //-------------------------------------------------------------------------
    /** Getter for property iterator.
     * @return Value of property iterator.
     */
    public Iterator getIterator() {
        return iterator;
    }
    /** Setter for property iterator.
     * @param iterator New value of property iterator.
     */
    public void setIterator(Iterator iterator) {
        this.iterator = iterator;
    }
}
