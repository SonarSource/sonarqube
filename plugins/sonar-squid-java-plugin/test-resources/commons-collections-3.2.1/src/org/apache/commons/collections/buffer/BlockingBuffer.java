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

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUnderflowException;

/**
 * Decorates another <code>Buffer</code> to make {@link #get()} and
 * {@link #remove()} block when the <code>Buffer</code> is empty.
 * <p>
 * If either <code>get</code> or <code>remove</code> is called on an empty
 * <code>Buffer</code>, the calling thread waits for notification that
 * an <code>add</code> or <code>addAll</code> operation has completed.
 * <p>
 * When one or more entries are added to an empty <code>Buffer</code>,
 * all threads blocked in <code>get</code> or <code>remove</code> are notified.
 * There is no guarantee that concurrent blocked <code>get</code> or
 * <code>remove</code> requests will be "unblocked" and receive data in the
 * order that they arrive.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 * This class contains an extra field in 3.2, however the serialization
 * specification will handle this gracefully.
 *
 * @author Stephen Colebourne
 * @author Janek Bogucki
 * @author Phil Steitz
 * @author James Carman
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * @since Commons Collections 3.0
 */
public class BlockingBuffer extends SynchronizedBuffer {

    /** Serialization version. */
    private static final long serialVersionUID = 1719328905017860541L;
    /** The timeout value in milliseconds. */
    private final long timeout;

    /**
     * Factory method to create a blocking buffer.
     *
     * @param buffer the buffer to decorate, must not be null
     * @return a new blocking Buffer
     * @throws IllegalArgumentException if buffer is null
     */
    public static Buffer decorate(Buffer buffer) {
        return new BlockingBuffer(buffer);
    }

    /**
     * Factory method to create a blocking buffer with a timeout value.
     *
     * @param buffer  the buffer to decorate, must not be null
     * @param timeoutMillis  the timeout value in milliseconds, zero or less for no timeout
     * @return a new blocking buffer
     * @throws IllegalArgumentException if the buffer is null
     * @since Commons Collections 3.2
     */
    public static Buffer decorate(Buffer buffer, long timeoutMillis) {
        return new BlockingBuffer(buffer, timeoutMillis);
    }

    //-----------------------------------------------------------------------    
    /**
     * Constructor that wraps (not copies).
     *
     * @param buffer the buffer to decorate, must not be null
     * @throws IllegalArgumentException if the buffer is null
     */
    protected BlockingBuffer(Buffer buffer) {
        super(buffer);
        this.timeout = 0;
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param buffer  the buffer to decorate, must not be null
     * @param timeoutMillis  the timeout value in milliseconds, zero or less for no timeout
     * @throws IllegalArgumentException if the buffer is null
     * @since Commons Collections 3.2
     */
    protected BlockingBuffer(Buffer buffer, long timeoutMillis) {
        super(buffer);
        this.timeout = (timeoutMillis < 0 ? 0 : timeoutMillis);
    }

    //-----------------------------------------------------------------------
    public boolean add(Object o) {
        synchronized (lock) {
            boolean result = collection.add(o);
            lock.notifyAll();
            return result;
        }
    }

    public boolean addAll(Collection c) {
        synchronized (lock) {
            boolean result = collection.addAll(c);
            lock.notifyAll();
            return result;
        }
    }

    /**
     * Gets the next value from the buffer, waiting until an object is
     * added if the buffer is empty. This method uses the default timeout
     * set in the constructor.
     *
     * @throws BufferUnderflowException if an interrupt is received
     */
    public Object get() {
        synchronized (lock) {
            while (collection.isEmpty()) {
                try {
                    if (timeout <= 0) {
                        lock.wait();
                    } else {
                        return get(timeout);
                    }
                } catch (InterruptedException e) {
                    PrintWriter out = new PrintWriter(new StringWriter());
                    e.printStackTrace(out);
                    throw new BufferUnderflowException("Caused by InterruptedException: " + out.toString());
                }
            }
            return getBuffer().get();
        }
    }

    /**
     * Gets the next value from the buffer, waiting until an object is
     * added for up to the specified timeout value if the buffer is empty.
     *
     * @param timeout  the timeout value in milliseconds
     * @throws BufferUnderflowException if an interrupt is received
     * @throws BufferUnderflowException if the timeout expires
     * @since Commons Collections 3.2
     */
    public Object get(final long timeout) {
        synchronized (lock) {
            final long expiration = System.currentTimeMillis() + timeout;
            long timeLeft = expiration - System.currentTimeMillis();
            while (timeLeft > 0 && collection.isEmpty()) {
                try {
                    lock.wait(timeLeft);
                    timeLeft = expiration - System.currentTimeMillis();
                } catch(InterruptedException e) {
                    PrintWriter out = new PrintWriter(new StringWriter());
                    e.printStackTrace(out);
                    throw new BufferUnderflowException("Caused by InterruptedException: " + out.toString());
                }
            }
            if (collection.isEmpty()) {
                throw new BufferUnderflowException("Timeout expired");
            }
            return getBuffer().get();
        }
    }

    /**
     * Removes the next value from the buffer, waiting until an object is
     * added if the buffer is empty. This method uses the default timeout
     * set in the constructor.
     *
     * @throws BufferUnderflowException if an interrupt is received
     */
    public Object remove() {
        synchronized (lock) {
            while (collection.isEmpty()) {
                try {
                    if (timeout <= 0) {
                        lock.wait();
                    } else {
                        return remove(timeout);
                    }
                } catch (InterruptedException e) {
                    PrintWriter out = new PrintWriter(new StringWriter());
                    e.printStackTrace(out);
                    throw new BufferUnderflowException("Caused by InterruptedException: " + out.toString());
                }
            }
            return getBuffer().remove();
        }
    }

    /**
     * Removes the next value from the buffer, waiting until an object is
     * added for up to the specified timeout value if the buffer is empty.
     *
     * @param timeout  the timeout value in milliseconds
     * @throws BufferUnderflowException if an interrupt is received
     * @throws BufferUnderflowException if the timeout expires
     * @since Commons Collections 3.2
     */
    public Object remove(final long timeout) {
        synchronized (lock) {
            final long expiration = System.currentTimeMillis() + timeout;
            long timeLeft = expiration - System.currentTimeMillis();
            while (timeLeft > 0 && collection.isEmpty()) {
                try {
                    lock.wait(timeLeft);
                    timeLeft = expiration - System.currentTimeMillis();
                } catch(InterruptedException e) {
                    PrintWriter out = new PrintWriter(new StringWriter());
                    e.printStackTrace(out);
                    throw new BufferUnderflowException("Caused by InterruptedException: " + out.toString());
                }
            }
            if (collection.isEmpty()) {
                throw new BufferUnderflowException("Timeout expired");
            }
            return getBuffer().remove();
        }
    }

}
