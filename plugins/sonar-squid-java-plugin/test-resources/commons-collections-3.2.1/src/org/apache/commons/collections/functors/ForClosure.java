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
package org.apache.commons.collections.functors;

import java.io.Serializable;

import org.apache.commons.collections.Closure;

/**
 * Closure implementation that calls another closure n times, like a for loop.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class ForClosure implements Closure, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -1190120533393621674L;

    /** The number of times to loop */
    private final int iCount;
    /** The closure to call */
    private final Closure iClosure;

    /**
     * Factory method that performs validation.
     * <p>
     * A null closure or zero count returns the <code>NOPClosure</code>.
     * A count of one returns the specified closure.
     * 
     * @param count  the number of times to execute the closure
     * @param closure  the closure to execute, not null
     * @return the <code>for</code> closure
     */
    public static Closure getInstance(int count, Closure closure) {
        if (count <= 0 || closure == null) {
            return NOPClosure.INSTANCE;
        }
        if (count == 1) {
            return closure;
        }
        return new ForClosure(count, closure);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param count  the number of times to execute the closure
     * @param closure  the closure to execute, not null
     */
    public ForClosure(int count, Closure closure) {
        super();
        iCount = count;
        iClosure = closure;
    }

    /**
     * Executes the closure <code>count</code> times.
     * 
     * @param input  the input object
     */
    public void execute(Object input) {
        for (int i = 0; i < iCount; i++) {
            iClosure.execute(input);
        }
    }

    /**
     * Gets the closure.
     * 
     * @return the closure
     * @since Commons Collections 3.1
     */
    public Closure getClosure() {
        return iClosure;
    }

    /**
     * Gets the count.
     * 
     * @return the count
     * @since Commons Collections 3.1
     */
    public int getCount() {
        return iCount;
    }

}
