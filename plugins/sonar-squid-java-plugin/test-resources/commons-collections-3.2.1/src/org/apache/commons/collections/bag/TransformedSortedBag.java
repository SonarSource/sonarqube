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
package org.apache.commons.collections.bag;

import java.util.Comparator;

import org.apache.commons.collections.SortedBag;
import org.apache.commons.collections.Transformer;

/**
 * Decorates another <code>SortedBag</code> to transform objects that are added.
 * <p>
 * The add methods are affected by this class.
 * Thus objects must be removed or searched for using their transformed form.
 * For example, if the transformation converts Strings to Integers, you must
 * use the Integer form to remove objects.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public class TransformedSortedBag
        extends TransformedBag implements SortedBag {

    /** Serialization version */
    private static final long serialVersionUID = -251737742649401930L;

    /**
     * Factory method to create a transforming sorted bag.
     * <p>
     * If there are any elements already in the bag being decorated, they
     * are NOT transformed.
     * 
     * @param bag  the bag to decorate, must not be null
     * @param transformer  the transformer to use for conversion, must not be null
     * @return a new transformed SortedBag
     * @throws IllegalArgumentException if bag or transformer is null
     */
    public static SortedBag decorate(SortedBag bag, Transformer transformer) {
        return new TransformedSortedBag(bag, transformer);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * <p>
     * If there are any elements already in the bag being decorated, they
     * are NOT transformed.
     * 
     * @param bag  the bag to decorate, must not be null
     * @param transformer  the transformer to use for conversion, must not be null
     * @throws IllegalArgumentException if bag or transformer is null
     */
    protected TransformedSortedBag(SortedBag bag, Transformer transformer) {
        super(bag, transformer);
    }

    /**
     * Gets the decorated bag.
     * 
     * @return the decorated bag
     */
    protected SortedBag getSortedBag() {
        return (SortedBag) collection;
    }

    //-----------------------------------------------------------------------
    public Object first() {
        return getSortedBag().first();
    }

    public Object last() {
        return getSortedBag().last();
    }

    public Comparator comparator() {
        return getSortedBag().comparator();
    }

}
