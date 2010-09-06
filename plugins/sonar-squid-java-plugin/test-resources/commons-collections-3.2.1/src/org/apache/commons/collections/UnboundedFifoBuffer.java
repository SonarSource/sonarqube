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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * UnboundedFifoBuffer is a very efficient buffer implementation.
 * According to performance testing, it exhibits a constant access time, but it
 * also outperforms ArrayList when used for the same purpose.
 * <p>
 * The removal order of an <code>UnboundedFifoBuffer</code> is based on the insertion
 * order; elements are removed in the same order in which they were added.
 * The iteration order is the same as the removal order.
 * <p>
 * The {@link #remove()} and {@link #get()} operations perform in constant time.
 * The {@link #add(Object)} operation performs in amortized constant time.  All
 * other operations perform in linear time or worse.
 * <p>
 * Note that this implementation is not synchronized.  The following can be
 * used to provide synchronized access to your <code>UnboundedFifoBuffer</code>:
 * <pre>
 *   Buffer fifo = BufferUtils.synchronizedBuffer(new UnboundedFifoBuffer());
 * </pre>
 * <p>
 * This buffer prevents null objects from being added.
 * 
 * @deprecated Moved to buffer subpackage. Due to be removed in v4.0.
 * @since Commons Collections 2.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Avalon
 * @author Federico Barbieri
 * @author Berin Loritsch
 * @author Paul Jack
 * @author Stephen Colebourne
 * @author Andreas Schlosser
 */
public class UnboundedFifoBuffer extends AbstractCollection implements Buffer {
    
    protected Object[] m_buffer;
    protected int m_head;
    protected int m_tail;

    /**
     * Constructs an UnboundedFifoBuffer with the default number of elements.
     * It is exactly the same as performing the following:
     *
     * <pre>
     *   new UnboundedFifoBuffer(32);
     * </pre>
     */
    public UnboundedFifoBuffer() {
        this(32);
    }

    /**
     * Constructs an UnboundedFifoBuffer with the specified number of elements.
     * The integer must be a positive integer.
     * 
     * @param initialSize  the initial size of the buffer
     * @throws IllegalArgumentException  if the size is less than 1
     */
    public UnboundedFifoBuffer(int initialSize) {
        if (initialSize <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        m_buffer = new Object[initialSize + 1];
        m_head = 0;
        m_tail = 0;
    }

    /**
     * Returns the number of elements stored in the buffer.
     *
     * @return this buffer's size
     */
    public int size() {
        int size = 0;

        if (m_tail < m_head) {
            size = m_buffer.length - m_head + m_tail;
        } else {
            size = m_tail - m_head;
        }

        return size;
    }

    /**
     * Returns true if this buffer is empty; false otherwise.
     *
     * @return true if this buffer is empty
     */
    public boolean isEmpty() {
        return (size() == 0);
    }

    /**
     * Adds the given element to this buffer.
     *
     * @param obj  the element to add
     * @return true, always
     * @throws NullPointerException  if the given element is null
     * @throws BufferOverflowException  if this buffer is full
     */
    public boolean add(final Object obj) {
        if (obj == null) {
            throw new NullPointerException("Attempted to add null object to buffer");
        }

        if (size() + 1 >= m_buffer.length) {
            Object[] tmp = new Object[((m_buffer.length - 1) * 2) + 1];

            int j = 0;
            for (int i = m_head; i != m_tail;) {
                tmp[j] = m_buffer[i];
                m_buffer[i] = null;

                j++;
                i++;
                if (i == m_buffer.length) {
                    i = 0;
                }
            }

            m_buffer = tmp;
            m_head = 0;
            m_tail = j;
        }

        m_buffer[m_tail] = obj;
        m_tail++;
        if (m_tail >= m_buffer.length) {
            m_tail = 0;
        }
        return true;
    }

    /**
     * Returns the next object in the buffer.
     *
     * @return the next object in the buffer
     * @throws BufferUnderflowException  if this buffer is empty
     */
    public Object get() {
        if (isEmpty()) {
            throw new BufferUnderflowException("The buffer is already empty");
        }

        return m_buffer[m_head];
    }

    /**
     * Removes the next object from the buffer
     *
     * @return the removed object
     * @throws BufferUnderflowException  if this buffer is empty
     */
    public Object remove() {
        if (isEmpty()) {
            throw new BufferUnderflowException("The buffer is already empty");
        }

        Object element = m_buffer[m_head];

        if (null != element) {
            m_buffer[m_head] = null;

            m_head++;
            if (m_head >= m_buffer.length) {
                m_head = 0;
            }
        }

        return element;
    }

    /**
     * Increments the internal index.
     * 
     * @param index  the index to increment
     * @return the updated index
     */
    private int increment(int index) {
        index++;
        if (index >= m_buffer.length) {
            index = 0;
        }
        return index;
    }

    /**
     * Decrements the internal index.
     * 
     * @param index  the index to decrement
     * @return the updated index
     */
    private int decrement(int index) {
        index--;
        if (index < 0) {
            index = m_buffer.length - 1;
        }
        return index;
    }

    /**
     * Returns an iterator over this buffer's elements.
     *
     * @return an iterator over this buffer's elements
     */
    public Iterator iterator() {
        return new Iterator() {

            private int index = m_head;
            private int lastReturnedIndex = -1;

            public boolean hasNext() {
                return index != m_tail;

            }

            public Object next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                lastReturnedIndex = index;
                index = increment(index);
                return m_buffer[lastReturnedIndex];
            }

            public void remove() {
                if (lastReturnedIndex == -1)
                    throw new IllegalStateException();

                // First element can be removed quickly
                if (lastReturnedIndex == m_head) {
                    UnboundedFifoBuffer.this.remove();
                    lastReturnedIndex = -1;
                    return;
                }

                // Other elements require us to shift the subsequent elements
                int i = increment(lastReturnedIndex);
                while (i != m_tail) {
                    m_buffer[decrement(i)] = m_buffer[i];
                    i = increment(i);
                }

                lastReturnedIndex = -1;
                m_tail = decrement(m_tail);
                m_buffer[m_tail] = null;
                index = decrement(index);
            }

        };
    }
    
}
