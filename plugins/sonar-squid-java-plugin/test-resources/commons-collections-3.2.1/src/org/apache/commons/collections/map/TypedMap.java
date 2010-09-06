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
package org.apache.commons.collections.map;

import java.util.Map;

import org.apache.commons.collections.functors.InstanceofPredicate;

/**
 * Decorates another <code>Map</code> to validate that elements added
 * are of a specific type.
 * <p>
 * The validation of additions is performed via an instanceof test against 
 * a specified <code>Class</code>. If an object cannot be added to the
 * collection, an IllegalArgumentException is thrown.
 * <p>
 * <strong>Note that TypedMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap(Map)}. This class may throw 
 * exceptions when accessed by concurrent threads without synchronization.
 * <p>
 * The returned implementation is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 * @author Matthew Hawthorne
 */
public class TypedMap {

    /**
     * Factory method to create a typed map.
     * <p>
     * If there are any elements already in the map being decorated, they
     * are validated.
     * 
     * @param map  the map to decorate, must not be null
     * @param keyType  the type to allow as keys, must not be null
     * @param valueType  the type to allow as values, must not be null
     * @throws IllegalArgumentException if list or type is null
     * @throws IllegalArgumentException if the list contains invalid elements
     */
    public static Map decorate(Map map, Class keyType, Class valueType) {
        return new PredicatedMap(
            map,
            InstanceofPredicate.getInstance(keyType),
            InstanceofPredicate.getInstance(valueType)
        );
    }

    /**
     * Restrictive constructor.
     */
    protected TypedMap() {
    }

}
