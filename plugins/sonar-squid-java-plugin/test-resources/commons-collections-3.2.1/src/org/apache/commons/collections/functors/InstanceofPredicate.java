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

/**
 * Predicate implementation that returns true if the input is an instanceof
 * the type stored in this predicate.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public final class InstanceofPredicate implements Predicate, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -6682656911025165584L;

    /** The type to compare to */
    private final Class iType;
    
    /**
     * Factory to create the identity predicate.
     * 
     * @param type  the type to check for, may not be null
     * @return the predicate
     * @throws IllegalArgumentException if the class is null
     */
    public static Predicate getInstance(Class type) {
        if (type == null) {
            throw new IllegalArgumentException("The type to check instanceof must not be null");
        }
        return new InstanceofPredicate(type);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param type  the type to check for
     */
    public InstanceofPredicate(Class type) {
        super();
        iType = type;
    }

    /**
     * Evaluates the predicate returning true if the input object is of the correct type.
     * 
     * @param object  the input object
     * @return true if input is of stored type
     */
    public boolean evaluate(Object object) {
        return (iType.isInstance(object));
    }

    /**
     * Gets the type to compare to.
     * 
     * @return the type
     * @since Commons Collections 3.1
     */
    public Class getType() {
        return iType;
    }

}
