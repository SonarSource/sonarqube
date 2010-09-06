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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.BoundedCollection;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferOverflowException;
import org.apache.commons.collections.BufferUnderflowException;
import org.apache.commons.collections.iterators.AbstractIteratorDecorator;

/**
 * Decorates another <code>Buffer</code> to ensure a fixed maximum size.
 * <p>
 * Note: This class should only be used if you need to add bounded
 * behaviour to another buffer. If you just want a bounded buffer then
 * you should use {@link BoundedFifoBuffer} or {@link CircularFifoBuffer}.
 * <p>
 * The decoration methods allow you to specify a timeout value.
 * This alters the behaviour of the add methods when the buffer is full.
 * Normally, when the buffer is full, the add method will throw an exception.
 * With a timeout, the add methods will wait for up to the timeout period
 * to try and add the elements.
 *
 * @author James Carman
 * @author Stephen Colebourne
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * @since Commons Collections 3.2
 */
public class BoundedBuffer extends SynchronizedBuffer implements BoundedCollection {

    /** The serialization version. */
    private static final long serialVersionUID = 1536432911093974264L;

    /** The maximum size. */
    private final int maximumSize;
    /** The timeout milliseconds. */
    private final long timeout;

    /**
     * Factory method to create a bounded buffer.
     * <p>
     * When the buffer is full, it will immediately throw a
     * <code>BufferOverflowException</code> on calling <code>add()</code>.
     *
     * @param buffer  the buffer to decorate, must not be null
     * @param maximumSize  the maximum size, must be size one or greater
     * @return a new bounded buffer
     * @throws IllegalArgumentException if the buffer is null
     * @throws IllegalArgumentException if the maximum size is zero or less
     */
    public static BoundedBuffer decorate(Buffer buffer, int maximumSize) {
        return new BoundedBuffer(buffer, maximumSize, 0L);
    }

    /**
     * Factory method to create a bounded buffer that blocks for a maximum
     * amount of time.
     *
     * @param buffer  the buffer to decorate, must not be null
     * @param maximumSize  the maximum size, must be size one or greater
     * @param timeout  the maximum amount of time to wait in milliseconds
     * @return a new bounded buffer
     * @throws IllegalArgumentException if the buffer is null
     * @throws IllegalArgumentException if the maximum size is zero or less
     */
    public static BoundedBuffer decorate(Buffer buffer, int maximumSize, long timeout) {
        return new BoundedBuffer(buffer, maximumSize, timeout);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies) another buffer, making it bounded
     * waiting only up to a maximum amount of time.
     *
     * @param buffer  the buffer to wrap, must not be null
     * @param maximumSize  the maximum size, must be size one or greater
     * @param timeout  the maximum amount of time to wait
     * @throws IllegalArgumentException if the buffer is null
     * @throws IllegalArgumentException if the maximum size is zero or less
     */
    protected BoundedBuffer(Buffer buffer, int maximumSize, long timeout) {
        super(buffer);
        if (maximumSize < 1) {
            throw new IllegalArgumentException();
        }
        this.maximumSize = maximumSize;
        this.timeout = timeout;
    }

    //-----------------------------------------------------------------------
    public Object remove() {
        synchronized (lock) {
            Object returnValue = getBuffer().remove();
            lock.notifyAll();
            return returnValue;
        }
    }

    public boolean add(Object o) {
        synchronized (lock) {
            timeoutWait(1);
            return getBuffer().add(o);
        }
    }

    public boolean addAll(final Collection c) {
        synchronized (lock) {
            timeoutWait(c.size());
            return getBuffer().addAll(c);
        }
    }

    public Iterator iterator() {
        return new NotifyingIterator(collection.iterator());
    }

    private void timeoutWait(final int nAdditions) {
        // method synchronized by callers
        if (nAdditions > maximumSize) {
            throw new BufferOverflowException(
                    "Buffer size cannot exceed " + maximumSize);
        }
        if (timeout <= 0) {
            // no wait period (immediate timeout)
            if (getBuffer().size() + nAdditions > maximumSize) {
                throw new BufferOverflowException(
                        "Buffer size cannot exceed " + maximumSize);
            }
            return;
        }
        final long expiration = System.currentTimeMillis() + timeout;
        long timeLeft = expiration - System.currentTimeMillis();
        while (timeLeft > 0 && getBuffer().size() + nAdditions > maximumSize) {
            try {
                lock.wait(timeLeft);
                timeLeft = expiration - System.currentTimeMillis();
            } catch (InterruptedException ex) {
                PrintWriter out = new PrintWriter(new StringWriter());
                ex.printStackTrace(out);
                throw new BufferUnderflowException(
                    "Caused by InterruptedException: " + out.toString());
            }
        }
        if (getBuffer().size() + nAdditions > maximumSize) {
            throw new BufferOverflowException("Timeout expired");
        }
    }

    public boolean isFull() {
        // size() is synchronized
        return (size() == maxSize());
    }

    public int maxSize() {
        return maximumSize;
    }

    //-----------------------------------------------------------------------
    /**
     * BoundedBuffer iterator.
     */
    private class NotifyingIterator extends AbstractIteratorDecorator {

        public NotifyingIterator(Iterator it) {
            super(it);
        }

        public void remove() {
            synchronized (lock) {
                iterator.remove();
                lock.notifyAll();
            }
        }
    }
}
