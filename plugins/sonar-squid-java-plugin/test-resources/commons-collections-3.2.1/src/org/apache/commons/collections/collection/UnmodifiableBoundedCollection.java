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
package org.apache.commons.collections.collection;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.BoundedCollection;
import org.apache.commons.collections.iterators.UnmodifiableIterator;

/**
 * <code>UnmodifiableBoundedCollection</code> decorates another 
 * <code>BoundedCollection</code> to ensure it can't be altered.
 * <p>
 * If a BoundedCollection is first wrapped in some other collection decorator,
 * such as synchronized or predicated, the BoundedCollection methods are no 
 * longer accessible.
 * The factory on this class will attempt to retrieve the bounded nature by
 * examining the package scope variables.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public final class UnmodifiableBoundedCollection
        extends AbstractSerializableCollectionDecorator
        implements BoundedCollection {

    /** Serialization version */
    private static final long serialVersionUID = -7112672385450340330L;

    /**
     * Factory method to create an unmodifiable bounded collection.
     * 
     * @param coll  the <code>BoundedCollection</code> to decorate, must not be null
     * @return a new unmodifiable bounded collection
     * @throws IllegalArgumentException if bag is null
     */
    public static BoundedCollection decorate(BoundedCollection coll) {
        return new UnmodifiableBoundedCollection(coll);
    }
    
    /**
     * Factory method to create an unmodifiable bounded collection.
     * <p>
     * This method is capable of drilling down through up to 1000 other decorators 
     * to find a suitable BoundedCollection.
     * 
     * @param coll  the <code>BoundedCollection</code> to decorate, must not be null
     * @return a new unmodifiable bounded collection
     * @throws IllegalArgumentException if bag is null
     */
    public static BoundedCollection decorateUsing(Collection coll) {
        if (coll == null) {
            throw new IllegalArgumentException("The collection must not be null");
        }
        
        // handle decorators
        for (int i = 0; i < 1000; i++) {  // counter to prevent infinite looping
            if (coll instanceof BoundedCollection) {
                break;  // normal loop exit
            } else if (coll instanceof AbstractCollectionDecorator) {
                coll = ((AbstractCollectionDecorator) coll).collection;
            } else if (coll instanceof SynchronizedCollection) {
                coll = ((SynchronizedCollection) coll).collection;
            } else {
                break;  // normal loop exit
            }
        }
            
        if (coll instanceof BoundedCollection == false) {
            throw new IllegalArgumentException("The collection is not a bounded collection");
        }
        return new UnmodifiableBoundedCollection((BoundedCollection) coll);
    }    
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param coll  the collection to decorate, must not be null
     * @throws IllegalArgumentException if coll is null
     */
    private UnmodifiableBoundedCollection(BoundedCollection coll) {
        super(coll);
    }

    //-----------------------------------------------------------------------
    public Iterator iterator() {
        return UnmodifiableIterator.decorate(getCollection().iterator());
    }

    public boolean add(Object object) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    //-----------------------------------------------------------------------    
    public boolean isFull() {
        return ((BoundedCollection) collection).isFull();
    }

    public int maxSize() {
        return ((BoundedCollection) collection).maxSize();
    }

}
