/*
 * $Id: InvalidPathException.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.commands;


/**
 * <p>Exception thrown when no mapping can be found for the specified
 * path.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-11-05 21:44:59 -0500 (Sat, 05 Nov 2005)
 *          $
 */
public class InvalidPathException extends Exception {
    /**
     * Field for Path property.
     */
    private String path;

    /**
     * <p> Default, no-argument constructor. </p>
     */
    public InvalidPathException() {
        super();
    }

    /**
     * <p> Constructor to inject message and path upon instantiation. </p>
     *
     * @param message The error or warning message.
     * @param path    The invalid path.
     */
    public InvalidPathException(String message, String path) {
        super(message);
        this.path = path;
    }

    /**
     * <p> Return the invalid path causing the exception. </p>
     *
     * @return The invalid path causing the exception.
     */
    public String getPath() {
        return path;
    }
}
