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

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.Transformer;

/**
 * Transformer implementation that calls a Factory and returns the result.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class FactoryTransformer implements Transformer, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -6817674502475353160L;

    /** The factory to wrap */
    private final Factory iFactory;

    /**
     * Factory method that performs validation.
     * 
     * @param factory  the factory to call, not null
     * @return the <code>factory</code> transformer
     * @throws IllegalArgumentException if the factory is null
     */
    public static Transformer getInstance(Factory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Factory must not be null");
        }
        return new FactoryTransformer(factory);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param factory  the factory to call, not null
     */
    public FactoryTransformer(Factory factory) {
        super();
        iFactory = factory;
    }

    /**
     * Transforms the input by ignoring the input and returning the result of
     * calling the decorated factory.
     * 
     * @param input  the input object to transform
     * @return the transformed result
     */
    public Object transform(Object input) {
        return iFactory.create();
    }

    /**
     * Gets the factory.
     * 
     * @return the factory
     * @since Commons Collections 3.1
     */
    public Factory getFactory() {
        return iFactory;
    }

}
