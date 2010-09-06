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

import java.util.Set;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.collection.TransformedCollection;
import org.apache.commons.collections.set.TransformedSet;

/**
 * Decorates another <code>Bag</code> to transform objects that are added.
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
public class TransformedBag
        extends TransformedCollection implements Bag {

    /** Serialization version */
    private static final long serialVersionUID = 5421170911299074185L;

    /**
     * Factory method to create a transforming bag.
     * <p>
     * If there are any elements already in the bag being decorated, they
     * are NOT transformed.
     * 
     * @param bag  the bag to decorate, must not be null
     * @param transformer  the transformer to use for conversion, must not be null
     * @return a new transformed Bag
     * @throws IllegalArgumentException if bag or transformer is null
     */
    public static Bag decorate(Bag bag, Transformer transformer) {
        return new TransformedBag(bag, transformer);
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
    protected TransformedBag(Bag bag, Transformer transformer) {
        super(bag, transformer);
    }

    /**
     * Gets the decorated bag.
     * 
     * @return the decorated bag
     */
    protected Bag getBag() {
        return (Bag) collection;
    }

    //-----------------------------------------------------------------------
    public int getCount(Object object) {
        return getBag().getCount(object);
    }

    public boolean remove(Object object, int nCopies) {
        return getBag().remove(object, nCopies);
    }

    //-----------------------------------------------------------------------
    public boolean add(Object object, int nCopies) {
        object = transform(object);
        return getBag().add(object, nCopies);
    }

    public Set uniqueSet() {
        Set set = getBag().uniqueSet();
        return TransformedSet.decorate(set, transformer);
    }

}
