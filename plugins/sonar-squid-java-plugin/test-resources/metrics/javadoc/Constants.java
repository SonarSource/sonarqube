/*
 * $Id: Constants.java 471754 2006-11-06 14:55:09Z husted $
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



/**
 * <p>Global constants for the Chain of Responsibility Library.</p>
 */
public final class Constants {
    // -------------------------------------------------- Context Attribute Keys

    /**
     * <p>The default context attribute under which the ForwardConfig for the
     * current request will be stored.</p>
     */
    public static final String FORWARD_CONFIG_KEY = "forwardConfig";

    /**
     * <p>The default context attribute under which the include path for the
     * current request will be stored.</p>
     */
    public static final String INCLUDE_KEY = "include";

    /**
     * <p>The default context attribute under which the Locale for the current
     * request will be stored.</p>
     */
    public static final String LOCALE_KEY = "locale";

    /**
     * <p>The default context attribute under which the MessageResources for
     * the current request will be stored.</p>
     */
    public static final String MESSAGE_RESOURCES_KEY = "messageResources";

    /**
     * <p>The default context attribute under which the ModuleConfig for the
     * current request will be stored.</p>
     */
    public static final String MODULE_CONFIG_KEY = "moduleConfig";

    /**
     * <p>The default context attribute key under which a Boolean is stored,
     * indicating the valid state of the current request.  If not present, a
     * value of Boolean.FALSE should be assumed.
     */
    public static final String VALID_KEY = "valid";

    // --------------------------------------------------------- Other Constants

    /**
     * <p>The base part of the context attribute under which a Map containing
     * the Action instances associated with this module are stored. This value
     * must be suffixed with the module prefix in order to create a unique key
     * per module.</p>
     */
    public static final String ACTIONS_KEY = "actions";
}
