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
package org.apache.commons.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A {@link Bag} that is backed by a {@link TreeMap}. 
 * Order will be maintained among the unique representative
 * members.
 *
 * @deprecated Moved to bag subpackage and rewritten internally. Due to be removed in v4.0.
 * @since Commons Collections 2.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Chuck Burdick
 */
public class TreeBag extends DefaultMapBag implements SortedBag {

    /**
     * Constructs an empty <code>TreeBag</code>.
     */
    public TreeBag() {
        super(new TreeMap());
    }

    /**
     * Constructs an empty {@link Bag} that maintains order on its unique
     * representative members according to the given {@link Comparator}.
     * 
     * @param comparator  the comparator to use
     */
    public TreeBag(Comparator comparator) {
        super(new TreeMap(comparator));
    }

    /**
     * Constructs a {@link Bag} containing all the members of the given
     * collection.
     * 
     * @param coll  the collection to copy into the bag
     */
    public TreeBag(Collection coll) {
        this();
        addAll(coll);
    }

    public Object first() {
        return ((SortedMap) getMap()).firstKey();
    }

    public Object last() {
        return ((SortedMap) getMap()).lastKey();
    }

    public Comparator comparator() {
        return ((SortedMap) getMap()).comparator();
    }
    
}
