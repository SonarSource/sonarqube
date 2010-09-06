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

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;

/**
 * Transformer implementation that calls a Predicate using the input object
 * and then returns the input.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class PredicateTransformer implements Transformer, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = 5278818408044349346L;

    /** The closure to wrap */
    private final Predicate iPredicate;

    /**
     * Factory method that performs validation.
     * 
     * @param predicate  the predicate to call, not null
     * @return the <code>predicate</code> transformer
     * @throws IllegalArgumentException if the predicate is null
     */
    public static Transformer getInstance(Predicate predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Predicate must not be null");
        }
        return new PredicateTransformer(predicate);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param predicate  the predicate to call, not null
     */
    public PredicateTransformer(Predicate predicate) {
        super();
        iPredicate = predicate;
    }

    /**
     * Transforms the input to result by calling a predicate.
     * 
     * @param input  the input object to transform
     * @return the transformed result
     */
    public Object transform(Object input) {
        return (iPredicate.evaluate(input) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Gets the predicate.
     * 
     * @return the predicate
     * @since Commons Collections 3.1
     */
    public Predicate getPredicate() {
        return iPredicate;
    }

}
