/*
 * $Id: ActionConfig.java 480593 2006-11-29 15:17:52Z niallp $
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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>A JavaBean representing the configuration information of an
 * <code>&lt;action&gt;</code> element from a Struts module configuration
 * file.</p>
 *
 * @version $Rev: 480593 $ $Date: 2006-11-29 16:17:52 +0100 (Wed, 29 Nov 2006) $
 * @since Struts 1.1
 */
public class ActionConfig extends BaseConfig {
    private static final Log log = LogFactory.getLog(ActionConfig.class);

    // ----------------------------------------------------- Instance Variables

    /**
     * <p> The set of exception handling configurations for this action, if
     * any, keyed by the <code>type</code> property. </p>
     */
    protected HashMap exceptions = new HashMap();

    /**
     * <p> The set of local forward configurations for this action, if any,
     * keyed by the <code>name</code> property. </p>
     */
    protected HashMap forwards = new HashMap();

    // ------------------------------------------------------------- Properties

    /**
     * <p> The module configuration with which we are associated. </p>
     */
    protected ModuleConfig moduleConfig = null;

    /**
     * <p> The request-scope or session-scope attribute name under which our
     * form bean is accessed, if it is different from the form bean's
     * specified <code>name</code>. </p>
     */
    protected String attribute = null;

    /**
     * <p>The internal identification of this action mapping. Identifications are
     * not inheritable and must be unique within a module.</p>
     *
     * @since Struts 1.3.6
     */
    protected String actionId = null;

    /**
     * <p>The path of the ActionConfig that this object should inherit
     * properties from.</p> </p>
     */
    protected String inherit = null;

    /**
     * Indicates whether the "cancellable " property has been set or not.
     */
    private boolean cancellableSet = false;

    /**
     * <p>Can this Action be cancelled? [false]</p> <p> By default, when an
     * Action is cancelled, validation is bypassed and the Action should not
     * execute the business operation. If a request tries to cancel an Action
     * when cancellable is not set, a "InvalidCancelException" is thrown.
     * </p>
     */
    protected boolean cancellable = false;

    /**
     * <p> Have the inheritance values for this class been applied?</p>
     */
    protected boolean extensionProcessed = false;

    /**
     * <p> Context-relative path of the web application resource that will
     * process this request via RequestDispatcher.forward(), instead of
     * instantiating and calling the <code>Action</code> class specified by
     * "type". Exactly one of <code>forward</code>, <code>include</code>, or
     * <code>type</code> must be specified. </p>
     */
    protected String forward = null;

    /**
     * <p> Context-relative path of the web application resource that will
     * process this request via RequestDispatcher.include(), instead of
     * instantiating and calling the <code>Action</code> class specified by
     * "type". Exactly one of <code>forward</code>, <code>include</code>, or
     * <code>type</code> must be specified. </p>
     */
    protected String include = null;

    /**
     * <p> Context-relative path of the input form to which control should be
     * returned if a validation error is encountered.  Required if "name" is
     * specified and the input bean returns validation errors. </p>
     */
    protected String input = null;

    /**
     * <p> Fully qualified Java class name of the <code>MultipartRequestHandler</code>
     * implementation class used to process multi-part request data for this
     * Action. </p>
     */
    protected String multipartClass = null;

    /**
     * <p> Name of the form bean, if any, associated with this Action. </p>
     */
    protected String name = null;

    /**
     * <p> General purpose configuration parameter that can be used to pass
     * extra information to the Action instance selected by this Action.
     * Struts does not itself use this value in any way. </p>
     */
    protected String parameter = null;

    /**
     * <p> Context-relative path of the submitted request, starting with a
     * slash ("/") character, and omitting any filename extension if extension
     * mapping is being used. </p>
     */
    protected String path = null;

    /**
     * <p> Prefix used to match request parameter names to form bean property
     * names, if any. </p>
     */
    protected String prefix = null;

    /**
     * <p> Comma-delimited list of security role names allowed to request this
     * Action. </p>
     */
    protected String roles = null;

    /**
     * <p> The set of security role names used to authorize access to this
     * Action, as an array for faster access. </p>
     */
    protected String[] roleNames = new String[0];

    /**
     * <p> Identifier of the scope ("request" or "session") within which our
     * form bean is accessed, if any. </p>
     */
    protected String scope = "session";

    /**
     * <p> Suffix used to match request parameter names to form bean property
     * names, if any. </p>
     */
    protected String suffix = null;

    /**
     * <p> Fully qualified Java class name of the <code>Action</code> class to
     * be used to process requests for this mapping if the
     * <code>forward</code> and <code>include</code> properties are not set.
     * Exactly one of <code>forward</code>, <code>include</code>, or
     * <code>type</code> must be specified.
     */
    protected String type = null;

    /**
     * <p> Indicates Action be configured as the default one for this module,
     * when true.
     */
    protected boolean unknown = false;

    /**
     * Indicates whether the "validate" property has been set or not.
     */
    private boolean validateSet = false;

    /**
     * <p> Should the <code>validate()</code> method of the form bean
     * associated with this action be called?
     */
    protected boolean validate = true;

    /**
     * <p> The name of a <code>commons-chain</code> command which should be
     * executed as part of the processing of this action.
     *
     * @since Struts 1.3.0
     */
    protected String command = null;

    /**
     * <p> The name of a <code>commons-chain</code> catalog in which
     * <code>command</code> should be sought.  If a <code>command</code> is
     * defined and this property is undefined, the "default" catalog will be
     * used. This is likely to be infrequently used after a future release of
     * <code>commons-chain</code> supports a one-string expression of a
     * catalog/chain combination.
     *
     * @since Struts 1.3.0
     */
    protected String catalog = null;

    /**
     * <p>The internal name of this action mapping. If an action has a name, it may be used
     * as a shortcut in a URI. For example, an action with an identification of "editPerson"
     * may be internally forwarded as "editPerson?id=1" which will then resolve to the
     * real URI path at execution time.</p>
     *
     * @return the actionId
     * @since Struts 1.3.6
     */
    public String getActionId() {
        return this.actionId;
    }

    /**
     * <p>The internal name of this action mapping. The name is not inheritable,
     * may not contain a forward slash, and must be unique within a module. </p>
     *
     * @param actionId the action identifier
     * @since Struts 1.3.6
     * @throws IllegalStateException if the configuration is frozen
     * @throws IllegalArgumentException if the identifier contains a forward slash
     */
    public void setActionId(String actionId) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }
        
        if ((actionId != null) && (actionId.indexOf("/") > -1)) {
            throw new IllegalArgumentException("actionId '" + actionId + "' may not contain a forward slash");
        }

        this.actionId = actionId;
    }

    /**
     * <p> The module configuration with which we are associated.
     */
    public ModuleConfig getModuleConfig() {
        return (this.moduleConfig);
    }

    /**
     * <p> The module configuration with which we are associated.
     */
    public void setModuleConfig(ModuleConfig moduleConfig) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.moduleConfig = moduleConfig;
    }

    /**
     * <p> Returns the request-scope or session-scope attribute name under
     * which our form bean is accessed, if it is different from the form
     * bean's specified <code>name</code>.
     *
     * @return attribute name under which our form bean is accessed.
     */
    public String getAttribute() {
        if (this.attribute == null) {
            return (this.name);
        } else {
            return (this.attribute);
        }
    }

    /**
     * <p> Set the request-scope or session-scope attribute name under which
     * our form bean is accessed, if it is different from the form bean's
     * specified <code>name</code>.
     *
     * @param attribute the request-scope or session-scope attribute name
     *                  under which our form bean is access.
     */
    public void setAttribute(String attribute) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.attribute = attribute;
    }

    /**
     * <p>Accessor for cancellable property</p>
     *
     * @return True if Action can be cancelled
     */
    public boolean getCancellable() {
        return (this.cancellable);
    }

    /**
     * <p>Mutator for for cancellable property</p>
     *
     * @param cancellable
     */
    public void setCancellable(boolean cancellable) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.cancellable = cancellable;
        this.cancellableSet = true;
    }

    /**
     * <p>Returns the path of the ActionConfig that this object should inherit
     * properties from.</p>
     *
     * @return the path of the ActionConfig that this object should inherit
     *         properties from.
     */
    public String getExtends() {
        return (this.inherit);
    }

    /**
     * <p>Set the path of the ActionConfig that this object should inherit
     * properties from.</p>
     *
     * @param inherit the path of the ActionConfig that this object should
     *                inherit properties from.
     */
    public void setExtends(String inherit) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.inherit = inherit;
    }

    public boolean isExtensionProcessed() {
        return extensionProcessed;
    }

    /**
     * <p> Returns context-relative path of the web application resource that
     * will process this request.
     *
     * @return context-relative path of the web application resource that will
     *         process this request.
     */
    public String getForward() {
        return (this.forward);
    }

    /**
     * <p> Set the context-relative path of the web application resource that
     * will process this request. Exactly one of <code>forward</code>,
     * <code>include</code>, or <code>type</code> must be specified.
     *
     * @param forward context-relative path of the web application resource
     *                that will process this request.
     */
    public void setForward(String forward) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.forward = forward;
    }

    /**
     * <p> Context-relative path of the web application resource that will
     * process this request.
     *
     * @return Context-relative path of the web application resource that will
     *         process this request.
     */
    public String getInclude() {
        return (this.include);
    }

    /**
     * <p> Set context-relative path of the web application resource that will
     * process this request. Exactly one of <code>forward</code>,
     * <code>include</code>, or <code>type</code> must be specified.
     *
     * @param include context-relative path of the web application resource
     *                that will process this request.
     */
    public void setInclude(String include) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.include = include;
    }

    /**
     * <p> Get the context-relative path of the input form to which control
     * should be returned if a validation error is encountered.
     *
     * @return context-relative path of the input form to which control should
     *         be returned if a validation error is encountered.
     */
    public String getInput() {
        return (this.input);
    }

    /**
     * <p> Set the context-relative path of the input form to which control
     * should be returned if a validation error is encountered.  Required if
     * "name" is specified and the input bean returns validation errors.
     *
     * @param input context-relative path of the input form to which control
     *              should be returned if a validation error is encountered.
     */
    public void setInput(String input) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.input = input;
    }

    /**
     * <p> Return the fully qualified Java class name of the
     * <code>MultipartRequestHandler</code> implementation class used to
     * process multi-part request data for this Action.
     */
    public String getMultipartClass() {
        return (this.multipartClass);
    }

    /**
     * <p> Set the fully qualified Java class name of the
     * <code>MultipartRequestHandler</code> implementation class used to
     * process multi-part request data for this Action.
     *
     * @param multipartClass fully qualified class name of the
     *                       <code>MultipartRequestHandler</code>
     *                       implementation class.
     */
    public void setMultipartClass(String multipartClass) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.multipartClass = multipartClass;
    }

    /**
     * <p> Return name of the form bean, if any, associated with this Action.
     */
    public String getName() {
        return (this.name);
    }

    /**
     * @param name name of the form bean associated with this Action.
     */
    public void setName(String name) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.name = name;
    }

    /**
     * <p> Return general purpose configuration parameter that can be used to
     * pass extra information to the Action instance selected by this Action.
     * Struts does not itself use this value in any way.
     */
    public String getParameter() {
        return (this.parameter);
    }

    /**
     * <p> General purpose configuration parameter that can be used to pass
     * extra information to the Action instance selected by this Action.
     * Struts does not itself use this value in any way.
     *
     * @param parameter General purpose configuration parameter.
     */
    public void setParameter(String parameter) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.parameter = parameter;
    }

    /**
     * <p> Return context-relative path of the submitted request, starting
     * with a slash ("/") character, and omitting any filename extension if
     * extension mapping is being used.
     */
    public String getPath() {
        return (this.path);
    }

    /**
     * <p> Set context-relative path of the submitted request, starting with a
     * slash ("/") character, and omitting any filename extension if extension
     * mapping is being used.
     *
     * @param path context-relative path of the submitted request.
     */
    public void setPath(String path) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.path = path;
    }

    /**
     * <p> Retruns prefix used to match request parameter names to form bean
     * property names, if any.
     */
    public String getPrefix() {
        return (this.prefix);
    }

    /**
     * @param prefix Prefix used to match request parameter names to form bean
     *               property names, if any.
     */
    public void setPrefix(String prefix) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.prefix = prefix;
    }

    public String getRoles() {
        return (this.roles);
    }

    public void setRoles(String roles) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.roles = roles;

        if (roles == null) {
            roleNames = new String[0];

            return;
        }

        ArrayList list = new ArrayList();

        while (true) {
            int comma = roles.indexOf(',');

            if (comma < 0) {
                break;
            }

            list.add(roles.substring(0, comma).trim());
            roles = roles.substring(comma + 1);
        }

        roles = roles.trim();

        if (roles.length() > 0) {
            list.add(roles);
        }

        roleNames = (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * <p> Get array of security role names used to authorize access to this
     * Action.
     */
    public String[] getRoleNames() {
        return (this.roleNames);
    }

    /**
     * <p> Get the scope ("request" or "session") within which our form bean
     * is accessed, if any.
     */
    public String getScope() {
        return (this.scope);
    }

    /**
     * @param scope scope ("request" or "session") within which our form bean
     *              is accessed, if any.
     */
    public void setScope(String scope) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.scope = scope;
    }

    /**
     * <p> Return suffix used to match request parameter names to form bean
     * property names, if any. </p>
     */
    public String getSuffix() {
        return (this.suffix);
    }

    /**
     * @param suffix Suffix used to match request parameter names to form bean
     *               property names, if any.
     */
    public void setSuffix(String suffix) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.suffix = suffix;
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

    /**
     * <p> Determine whether Action is configured as the default one for this
     * module. </p>
     */
    public boolean getUnknown() {
        return (this.unknown);
    }

    /**
     * @param unknown Indicates Action is configured as the default one for
     *                this module, when true.
     */
    public void setUnknown(boolean unknown) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.unknown = unknown;
    }

    public boolean getValidate() {
        return (this.validate);
    }

    public void setValidate(boolean validate) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.validate = validate;
        this.validateSet = true;
    }

    /**
     * <p> Get the name of a <code>commons-chain</code> command which should
     * be executed as part of the processing of this action. </p>
     *
     * @return name of a <code>commons-chain</code> command which should be
     *         executed as part of the processing of this action.
     * @since Struts 1.3.0
     */
    public String getCommand() {
        return (this.command);
    }

    /**
     * <p> Get the name of a <code>commons-chain</code> catalog in which a
     * specified command should be sought.  This is likely to be infrequently
     * used after a future release of <code>commons-chain</code> supports a
     * one-string expression of a catalog/chain combination. </p>
     *
     * @return name of a <code>commons-chain</code> catalog in which a
     *         specified command should be sought.
     * @since Struts 1.3.0
     */
    public String getCatalog() {
        return (this.catalog);
    }

    /**
     * <p> Set the name of a <code>commons-chain</code> command which should
     * be executed as part of the processing of this action. </p>
     *
     * @param command name of a <code>commons-chain</code> command which
     *                should be executed as part of the processing of this
     *                action.
     * @since Struts 1.3.0
     */
    public void setCommand(String command) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.command = command;
    }

    /**
     * <p> Set the name of a <code>commons-chain</code> catalog in which a
     * specified command should be sought. This is likely to be infrequently
     * used after a future release of <code>commons-chain</code> supports a
     * one-string expression of a catalog/chain combination. </p>
     *
     * @param catalog name of a <code>commons-chain</code> catalog in which a
     *                specified command should be sought.
     * @since Struts 1.3.0
     */
    public void setCatalog(String catalog) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        this.catalog = catalog;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * <p>Traces the hierarchy of this object to check if any of the ancestors
     * is extending this instance.</p>
     *
     * @param moduleConfig The configuration for the module being configured.
     * @return true if circular inheritance was detected.
     */
    protected boolean checkCircularInheritance(ModuleConfig moduleConfig) {
        String ancestorPath = getExtends();

        while (ancestorPath != null) {
            // check if we have the same path as an ancestor
            if (getPath().equals(ancestorPath)) {
                return true;
            }

            // get our ancestor's ancestor
            ActionConfig ancestor = moduleConfig.findActionConfig(ancestorPath);

            if (ancestor != null) {
                ancestorPath = ancestor.getExtends();
            } else {
                ancestorPath = null;
            }
        }

        return false;
    }

    /**
     * <p>Compare the exception handlers of this action with that of the given
     * and copy those that are not present.</p>
     *
     * @param baseConfig The action config to copy handlers from.
     * @see #inheritFrom(ActionConfig)
     */
    protected void inheritExceptionHandlers(ActionConfig baseConfig)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        // Inherit exception handler configs
        ExceptionConfig[] baseHandlers = baseConfig.findExceptionConfigs();

        for (int i = 0; i < baseHandlers.length; i++) {
            ExceptionConfig baseHandler = baseHandlers[i];

            // Do we have this handler?
            ExceptionConfig copy =
                this.findExceptionConfig(baseHandler.getType());

            if (copy == null) {
                // We don't have this, so let's copy it
                copy =
                    (ExceptionConfig) RequestUtils.applicationInstance(baseHandler.getClass()
                                                                                  .getName());

                BeanUtils.copyProperties(copy, baseHandler);
                this.addExceptionConfig(copy);
                copy.setProperties(baseHandler.copyProperties());
            } else {
                // process any extension that this config might have
                copy.processExtends(getModuleConfig(), this);
            }
        }
    }

    /**
     * <p>Compare the forwards of this action with that of the given and copy
     * those that are not present.</p>
     *
     * @param baseConfig The action config to copy forwards from.
     * @see #inheritFrom(ActionConfig)
     */
    protected void inheritForwards(ActionConfig baseConfig)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        // Inherit forward configs
        ForwardConfig[] baseForwards = baseConfig.findForwardConfigs();

        for (int i = 0; i < baseForwards.length; i++) {
            ForwardConfig baseForward = baseForwards[i];

            // Do we have this forward?
            ForwardConfig copy = this.findForwardConfig(baseForward.getName());

            if (copy == null) {
                // We don't have this, so let's copy it
                copy =
                    (ForwardConfig) RequestUtils.applicationInstance(baseForward.getClass()
                                                                                .getName());
                BeanUtils.copyProperties(copy, baseForward);

                this.addForwardConfig(copy);
                copy.setProperties(baseForward.copyProperties());
            } else {
                // process any extension for this forward
                copy.processExtends(getModuleConfig(), this);
            }
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * <p> Add a new <code>ExceptionConfig</code> instance to the set
     * associated with this action. </p>
     *
     * @param config The new configuration instance to be added
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    public void addExceptionConfig(ExceptionConfig config) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        exceptions.put(config.getType(), config);
    }

    /**
     * <p> Add a new <code>ForwardConfig</code> instance to the set of global
     * forwards associated with this action. </p>
     *
     * @param config The new configuration instance to be added
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    public void addForwardConfig(ForwardConfig config) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        forwards.put(config.getName(), config);
    }

    /**
     * <p> Return the exception configuration for the specified type, if any;
     * otherwise return <code>null</code>. </p>
     *
     * @param type Exception class name to find a configuration for
     */
    public ExceptionConfig findExceptionConfig(String type) {
        return ((ExceptionConfig) exceptions.get(type));
    }

    /**
     * <p> Return the exception configurations for this action.  If there are
     * none, a zero-length array is returned. </p>
     */
    public ExceptionConfig[] findExceptionConfigs() {
        ExceptionConfig[] results = new ExceptionConfig[exceptions.size()];

        return ((ExceptionConfig[]) exceptions.values().toArray(results));
    }

    /**
     * <p>Find and return the <code>ExceptionConfig</code> instance defining
     * how <code>Exceptions</code> of the specified type should be handled.
     * This is performed by checking local and then global configurations for
     * the specified exception's class, and then looking up the superclass
     * chain (again checking local and then global configurations). If no
     * handler configuration can be found, return <code>null</code>.</p>
     *
     * <p>Introduced in <code>ActionMapping</code> in Struts 1.1, but pushed
     * up to <code>ActionConfig</code> in Struts 1.2.0.</p>
     *
     * @param type Exception class for which to find a handler
     * @since Struts 1.2.0
     */
    public ExceptionConfig findException(Class type) {
        // Check through the entire superclass hierarchy as needed
        ExceptionConfig config;

        while (true) {
            // Check for a locally defined handler
            String name = type.getName();

            log.debug("findException: look locally for " + name);
            config = findExceptionConfig(name);

            if (config != null) {
                return (config);
            }

            // Check for a globally defined handler
            log.debug("findException: look globally for " + name);
            config = getModuleConfig().findExceptionConfig(name);

            if (config != null) {
                return (config);
            }

            // Loop again for our superclass (if any)
            type = type.getSuperclass();

            if (type == null) {
                break;
            }
        }

        return (null); // No handler has been configured
    }

    /**
     * <p> Return the forward configuration for the specified key, if any;
     * otherwise return <code>null</code>. </p>
     *
     * @param name Name of the forward configuration to return
     */
    public ForwardConfig findForwardConfig(String name) {
        return ((ForwardConfig) forwards.get(name));
    }

    /**
     * <p> Return all forward configurations for this module.  If there are
     * none, a zero-length array is returned. </p>
     */
    public ForwardConfig[] findForwardConfigs() {
        ForwardConfig[] results = new ForwardConfig[forwards.size()];

        return ((ForwardConfig[]) forwards.values().toArray(results));
    }

    /**
     * <p> Freeze the configuration of this action. </p>
     */
    public void freeze() {
        super.freeze();

        ExceptionConfig[] econfigs = findExceptionConfigs();

        for (int i = 0; i < econfigs.length; i++) {
            econfigs[i].freeze();
        }

        ForwardConfig[] fconfigs = findForwardConfigs();

        for (int i = 0; i < fconfigs.length; i++) {
            fconfigs[i].freeze();
        }
    }

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
     * @see #processExtends(ModuleConfig)
     */
    public void inheritFrom(ActionConfig config)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        // Inherit values that have not been overridden
        if (getAttribute() == null) {
            setAttribute(config.getAttribute());
        }

        if (!cancellableSet) {
            setCancellable(config.getCancellable());
        }

        if (getCatalog() == null) {
            setCatalog(config.getCatalog());
        }

        if (getCommand() == null) {
            setCommand(config.getCommand());
        }

        if (getForward() == null) {
            setForward(config.getForward());
        }

        if (getInclude() == null) {
            setInclude(config.getInclude());
        }

        if (getInput() == null) {
            setInput(config.getInput());
        }

        if (getMultipartClass() == null) {
            setMultipartClass(config.getMultipartClass());
        }

        if (getName() == null) {
            setName(config.getName());
        }

        if (getParameter() == null) {
            setParameter(config.getParameter());
        }

        if (getPath() == null) {
            setPath(config.getPath());
        }

        if (getPrefix() == null) {
            setPrefix(config.getPrefix());
        }

        if (getRoles() == null) {
            setRoles(config.getRoles());
        }

        if (getScope().equals("session")) {
            setScope(config.getScope());
        }

        if (getSuffix() == null) {
            setSuffix(config.getSuffix());
        }

        if (getType() == null) {
            setType(config.getType());
        }

        if (!getUnknown()) {
            setUnknown(config.getUnknown());
        }

        if (!validateSet) {
            setValidate(config.getValidate());
        }

        inheritExceptionHandlers(config);
        inheritForwards(config);
        inheritProperties(config);
    }

    /**
     * <p>Inherit configuration information from the ActionConfig that this
     * instance is extending.  This method verifies that any action config
     * object that it inherits from has also had its processExtends() method
     * called.</p>
     *
     * @param moduleConfig The {@link ModuleConfig} that this bean is from.
     * @see #inheritFrom(ActionConfig)
     */
    public void processExtends(ModuleConfig moduleConfig)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        String ancestorPath = getExtends();

        if ((!extensionProcessed) && (ancestorPath != null)) {
            ActionConfig baseConfig =
                moduleConfig.findActionConfig(ancestorPath);

            if (baseConfig == null) {
                throw new NullPointerException("Unable to find "
                    + "action for '" + ancestorPath + "' to extend.");
            }

            // Check against circular inheritance and make sure the base
            //  config's own extends has been processed already
            if (checkCircularInheritance(moduleConfig)) {
                throw new IllegalArgumentException(
                    "Circular inheritance detected for action " + getPath());
            }

            // Make sure the ancestor's own extension has been processed.
            if (!baseConfig.isExtensionProcessed()) {
                baseConfig.processExtends(moduleConfig);
            }

            // Copy values from the base config
            inheritFrom(baseConfig);
        }

        extensionProcessed = true;
    }

    /**
     * <p> Remove the specified exception configuration instance. </p>
     *
     * @param config ExceptionConfig instance to be removed
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    public void removeExceptionConfig(ExceptionConfig config) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        exceptions.remove(config.getType());
    }

    /**
     * <p> Remove the specified forward configuration instance. </p>
     *
     * @param config ForwardConfig instance to be removed
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    public void removeForwardConfig(ForwardConfig config) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        forwards.remove(config.getName());
    }

    /**
     * <p> Return a String representation of this object. </p>
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("ActionConfig[");

        sb.append("cancellable=");
        sb.append(cancellable);

        sb.append(",path=");
        sb.append(path);

        sb.append(",validate=");
        sb.append(validate);

        if (actionId != null) {
            sb.append(",actionId=");
            sb.append(actionId);
        }

        if (attribute != null) {
            sb.append(",attribute=");
            sb.append(attribute);
        }

        if (catalog != null) {
            sb.append(",catalog=");
            sb.append(catalog);
        }

        if (command != null) {
            sb.append(",command=");
            sb.append(command);
        }

        if (inherit != null) {
            sb.append(",extends=");
            sb.append(inherit);
        }

        if (forward != null) {
            sb.append(",forward=");
            sb.append(forward);
        }

        if (include != null) {
            sb.append(",include=");
            sb.append(include);
        }

        if (input != null) {
            sb.append(",input=");
            sb.append(input);
        }

        if (multipartClass != null) {
            sb.append(",multipartClass=");
            sb.append(multipartClass);
        }

        if (name != null) {
            sb.append(",name=");
            sb.append(name);
        }

        if (parameter != null) {
            sb.append(",parameter=");
            sb.append(parameter);
        }

        if (prefix != null) {
            sb.append(",prefix=");
            sb.append(prefix);
        }

        if (roles != null) {
            sb.append(",roles=");
            sb.append(roles);
        }

        if (scope != null) {
            sb.append(",scope=");
            sb.append(scope);
        }

        if (suffix != null) {
            sb.append(",suffix=");
            sb.append(suffix);
        }

        if (type != null) {
            sb.append(",type=");
            sb.append(type);
        }

        return (sb.toString());
    }
}
