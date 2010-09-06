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

/**
 * Defines a functor interface implemented by classes that create objects.
 * <p>
 * A <code>Factory</code> creates an object without using an input parameter.
 * If an input parameter is required, then {@link Transformer} is more appropriate.
 * <p>
 * Standard implementations of common factories are provided by
 * {@link FactoryUtils}. These include factories that return a constant,
 * a copy of a prototype or a new instance.
 * 
 * @since Commons Collections 2.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Arron Bates
 * @author Stephen Colebourne
 */
public interface Factory {

    /**
     * Create a new object.
     *
     * @return a new object
     * @throws FunctorException (runtime) if the factory cannot create an object
     */
    public Object create();

}
