/*
 * $Id: ActionContext.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.MessageResources;

import java.util.Locale;
import java.util.Map;

/**
 * <p>An ActionContext represents a view of a commons-chain
 * <code>Context</code> which encapsulates access to request and
 * session-scoped resources and services</p>
 */
public interface ActionContext extends Context {
    public static final String APPLICATION_SCOPE = "application";
    public static final String SESSION_SCOPE = "session";
    public static final String REQUEST_SCOPE = "request";

    // -------------------------------
    // General Application Support
    // -------------------------------

    /**
     * Signal to the instance that it will not be used any more, so that any
     * resources which should be cleaned up can be cleaned up.
     */
    void release();

    /**
     * <p>Return a <code>Map</code> of Application scoped values.</p>
     *
     * <p>This is implemented in analogy with the Application scope in the
     * Servlet API, but it seems reasonable to expect that any Struts
     * implementation will have an equivalent concept.</p>
     *
     * <p>The ultimate meaning of "application scope" is an implementation
     * detail left unspecified by Struts.</p>
     *
     * @return A Map of "application scope" attributes.
     */
    Map getApplicationScope();

    /**
     * <p>Return a <code>Map</code> of Session scoped values.  A session is
     * understood as a sequence of requests made by the same user.</p>
     *
     * <p>This is implemented in analogy with the Session scope in the Servlet
     * API, but it seems reasonable to expect that any Struts implementation
     * will have an equivalent concept.</p>
     *
     * <p>The ultimate meaning of "session scope" is an implementation detail
     * left unspecified by Struts.</p>
     *
     * @return A Map of "session scope" attributes.
     */
    Map getSessionScope();

    /**
     * <p>Return a <code>Map</code> of request scoped values.  A request is
     * understood as the fundamental motivation for any particular instance of
     * an <code>ActionContext</code>.</p>
     *
     * <p>This is implemented in analogy with the Request Context in the
     * Servlet API, but it seems reasonable to expect that any Struts
     * implementation will have an equivalent concept.</p>
     *
     * <p>The ultimate meaning of "request scope" is an implementation detail
     * left unspecified by Struts.</p>
     *
     * @return a Map of "request scope" attributes.
     */
    Map getRequestScope();

    /**
     * Return the Map representing the scope identified by
     * <code>scopeName</code>. Implementations should support at minimum the
     * names associated with the constants <code>APPLICATION_SCOPE</code>,
     * <code>SESSION_SCOPE</code>, and <code>REQUEST_SCOPE</code>, but are
     * permitted to support others as well.
     *
     * @param scopeName A token identifying a scope, including but not limited
     *                  to <code>APPLICATION_SCOPE</code>, <code>SESSION_SCOPE</code>,
     *                  <code>REQUEST_SCOPE</code>.
     * @return A Map of attributes for the specified scope.
     */
    Map getScope(String scopeName);

    /**
     * <p>Return a <code>Map</code> of parameters submitted by the user as
     * part of this request.  The keys to this map will be request parameter
     * names (of type <code>String</code>), and the values will be
     * <code>String[]</code>.</p>
     *
     * <p>This is implemented in analogy with the Request parameters of the
     * Servlet API, but it seems reasonable to expect that any Struts
     * implementation will have an equivalent concept.</p>
     *
     * @return A map of the request parameter attributes
     */
    Map getParameterMap();

    // -------------------------------
    // General Struts properties
    // -------------------------------

    /**
     * <p> Set the action which has been identified to be executed as part of
     * processing this request. </p>
     *
     * @param action
     */
    void setAction(Action action);

    /**
     * <p> Get the action which has been identified to be executed as part of
     * processing this request. </p>
     *
     * @return The action to be executed with this request
     */
    Action getAction();

    /**
     * <p> Set the ActionForm instance which will carry any data submitted as
     * part of this request. </p>
     *
     * @param form The ActionForm instance to use with this request
     */
    void setActionForm(ActionForm form);

    /**
     * <p> Get the ActionForm instance which will carry any data submitted as
     * part of this request. </p>
     *
     * @return The ActionForm being used with this request
     */
    ActionForm getActionForm();

    /**
     * <p> Set the ActionConfig class contains the details for processing this
     * request. </p>
     *
     * @param config The ActionConfig class to use with this request
     */
    void setActionConfig(ActionConfig config);

    /**
     * <p> Get the ActionConfig which contains the details for processing this
     * request.
     *
     * @return The ActionConfig class being used with this request </p>
     */
    ActionConfig getActionConfig();

    /**
     * <p> Set the ForwardConfig which should be used as the basis of the view
     * segment of the overall processing. This is the primary method of
     * "communication" with the "view" sub-chain. </p>
     *
     * @param forward The ForwardConfig to use with this request
     */
    void setForwardConfig(ForwardConfig forward);

    /**
     * <p> Get the ForwardConfig which has been identified as the basis for
     * view-processing. </p>
     *
     * @return The ForwardConfig being used with this request
     */
    ForwardConfig getForwardConfig();

    /**
     * <p> Set the include path which should be processed as part of
     * processing this request. </p>
     *
     * @param include The include path to be used with this request
     */
    void setInclude(String include);

    /**
     * <p> Get the include path which should be processed as part of
     * processing this request. </p>
     *
     * @return The include path being used with this request
     */
    String getInclude();

    /**
     * <p> Set the ModuleConfig which is operative for the current request.
     * </p>
     *
     * @param config The ModuleConfig to be used with this request
     */
    void setModuleConfig(ModuleConfig config);

    /**
     * <p> Get the ModuleConfig which is operative for the current request.
     * </p>
     *
     * @return The MooduleConfig being used with this request
     */
    ModuleConfig getModuleConfig();

    /**
     * <p> Is the ActionForm for this context valid? This method <em>does
     * not</em> actually perform form validation. It is simply a holder
     * property where processes which perform validation can store the results
     * of the validation for other processes' benefit. </p>
     *
     * @return <code>Boolean.TRUE</code> if the form passed validation;
     *         <code>Boolean.FALSE</code> if the form failed validation; null
     *         if the form has not yet been validated
     */
    Boolean getFormValid();

    /**
     * <p> Store the result of the validation of the Context's ActionForm.
     * </p>
     *
     * @param valid Whether the ActionForm for this request passes validation
     */
    void setFormValid(Boolean valid);

    /**
     * <p> Retrieve an exception which may have been caught by some code using
     * this ActionContext, usually by an exception handler. </p>
     *
     * @return Any exception that may have been caught by this ActionContext
     */
    Exception getException();

    /**
     * <p> Store an exception in this context for use by other handling code.
     * </p>
     *
     * @param e An exception to be stored for handling by another member
     */
    void setException(Exception e);

    // -------------------------------
    // ActionMessage Processing
    // -------------------------------

    /**
     * <p> Append the given messages keys to an internal cache, creating the
     * cache if one is not already present. </p>
     *
     * @param messages New ActionMessages to cache
     */
    void addMessages(ActionMessages messages);

    /**
     * <p> Append the given errors keys to an internal cache, creating the
     * cache if one is not already present. </p>
     *
     * @param errors New ActionMessages to cache as errors
     */
    void addErrors(ActionMessages errors);

    /**
     * <p> Retrieve error messages from an internal cache, creating an empty
     * cache if one is not already present. </p>
     *
     * @return The ActionMessage cache for errors
     */
    ActionMessages getErrors();

    /**
     * <p> Retrieve messages from an internal cache, creating an empty cache
     * if one is not already present. </p>
     *
     * @return The ActionMessage cache for errors
     */
    ActionMessages getMessages();

    /**
     * <p> Save the given error messages to the internal cache, clearing any
     * previous messages in the cache. </p> <p> If the parameter is null or
     * empty, the internal cache is removed. </p>
     *
     * @param errors ActionMesssages to cache as errors
     */
    void saveErrors(ActionMessages errors);

    /**
     * <p> Save the given messages to the internal cache, clearing any
     * previous messages in the cache. </p> <p> If the parameter is null or
     * empty, the internal cache is removed. </p>
     *
     * @param messages ActionMesssages to cache
     */
    void saveMessages(ActionMessages messages);

    /**
     * <p> Save the given messages to the internal cache, clearing any
     * previous messages in the cache, but only for the specified scope. </p>
     * <p> If the parameter is null or empty, the internal cache is removed.
     * </p>
     *
     * @param scope    The scope for the internal cache
     * @param messages ActionMesssages to cache
     */
    void saveMessages(String scope, ActionMessages messages);

    // -------------------------------
    // Token Processing
    // -------------------------------

    /**
     * <p>Generate a new transaction token, to be used for enforcing a single
     * request for a particular transaction.</p>
     */
    String generateToken();

    /**
     * <p> Indicate whether a transaction token for this context is valid.
     * </p> <p> A typical implementation will place a transaction token in the
     * session" scope Map and a matching value in the  "parameter" Map. If the
     * "session" token does not match the "parameter" attribute, or the
     * session token is missing, then the transactional token is deemed
     * invalid. </p>
     */
    boolean isTokenValid();

    /**
     * <p> Indicate whether a transaction token is stored in the "session"
     * scope for this context, optionally clearing the token, so that the next
     * check would return false. </p>
     *
     * @param reset On true, clear the transactional token
     */
    boolean isTokenValid(boolean reset);

    /**
     * <p> Clear any transactional token stored in the "session" scope for
     * this context, so that the next check would return false. </p>
     */
    void resetToken();

    /**
     * <p> Save a new transaction token in the "session" scope for this
     * context, creating new resources, if needed. </p>
     */
    void saveToken();

    // -------------------------------
    // Cancel Processing
    // -------------------------------

    /**
     * <p> Indicate if the "cancel event" state is set for for this context,
     * </p>
     *
     * @see ActionContextBase.CANCEL_KEY
     */
    Boolean getCancelled();

    /**
     * <p> Set the "cancel event" state for this context. </p> <p>
     *
     * @param cancelled On true, set the cancel event state to true. On false,
     *                  set the cancel event state to false.
     * @see ActionContextBase.CANCEL_KEY
     */
    void setCancelled(Boolean cancelled);

    // -------------------------------
    // MessageResources Processing
    // -------------------------------

    /**
     * <p>Return the default message resources for the current module.</p>
     */
    MessageResources getMessageResources();

    /**
     * <p>Set the default message resources for the current module.</p>
     */
    void setMessageResources(MessageResources resources);

    /**
     * <p>Return the specified message resources for the current module.</p>
     *
     * @param key The key specified in the <code>&lt;message-resources&gt;</code>
     *            element for the requested bundle
     */
    MessageResources getMessageResources(String key);

    // -------------------------------
    // Locale Processing
    // -------------------------------

    /**
     * <p>Return the user's currently selected Locale.</p>
     */
    Locale getLocale();

    /**
     * <p>Set the user's currently selected <code>Locale</code>.</p>
     *
     * @param locale The user's selected Locale to be set, or null to select
     *               the server's default Locale
     */
    void setLocale(Locale locale);
}
