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

import org.apache.commons.collections.Predicate;

/**
 * Decorates another <code>Collection</code> to validate that additions
 * match a specified predicate.
 * <p>
 * This collection exists to provide validation for the decorated collection.
 * It is normally created to decorate an empty collection.
 * If an object cannot be added to the collection, an IllegalArgumentException is thrown.
 * <p>
 * One usage would be to ensure that no null entries are added to the collection.
 * <pre>Collection coll = PredicatedCollection.decorate(new ArrayList(), NotNullPredicate.INSTANCE);</pre>
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 * @author Paul Jack
 */
public class PredicatedCollection extends AbstractSerializableCollectionDecorator {

    /** Serialization version */
    private static final long serialVersionUID = -5259182142076705162L;

    /** The predicate to use */
    protected final Predicate predicate;

    /**
     * Factory method to create a predicated (validating) collection.
     * <p>
     * If there are any elements already in the collection being decorated, they
     * are validated.
     * 
     * @param coll  the collection to decorate, must not be null
     * @param predicate  the predicate to use for validation, must not be null
     * @return a new predicated collection
     * @throws IllegalArgumentException if collection or predicate is null
     * @throws IllegalArgumentException if the collection contains invalid elements
     */
    public static Collection decorate(Collection coll, Predicate predicate) {
        return new PredicatedCollection(coll, predicate);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * <p>
     * If there are any elements already in the collection being decorated, they
     * are validated.
     * 
     * @param coll  the collection to decorate, must not be null
     * @param predicate  the predicate to use for validation, must not be null
     * @throws IllegalArgumentException if collection or predicate is null
     * @throws IllegalArgumentException if the collection contains invalid elements
     */
    protected PredicatedCollection(Collection coll, Predicate predicate) {
        super(coll);
        if (predicate == null) {
            throw new IllegalArgumentException("Predicate must not be null");
        }
        this.predicate = predicate;
        for (Iterator it = coll.iterator(); it.hasNext(); ) {
            validate(it.next());
        }
    }

    /**
     * Validates the object being added to ensure it matches the predicate.
     * <p>
     * The predicate itself should not throw an exception, but return false to
     * indicate that the object cannot be added.
     * 
     * @param object  the object being added
     * @throws IllegalArgumentException if the add is invalid
     */
    protected void validate(Object object) {
        if (predicate.evaluate(object) == false) {
            throw new IllegalArgumentException("Cannot add Object '" + object + "' - Predicate rejected it");
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Override to validate the object being added to ensure it matches
     * the predicate.
     * 
     * @param object  the object being added
     * @return the result of adding to the underlying collection
     * @throws IllegalArgumentException if the add is invalid
     */
    public boolean add(Object object) {
        validate(object);
        return getCollection().add(object);
    }

    /**
     * Override to validate the objects being added to ensure they match
     * the predicate. If any one fails, no update is made to the underlying
     * collection.
     * 
     * @param coll  the collection being added
     * @return the result of adding to the underlying collection
     * @throws IllegalArgumentException if the add is invalid
     */
    public boolean addAll(Collection coll) {
        for (Iterator it = coll.iterator(); it.hasNext(); ) {
            validate(it.next());
        }
        return getCollection().addAll(coll);
    }

}
