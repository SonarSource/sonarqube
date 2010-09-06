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

/**
 * Defines a collection for priority queues, which can insert, peek and pop.
 * <p>
 * This interface is now replaced by the <code>Buffer</code> interface.
 *
 * @deprecated Replaced by the Buffer interface and implementations in buffer subpackage.
 *  Due to be removed in v4.0.
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Peter Donald
 */
public interface PriorityQueue {
    
    /**
     * Clear all elements from queue.
     */
    void clear();

    /**
     * Test if queue is empty.
     *
     * @return true if queue is empty else false.
     */
    boolean isEmpty();

    /**
     * Insert an element into queue.
     *
     * @param element the element to be inserted
     *
     * @throws ClassCastException if the specified <code>element</code>'s
     * type prevents it from being compared to other items in the queue to
     * determine its relative priority.  
     */
    void insert(Object element);

    /**
     * Return element on top of heap but don't remove it.
     *
     * @return the element at top of heap
     * @throws java.util.NoSuchElementException if <code>isEmpty() == true</code>
     */
    Object peek();

    /**
     * Return element on top of heap and remove it.
     *
     * @return the element at top of heap
     * @throws java.util.NoSuchElementException if <code>isEmpty() == true</code>
     */
    Object pop();
    
}
