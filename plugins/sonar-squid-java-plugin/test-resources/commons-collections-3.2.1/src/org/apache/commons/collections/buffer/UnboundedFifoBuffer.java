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
package org.apache.commons.collections.buffer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUnderflowException;

/**
 * UnboundedFifoBuffer is a very efficient implementation of
 * <code>Buffer</code> that can grow to any size.
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
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 * 
 * @since Commons Collections 3.0 (previously in main package v2.1)
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Avalon
 * @author Federico Barbieri
 * @author Berin Loritsch
 * @author Paul Jack
 * @author Stephen Colebourne
 * @author Andreas Schlosser
 * @author Thomas Knych
 * @author Jordan Krey
 */
public class UnboundedFifoBuffer extends AbstractCollection implements Buffer, Serializable {
    // invariant: buffer.length > size()
    //   ie.buffer always has at least one empty entry

    /** Serialization vesrion */
    private static final long serialVersionUID = -3482960336579541419L;

    /** The array of objects in the buffer. */
    protected transient Object[] buffer;
    /** The current head index. */
    protected transient int head;
    /** The current tail index. */
    protected transient int tail;

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
        buffer = new Object[initialSize + 1];
        head = 0;
        tail = 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Write the buffer out using a custom routine.
     * 
     * @param out  the output stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(size());
        for (Iterator it = iterator(); it.hasNext();) {
            out.writeObject(it.next());
        }
    }

    /**
     * Read the buffer in using a custom routine.
     * 
     * @param in  the input stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int size = in.readInt();
        buffer = new Object[size + 1];
        for (int i = 0; i < size; i++) {
            buffer[i] = in.readObject();
        }
        head = 0;
        tail = size;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the number of elements stored in the buffer.
     *
     * @return this buffer's size
     */
    public int size() {
        int size = 0;

        if (tail < head) {
            size = buffer.length - head + tail;
        } else {
            size = tail - head;
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
     */
    public boolean add(final Object obj) {
        if (obj == null) {
            throw new NullPointerException("Attempted to add null object to buffer");
        }

        if (size() + 1 >= buffer.length) {
            // copy contents to a new buffer array
            Object[] tmp = new Object[((buffer.length - 1) * 2) + 1];
            int j = 0;
            // move head to element zero in the new array
            for (int i = head; i != tail;) {
                tmp[j] = buffer[i];
                buffer[i] = null;

                j++;
                i = increment(i);
            }
            buffer = tmp;
            head = 0;
            tail = j;
        }

        buffer[tail] = obj;
        tail = increment(tail);
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

        return buffer[head];
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

        Object element = buffer[head];
        if (element != null) {
            buffer[head] = null;
            head = increment(head);
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
        if (index >= buffer.length) {
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
            index = buffer.length - 1;
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

            private int index = head;
            private int lastReturnedIndex = -1;

            public boolean hasNext() {
                return index != tail;

            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                lastReturnedIndex = index;
                index = increment(index);
                return buffer[lastReturnedIndex];
            }

            public void remove() {
                if (lastReturnedIndex == -1) {
                    throw new IllegalStateException();
                }

                // First element can be removed quickly
                if (lastReturnedIndex == head) {
                    UnboundedFifoBuffer.this.remove();
                    lastReturnedIndex = -1;
                    return;
                }

                // Other elements require us to shift the subsequent elements
                int i = increment(lastReturnedIndex);
                while (i != tail) {
                    buffer[decrement(i)] = buffer[i];
                    i = increment(i);
                }

                lastReturnedIndex = -1;
                tail = decrement(tail);
                buffer[tail] = null;
                index = decrement(index);
            }

        };
    }

}
