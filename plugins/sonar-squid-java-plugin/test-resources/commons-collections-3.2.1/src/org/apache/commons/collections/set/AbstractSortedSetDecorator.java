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
package org.apache.commons.collections.set;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;

/**
 * Decorates another <code>SortedSet</code> to provide additional behaviour.
 * <p>
 * Methods are forwarded directly to the decorated set.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public abstract class AbstractSortedSetDecorator extends AbstractSetDecorator implements SortedSet {

    /**
     * Constructor only used in deserialization, do not use otherwise.
     * @since Commons Collections 3.1
     */
    protected AbstractSortedSetDecorator() {
        super();
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param set  the set to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    protected AbstractSortedSetDecorator(Set set) {
        super(set);
    }

    /**
     * Gets the sorted set being decorated.
     * 
     * @return the decorated set
     */
    protected SortedSet getSortedSet() {
        return (SortedSet) getCollection();
    }
    
    //-----------------------------------------------------------------------
    public SortedSet subSet(Object fromElement, Object toElement) {
        return getSortedSet().subSet(fromElement, toElement);
    }

    public SortedSet headSet(Object toElement) {
        return getSortedSet().headSet(toElement);
    }

    public SortedSet tailSet(Object fromElement) {
        return getSortedSet().tailSet(fromElement);
    }

    public Object first() {
        return getSortedSet().first();
    }

    public Object last() {
        return getSortedSet().last();
    }

    public Comparator comparator() {
        return getSortedSet().comparator();
    }

}
