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

import org.apache.commons.collections.buffer.BlockingBuffer;
import org.apache.commons.collections.buffer.PredicatedBuffer;
import org.apache.commons.collections.buffer.SynchronizedBuffer;
import org.apache.commons.collections.buffer.TransformedBuffer;
import org.apache.commons.collections.buffer.TypedBuffer;
import org.apache.commons.collections.buffer.UnmodifiableBuffer;
import org.apache.commons.collections.buffer.BoundedBuffer;

/**
 * Provides utility methods and decorators for {@link Buffer} instances.
 *
 * @since Commons Collections 2.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Paul Jack
 * @author Stephen Colebourne
 */
public class BufferUtils {

    /**
     * An empty unmodifiable buffer.
     */
    public static final Buffer EMPTY_BUFFER = UnmodifiableBuffer.decorate(new ArrayStack(1));

    /**
     * <code>BufferUtils</code> should not normally be instantiated.
     */
    public BufferUtils() {
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a synchronized buffer backed by the given buffer.
     * Much like the synchronized collections returned by
     * {@link java.util.Collections}, you must manually synchronize on
     * the returned buffer's iterator to avoid non-deterministic behavior:
     *
     * <pre>
     * Buffer b = BufferUtils.synchronizedBuffer(myBuffer);
     * synchronized (b) {
     *     Iterator i = b.iterator();
     *     while (i.hasNext()) {
     *         process (i.next());
     *     }
     * }
     * </pre>
     *
     * @param buffer  the buffer to synchronize, must not be null
     * @return a synchronized buffer backed by that buffer
     * @throws IllegalArgumentException  if the Buffer is null
     */
    public static Buffer synchronizedBuffer(Buffer buffer) {
        return SynchronizedBuffer.decorate(buffer);
    }

    /**
     * Returns a synchronized buffer backed by the given buffer that will
     * block on {@link Buffer#get()} and {@link Buffer#remove()} operations.
     * If the buffer is empty, then the {@link Buffer#get()} and
     * {@link Buffer#remove()} operations will block until new elements
     * are added to the buffer, rather than immediately throwing a
     * <code>BufferUnderflowException</code>.
     *
     * @param buffer  the buffer to synchronize, must not be null
     * @return a blocking buffer backed by that buffer
     * @throws IllegalArgumentException  if the Buffer is null
     */
    public static Buffer blockingBuffer(Buffer buffer) {
        return BlockingBuffer.decorate(buffer);
    }

    /**
     * Returns a synchronized buffer backed by the given buffer that will
     * block on {@link Buffer#get()} and {@link Buffer#remove()} operations
     * until <code>timeout</code> expires.  If the buffer is empty, then the
     * {@link Buffer#get()} and {@link Buffer#remove()} operations will block
     * until new elements are added to the buffer, rather than immediately
     * throwing a <code>BufferUnderflowException</code>.
     *
     * @param buffer  the buffer to synchronize, must not be null
     * @param timeoutMillis  the timeout value in milliseconds, zero or less for no timeout
     * @return a blocking buffer backed by that buffer
     * @throws IllegalArgumentException  if the Buffer is null
     * @since Commons Collections 3.2
     */
    public static Buffer blockingBuffer(Buffer buffer, long timeoutMillis) {
        return BlockingBuffer.decorate(buffer, timeoutMillis);
    }

    /**
     * Returns a synchronized buffer backed by the given buffer that will
     * block on {@link Buffer#add(Object)} and
     * {@link Buffer#addAll(java.util.Collection)} until enough object(s) are
     * removed from the buffer to allow the object(s) to be added and still
     * maintain the maximum size.
     *
     * @param buffer  the buffer to make bounded,  must not be null
     * @param maximumSize  the maximum size
     * @return a bounded buffer backed by the given buffer
     * @throws IllegalArgumentException if the given buffer is null
     * @since Commons Collections 3.2
     */
    public static Buffer boundedBuffer(Buffer buffer, int maximumSize) {
        return BoundedBuffer.decorate(buffer, maximumSize);
    }

    /**
     * Returns a synchronized buffer backed by the given buffer that will
     * block on {@link Buffer#add(Object)} and
     * {@link Buffer#addAll(java.util.Collection)} until enough object(s) are
     * removed from the buffer to allow the object(s) to be added and still
     * maintain the maximum size or the timeout expires.
     *
     * @param buffer the buffer to make bounded, must not be null
     * @param maximumSize the maximum size
     * @param timeoutMillis  the timeout value in milliseconds, zero or less for no timeout
     * @return a bounded buffer backed by the given buffer
     * @throws IllegalArgumentException if the given buffer is null
     * @since Commons Collections 3.2
     */
    public static Buffer boundedBuffer(Buffer buffer, int maximumSize, long timeoutMillis) {
        return BoundedBuffer.decorate(buffer, maximumSize, timeoutMillis);
    }

    /**
     * Returns an unmodifiable buffer backed by the given buffer.
     *
     * @param buffer  the buffer to make unmodifiable, must not be null
     * @return an unmodifiable buffer backed by that buffer
     * @throws IllegalArgumentException  if the Buffer is null
     */
    public static Buffer unmodifiableBuffer(Buffer buffer) {
        return UnmodifiableBuffer.decorate(buffer);
    }

    /**
     * Returns a predicated (validating) buffer backed by the given buffer.
     * <p>
     * Only objects that pass the test in the given predicate can be added to the buffer.
     * Trying to add an invalid object results in an IllegalArgumentException.
     * It is important not to use the original buffer after invoking this method,
     * as it is a backdoor for adding invalid objects.
     *
     * @param buffer  the buffer to predicate, must not be null
     * @param predicate  the predicate used to evaluate new elements, must not be null
     * @return a predicated buffer
     * @throws IllegalArgumentException  if the Buffer or Predicate is null
     */
    public static Buffer predicatedBuffer(Buffer buffer, Predicate predicate) {
        return PredicatedBuffer.decorate(buffer, predicate);
    }

    /**
     * Returns a typed buffer backed by the given buffer.
     * <p>
     * Only elements of the specified type can be added to the buffer.
     *
     * @param buffer  the buffer to predicate, must not be null
     * @param type  the type to allow into the buffer, must not be null
     * @return a typed buffer
     * @throws IllegalArgumentException  if the buffer or type is null
     */
    public static Buffer typedBuffer(Buffer buffer, Class type) {
        return TypedBuffer.decorate(buffer, type);
    }

    /**
     * Returns a transformed buffer backed by the given buffer.
     * <p>
     * Each object is passed through the transformer as it is added to the
     * Buffer. It is important not to use the original buffer after invoking this 
     * method, as it is a backdoor for adding untransformed objects.
     *
     * @param buffer  the buffer to predicate, must not be null
     * @param transformer  the transformer for the buffer, must not be null
     * @return a transformed buffer backed by the given buffer
     * @throws IllegalArgumentException  if the Buffer or Transformer is null
     */
    public static Buffer transformedBuffer(Buffer buffer, Transformer transformer) {
        return TransformedBuffer.decorate(buffer, transformer);
    }

}
