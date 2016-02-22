/*
 * $Id: ModuleException.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.util;

import org.apache.struts.action.ActionMessage;

/**
 * Used for specialized exception handling.
 */
public class ModuleException extends Exception {
    protected String property = null;

    /**
     * The ActionMessage associated with this exception.
     *
     * @since Struts 1.2
     */
    protected ActionMessage message = null;

    /**
     * Construct an module exception with no replacement values.
     *
     * @param key Message key for this error message
     */
    public ModuleException(String key) {
        super(key);
        message = new ActionMessage(key);
    }

    /**
     * Construct an module exception with the specified replacement values.
     *
     * @param key   Message key for this error message
     * @param value First replacement value
     */
    public ModuleException(String key, Object value) {
        super(key);
        message = new ActionMessage(key, value);
    }

    /**
     * Construct an module exception with the specified replacement values.
     *
     * @param key    Message key for this error message
     * @param value0 First replacement value
     * @param value1 Second replacement value
     */
    public ModuleException(String key, Object value0, Object value1) {
        super(key);
        message = new ActionMessage(key, value0, value1);
    }

    /**
     * Construct an module exception with the specified replacement values.
     *
     * @param key    Message key for this error message
     * @param value0 First replacement value
     * @param value1 Second replacement value
     * @param value2 Third replacement value
     */
    public ModuleException(String key, Object value0, Object value1,
        Object value2) {
        super(key);
        message = new ActionMessage(key, value0, value1, value2);
    }

    /**
     * Construct an module exception with the specified replacement values.
     *
     * @param key    Message key for this error message
     * @param value0 First replacement value
     * @param value1 Second replacement value
     * @param value2 Third replacement value
     * @param value3 Fourth replacement value
     */
    public ModuleException(String key, Object value0, Object value1,
        Object value2, Object value3) {
        super(key);
        message = new ActionMessage(key, value0, value1, value2, value3);
    }

    /**
     * Construct an error with the specified replacement values.
     *
     * @param key    Message key for this message
     * @param values Array of replacement values
     */
    public ModuleException(String key, Object[] values) {
        super(key);
        message = new ActionMessage(key, values);
    }

    /**
     * Returns the property associated with the exception.
     *
     * @return Value of property.
     */
    public String getProperty() {
        return (property != null) ? property : message.getKey();
    }

    /**
     * Set the property associated with the exception. It can be a name of the
     * edit field, which 'caused' the exception.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Returns the error associated with the exception.
     *
     * @return Value of property error.
     * @since Struts 1.2
     */
    public ActionMessage getActionMessage() {
        return this.message;
    }
}
