/*
 * $Id: ExceptionConfig.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.config;

import java.lang.reflect.InvocationTargetException;

/**
 * <p>A JavaBean representing the configuration information of an
 * <code>&lt;exception&gt;</code> element from a Struts configuration
 * file.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-06 18:03:30 -0400 (Sat, 06 Aug 2005)
 *          $
 * @since Struts 1.1
 */
public class ExceptionConfig extends BaseConfig {
    // ------------------------------------------------------------- Properties

    /**
     * The servlet context attribute under which the message resources bundle
     * to be used for this exception is located.  If not set, the default
     * message resources for the current module is assumed.
     */
    protected String bundle = null;

    /**
     * The type of the ExceptionConfig that this object should inherit
     * properties from.
     */
    protected String inherit = null;

    /**
     * Have the inheritance values for this class been applied?
     */
    protected boolean extensionProcessed = false;

    /**
     * The fully qualified Java class name of the exception handler class
     * which should be instantiated to handle this exception.
     */
    protected String handler = "org.apache.struts.action.ExceptionHandler";

    /**
     * The message resources key specifying the error message associated with
     * this exception.
     */
    protected String key = null;

    /**
     * The module-relative path of the resource to forward to if this
     * exception occurs during an <code>Action</code>.
     */
    protected String path = null;

    /**
     * The scope in which we should expose the ActionMessage for this
     * exception handler.
     */
    protected String scope = "request";

    /**
     * The fully qualified Java class name of the exception that is to be
     * handled by this handler.
     */
    protected String type = null;

    public String getBundle() {
        return (this.bundle);
    }

    public void setBundle(String bundle) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.bundle = bundle;
    }

    public String getExtends() {
        return (this.inherit);
    }

    public void setExtends(String inherit) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.inherit = inherit;
    }

    public boolean isExtensionProcessed() {
        return extensionProcessed;
    }

    public String getHandler() {
        return (this.handler);
    }

    public void setHandler(String handler) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.handler = handler;
    }

    public String getKey() {
        return (this.key);
    }

    public void setKey(String key) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.key = key;
    }

    public String getPath() {
        return (this.path);
    }

    public void setPath(String path) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.path = path;
    }

    public String getScope() {
        return (this.scope);
    }

    public void setScope(String scope) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.scope = scope;
    }

    public String getType() {
        return (this.type);
    }

    public void setType(String type) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.type = type;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * <p>Traces the hierarchy of this object to check if any of the ancestors
     * are extending this instance.</p>
     *
     * @param moduleConfig The {@link ModuleConfig} that this config is from.
     * @param actionConfig The {@link ActionConfig} that this config is from,
     *                     if applicable.  This parameter must be null if this
     *                     is a global handler.
     * @return true if circular inheritance was detected.
     */
    protected boolean checkCircularInheritance(ModuleConfig moduleConfig,
        ActionConfig actionConfig) {
        String ancestorType = getExtends();

        if (ancestorType == null) {
            return false;
        }

        // Find our ancestor
        ExceptionConfig ancestor = null;

        // First check the action config
        if (actionConfig != null) {
            ancestor = actionConfig.findExceptionConfig(ancestorType);

            // If we found *this*, set ancestor to null to check for a global def
            if (ancestor == this) {
                ancestor = null;
            }
        }

        // Then check the global handlers
        if (ancestor == null) {
            ancestor = moduleConfig.findExceptionConfig(ancestorType);

            if (ancestor != null) {
                // If the ancestor is a global handler, set actionConfig
                //  to null so further searches are only done among
                //  global handlers.
                actionConfig = null;
            }
        }

        while (ancestor != null) {
            // Check if an ancestor is extending *this*
            if (ancestor == this) {
                return true;
            }

            // Get our ancestor's ancestor
            ancestorType = ancestor.getExtends();

            // check against ancestors extending same typed ancestors
            if (ancestor.getType().equals(ancestorType)) {
                // If the ancestor is extending a config for the same type,
                //  make sure we look for its ancestor in the global handlers.
                //  If we're already at that level, we return false.
                if (actionConfig == null) {
                    return false;
                } else {
                    // Set actionConfig = null to force us to look for global
                    //  forwards
                    actionConfig = null;
                }
            }

            ancestor = null;

            // First check the action config
            if (actionConfig != null) {
                ancestor = actionConfig.findExceptionConfig(ancestorType);
            }

            // Then check the global handlers
            if (ancestor == null) {
                ancestor = moduleConfig.findExceptionConfig(ancestorType);

                if (ancestor != null) {
                    // Limit further checks to moduleConfig.
                    actionConfig = null;
                }
            }
        }

        return false;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * <p>Inherit values that have not been overridden from the provided
     * config object.  Subclasses overriding this method should verify that
     * the given parameter is of a class that contains a property it is trying
     * to inherit:</p>
     *
     * <pre>
     * if (config instanceof MyCustomConfig) {
     *     MyCustomConfig myConfig =
     *         (MyCustomConfig) config;
     *
     *     if (getMyCustomProp() == null) {
     *         setMyCustomProp(myConfig.getMyCustomProp());
     *     }
     * }
     * </pre>
     *
     * <p>If the given <code>config</code> is extending another object, those
     * extensions should be resolved before it's used as a parameter to this
     * method.</p>
     *
     * @param config The object that this instance will be inheriting its
     *               values from.
     * @see #processExtends(ModuleConfig, ActionConfig)
     */
    public void inheritFrom(ExceptionConfig config)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        // Inherit values that have not been overridden
        if (getBundle() == null) {
            setBundle(config.getBundle());
        }

        if (getHandler().equals("org.apache.struts.action.ExceptionHandler")) {
            setHandler(config.getHandler());
        }

        if (getKey() == null) {
            setKey(config.getKey());
        }

        if (getPath() == null) {
            setPath(config.getPath());
        }

        if (getScope().equals("request")) {
            setScope(config.getScope());
        }

        if (getType() == null) {
            setType(config.getType());
        }

        inheritProperties(config);
    }

    /**
     * <p>Inherit configuration information from the ExceptionConfig that this
     * instance is extending.  This method verifies that any exception config
     * object that it inherits from has also had its processExtends() method
     * called.</p>
     *
     * @param moduleConfig The {@link ModuleConfig} that this config is from.
     * @param actionConfig The {@link ActionConfig} that this config is from,
     *                     if applicable.  This must be null for global
     *                     forwards.
     * @see #inheritFrom(ExceptionConfig)
     */
    public void processExtends(ModuleConfig moduleConfig,
        ActionConfig actionConfig)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        String ancestorType = getExtends();

        if ((!extensionProcessed) && (ancestorType != null)) {
            ExceptionConfig baseConfig = null;

            // We only check the action config if we're not a global handler
            boolean checkActionConfig =
                (this != moduleConfig.findExceptionConfig(getType()));

            // ... and the action config was provided
            checkActionConfig &= (actionConfig != null);

            // ... and we're not extending a config with the same type value
            // (because if we are, that means we're an action-level handler
            //  extending a global handler).
            checkActionConfig &= !ancestorType.equals(getType());

            // We first check in the action config's exception handlers
            if (checkActionConfig) {
                baseConfig = actionConfig.findExceptionConfig(ancestorType);
            }

            // Then check the global exception handlers
            if (baseConfig == null) {
                baseConfig = moduleConfig.findExceptionConfig(ancestorType);
            }

            if (baseConfig == null) {
                throw new NullPointerException("Unable to find "
                    + "handler for '" + ancestorType + "' to extend.");
            }

            // Check for circular inheritance and make sure the base config's
            //  own inheritance has been processed already
            if (checkCircularInheritance(moduleConfig, actionConfig)) {
                throw new IllegalArgumentException(
                    "Circular inheritance detected for forward " + getType());
            }

            if (!baseConfig.isExtensionProcessed()) {
                baseConfig.processExtends(moduleConfig, actionConfig);
            }

            // copy values from the base config
            inheritFrom(baseConfig);
        }

        extensionProcessed = true;
    }

    /**
     * Return a String representation of this object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("ExceptionConfig[");

        sb.append("type=");
        sb.append(this.type);

        if (this.bundle != null) {
            sb.append(",bundle=");
            sb.append(this.bundle);
        }

        if (this.inherit != null) {
            sb.append(",extends=");
            sb.append(this.inherit);
        }

        sb.append(",handler=");
        sb.append(this.handler);
        sb.append(",key=");
        sb.append(this.key);
        sb.append(",path=");
        sb.append(this.path);
        sb.append(",scope=");
        sb.append(this.scope);
        sb.append("]");

        return (sb.toString());
    }
}
