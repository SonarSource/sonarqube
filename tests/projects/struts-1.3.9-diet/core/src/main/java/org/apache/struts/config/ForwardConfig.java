/*
 * $Id: ForwardConfig.java 471754 2006-11-06 14:55:09Z husted $
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
 * <p>A JavaBean representing the configuration information of a
 * <code>&lt;forward&gt;</code> element from a Struts configuration file.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-14 17:24:39 -0400 (Sun, 14 Aug 2005)
 *          $
 * @since Struts 1.1
 */
public class ForwardConfig extends BaseConfig {
    // ------------------------------------------------------------- Properties

    /**
     * The name of the ForwardConfig that this object should inherit
     * properties from.
     */
    protected String inherit = null;

    /**
     * Have the inheritance values for this class been applied?
     */
    protected boolean extensionProcessed = false;

    /**
     * The unique identifier of this forward, which is used to reference it in
     * <code>Action</code> classes.
     */
    protected String name = null;

    /**
     * <p>The URL to which this <code>ForwardConfig</code> entry points, which
     * must start with a slash ("/") character.  It is interpreted according
     * to the following rules:</p>
     *
     * <ul>
     *
     * <li>If <code>contextRelative</code> property is <code>true</code>, the
     * path is considered to be context-relative within the current web
     * application (even if we are in a named module).  It will be prefixed by
     * the context path to create a server-relative URL.</li>
     *
     * <li>If the <code>contextRelative</code> property is false, the path is
     * considered to be the module-relative portion of the URL. It will be
     * used as the replacement for the <code>$P</code> marker in the
     * <code>forwardPattern</code> property defined on the {@link
     * ControllerConfig} element for our current module. For the default
     * <code>forwardPattern</code> value of <code>$C$M$P</code>, the resulting
     * server-relative URL will be the concatenation of the context path, the
     * module prefix, and the <code>path</code> from this
     * <code>ForwardConfig</code>.
     *
     * </li>
     *
     * </ul>
     */
    protected String path = null;

    /**
     * <p>The prefix of the module to which this <code>ForwardConfig</code>
     * entry points, which must start with a slash ("/") character.  </p>
     * <p>Usage note: If a forward config is used in a hyperlink, and a module
     * is specified, the path must lead to another action and not directly to
     * a page. This is in keeping with rule that in a modular application all
     * links must be to an action rather than a page. </p>
     */
    protected String module = null;

    /**
     * Should a redirect be used to transfer control to the specified path?
     */
    protected boolean redirect = false;

    /**
     * <p>The name of a <code>commons-chain</code> command which should be
     * looked up and executed before Struts dispatches control to the view
     * represented by this config.</p>
     */
    protected String command = null;

    /**
     * <p>The name of a <code>commons-chain</code> catalog in which
     * <code>command</code> should be looked up.  If this value is undefined,
     * then the command will be looked up in the "default" catalog.  This
     * value has no meaning except in the context of the <code>command</code>
     * property.</p>
     */
    protected String catalog = null;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance with default values.
     */
    public ForwardConfig() {
        super();
    }

    /**
     * Construct a new instance with the specified values.
     *
     * @param name     Name of this forward
     * @param path     Path to which control should be forwarded or
     *                 redirected
     * @param redirect Should we do a redirect?
     */
    public ForwardConfig(String name, String path, boolean redirect) {
        super();
        setName(name);
        setPath(path);
        setRedirect(redirect);
    }

    /**
     * <p>Construct a new instance with the specified values.</p>
     *
     * @param name     Name of this forward
     * @param path     Path to which control should be forwarded or
     *                 redirected
     * @param redirect Should we do a redirect?
     * @param module   Module prefix, if any
     */
    public ForwardConfig(String name, String path, boolean redirect,
        String module) {
        super();
        setName(name);
        setPath(path);
        setRedirect(redirect);
        setModule(module);
    }

    /**
     * <p>Construct a new instance based on the values of another
     * ForwardConfig.</p>
     *
     * @param copyMe A ForwardConfig instance to copy
     * @since Struts 1.3.6
     */
    public ForwardConfig(ForwardConfig copyMe) {
        this(copyMe.getName(), copyMe.getPath(), copyMe.getRedirect(),
            copyMe.getModule());
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

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.name = name;
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

    public String getModule() {
        return (this.module);
    }

    public void setModule(String module) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.module = module;
    }

    public boolean getRedirect() {
        return (this.redirect);
    }

    public void setRedirect(boolean redirect) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.redirect = redirect;
    }

    public String getCommand() {
        return (this.command);
    }

    public void setCommand(String command) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.command = command;
    }

    public String getCatalog() {
        return (this.catalog);
    }

    public void setCatalog(String catalog) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.catalog = catalog;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * <p>Traces the hierarchy of this object to check if any of the ancestors
     * are extending this instance.</p>
     *
     * @param moduleConfig The {@link ModuleConfig} that this config is from.
     * @param actionConfig The {@link ActionConfig} that this config is from,
     *                     if applicable.  This parameter must be null if this
     *                     forward config is a global forward.
     * @return true if circular inheritance was detected.
     */
    protected boolean checkCircularInheritance(ModuleConfig moduleConfig,
        ActionConfig actionConfig) {
        String ancestorName = getExtends();

        if (ancestorName == null) {
            return false;
        }

        // Find our ancestor
        ForwardConfig ancestor = null;

        // First check the action config
        if (actionConfig != null) {
            ancestor = actionConfig.findForwardConfig(ancestorName);

            // If we found *this*, set ancestor to null to check for a global def
            if (ancestor == this) {
                ancestor = null;
            }
        }

        // Then check the global forwards
        if (ancestor == null) {
            ancestor = moduleConfig.findForwardConfig(ancestorName);

            if (ancestor != null) {
                // If the ancestor is a global forward, set actionConfig
                //  to null so further searches are only done among
                //  global forwards.
                actionConfig = null;
            }
        }

        while (ancestor != null) {
            // Check if an ancestor is extending *this*
            if (ancestor == this) {
                return true;
            }

            // Get our ancestor's ancestor
            ancestorName = ancestor.getExtends();

            // check against ancestors extending same named ancestors
            if (ancestor.getName().equals(ancestorName)) {
                // If the ancestor is extending a config with the same name
                //  make sure we look for its ancestor in the global forwards.
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
                ancestor = actionConfig.findForwardConfig(ancestorName);
            }

            // Then check the global forwards
            if (ancestor == null) {
                ancestor = moduleConfig.findForwardConfig(ancestorName);

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
    public void inheritFrom(ForwardConfig config)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        // Inherit values that have not been overridden
        if (getCatalog() == null) {
            setCatalog(config.getCatalog());
        }

        if (getCommand() == null) {
            setCommand(config.getCommand());
        }

        if (getModule() == null) {
            setModule(config.getModule());
        }

        if (getName() == null) {
            setName(config.getName());
        }

        if (getPath() == null) {
            setPath(config.getPath());
        }

        if (!getRedirect()) {
            setRedirect(config.getRedirect());
        }

        inheritProperties(config);
    }

    /**
     * <p>Inherit configuration information from the ForwardConfig that this
     * instance is extending.  This method verifies that any forward config
     * object that it inherits from has also had its processExtends() method
     * called.</p>
     *
     * @param moduleConfig The {@link ModuleConfig} that this config is from.
     * @param actionConfig The {@link ActionConfig} that this config is from,
     *                     if applicable.  This must be null for global
     *                     forwards.
     * @see #inheritFrom(ForwardConfig)
     */
    public void processExtends(ModuleConfig moduleConfig,
        ActionConfig actionConfig)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        String ancestorName = getExtends();

        if ((!extensionProcessed) && (ancestorName != null)) {
            ForwardConfig baseConfig = null;

            // We only check the action config if we're not a global forward
            boolean checkActionConfig =
                (this != moduleConfig.findForwardConfig(getName()));

            // ... and the action config was provided
            checkActionConfig &= (actionConfig != null);

            // ... and we're not extending a config with the same name
            // (because if we are, that means we're an action-level forward
            //  extending a global forward).
            checkActionConfig &= !ancestorName.equals(getName());

            // We first check in the action config's forwards
            if (checkActionConfig) {
                baseConfig = actionConfig.findForwardConfig(ancestorName);
            }

            // Then check the global forwards
            if (baseConfig == null) {
                baseConfig = moduleConfig.findForwardConfig(ancestorName);
            }

            if (baseConfig == null) {
                throw new NullPointerException("Unable to find " + "forward '"
                    + ancestorName + "' to extend.");
            }

            // Check for circular inheritance and make sure the base config's
            //  own extends have been processed already
            if (checkCircularInheritance(moduleConfig, actionConfig)) {
                throw new IllegalArgumentException(
                    "Circular inheritance detected for forward " + getName());
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
        StringBuffer sb = new StringBuffer("ForwardConfig[");

        sb.append("name=");
        sb.append(this.name);
        sb.append(",path=");
        sb.append(this.path);
        sb.append(",redirect=");
        sb.append(this.redirect);
        sb.append(",module=");
        sb.append(this.module);
        sb.append(",extends=");
        sb.append(this.inherit);
        sb.append(",catalog=");
        sb.append(this.catalog);
        sb.append(",command=");
        sb.append(this.command);
        sb.append("]");

        return (sb.toString());
    }
}
