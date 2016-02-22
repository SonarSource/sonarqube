/*
 * $Id: ActionContextBase.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.contexts;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.chain.Constants;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.TokenProcessor;

import java.util.Locale;
import java.util.Map;

/**
 * <p> Provide an abstract but semi-complete implementation of ActionContext
 * to serve as the base for concrete implementations. </p> <p> The abstract
 * methods to implement are the accessors for the named states,
 * <code>getApplicationScope</code>, <code>getRequestScope</code>, and
 * <code>getSessionScope</code>. </p>
 */
public abstract class ActionContextBase extends ContextWrapper
    implements ActionContext {
    /**
     * @see Constants.ACTION_KEY
     */
    public static final String ACTION_KEY = Constants.ACTION_KEY;

    /**
     * @see
     */
    public static final String ACTION_CONFIG_KEY = Constants.ACTION_CONFIG_KEY;

    /**
     * @see Constants.ACTION_FORM_KEY
     */
    public static final String ACTION_FORM_KEY = Constants.ACTION_FORM_KEY;

    /**
     * @see Constants.FORWARD_CONFIG_KEY
     */
    public static final String FORWARD_CONFIG_KEY =
        Constants.FORWARD_CONFIG_KEY;

    /**
     * @see Constants.MODULE_CONFIG_KEY
     */
    public static final String MODULE_CONFIG_KEY = Constants.MODULE_CONFIG_KEY;

    /**
     * @see Constants.EXCEPTION_KEY
     */
    public static final String EXCEPTION_KEY = Constants.EXCEPTION_KEY;

    /**
     * <p> Provide the default context attribute under which to store the
     * ActionMessage cache for errors. </p>
     */
    public static final String ERROR_ACTION_MESSAGES_KEY = "errors";

    /**
     * <p> Provide the default context attribute under which to store the
     * ActionMessage cache. </p>
     */
    public static final String MESSAGE_ACTION_MESSAGES_KEY = "messages";

    /**
     * @see Constants.MESSAGE_RESOURCES_KEY
     */
    public static final String MESSAGE_RESOURCES_KEY =
        Constants.MESSAGE_RESOURCES_KEY;

    /**
     * @see Constants.INCLUDE_KEY
     */
    public static final String INCLUDE_KEY = Constants.INCLUDE_KEY;

    /**
     * @see Constants.LOCALE_KEY
     */
    public static final String LOCALE_KEY = Constants.LOCALE_KEY;

    /**
     * @see Constants.CANCEL_KEY
     */
    public static final String CANCEL_KEY = Constants.CANCEL_KEY;

    /**
     * @see Constants.VALID_KEY
     */
    public static final String VALID_KEY = Constants.VALID_KEY;

    /**
     * Provide the default context attribute under which to store the
     * transaction token key.
     */
    public static final String TRANSACTION_TOKEN_KEY = "TRANSACTION_TOKEN_KEY";

    /**
     * Provide the default context attribute under which to store the token
     * key.
     */
    public static final String TOKEN_KEY = "TOKEN_KEY";

    /**
     * Store the TokenProcessor instance for this Context.
     */
    protected TokenProcessor token = null;

    /**
     * Store the Log instance for this Context.
     */
    private Log logger = null;

    /**
     * Instantiate ActionContextBase, wrapping the given Context.
     *
     * @param context Context to wrap
     */
    public ActionContextBase(Context context) {
        super(context);
        token = TokenProcessor.getInstance();
        logger = LogFactory.getLog(this.getClass());
    }

    /**
     * Instantiate ActionContextBase, wrapping a default ContextBase
     * instance.
     */
    public ActionContextBase() {
        this(new ContextBase());
    }

    // -------------------------------
    // General Application Support
    // -------------------------------
    public void release() {
        this.token = null;
    }

    public abstract Map getApplicationScope();

    public abstract Map getRequestScope();

    public abstract Map getSessionScope();

    public Map getScope(String scopeName) {
        if (REQUEST_SCOPE.equals(scopeName)) {
            return this.getRequestScope();
        }

        if (SESSION_SCOPE.equals(scopeName)) {
            return this.getSessionScope();
        }

        if (APPLICATION_SCOPE.equals(scopeName)) {
            return this.getApplicationScope();
        }

        throw new IllegalArgumentException("Invalid scope: " + scopeName);
    }

    // -------------------------------
    // General Struts properties
    // -------------------------------
    public void setAction(Action action) {
        this.put(ACTION_KEY, action);
    }

    public Action getAction() {
        return (Action) this.get(ACTION_KEY);
    }

    public void setActionForm(ActionForm form) {
        this.put(ACTION_FORM_KEY, form);
    }

    public ActionForm getActionForm() {
        return (ActionForm) this.get(ACTION_FORM_KEY);
    }

    public void setActionConfig(ActionConfig config) {
        this.put(ACTION_CONFIG_KEY, config);
    }

    public ActionConfig getActionConfig() {
        return (ActionConfig) this.get(ACTION_CONFIG_KEY);
    }

    public void setForwardConfig(ForwardConfig forward) {
        this.put(FORWARD_CONFIG_KEY, forward);
    }

    public ForwardConfig getForwardConfig() {
        return (ForwardConfig) this.get(FORWARD_CONFIG_KEY);
    }

    public void setInclude(String include) {
        this.put(INCLUDE_KEY, include);
    }

    public String getInclude() {
        return (String) this.get(INCLUDE_KEY);
    }

    public Boolean getFormValid() {
        return (Boolean) this.get(VALID_KEY);
    }

    public void setFormValid(Boolean valid) {
        this.put(VALID_KEY, valid);
    }

    public ModuleConfig getModuleConfig() {
        return (ModuleConfig) this.get(MODULE_CONFIG_KEY);
    }

    public void setModuleConfig(ModuleConfig config) {
        this.put(MODULE_CONFIG_KEY, config);
    }

    public Exception getException() {
        return (Exception) this.get(EXCEPTION_KEY);
    }

    public void setException(Exception e) {
        this.put(EXCEPTION_KEY, e);
    }

    // -------------------------------
    // ActionMessage Processing
    // -------------------------------
    public void addMessages(ActionMessages messages) {
        this.addActionMessages(MESSAGE_ACTION_MESSAGES_KEY, messages);
    }

    public void addErrors(ActionMessages errors) {
        this.addActionMessages(ERROR_ACTION_MESSAGES_KEY, errors);
    }

    public ActionMessages getErrors() {
        return (ActionMessages) this.get(ERROR_ACTION_MESSAGES_KEY);
    }

    public ActionMessages getMessages() {
        return (ActionMessages) this.get(MESSAGE_ACTION_MESSAGES_KEY);
    }

    public void saveErrors(ActionMessages errors) {
        this.saveActionMessages(ERROR_ACTION_MESSAGES_KEY, errors);
    }

    public void saveMessages(ActionMessages messages) {
        this.saveActionMessages(MESSAGE_ACTION_MESSAGES_KEY, messages);
    }

    // ISSUE: do we want to add this to the public API?

    /**
     * <p> Add the given messages to a cache stored in this Context, under
     * key. </p>
     *
     * @param key      The attribute name for the message cache
     * @param messages The ActionMessages to add
     */
    public void addActionMessages(String key, ActionMessages messages) {
        if (messages == null) {
            // bad programmer! *slap*
            return;
        }

        // get any existing messages from the request, or make a new one
        ActionMessages requestMessages = (ActionMessages) this.get(key);

        if (requestMessages == null) {
            requestMessages = new ActionMessages();
        }

        // add incoming messages
        requestMessages.add(messages);

        // if still empty, just wipe it out from the request
        this.remove(key);

        // save the messages
        this.saveActionMessages(key, requestMessages);
    }

    // ISSUE: do we want to add this to the public API?

    /**
     * <p> Save the given ActionMessages into the request scope under the
     * given key, clearing the attribute if the messages are empty or null.
     * </p>
     *
     * @param key      The attribute name for the message cache
     * @param messages The ActionMessages to add
     */
    public void saveActionMessages(String key, ActionMessages messages) {
        this.saveActionMessages(REQUEST_SCOPE, key, messages);
    }

    /**
     * <p>Save the given <code>messages</code> into the map identified by the
     * given <code>scopeId</code> under the given <code>key</code>.</p>
     *
     * @param scopeId
     * @param key
     * @param messages
     */
    public void saveActionMessages(String scopeId, String key,
        ActionMessages messages) {
        Map scope = getScope(scopeId);

        if ((messages == null) || messages.isEmpty()) {
            scope.remove(key);

            return;
        }

        scope.put(key, messages);
    }

    // ISSUE: Should we deprecate this method, since it is misleading?
    // Do we need it for backward compatibility?

    /**
     * <p> Adapt a legacy form of SaveMessages to the ActionContext API by
     * storing the ActoinMessages under the default scope.
     *
     * @param scope    The scope for the internal cache
     * @param messages ActionMesssages to cache
     */
    public void saveMessages(String scope, ActionMessages messages) {
        this.saveMessages(messages);
    }

    // -------------------------------
    // Token Processing
    // -------------------------------
    // ISSUE: Should there be a getToken method?
    // Is there a problem trying to map this method from Action
    // to ActionContext when we aren't necessarily sure how token
    // processing maps into a context with an ill-defined "session"?
    // There's no getToken() method, but maybe there should be. *
    public void saveToken() {
        String token = this.generateToken();

        this.put(TRANSACTION_TOKEN_KEY, token);
    }

    public String generateToken() {
        return token.generateToken(getTokenGeneratorId());
    }

    // ISSUE: The original implementation was based on the HttpSession
    // identifier; what would be a way to do that without depending on the
    // Servlet API?
    // REPLY: uuid's
    // http://java.sun.com/products/jini/2.0/doc/specs/api/net/jini/id/Uuid.html
    protected String getTokenGeneratorId() {
        return "";
    }

    public boolean isTokenValid() {
        return this.isTokenValid(false);
    }

    public boolean isTokenValid(boolean reset) {
        // Retrieve the transaction token from this session, and
        // reset it if requested
        String saved = (String) this.get(TRANSACTION_TOKEN_KEY);

        if (saved == null) {
            return false;
        }

        if (reset) {
            this.resetToken();
        }

        // Retrieve the transaction token included in this request
        String token = (String) this.get(TOKEN_KEY);

        if (token == null) {
            return false;
        }

        return saved.equals(token);
    }

    public void resetToken() {
        this.remove(TRANSACTION_TOKEN_KEY);
    }

    // -------------------------------
    // Cancel Processing
    // -------------------------------
    public Boolean getCancelled() {
        return (Boolean) this.get(CANCEL_KEY);
    }

    public void setCancelled(Boolean cancelled) {
        this.put(CANCEL_KEY, cancelled);
    }

    // -------------------------------
    // MessageResources Processing
    // -------------------------------
    public void setMessageResources(MessageResources messageResources) {
        this.put(MESSAGE_RESOURCES_KEY, messageResources);
    }

    public MessageResources getMessageResources() {
        return (MessageResources) this.get(MESSAGE_RESOURCES_KEY);
    }

    public MessageResources getMessageResources(String key) {
        return (MessageResources) this.get(key);
    }

    // -------------------------------
    // Locale Processing
    // -------------------------------
    public void setLocale(Locale locale) {
        this.put(LOCALE_KEY, locale);
    }

    public Locale getLocale() {
        return (Locale) this.get(LOCALE_KEY);
    }

    // -------------------------------
    // Convenience Methods: these are not part of the formal ActionContext API,
    // but are likely to be commonly useful.
    // -------------------------------

    /**
     * <p> Provide the currently configured commons-logging <code>Log</code>
     * instance. </p>
     *
     * @return Log instance for this context
     */
    public Log getLogger() {
        return this.logger;
    }

    /**
     * <p> Set the commons-logging <code>Log</code> instance which should be
     * used to LOG messages. This is initialized at instantiation time but may
     * be overridden. Be advised not to set the value to null, as
     * <code>ActionContextBase</code> uses the logger for some of its own
     * operations. </p>
     */
    public void setLogger(Log logger) {
        this.logger = logger;
    }

    /**
     * <p> Using this <code>ActionContext</code>'s default
     * <code>ModuleConfig</code>, return an existing <code>ActionForm</code>
     * in the specified scope, or create a new one and add it to the specified
     * scope. </p>
     *
     * @param formName  The name attribute of our ActionForm
     * @param scopeName The scope identier (request, session)
     * @return The ActionForm for this request
     * @throws IllegalAccessException If object cannot be created
     * @throws InstantiationException If object cannot be created
     * @see this.findOrCreateActionForm(String, String, ModuleConfig)
     */
    public ActionForm findOrCreateActionForm(String formName, String scopeName)
        throws IllegalAccessException, InstantiationException {
        return this.findOrCreateActionForm(formName, scopeName,
            this.getModuleConfig());
    }

    /**
     * <p> In the context of the given <code>ModuleConfig</code> and this
     * <code>ActionContext</code>, look for an existing
     * <code>ActionForm</code> in the specified scope. If one is found, return
     * it; otherwise, create a new instance, add it to that scope, and then
     * return it. </p>
     *
     * @param formName  The name attribute of our ActionForm
     * @param scopeName The scope identier (request, session)
     * @return The ActionForm for this request
     * @throws IllegalAccessException   If object cannot be created
     * @throws InstantiationException   If object cannot be created
     * @throws IllegalArgumentException If form config is missing from module
     *                                  or scopeName is invalid
     */
    public ActionForm findOrCreateActionForm(String formName, String scopeName,
        ModuleConfig moduleConfig)
        throws IllegalAccessException, InstantiationException {
        Map scope = this.getScope(scopeName);

        ActionForm instance;
        FormBeanConfig formBeanConfig =
            moduleConfig.findFormBeanConfig(formName);

        if (formBeanConfig == null) {
            throw new IllegalArgumentException("No form config found under "
                + formName + " in module " + moduleConfig.getPrefix());
        }

        instance = (ActionForm) scope.get(formName);

        // ISSUE: Can we recycle the existing instance (if any)?
        if (instance != null) {
            getLogger().trace("Found an instance in scope " + scopeName
                + "; test for reusability");

            if (formBeanConfig.canReuse(instance)) {
                return instance;
            }
        }

        ActionForm form = formBeanConfig.createActionForm(this);

        // ISSUE: Should we check this call to put?
        scope.put(formName, form);

        return form;
    }
}
