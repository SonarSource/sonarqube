/*
 * $Id: ClassUtils.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts.chain.commands.util;


/**
 * <p>Utility methods to load application classes and create instances.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-11-12 13:01:44 -0500 (Sat, 12 Nov 2005)
 *          $
 */
public final class ClassUtils {
    // ---------------------------------------------------------- Static Methods

    /**
     * <p>Return the <code>Class</code> object for the specified fully
     * qualified class name, from this web application's class loader.
     *
     * @param className Fully qualified class name
     * @throws ClassNotFoundException if the specified class cannot be loaded
     */
    public static Class getApplicationClass(String className)
        throws ClassNotFoundException {
        if (className == null) {
            throw new NullPointerException(
                "getApplicationClass called with null className");
        }

        ClassLoader classLoader =
            Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = ClassUtils.class.getClassLoader();
        }

        return (classLoader.loadClass(className));
    }

    /**
     * <p>Return a new instance of the specified fully qualified class name,
     * after loading the class (if necessary) from this web application's
     * class loader.</p>
     *
     * @param className Fully qualified class name
     * @throws ClassNotFoundException if the specified class cannot be loaded
     * @throws IllegalAccessException if this class is not concrete
     * @throws InstantiationException if this class has no zero-arguments
     *                                constructor
     */
    public static Object getApplicationInstance(String className)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        return (getApplicationClass(className).newInstance());
    }
}
