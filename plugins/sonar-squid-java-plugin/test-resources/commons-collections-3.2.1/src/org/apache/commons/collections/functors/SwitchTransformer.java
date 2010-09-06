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
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;

/**
 * Transformer implementation calls the transformer whose predicate returns true,
 * like a switch statement.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public class SwitchTransformer implements Transformer, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -6404460890903469332L;

    /** The tests to consider */
    private final Predicate[] iPredicates;
    /** The matching transformers to call */
    private final Transformer[] iTransformers;
    /** The default transformer to call if no tests match */
    private final Transformer iDefault;

    /**
     * Factory method that performs validation and copies the parameter arrays.
     * 
     * @param predicates  array of predicates, cloned, no nulls
     * @param transformers  matching array of transformers, cloned, no nulls
     * @param defaultTransformer  the transformer to use if no match, null means return null
     * @return the <code>chained</code> transformer
     * @throws IllegalArgumentException if array is null
     * @throws IllegalArgumentException if any element in the array is null
     */
    public static Transformer getInstance(Predicate[] predicates, Transformer[] transformers, Transformer defaultTransformer) {
        FunctorUtils.validate(predicates);
        FunctorUtils.validate(transformers);
        if (predicates.length != transformers.length) {
            throw new IllegalArgumentException("The predicate and transformer arrays must be the same size");
        }
        if (predicates.length == 0) {
            return (defaultTransformer == null ? ConstantTransformer.NULL_INSTANCE : defaultTransformer);
        }
        predicates = FunctorUtils.copy(predicates);
        transformers = FunctorUtils.copy(transformers);
        return new SwitchTransformer(predicates, transformers, defaultTransformer);
    }

    /**
     * Create a new Transformer that calls one of the transformers depending 
     * on the predicates. 
     * <p>
     * The Map consists of Predicate keys and Transformer values. A transformer 
     * is called if its matching predicate returns true. Each predicate is evaluated
     * until one returns true. If no predicates evaluate to true, the default
     * transformer is called. The default transformer is set in the map with a 
     * null key. The ordering is that of the iterator() method on the entryset 
     * collection of the map.
     * 
     * @param predicatesAndTransformers  a map of predicates to transformers
     * @return the <code>switch</code> transformer
     * @throws IllegalArgumentException if the map is null
     * @throws IllegalArgumentException if any transformer in the map is null
     * @throws ClassCastException  if the map elements are of the wrong type
     */
    public static Transformer getInstance(Map predicatesAndTransformers) {
        Transformer[] transformers = null;
        Predicate[] preds = null;
        if (predicatesAndTransformers == null) {
            throw new IllegalArgumentException("The predicate and transformer map must not be null");
        }
        if (predicatesAndTransformers.size() == 0) {
            return ConstantTransformer.NULL_INSTANCE;
        }
        // convert to array like this to guarantee iterator() ordering
        Transformer defaultTransformer = (Transformer) predicatesAndTransformers.remove(null);
        int size = predicatesAndTransformers.size();
        if (size == 0) {
            return (defaultTransformer == null ? ConstantTransformer.NULL_INSTANCE : defaultTransformer);
        }
        transformers = new Transformer[size];
        preds = new Predicate[size];
        int i = 0;
        for (Iterator it = predicatesAndTransformers.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            preds[i] = (Predicate) entry.getKey();
            transformers[i] = (Transformer) entry.getValue();
            i++;
        }
        return new SwitchTransformer(preds, transformers, defaultTransformer);
    }
    
    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param predicates  array of predicates, not cloned, no nulls
     * @param transformers  matching array of transformers, not cloned, no nulls
     * @param defaultTransformer  the transformer to use if no match, null means return null
     */
    public SwitchTransformer(Predicate[] predicates, Transformer[] transformers, Transformer defaultTransformer) {
        super();
        iPredicates = predicates;
        iTransformers = transformers;
        iDefault = (defaultTransformer == null ? ConstantTransformer.NULL_INSTANCE : defaultTransformer);
    }

    /**
     * Transforms the input to result by calling the transformer whose matching
     * predicate returns true.
     * 
     * @param input  the input object to transform
     * @return the transformed result
     */
    public Object transform(Object input) {
        for (int i = 0; i < iPredicates.length; i++) {
            if (iPredicates[i].evaluate(input) == true) {
                return iTransformers[i].transform(input);
            }
        }
        return iDefault.transform(input);
    }

    /**
     * Gets the predicates, do not modify the array.
     * 
     * @return the predicates
     * @since Commons Collections 3.1
     */
    public Predicate[] getPredicates() {
        return iPredicates;
    }

    /**
     * Gets the transformers, do not modify the array.
     * 
     * @return the transformers
     * @since Commons Collections 3.1
     */
    public Transformer[] getTransformers() {
        return iTransformers;
    }

    /**
     * Gets the default transformer.
     * 
     * @return the default transformer
     * @since Commons Collections 3.1
     */
    public Transformer getDefaultTransformer() {
        return iDefault;
    }

}
