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

import java.util.Enumeration;
import java.util.List;

import org.apache.commons.collections.iterators.EnumerationIterator;

/**
 * Provides utility methods for {@link Enumeration} instances.
 * 
 * @since Commons Collections 3.0
 * @version $Id: EnumerationUtils.java 646777 2008-04-10 12:33:15Z niallp $
 * 
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 */
public class EnumerationUtils {

    /**
     * EnumerationUtils is not normally instantiated.
     */
    public EnumerationUtils() {
        // no init.
    }
    
    /**
     * Creates a list based on an enumeration.
     * 
     * <p>As the enumeration is traversed, an ArrayList of its values is
     * created. The new list is returned.</p>
     *
     * @param enumeration  the enumeration to traverse, which should not be <code>null</code>.
     * @throws NullPointerException if the enumeration parameter is <code>null</code>.
     */
    public static List toList(Enumeration enumeration) {
        return IteratorUtils.toList(new EnumerationIterator(enumeration));
    }

}
