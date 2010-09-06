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

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Binary heap implementation of <code>PriorityQueue</code>.
 * <p>
 * The <code>PriorityQueue</code> interface has now been replaced for most uses
 * by the <code>Buffer</code> interface. This class and the interface are
 * retained for backwards compatibility. The intended replacement is
 * {@link org.apache.commons.collections.buffer.PriorityBuffer PriorityBuffer}.
 * <p>
 * The removal order of a binary heap is based on either the natural sort
 * order of its elements or a specified {@link Comparator}.  The 
 * {@link #pop()} method always returns the first element as determined
 * by the sort order.  (The <code>isMinHeap</code> flag in the constructors
 * can be used to reverse the sort order, in which case {@link #pop()}
 * will always remove the last element.)  The removal order is 
 * <i>not</i> the same as the order of iteration; elements are
 * returned by the iterator in no particular order.
 * <p>
 * The {@link #insert(Object)} and {@link #pop()} operations perform
 * in logarithmic time.  The {@link #peek()} operation performs in constant
 * time.  All other operations perform in linear time or worse.
 * <p>
 * Note that this implementation is not synchronized.  Use SynchronizedPriorityQueue
 * to provide synchronized access to a <code>BinaryHeap</code>:
 *
 * <pre>
 * PriorityQueue heap = new SynchronizedPriorityQueue(new BinaryHeap());
 * </pre>
 *
 * @deprecated Replaced by PriorityBuffer in buffer subpackage.
 *  Due to be removed in v4.0.
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Peter Donald
 * @author Ram Chidambaram
 * @author Michael A. Smith
 * @author Paul Jack
 * @author Stephen Colebourne
 */
public final class BinaryHeap extends AbstractCollection
        implements PriorityQueue, Buffer {

    /**
     * The default capacity for a binary heap.
     */
    private final static int DEFAULT_CAPACITY = 13;
    /**
     * The number of elements currently in this heap.
     */
    int m_size;  // package scoped for testing
    /**
     * The elements in this heap.
     */
    Object[] m_elements;  // package scoped for testing
    /**
     * If true, the first element as determined by the sort order will 
     * be returned.  If false, the last element as determined by the
     * sort order will be returned.
     */
    boolean m_isMinHeap;  // package scoped for testing
    /**
     * The comparator used to order the elements
     */
    Comparator m_comparator;  // package scoped for testing

    /**
     * Constructs a new minimum binary heap.
     */
    public BinaryHeap() {
        this(DEFAULT_CAPACITY, true);
    }

    /**
     * Constructs a new <code>BinaryHeap</code> that will use the given
     * comparator to order its elements.
     * 
     * @param comparator  the comparator used to order the elements, null
     *  means use natural order
     */
    public BinaryHeap(Comparator comparator) {
        this();
        m_comparator = comparator;
    }
    
    /**
     * Constructs a new minimum binary heap with the specified initial capacity.
     *  
     * @param capacity  The initial capacity for the heap.  This value must
     *  be greater than zero.
     * @throws IllegalArgumentException  
     *  if <code>capacity</code> is &lt;= <code>0</code>
     */
    public BinaryHeap(int capacity) {
        this(capacity, true);
    }

    /**
     * Constructs a new <code>BinaryHeap</code>.
     *
     * @param capacity  the initial capacity for the heap
     * @param comparator  the comparator used to order the elements, null
     *  means use natural order
     * @throws IllegalArgumentException  
     *  if <code>capacity</code> is &lt;= <code>0</code>
     */
    public BinaryHeap(int capacity, Comparator comparator) {
        this(capacity);
        m_comparator = comparator;
    }

    /**
     * Constructs a new minimum or maximum binary heap
     *
     * @param isMinHeap  if <code>true</code> the heap is created as a 
     * minimum heap; otherwise, the heap is created as a maximum heap
     */
    public BinaryHeap(boolean isMinHeap) {
        this(DEFAULT_CAPACITY, isMinHeap);
    }

    /**
     * Constructs a new <code>BinaryHeap</code>.
     *
     * @param isMinHeap  true to use the order imposed by the given 
     *   comparator; false to reverse that order
     * @param comparator  the comparator used to order the elements, null
     *  means use natural order
     */
    public BinaryHeap(boolean isMinHeap, Comparator comparator) {
        this(isMinHeap);
        m_comparator = comparator;
    }

    /**
     * Constructs a new minimum or maximum binary heap with the specified 
     * initial capacity.
     *
     * @param capacity the initial capacity for the heap.  This value must 
     * be greater than zero.
     * @param isMinHeap if <code>true</code> the heap is created as a 
     *  minimum heap; otherwise, the heap is created as a maximum heap.
     * @throws IllegalArgumentException 
     *  if <code>capacity</code> is <code>&lt;= 0</code>
     */
    public BinaryHeap(int capacity, boolean isMinHeap) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("invalid capacity");
        }
        m_isMinHeap = isMinHeap;

        //+1 as 0 is noop
        m_elements = new Object[capacity + 1];
    }

    /**
     * Constructs a new <code>BinaryHeap</code>.
     *
     * @param capacity  the initial capacity for the heap
     * @param isMinHeap  true to use the order imposed by the given 
     *   comparator; false to reverse that order
     * @param comparator  the comparator used to order the elements, null
     *  means use natural order
     * @throws IllegalArgumentException 
     *  if <code>capacity</code> is <code>&lt;= 0</code>
     */
    public BinaryHeap(int capacity, boolean isMinHeap, Comparator comparator) {
        this(capacity, isMinHeap);
        m_comparator = comparator;
    }

    //-----------------------------------------------------------------------
    /**
     * Clears all elements from queue.
     */
    public void clear() {
        m_elements = new Object[m_elements.length];  // for gc
        m_size = 0;
    }

    /**
     * Tests if queue is empty.
     *
     * @return <code>true</code> if queue is empty; <code>false</code> 
     *  otherwise.
     */
    public boolean isEmpty() {
        return m_size == 0;
    }

    /**
     * Tests if queue is full.
     *
     * @return <code>true</code> if queue is full; <code>false</code>
     *  otherwise.
     */
    public boolean isFull() {
        //+1 as element 0 is noop
        return m_elements.length == m_size + 1;
    }

    /**
     * Inserts an element into queue.
     *
     * @param element  the element to be inserted
     */
    public void insert(Object element) {
        if (isFull()) {
            grow();
        }
        //percolate element to it's place in tree
        if (m_isMinHeap) {
            percolateUpMinHeap(element);
        } else {
            percolateUpMaxHeap(element);
        }
    }

    /**
     * Returns the element on top of heap but don't remove it.
     *
     * @return the element at top of heap
     * @throws NoSuchElementException  if <code>isEmpty() == true</code>
     */
    public Object peek() throws NoSuchElementException {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return m_elements[1];
        }
    }

    /**
     * Returns the element on top of heap and remove it.
     *
     * @return the element at top of heap
     * @throws NoSuchElementException  if <code>isEmpty() == true</code>
     */
    public Object pop() throws NoSuchElementException {
        final Object result = peek();
        m_elements[1] = m_elements[m_size--];

        // set the unused element to 'null' so that the garbage collector
        // can free the object if not used anywhere else.(remove reference)
        m_elements[m_size + 1] = null;

        if (m_size != 0) {
            // percolate top element to it's place in tree
            if (m_isMinHeap) {
                percolateDownMinHeap(1);
            } else {
                percolateDownMaxHeap(1);
            }
        }

        return result;
    }

    /**
     * Percolates element down heap from the position given by the index.
     * <p>
     * Assumes it is a minimum heap.
     *
     * @param index the index for the element
     */
    protected void percolateDownMinHeap(final int index) {
        final Object element = m_elements[index];
        int hole = index;

        while ((hole * 2) <= m_size) {
            int child = hole * 2;

            // if we have a right child and that child can not be percolated
            // up then move onto other child
            if (child != m_size && compare(m_elements[child + 1], m_elements[child]) < 0) {
                child++;
            }

            // if we found resting place of bubble then terminate search
            if (compare(m_elements[child], element) >= 0) {
                break;
            }

            m_elements[hole] = m_elements[child];
            hole = child;
        }

        m_elements[hole] = element;
    }

    /**
     * Percolates element down heap from the position given by the index.
     * <p>
     * Assumes it is a maximum heap.
     *
     * @param index the index of the element
     */
    protected void percolateDownMaxHeap(final int index) {
        final Object element = m_elements[index];
        int hole = index;

        while ((hole * 2) <= m_size) {
            int child = hole * 2;

            // if we have a right child and that child can not be percolated
            // up then move onto other child
            if (child != m_size && compare(m_elements[child + 1], m_elements[child]) > 0) {
                child++;
            }

            // if we found resting place of bubble then terminate search
            if (compare(m_elements[child], element) <= 0) {
                break;
            }

            m_elements[hole] = m_elements[child];
            hole = child;
        }

        m_elements[hole] = element;
    }

    /**
     * Percolates element up heap from the position given by the index.
     * <p>
     * Assumes it is a minimum heap.
     *
     * @param index the index of the element to be percolated up
     */
    protected void percolateUpMinHeap(final int index) {
        int hole = index;
        Object element = m_elements[hole];
        while (hole > 1 && compare(element, m_elements[hole / 2]) < 0) {
            // save element that is being pushed down
            // as the element "bubble" is percolated up
            final int next = hole / 2;
            m_elements[hole] = m_elements[next];
            hole = next;
        }
        m_elements[hole] = element;
    }

    /**
     * Percolates a new element up heap from the bottom.
     * <p>
     * Assumes it is a minimum heap.
     *
     * @param element the element
     */
    protected void percolateUpMinHeap(final Object element) {
        m_elements[++m_size] = element;
        percolateUpMinHeap(m_size);
    }

    /**
     * Percolates element up heap from from the position given by the index.
     * <p>
     * Assume it is a maximum heap.
     *
     * @param index the index of the element to be percolated up
     */
    protected void percolateUpMaxHeap(final int index) {
        int hole = index;
        Object element = m_elements[hole];
        
        while (hole > 1 && compare(element, m_elements[hole / 2]) > 0) {
            // save element that is being pushed down
            // as the element "bubble" is percolated up
            final int next = hole / 2;
            m_elements[hole] = m_elements[next];
            hole = next;
        }

        m_elements[hole] = element;
    }
    
    /**
     * Percolates a new element up heap from the bottom.
     * <p>
     * Assume it is a maximum heap.
     *
     * @param element the element
     */
    protected void percolateUpMaxHeap(final Object element) {
        m_elements[++m_size] = element;
        percolateUpMaxHeap(m_size);
    }
    
    /**
     * Compares two objects using the comparator if specified, or the
     * natural order otherwise.
     * 
     * @param a  the first object
     * @param b  the second object
     * @return -ve if a less than b, 0 if they are equal, +ve if a greater than b
     */
    private int compare(Object a, Object b) {
        if (m_comparator != null) {
            return m_comparator.compare(a, b);
        } else {
            return ((Comparable) a).compareTo(b);
        }
    }

    /**
     * Increases the size of the heap to support additional elements
     */
    protected void grow() {
        final Object[] elements = new Object[m_elements.length * 2];
        System.arraycopy(m_elements, 0, elements, 0, m_elements.length);
        m_elements = elements;
    }

    /**
     * Returns a string representation of this heap.  The returned string
     * is similar to those produced by standard JDK collections.
     *
     * @return a string representation of this heap
     */
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        sb.append("[ ");

        for (int i = 1; i < m_size + 1; i++) {
            if (i != 1) {
                sb.append(", ");
            }
            sb.append(m_elements[i]);
        }

        sb.append(" ]");

        return sb.toString();
    }


    /**
     * Returns an iterator over this heap's elements.
     *
     * @return an iterator over this heap's elements
     */
    public Iterator iterator() {
        return new Iterator() {

            private int index = 1;
            private int lastReturnedIndex = -1;

            public boolean hasNext() {
                return index <= m_size;
            }

            public Object next() {
                if (!hasNext()) throw new NoSuchElementException();
                lastReturnedIndex = index;
                index++;
                return m_elements[lastReturnedIndex];
            }

            public void remove() {
                if (lastReturnedIndex == -1) {
                    throw new IllegalStateException();
                }
                m_elements[ lastReturnedIndex ] = m_elements[ m_size ];
                m_elements[ m_size ] = null;
                m_size--;  
                if( m_size != 0 && lastReturnedIndex <= m_size) {
                    int compareToParent = 0;
                    if (lastReturnedIndex > 1) {
                        compareToParent = compare(m_elements[lastReturnedIndex], 
                            m_elements[lastReturnedIndex / 2]);  
                    }
                    if (m_isMinHeap) {
                        if (lastReturnedIndex > 1 && compareToParent < 0) {
                            percolateUpMinHeap(lastReturnedIndex); 
                        } else {
                            percolateDownMinHeap(lastReturnedIndex);
                        }
                    } else {  // max heap
                        if (lastReturnedIndex > 1 && compareToParent > 0) {
                            percolateUpMaxHeap(lastReturnedIndex); 
                        } else {
                            percolateDownMaxHeap(lastReturnedIndex);
                        }
                    }          
                }
                index--;
                lastReturnedIndex = -1; 
            }

        };
    }


    /**
     * Adds an object to this heap. Same as {@link #insert(Object)}.
     *
     * @param object  the object to add
     * @return true, always
     */
    public boolean add(Object object) {
        insert(object);
        return true;
    }

    /**
     * Returns the priority element. Same as {@link #peek()}.
     *
     * @return the priority element
     * @throws BufferUnderflowException if this heap is empty
     */
    public Object get() {
        try {
            return peek();
        } catch (NoSuchElementException e) {
            throw new BufferUnderflowException();
        }
    }

    /**
     * Removes the priority element. Same as {@link #pop()}.
     *
     * @return the removed priority element
     * @throws BufferUnderflowException if this heap is empty
     */
    public Object remove() {
        try {
            return pop();
        } catch (NoSuchElementException e) {
            throw new BufferUnderflowException();
        }
    }

    /**
     * Returns the number of elements in this heap.
     *
     * @return the number of elements in this heap
     */
    public int size() {
        return m_size;
    }

}
