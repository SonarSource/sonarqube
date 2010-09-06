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
package org.apache.commons.collections.bidimap;

import java.util.Comparator;
import java.util.SortedMap;

import org.apache.commons.collections.SortedBidiMap;

/** 
 * Provides a base decorator that enables additional functionality to be added
 * to a SortedBidiMap via decoration.
 * <p>
 * Methods are forwarded directly to the decorated map.
 * <p>
 * This implementation does not perform any special processing with the map views.
 * Instead it simply returns the inverse from the wrapped map. This may be
 * undesirable, for example if you are trying to write a validating implementation
 * it would provide a loophole around the validation.
 * But, you might want that loophole, so this class is kept simple.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public abstract class AbstractSortedBidiMapDecorator
        extends AbstractOrderedBidiMapDecorator implements SortedBidiMap {
    
    /**
     * Constructor that wraps (not copies).
     *
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if the collection is null
     */
    public AbstractSortedBidiMapDecorator(SortedBidiMap map) {
        super(map);
    }

    /**
     * Gets the map being decorated.
     * 
     * @return the decorated map
     */
    protected SortedBidiMap getSortedBidiMap() {
        return (SortedBidiMap) map;
    }

    //-----------------------------------------------------------------------
    public SortedBidiMap inverseSortedBidiMap() {
        return getSortedBidiMap().inverseSortedBidiMap();
    }

    public Comparator comparator() {
        return getSortedBidiMap().comparator();
    }

    public SortedMap subMap(Object fromKey, Object toKey) {
        return getSortedBidiMap().subMap(fromKey, toKey);
    }

    public SortedMap headMap(Object toKey) {
        return getSortedBidiMap().headMap(toKey);
    }

    public SortedMap tailMap(Object fromKey) {
        return getSortedBidiMap().tailMap(fromKey);
    }

}
