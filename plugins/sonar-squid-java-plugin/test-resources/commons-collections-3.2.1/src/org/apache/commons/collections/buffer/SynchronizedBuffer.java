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

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.collection.SynchronizedCollection;

/**
 * Decorates another <code>Buffer</code> to synchronize its behaviour
 * for a multi-threaded environment.
 * <p>
 * Methods are synchronized, then forwarded to the decorated buffer.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public class SynchronizedBuffer extends SynchronizedCollection implements Buffer {

    /** Serialization version */
    private static final long serialVersionUID = -6859936183953626253L;

    /**
     * Factory method to create a synchronized buffer.
     * 
     * @param buffer  the buffer to decorate, must not be null
     * @return a new synchronized Buffer
     * @throws IllegalArgumentException if buffer is null
     */
    public static Buffer decorate(Buffer buffer) {
        return new SynchronizedBuffer(buffer);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * 
     * @param buffer  the buffer to decorate, must not be null
     * @throws IllegalArgumentException if the buffer is null
     */
    protected SynchronizedBuffer(Buffer buffer) {
        super(buffer);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param buffer  the buffer to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if the buffer is null
     */
    protected SynchronizedBuffer(Buffer buffer, Object lock) {
        super(buffer, lock);
    }

    /**
     * Gets the buffer being decorated.
     * 
     * @return the decorated buffer
     */
    protected Buffer getBuffer() {
        return (Buffer) collection;
    }

    //-----------------------------------------------------------------------
    public Object get() {
        synchronized (lock) {
            return getBuffer().get();
        }
    }

    public Object remove() {
        synchronized (lock) {
            return getBuffer().remove();
        }
    }
    
}
