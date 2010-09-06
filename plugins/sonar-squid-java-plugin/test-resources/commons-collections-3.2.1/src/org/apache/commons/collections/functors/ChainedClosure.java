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
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.Closure;

/**
 * Closure implementation that chains the specified closures together.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class ChainedClosure implements Closure, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -3520677225766901240L;

    /** The closures to call in turn */
    private final Closure[] iClosures;

    /**
     * Factory method that performs validation and copies the parameter array.
     * 
     * @param closures  the closures to chain, copied, no nulls
     * @return the <code>chained</code> closure
     * @throws IllegalArgumentException if the closures array is null
     * @throws IllegalArgumentException if any closure in the array is null
     */
    public static Closure getInstance(Closure[] closures) {
        FunctorUtils.validate(closures);
        if (closures.length == 0) {
            return NOPClosure.INSTANCE;
        }
        closures = FunctorUtils.copy(closures);
        return new ChainedClosure(closures);
    }
    
    /**
     * Create a new Closure that calls each closure in turn, passing the 
     * result into the next closure. The ordering is that of the iterator()
     * method on the collection.
     * 
     * @param closures  a collection of closures to chain
     * @return the <code>chained</code> closure
     * @throws IllegalArgumentException if the closures collection is null
     * @throws IllegalArgumentException if any closure in the collection is null
     */
    public static Closure getInstance(Collection closures) {
        if (closures == null) {
            throw new IllegalArgumentException("Closure collection must not be null");
        }
        if (closures.size() == 0) {
            return NOPClosure.INSTANCE;
        }
        // convert to array like this to guarantee iterator() ordering
        Closure[] cmds = new Closure[closures.size()];
        int i = 0;
        for (Iterator it = closures.iterator(); it.hasNext();) {
            cmds[i++] = (Closure) it.next();
        }
        FunctorUtils.validate(cmds);
        return new ChainedClosure(cmds);
    }

    /**
     * Factory method that performs validation.
     * 
     * @param closure1  the first closure, not null
     * @param closure2  the second closure, not null
     * @return the <code>chained</code> closure
     * @throws IllegalArgumentException if either closure is null
     */
    public static Closure getInstance(Closure closure1, Closure closure2) {
        if (closure1 == null || closure2 == null) {
            throw new IllegalArgumentException("Closures must not be null");
        }
        Closure[] closures = new Closure[] { closure1, closure2 };
        return new ChainedClosure(closures);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param closures  the closures to chain, not copied, no nulls
     */
    public ChainedClosure(Closure[] closures) {
        super();
        iClosures = closures;
    }

    /**
     * Execute a list of closures.
     * 
     * @param input  the input object passed to each closure
     */
    public void execute(Object input) {
        for (int i = 0; i < iClosures.length; i++) {
            iClosures[i].execute(input);
        }
    }

    /**
     * Gets the closures, do not modify the array.
     * @return the closures
     * @since Commons Collections 3.1
     */
    public Closure[] getClosures() {
        return iClosures;
    }

}
