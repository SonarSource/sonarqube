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
import org.apache.commons.collections.Transformer;

/**
 * Transformer implementation that calls a Closure using the input object
 * and then returns the input.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class ClosureTransformer implements Transformer, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = 478466901448617286L;

    /** The closure to wrap */
    private final Closure iClosure;

    /**
     * Factory method that performs validation.
     * 
     * @param closure  the closure to call, not null
     * @return the <code>closure</code> transformer
     * @throws IllegalArgumentException if the closure is null
     */
    public static Transformer getInstance(Closure closure) {
        if (closure == null) {
            throw new IllegalArgumentException("Closure must not be null");
        }
        return new ClosureTransformer(closure);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param closure  the closure to call, not null
     */
    public ClosureTransformer(Closure closure) {
        super();
        iClosure = closure;
    }

    /**
     * Transforms the input to result by executing a closure.
     * 
     * @param input  the input object to transform
     * @return the transformed result
     */
    public Object transform(Object input) {
        iClosure.execute(input);
        return input;
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

}
